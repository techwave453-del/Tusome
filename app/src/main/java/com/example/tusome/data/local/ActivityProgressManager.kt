package com.example.tusome.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.tusome.data.model.PerformanceLevel
import com.example.tusome.data.repository.LearnerProgressSyncRepository
import com.example.tusome.data.repository.SupabaseAuthRepository
import org.json.JSONObject

data class SubStrandProgress(
    val subStrandId: String,
    val isCompleted: Boolean,
    val learnerNote: String,
    val selfEvaluationLevel: PerformanceLevel,
    val completedTimestamp: Long
)

class ActivityProgressManager(context: Context) {

    private companion object {
        val LEGACY_MIGRATION_LOCK = Any()
    }

    internal val appContext: Context = context.applicationContext
    internal val signedInUserId: String? =
        SupabaseAuthRepository.currentSession(appContext)?.userId?.takeIf { it.isNotBlank() }

    private val prefs: SharedPreferences = appContext.getSharedPreferences(
        "tusome_cbc_progress_prefs_${signedInUserId ?: "guest"}",
        Context.MODE_PRIVATE
    )

    init {
        migrateLegacyProgressForFirstSignedInUser()
        // Cloud sync is best-effort. Local progress remains usable when the device is offline.
        if (signedInUserId != null) LearnerProgressSyncRepository.scheduleSync(this)
    }

    private fun migrateLegacyProgressForFirstSignedInUser() {
        val userId = signedInUserId ?: return
        val migrationPrefs = appContext.getSharedPreferences(
            "tusome_cbc_progress_migration",
            Context.MODE_PRIVATE
        )
        if (migrationPrefs.getBoolean("legacy_progress_assigned", false)) return

        synchronized(LEGACY_MIGRATION_LOCK) {
            if (migrationPrefs.getBoolean("legacy_progress_assigned", false)) return
            val legacy = appContext.getSharedPreferences("tusome_cbc_progress_prefs", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            legacy.all.forEach { (key, value) ->
                if (!key.startsWith("completed_") &&
                    !key.startsWith("note_") &&
                    !key.startsWith("level_") &&
                    !key.startsWith("time_")
                ) return@forEach
                if (prefs.contains(key)) return@forEach
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is String -> editor.putString(key, value)
                    is Long -> editor.putLong(key, value)
                    is Int -> editor.putInt(key, value)
                    is Float -> editor.putFloat(key, value)
                }
            }
            editor.apply()
            // Preserve the old preference file as a local backup; only the first signed-in
            // account receives its previously shared progress.
            migrationPrefs.edit()
                .putBoolean("legacy_progress_assigned", true)
                .putString("legacy_progress_user_id", userId)
                .apply()
        }
    }

    fun isSubStrandCompleted(subStrandId: String): Boolean =
        prefs.getBoolean("completed_$subStrandId", false)

    fun getLearnerNote(subStrandId: String): String =
        prefs.getString("note_$subStrandId", "") ?: ""

    fun getSelfEvaluation(subStrandId: String): PerformanceLevel {
        val levelName = prefs.getString("level_$subStrandId", PerformanceLevel.MEETS.name)
        return runCatching { PerformanceLevel.valueOf(levelName ?: PerformanceLevel.MEETS.name) }
            .getOrDefault(PerformanceLevel.MEETS)
    }

    fun saveSubStrandProgress(
        subStrandId: String,
        isCompleted: Boolean,
        learnerNote: String,
        evaluationLevel: PerformanceLevel
    ) {
        val updatedAt = System.currentTimeMillis()
        prefs.edit()
            .putBoolean("completed_$subStrandId", isCompleted)
            .putString("note_$subStrandId", learnerNote)
            .putString("level_$subStrandId", evaluationLevel.name)
            .putLong("time_$subStrandId", updatedAt)
            .apply()

        if (signedInUserId != null) LearnerProgressSyncRepository.scheduleSync(this)
    }

    fun getCompletedSubStrandsCount(): Int =
        prefs.all.keys.count { it.startsWith("completed_") && prefs.getBoolean(it, false) }

    fun getCompletedCountForLearningArea(subStrandIds: List<String>): Int =
        subStrandIds.count { isSubStrandCompleted(it) }

    internal fun getAllProgress(): List<SubStrandProgress> =
        prefs.all.keys.asSequence()
            .filter { it.startsWith("completed_") }
            .map { it.removePrefix("completed_") }
            .distinct()
            .map { id ->
                SubStrandProgress(
                    subStrandId = id,
                    isCompleted = prefs.getBoolean("completed_$id", false),
                    learnerNote = prefs.getString("note_$id", "") ?: "",
                    selfEvaluationLevel = getSelfEvaluation(id),
                    completedTimestamp = prefs.getLong("time_$id", 0L)
                )
            }
            .toList()

    internal fun mergeCloudProgress(progress: SubStrandProgress) {
        val localTimestamp = prefs.getLong("time_${progress.subStrandId}", 0L)
        if (progress.completedTimestamp <= localTimestamp) return

        prefs.edit()
            .putBoolean("completed_${progress.subStrandId}", progress.isCompleted)
            .putString("note_${progress.subStrandId}", progress.learnerNote)
            .putString("level_${progress.subStrandId}", progress.selfEvaluationLevel.name)
            .putLong("time_${progress.subStrandId}", progress.completedTimestamp)
            .apply()
    }

    internal fun toCloudJson(progress: SubStrandProgress, userId: String): JSONObject =
        JSONObject()
            .put("user_id", userId)
            .put("sub_strand_id", progress.subStrandId)
            .put("is_completed", progress.isCompleted)
            .put("learner_note", progress.learnerNote)
            .put("evaluation_level", progress.selfEvaluationLevel.code)
            .put("client_updated_at", progress.completedTimestamp)
}
