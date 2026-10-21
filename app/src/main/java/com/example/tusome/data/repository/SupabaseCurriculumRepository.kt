package com.example.tusome.data.repository

import android.os.Handler
import android.os.Looper
import com.example.tusome.R
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.model.LearningArea
import com.example.tusome.data.model.AssessmentRubric
import com.example.tusome.data.model.PerformanceLevel
import com.example.tusome.data.model.Strand
import com.example.tusome.data.model.SubStrand
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


    fun fetchLearningAreaOverview(
        grade: GradeLevel,
        learningAreaId: String,
        onResult: (Result<String>) -> Unit
    ) {
        executor.execute {
            val result = runCatching { requestLearningAreaOverview(grade, learningAreaId) }
            mainHandler.post { onResult(result) }
        }
    }

    /** Fetches only published strands belonging to the requested grade. */
    fun fetchStrands(
        grade: GradeLevel,
        onResult: (Result<List<Strand>>) -> Unit
    ) {
        executor.execute {
            val result = runCatching { requestStrands(grade) }
            mainHandler.post { onResult(result) }
        }
    }

    private fun requestStrands(grade: GradeLevel): List<Strand> {
        val gradeId = grade.code.lowercase(Locale.ROOT)
        val endpoint = "$BASE_URL/rest/v1/curriculum_strands" +
            "?select=id,learning_area_id,title,description,sequence," +
            "curriculum_sub_strands(id,title,sequence,is_published," +
            "learning_outcomes(outcome_text,sequence,is_published)," +
            "inquiry_questions(question_text,sequence,is_published)," +
            "learning_activities(activity_text,sequence,is_published)," +
            "assessment_rubrics(id,criteria,exceeds_expectation,meets_expectation," +
            "approaching_expectation,below_expectation,is_published))" +
            "&grade_id=eq.$gradeId&is_published=eq.true&content_status=eq.published" +
            "&curriculum_sub_strands.is_published=eq.true" +
            "&curriculum_sub_strands.learning_outcomes.is_published=eq.true" +
            "&curriculum_sub_strands.inquiry_questions.is_published=eq.true" +
            "&curriculum_sub_strands.learning_activities.is_published=eq.true" +
            "&curriculum_sub_strands.assessment_rubrics.is_published=eq.true" +
            "&order=sequence.asc&curriculum_sub_strands.order=sequence.asc" +
            "&curriculum_sub_strands.learning_outcomes.order=sequence.asc" +
            "&curriculum_sub_strands.inquiry_questions.order=sequence.asc" +
            "&curriculum_sub_strands.learning_activities.order=sequence.asc" +
            "&curriculum_sub_strands.assessment_rubrics.order=criteria.asc"

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 15000
            setRequestProperty("apikey", PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $PUBLISHABLE_KEY")
            setRequestProperty("Accept", "application/json")
        }

        return try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            check(status in 200..299) {
                "Supabase returned HTTP $status while loading " + grade.title + " strands."
            }

            val rows = JSONArray(body)
            buildList {
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val strandId = row.optString("id")
                    val areaId = row.optString("learning_area_id")
                    if (strandId.isBlank() || areaId.isBlank()) continue

                    val subRows = row.optJSONArray("curriculum_sub_strands") ?: JSONArray()
                    val subStrands = buildList {
                        for (j in 0 until subRows.length()) {
                            val sub = subRows.getJSONObject(j)
                            val subId = sub.optString("id")
                            if (subId.isBlank()) continue

                            fun stringList(array: JSONArray?, field: String): List<String> {
                                val values = mutableListOf<String>()
                                if (array != null) {
                                    for (k in 0 until array.length()) {
                                        val value = array.optJSONObject(k)?.optString(field).orEmpty()
                                        if (value.isNotBlank()) values.add(value)
                                    }
                                }
                                return values
                            }

                            val outcomes = stringList(sub.optJSONArray("learning_outcomes"), "outcome_text")
                            val questions = stringList(sub.optJSONArray("inquiry_questions"), "question_text")
                            val activities = stringList(sub.optJSONArray("learning_activities"), "activity_text")
                            val rubricRows = sub.optJSONArray("assessment_rubrics") ?: JSONArray()
                            val publishedRubric = (0 until rubricRows.length())
                                .map { rubricRows.optJSONObject(it) }
                                .filterNotNull()
                                .firstOrNull { it.optBoolean("is_published", true) }
                            val rubric = publishedRubric?.let { rubricRow ->
                                AssessmentRubric(
                                    id = rubricRow.optString("id").ifBlank { "rubric-" + subId },
                                    subStrandId = subId,
                                    criteria = rubricRow.optString("criteria").ifBlank { "Assessment criteria require teacher review." },
                                    exceedsDescription = rubricRow.optString("exceeds_expectation").ifBlank { "Demonstrates the learning independently and applies it in a new context." },
                                    meetsDescription = rubricRow.optString("meets_expectation").ifBlank { "Demonstrates the expected learning with age-appropriate accuracy." },
                                    approachesDescription = rubricRow.optString("approaching_expectation").ifBlank { "Demonstrates part of the learning with guided practice." },
                                    belowDescription = rubricRow.optString("below_expectation").ifBlank { "Needs additional modelling and guided practice." },
                                    learnerLevel = null,
                                    teacherComment = ""
                                )
                            } ?: AssessmentRubric(
                                id = "missing-published-rubric-" + subId,
                                subStrandId = subId,
                                criteria = "No published assessment rubric is available for this sub-strand yet.",
                                exceedsDescription = "Not assessed: a reviewed rubric has not been published.",
                                meetsDescription = "Not assessed: a reviewed rubric has not been published.",
                                approachesDescription = "Not assessed: a reviewed rubric has not been published.",
                                belowDescription = "Not assessed: a reviewed rubric has not been published.",
                                learnerLevel = null,
                                teacherComment = "Do not use this placeholder for grading. Publish a reviewed rubric first."
                            )
                            val strandSequence = row.optInt("sequence", i + 1)
                            val subSequence = sub.optInt("sequence", j + 1)
                            add(
                                SubStrand(
                                    id = subId,
                                    strandId = strandId,
                                    subStrandNumber = strandSequence.toString() + "." + subSequence,
                                    title = sub.optString("title", "Learning activity"),
                                    specificLearningOutcomes = outcomes,
                                    keyInquiryQuestions = questions,
                                    suggestedExperiences = activities,
                                    digitalTaskPrompt = activities.firstOrNull().orEmpty(),
                                    rubric = rubric
                                )
                            )
                        }
                    }

                    add(
                        Strand(
                            id = strandId,
                            learningAreaId = areaId,
                            strandNumber = row.optInt("sequence", i + 1),
                            title = row.optString("title", "Curriculum strand"),
                            description = row.optString("description").takeIf { it.isNotBlank() }
                                ?: "Published starter curriculum for " + grade.title + ".",
                            coreCompetencies = emptyList(),
                            subStrands = subStrands
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun requestLearningAreaOverview(grade: GradeLevel, learningAreaId: String): String {
        val gradeId = grade.code.lowercase(Locale.ROOT)
        val endpoint = "$BASE_URL/rest/v1/curriculum_strands" +
            "?select=title,sequence,description,curriculum_sub_strands(title,sequence,learning_outcomes(outcome_text,sequence))" +
            "&grade_id=eq.$gradeId&learning_area_id=eq.$learningAreaId" +
            "&is_published=eq.true&content_status=eq.published&order=sequence.asc" +
            "&curriculum_sub_strands.order=sequence.asc&curriculum_sub_strands.learning_outcomes.order=sequence.asc"

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
            check(status in 200..299) { "Supabase returned HTTP $status while loading curriculum details." }

            val rows = JSONArray(body)
            if (rows.length() == 0) {
                return "This learning area is listed in the Tusome curriculum catalogue, but strand-level content for " +
                    grade.title + " has not yet been reviewed and published.\n\n" +
                    "Use KICD curriculum designs as the authoritative source while grade-specific content is verified.\n\n" +
                    "KICD: https://kicd.ac.ke/cbc-materials/curriculum-designs/regular-curriculum-designs/"
            }

            val output = StringBuilder()
            for (i in 0 until rows.length()) {
                val strand = rows.getJSONObject(i)
                output.append(strand.optInt("sequence")).append(". ")
                    .append(strand.optString("title")).append("\n")
                val subs = strand.optJSONArray("curriculum_sub_strands") ?: JSONArray()
                for (j in 0 until subs.length()) {
                    val sub = subs.getJSONObject(j)
                    output.append("   • ").append(sub.optString("title")).append("\n")
                    val outcomes = sub.optJSONArray("learning_outcomes") ?: JSONArray()
                    for (k in 0 until outcomes.length()) {
                        output.append("      – ").append(outcomes.getJSONObject(k).optString("outcome_text")).append("\n")
                    }
                }
                output.append("\n")
            }
            if (learningAreaId == "agriculture_nutrition" && gradeId == "g4") {
                output.append("Source: KICD, Grade 4 Agriculture and Nutrition curriculum design (revised 2024).\n")
                output.append("https://kicd.ac.ke/wp-content/uploads/2024/02/GRADE.4.AGRICULTURE.NUTRITION.16.1.2024.pdf")
            }
            return output.toString().trim()
        } finally {
            connection.disconnect()
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

        return try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            check(status in 200..299) {
                "Supabase returned HTTP $status. Check project status, network access and RLS."
            }

            val rows = JSONArray(body)
            val contentCounts = requestLearningAreaCounts(gradeId)
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
                            strandsCount = contentCounts[id]?.first ?: 0,
                            subStrandsCount = contentCounts[id]?.second ?: 0,
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


    /**
     * Counts only published grade-specific content, so cards never display invented totals.
     * The nested relation is fetched in one request for the selected grade.
     */
    private fun requestLearningAreaCounts(gradeId: String): Map<String, Pair<Int, Int>> {
        val endpoint = "$BASE_URL/rest/v1/curriculum_strands" +
            "?select=learning_area_id,curriculum_sub_strands(id)" +
            "&grade_id=eq.$gradeId&is_published=eq.true&content_status=eq.published" +
            "&curriculum_sub_strands.is_published=eq.true&order=learning_area_id.asc"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("apikey", PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $PUBLISHABLE_KEY")
            setRequestProperty("Accept", "application/json")
        }

        return try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            check(status in 200..299) {
                "Supabase returned HTTP $status while loading curriculum counts."
            }

            val counts = mutableMapOf<String, Pair<Int, Int>>()
            val rows = JSONArray(body)
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                val areaId = row.optString("learning_area_id").takeIf { it.isNotBlank() } ?: continue
                val subs = row.optJSONArray("curriculum_sub_strands") ?: JSONArray()
                val previous = counts[areaId] ?: (0 to 0)
                counts[areaId] = (previous.first + 1) to (previous.second + subs.length())
            }
            counts
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
