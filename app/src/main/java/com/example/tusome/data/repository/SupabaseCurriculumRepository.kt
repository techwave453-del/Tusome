package com.example.tusome.data.repository

import android.os.Handler
import android.os.Looper
import com.example.tusome.R
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.model.LearningArea
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Read-only Supabase REST access for published CBC curriculum.
 *
 * The publishable key is intentionally client-side; database access is constrained by RLS.
 * Never put a service-role key in the Android app.
 */
object SupabaseCurriculumRepository {
    private const val BASE_URL = "https://wkwnoflielrotknvoflz.supabase.co"
    private const val PUBLISHABLE_KEY = "sb_publishable_jZsmwIcIMdu3Eq97AfU3xQ_929Bxo4J"

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun fetchLearningAreas(
        grade: GradeLevel,
        onResult: (Result<List<LearningArea>>) -> Unit
    ) {
        executor.execute {
            val result = runCatching { requestLearningAreas(grade) }
            mainHandler.post { onResult(result) }
        }
    }

    private fun requestLearningAreas(grade: GradeLevel): List<LearningArea> {
        val gradeId = grade.code.lowercase(Locale.ROOT)
        val endpoint = "$BASE_URL/rest/v1/grade_learning_areas" +
            "?select=learning_area_id,learning_areas(id,name,kiswahili_name)" +
            "&grade_id=eq.$gradeId&is_published=eq.true&order=learning_area_id.asc"

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("apikey", PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $PUBLISHABLE_KEY")
            setRequestProperty("Accept", "application/json")
        }

        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            check(status in 200..299) {
                "Supabase returned HTTP $status. Check project status, network access and RLS."
            }

            val rows = JSONArray(body)
            buildList {
                for (index in 0 until rows.length()) {
                    val row = rows.getJSONObject(index)
                    val area = row.optJSONObject("learning_areas") ?: continue
                    val id = area.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val name = area.optString("name").takeIf { it.isNotBlank() } ?: continue
                    val icon = iconFor(id)
                    val colors = colorsFor(id)
                    add(
                        LearningArea(
                            id = id,
                            title = name,
                            kiswahiliTitle = area.optString("kiswahili_name").takeIf { it.isNotBlank() } ?: name,
                            code = id.uppercase(Locale.ROOT),
                            gradeLevel = grade,
                            strandsCount = if (gradeId == "g4" && id == "agriculture_nutrition") 4 else 0,
                            subStrandsCount = if (gradeId == "g4" && id == "agriculture_nutrition") 13 else 0,
                            progressPercent = 0,
                            iconResId = icon,
                            bgColorHex = colors.first,
                            iconColorHex = colors.second
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun iconFor(id: String): Int = when {
        id.contains("agriculture") -> R.drawable.ic_leaf
        id.contains("math") -> R.drawable.ic_award
        id.contains("science") || id.contains("technical") -> R.drawable.ic_psychology
        id.contains("english") || id.contains("kiswahili") || id.contains("language") -> R.drawable.ic_book
        id.contains("social") || id.contains("history") || id.contains("geography") -> R.drawable.ic_school
        id.contains("creative") || id.contains("arts") || id.contains("music") -> R.drawable.ic_star
        else -> R.drawable.ic_book
    }

    private fun colorsFor(id: String): Pair<String, String> = when {
        id.contains("agriculture") -> "#F1F8E9" to "#558B2F"
        id.contains("math") -> "#E8F5E9" to "#2E7D32"
        id.contains("science") -> "#FFF3E0" to "#EF6C00"
        id.contains("english") -> "#E1F5FE" to "#0288D1"
        id.contains("kiswahili") -> "#E0F2F1" to "#00695C"
        id.contains("creative") || id.contains("arts") -> "#F3E5F5" to "#7B1FA2"
        else -> "#E8F5E9" to "#2E7D32"
    }
}
