package com.example.tusome.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.tusome.data.model.PerformanceLevel

data class SubStrandProgress(
    val subStrandId: String,
    val isCompleted: Boolean,
    val learnerNote: String,
    val selfEvaluationLevel: PerformanceLevel,
    val completedTimestamp: Long
)

class ActivityProgressManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("tusome_cbc_progress_prefs", Context.MODE_PRIVATE)

    fun isSubStrandCompleted(subStrandId: String): Boolean {
        return prefs.getBoolean("completed_$subStrandId", false)
    }

    fun getLearnerNote(subStrandId: String): String {
        return prefs.getString("note_$subStrandId", "") ?: ""
    }

    /** Returns null when the learner has not submitted a self-evaluation yet. */
    fun getSavedSelfEvaluation(subStrandId: String): PerformanceLevel? {
        val key = "level_$subStrandId"
        if (!prefs.contains(key)) return null
        return runCatching {
            PerformanceLevel.valueOf(prefs.getString(key, null) ?: return null)
        }.getOrNull()
    }

    fun getSelfEvaluation(subStrandId: String): PerformanceLevel {
        val levelName = prefs.getString("level_$subStrandId", PerformanceLevel.MEETS.name)
        return try {
            PerformanceLevel.valueOf(levelName ?: PerformanceLevel.MEETS.name)
        } catch (_: Exception) {
            PerformanceLevel.MEETS
        }
    }

    fun saveSubStrandProgress(
        subStrandId: String,
        isCompleted: Boolean,
        learnerNote: String,
        evaluationLevel: PerformanceLevel
    ) {
        prefs.edit().apply {
            putBoolean("completed_$subStrandId", isCompleted)
            putString("note_$subStrandId", learnerNote)
            putString("level_$subStrandId", evaluationLevel.name)
            putLong("time_$subStrandId", System.currentTimeMillis())
            apply()
        }
    }

    fun getCompletedSubStrandsCount(): Int {
        return prefs.all.keys.count { it.startsWith("completed_") && prefs.getBoolean(it, false) }
    }

    fun getCompletedCountForLearningArea(subStrandIds: List<String>): Int {
        return subStrandIds.count { isSubStrandCompleted(it) }
    }
}