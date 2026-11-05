package com.example.tusome.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.tusome.R
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.model.LearningArea
import org.json.JSONArray
import org.json.JSONObject

class OfflineCurriculumCache(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("tusome_offline_curriculum_cache", Context.MODE_PRIVATE)

    // Save fetched learning area list for offline use
    fun cacheLearningAreas(grade: GradeLevel, areas: List<LearningArea>) {
        val array = JSONArray()
        areas.forEach { area ->
            val obj = JSONObject().apply {
                put("id", area.id)
                put("title", area.title)
                put("kiswahiliTitle", area.kiswahiliTitle)
                put("code", area.code)
                put("strandsCount", area.strandsCount)
                put("subStrandsCount", area.subStrandsCount)
                put("progressPercent", area.progressPercent)
                put("iconResId", area.iconResId)
                put("bgColorHex", area.bgColorHex)
                put("iconColorHex", area.iconColorHex)
            }
            array.put(obj)
        }
        prefs.edit().putString("areas_${grade.code.lowercase()}", array.toString()).apply()
    }

    // Get cached learning area list for offline use
    fun getCachedLearningAreas(grade: GradeLevel): List<LearningArea>? {
        val jsonStr = prefs.getString("areas_${grade.code.lowercase()}", null) ?: return null
        return try {
            val array = JSONArray(jsonStr)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        LearningArea(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            kiswahiliTitle = obj.optString("kiswahiliTitle", obj.getString("title")),
                            code = obj.optString("code", grade.code),
                            gradeLevel = grade,
                            strandsCount = obj.optInt("strandsCount", 0),
                            subStrandsCount = obj.optInt("subStrandsCount", 0),
                            progressPercent = obj.optInt("progressPercent", 0),
                            iconResId = obj.optInt("iconResId", R.drawable.ic_book),
                            bgColorHex = obj.optString("bgColorHex", "#E8F5E9"),
                            iconColorHex = obj.optString("iconColorHex", "#2E7D32")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    // Save fetched learning area overview text/details for offline access
    fun cacheLearningAreaOverview(grade: GradeLevel, areaId: String, overviewText: String) {
        val key = "overview_${grade.code.lowercase()}_$areaId"
        prefs.edit().putString(key, overviewText).apply()
    }

    // Get cached learning area overview text/details
    fun getCachedLearningAreaOverview(grade: GradeLevel, areaId: String): String? {
        val key = "overview_${grade.code.lowercase()}_$areaId"
        return prefs.getString(key, null)
    }

    // Save AI generated KICD sub-strand content locally
    fun saveSubstrandKicdContent(subStrandId: String, jsonString: String) {
        prefs.edit().putString("sub_kicd_v2_$subStrandId", jsonString).apply()
    }

    // Get saved AI generated KICD sub-strand content
    fun getSubstrandKicdContent(subStrandId: String): String? {
        return prefs.getString("sub_kicd_v2_$subStrandId", null)
    }

    // Save AI generated study notes locally for permanent offline access
    fun saveGeneratedAiStudyNotes(strandId: String, notesText: String) {
        prefs.edit().putString("ai_notes_$strandId", notesText).apply()
    }

    // Get saved AI study notes
    fun getGeneratedAiStudyNotes(strandId: String): String? {
        return prefs.getString("ai_notes_$strandId", null)
    }

    // Save AI generated practice questions locally for offline study
    fun saveGeneratedAiPracticeQuestions(strandId: String, questionsJson: String) {
        prefs.edit().putString("ai_questions_$strandId", questionsJson).apply()
    }

    // Get saved AI practice questions
    fun getGeneratedAiPracticeQuestions(strandId: String): String? {
        return prefs.getString("ai_questions_$strandId", null)
    }

    // Save AI Tutor Q&A history for offline reading
    fun cacheAiTutorChatHistory(chatJsonString: String) {
        prefs.edit().putString("ai_tutor_chat_history", chatJsonString).apply()
    }

    // Get saved AI Tutor Q&A history
    fun getCachedAiTutorChatHistory(): String? {
        return prefs.getString("ai_tutor_chat_history", null)
    }

    // Check if offline curriculum exists for a grade
    fun hasOfflineCurriculum(grade: GradeLevel): Boolean {
        return prefs.contains("areas_${grade.code.lowercase()}")
    }
}