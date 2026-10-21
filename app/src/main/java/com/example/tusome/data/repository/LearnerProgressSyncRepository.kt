package com.example.tusome.data.repository

import android.os.Handler
import android.os.Looper
import com.example.tusome.data.local.ActivityProgressManager
import com.example.tusome.data.local.SubStrandProgress
import com.example.tusome.data.model.PerformanceLevel
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Best-effort offline-first synchronization. Local changes are saved first; cloud failures never
 * block learning. Each record is scoped to the authenticated Supabase user by RLS.
 */
object LearnerProgressSyncRepository {
    private const val BASE_URL = "https://wkwnoflielrotknvoflz.supabase.co"
    private const val PUBLISHABLE_KEY = "sb_publishable_jZsmwIcIMdu3Eq97AfU3xQ_929Bxo4J"
    private const val TABLE_PATH =
        "/rest/v1/learner_activity_progress?select=sub_strand_id,is_completed,learner_note,evaluation_level,client_updated_at"

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private val syncing = AtomicBoolean(false)
    @Volatile private var latestManager: ActivityProgressManager? = null

    private val syncRunnable: Runnable by lazy { Runnable {
        val manager = latestManager ?: return@Runnable
        if (manager.signedInUserId.isNullOrBlank()) return@Runnable
        if (!syncing.compareAndSet(false, true)) {
            handler.postDelayed(syncRunnable, 1500L)
            return@Runnable
        }

        SupabaseAuthRepository.refreshSessionIfNeeded(manager.appContext) { refreshed ->
            val session = refreshed.getOrNull()
            if (session == null || session.userId != manager.signedInUserId) {
                syncing.set(false)
                return@refreshSessionIfNeeded
            }

            executor.execute {
                try {
                    sync(manager, session)
                } catch (_: Exception) {
                    // Keep the local copy and retry on a later save or app/activity launch.
                } finally {
                    syncing.set(false)
                }
            }
        }
    } }

    fun scheduleSync(manager: ActivityProgressManager) {
        if (manager.signedInUserId.isNullOrBlank()) return
        latestManager = manager
        handler.removeCallbacks(syncRunnable)
        handler.postDelayed(syncRunnable, 1200L)
    }

    private fun sync(manager: ActivityProgressManager, session: AuthSession) {
        val userId = session.userId
        val remoteRows = request(TABLE_PATH, null, session.accessToken) as? JSONArray ?: JSONArray()
        val remoteById = LinkedHashMap<String, SubStrandProgress>()

        for (index in 0 until remoteRows.length()) {
            val row = remoteRows.optJSONObject(index) ?: continue
            val id = row.optString("sub_strand_id").takeIf { it.isNotBlank() } ?: continue
            val progress = SubStrandProgress(
                subStrandId = id,
                isCompleted = row.optBoolean("is_completed", false),
                learnerNote = row.optString("learner_note"),
                selfEvaluationLevel = PerformanceLevel.entries.firstOrNull {
                    it.code == row.optString("evaluation_level")
                } ?: PerformanceLevel.MEETS,
                completedTimestamp = row.optLong("client_updated_at", 0L)
            )
            remoteById[id] = progress
            manager.mergeCloudProgress(progress)
        }

        val localRowsToUpload = manager.getAllProgress().filter { local ->
            val remote = remoteById[local.subStrandId]
            remote == null || local.completedTimestamp > remote.completedTimestamp
        }
        if (localRowsToUpload.isEmpty()) return

        val payload = JSONArray()
        localRowsToUpload.forEach { payload.put(manager.toCloudJson(it, userId)) }
        request(
            "/rest/v1/learner_activity_progress?on_conflict=user_id,sub_strand_id",
            payload,
            session.accessToken,
            upsert = true
        )
    }

    private fun request(
        path: String,
        payload: JSONArray?,
        accessToken: String,
        upsert: Boolean = false
    ): Any {
        val connection = (URL(BASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = if (payload == null) "GET" else "POST"
            connectTimeout = 10000
            readTimeout = 12000
            setRequestProperty("apikey", PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
            if (payload != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                if (upsert) setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")
            }
        }

        try {
            if (payload != null) {
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.let {
                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader -> reader.readText() }
            }.orEmpty()

            if (status !in 200..299) {
                val error = runCatching { JSONObject(body) }.getOrNull()
                val message = error?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: error?.optString("details")?.takeIf { it.isNotBlank() }
                    ?: "Progress sync failed (HTTP $status)."
                throw IllegalStateException(message)
            }

            return if (body.trimStart().startsWith("[")) JSONArray(body)
            else JSONObject(body.ifBlank { "{}" })
        } finally {
            connection.disconnect()
        }
    }
}
