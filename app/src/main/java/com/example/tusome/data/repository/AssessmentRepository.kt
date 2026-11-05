package com.example.tusome.data.repository

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.tusome.data.local.AssessmentLocalStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.util.concurrent.Executors

/**
 * Supabase is the published assessment source; SQLite is the learner's durable offline copy.
 * This repository never writes generated or unreviewed content into the published catalogue.
 */
object AssessmentRepository {
    private const val BASE_URL = "https://wkwnoflielrotknvoflz.supabase.co"
    private const val PUBLISHABLE_KEY = "sb_publishable_jZsmwIcIMdu3Eq97AfU3xQ_929Bxo4J"
    private const val PAPERS_PATH = "/rest/v1/assessment_papers"
    private const val ATTEMPTS_PATH = "/rest/v1/learner_assessment_attempts"

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun fetchPublishedPapers(
        context: Context,
        gradeId: String,
        learningAreaId: String,
        onResult: (Result<List<JSONObject>>) -> Unit
    ) {
        val appContext = context.applicationContext
        val localStore = AssessmentLocalStore(appContext)
        SupabaseAuthRepository.refreshSessionIfNeeded(appContext) { sessionResult ->
            val session = sessionResult.getOrNull()
            if (session == null) {
                val cached = readCached(localStore, gradeId, learningAreaId)
                mainHandler.post {
                    if (cached.isNotEmpty()) onResult(Result.success(cached))
                    else onResult(Result.failure(sessionResult.exceptionOrNull()
                        ?: IllegalStateException("Sign in while online once to download assessments for offline use.")))
                }
                return@refreshSessionIfNeeded
            }

            executor.execute {
                val result = runCatching {
                    val query = buildString {
                        append("?select=id,paper_code,grade_id,learning_area_id,strand_id,sub_strand_id,")
                        append("assessment_type,title,description,instructions,duration_minutes,total_marks,")
                        append("question_payload,marking_payload,sections_payload,performance_level_grid,learner_detail_fields,")
                        append("paper_metadata,competency_tags,coverage_map,source_reference,term_number,trial_number,")
                        append("paper_number,pathway,academic_year,version,content_status,published_at")
                        append("&grade_id=eq.").append(encode(gradeId))
                        append("&learning_area_id=eq.").append(encode(learningAreaId))
                        append("&content_status=eq.published&order=updated_at.desc")
                    }
                    val rows = request("$PAPERS_PATH$query", null, session.accessToken) as JSONArray
                    val papers = buildList {
                        for (index in 0 until rows.length()) {
                            val row = rows.optJSONObject(index) ?: continue
                            val id = row.optString("id")
                            val version = row.optInt("version", 1)
                            if (id.isBlank() || version <= 0) continue
                            localStore.cachePaper(
                                paperId = id,
                                paperVersion = version,
                                gradeId = gradeId,
                                learningAreaId = learningAreaId,
                                paperJson = row.toString()
                            )
                            add(row)
                        }
                    }
                    papers
                }.recoverCatching { networkError ->
                    val cached = readCached(localStore, gradeId, learningAreaId)
                    if (cached.isNotEmpty()) cached else throw networkError
                }
                mainHandler.post { onResult(result) }
            }
        }
    }

    fun getCachedPaper(context: Context, paperId: String, version: Int? = null): JSONObject? {
        val json = AssessmentLocalStore(context).getCachedPaper(paperId, version) ?: return null
        return runCatching { JSONObject(json) }.getOrNull()
    }

    fun saveAttemptLocally(
        context: Context,
        attemptId: String,
        userId: String,
        paperId: String,
        paperVersion: Int,
        answers: JSONObject,
        startedAtMillis: Long,
        submittedAtMillis: Long?
    ) {
        AssessmentLocalStore(context).saveAttempt(
            attemptId = attemptId,
            userId = userId,
            paperId = paperId,
            paperVersion = paperVersion,
            answersJson = answers.toString(),
            startedAt = startedAtMillis,
            submittedAt = submittedAtMillis,
            syncState = "pending"
        )
    }

    /** Uploads local attempts for the currently signed-in learner; failed uploads remain queued. */
    fun syncPendingAttempts(context: Context, onComplete: ((Result<Int>) -> Unit)? = null) {
        val appContext = context.applicationContext
        SupabaseAuthRepository.refreshSessionIfNeeded(appContext) { sessionResult ->
            val session = sessionResult.getOrNull()
            if (session == null) {
                onComplete?.let { callback ->
                    mainHandler.post {
                        callback(Result.failure(sessionResult.exceptionOrNull()
                            ?: IllegalStateException("No authenticated learner session is available.")))
                    }
                }
                return@refreshSessionIfNeeded
            }

            executor.execute {
                val store = AssessmentLocalStore(appContext)
                val result = runCatching {
                    val pending = store.getPendingAttempts(session.userId)
                    var synced = 0
                    pending.forEach { attempt ->
                        val payload = JSONArray().put(
                            JSONObject()
                                .put("id", attempt.getString("attempt_id"))
                                .put("user_id", session.userId)
                                .put("paper_id", attempt.getString("paper_id"))
                                .put("paper_version", attempt.getInt("paper_version"))
                                .put("answers", attempt.getJSONObject("answers"))
                                .put("started_at", Instant.ofEpochMilli(attempt.getLong("started_at")).toString())
                                .put("submitted_at", attempt.opt("submitted_at").let { value ->
                                    if (value == null || value == JSONObject.NULL) JSONObject.NULL
                                    else Instant.ofEpochMilli((value as Number).toLong()).toString()
                                })
                                .put("client_updated_at", attempt.getLong("client_updated_at"))
                        )
                        request("$ATTEMPTS_PATH?on_conflict=id", payload, session.accessToken, upsert = true)
                        store.markAttemptSynced(session.userId, attempt.getString("attempt_id"))
                        synced++
                    }
                    synced
                }
                onComplete?.let { callback -> mainHandler.post { callback(result) } }
            }
        }
    }

    private fun readCached(
        store: AssessmentLocalStore,
        gradeId: String,
        learningAreaId: String
    ): List<JSONObject> = store.getCachedPapers(gradeId, learningAreaId).mapNotNull {
        runCatching { JSONObject(it) }.getOrNull()
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun request(
        path: String,
        payload: JSONArray?,
        accessToken: String,
        upsert: Boolean = false
    ): Any {
        val connection = (URL(BASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = if (payload == null) "GET" else "POST"
            connectTimeout = 12000
            readTimeout = 15000
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
                    ?: "Assessment request failed (HTTP $status)."
                throw IllegalStateException(message)
            }
            return if (body.trimStart().startsWith("[")) JSONArray(body) else JSONObject(body.ifBlank { "{}" })
        } finally {
            connection.disconnect()
        }
    }
}
