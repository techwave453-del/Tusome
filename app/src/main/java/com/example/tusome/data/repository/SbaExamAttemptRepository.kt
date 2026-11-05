package com.example.tusome.data.repository

import android.content.Context
import com.example.tusome.data.model.SbaExamPaper
import org.json.JSONObject

data class SbaExamAttempt(
    val answers: Map<String, String>,
    val startedAtMillis: Long,
    val deadlineAtMillis: Long,
    val submittedAtMillis: Long? = null
) {
    val isSubmitted: Boolean get() = submittedAtMillis != null
}

object SbaExamAttemptRepository {
    private const val PREFS = "tusome_sba_attempts"

    private fun key(context: Context, paper: SbaExamPaper): String {
        val userId = SupabaseAuthRepository.currentSession(context)?.userId
            ?.takeIf { it.isNotBlank() } ?: "guest"
        return "attempt_${userId}_${paper.id}"
    }

    fun loadOrCreate(context: Context, paper: SbaExamPaper): SbaExamAttempt {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(key(context, paper), null)
        if (!stored.isNullOrBlank()) {
            runCatching {
                val json = JSONObject(stored)
                val answersJson = json.optJSONObject("answers") ?: JSONObject()
                val answers = buildMap {
                    val keys = answersJson.keys()
                    while (keys.hasNext()) {
                        val answerKey = keys.next()
                        put(answerKey, answersJson.optString(answerKey, ""))
                    }
                }
                return SbaExamAttempt(
                    answers = answers,
                    startedAtMillis = json.getLong("startedAtMillis"),
                    deadlineAtMillis = json.getLong("deadlineAtMillis"),
                    submittedAtMillis = json.optLong("submittedAtMillis").takeIf { it > 0L }
                )
            }
        }
        val now = System.currentTimeMillis()
        return SbaExamAttempt(
            answers = emptyMap(),
            startedAtMillis = now,
            deadlineAtMillis = now + paper.timeAllowedMinutes * 60_000L
        ).also { save(context, paper, it) }
    }

    fun save(context: Context, paper: SbaExamPaper, attempt: SbaExamAttempt) {
        val answers = JSONObject()
        attempt.answers.forEach { (answerKey, answer) -> answers.put(answerKey, answer) }
        val json = JSONObject()
            .put("answers", answers)
            .put("startedAtMillis", attempt.startedAtMillis)
            .put("deadlineAtMillis", attempt.deadlineAtMillis)
            .put("submittedAtMillis", attempt.submittedAtMillis ?: 0L)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(key(context, paper), json.toString()).apply()
    }
}
