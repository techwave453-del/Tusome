package com.example.tusome.data.service

import android.content.Context
import com.example.tusome.data.repository.SupabaseAuthRepository
import org.json.JSONObject
import com.example.tusome.data.model.GradeCategory
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.model.QuizQuestion

data class KicdSubstrandContent(
    val specificLearningOutcomes: List<String>,
    val keyInquiryQuestions: List<String>,
    val suggestedExperiences: List<String>,
    val digitalTaskPrompt: String,
    val kicdCitation: String,
    val fullLessonContentJson: String = ""
)

object CbcAiTutorService {

    fun generateTutorResponse(
        userQuery: String,
        grade: GradeLevel,
        subjectContext: String? = null,
        onResponse: (String) -> Unit
    ) {
        val query = userQuery.trim().lowercase()

        val categoryPrefix = when (grade.category) {
            GradeCategory.LOWER_PRIMARY -> "Lower Primary (Grade 1-3) Foundational Stage"
            GradeCategory.UPPER_PRIMARY -> "Upper Primary (Grade 4-6) Inquiry Stage"
            GradeCategory.JUNIOR_SECONDARY -> "Junior Secondary (Grade 7-9) Pre-Vocational & Exploration Stage"
            else -> "${grade.title} KICD Curriculum Level"
        }

        val answer = when {
            query.contains("compost") || query.contains("manure") ->
                "[$categoryPrefix • KICD Agriculture]\nIn ${grade.title}, building a compost heap involves layering nitrogen-rich green waste (kitchen peels) and carbon-rich dry waste (grass/leaves) with wood ash and topsoil. Keep it moist for 6–8 weeks to decompose into organic manure!"

            query.contains("drip") || query.contains("irrigation") || query.contains("water") ->
                "[$categoryPrefix • KICD Conservation]\nWater conservation is vital in Kenya! Drip irrigation uses plastic bottles with small pinholes near plant root zones (like sukuma wiki/spinach) to reduce evaporation by over 50%!"

            query.contains("place value") || query.contains("number") || query.contains("digits") ->
                "[$categoryPrefix • KICD Mathematics]\nPlace value defines a digit's value by its position. For example, in ${if (grade.category == GradeCategory.LOWER_PRIMARY) "342" else "47,842"}:\n• Thousands/Hundreds: Shows total groups of 100/1,000\n• Tens & Ones: Represents smaller units!"

            query.contains("kiswahili") || query.contains("kilimo") || query.contains("lishe") ->
                "[$categoryPrefix • KICD Kiswahili & Kilimo]\nJambo! Katika Somo la Kilimo na Lishe (${grade.title}), tunajifunza kuhusu kuhifadhi udongo, kutengeneza samadi na umwagiliaji wa matone."

            query.contains("erosion") || query.contains("soil") ->
                "[$categoryPrefix • KICD Environmental Conservation]\nSoil erosion is the washing away of topsoil by rain or wind. In KICD curriculum, we prevent it by planting grass strips, building check dams, mulching, and terracing steep land!"

            query.contains("competency") || query.contains("cbc") ->
                "KICD 7 Core Competencies for ${grade.title}:\n1. Communication & Collaboration\n2. Critical Thinking & Problem Solving\n3. Creativity & Imagination\n4. Citizenship & Kenya Values\n5. Digital Literacy\n6. Learning to Learn\n7. Self-Efficacy!"

            else ->
                "Hello! I am your Tusome AI Learning Assistant for $categoryPrefix (${grade.title}). I provide guidance strictly aligned with KICD curriculum designs. What concept would you like to explore?"
        }

        onResponse(answer)
    }

    // Generates a sub-strand-specific lesson pack from the published curriculum records on the server.
    // The server validates the full grade → learning area → strand → sub-strand path and stores a reviewable draft.
    fun generateSubStrandKicdContent(
        context: Context,
        learningAreaId: String,
        strandId: String,
        subStrandId: String,
        subStrandTitle: String,
        strandTitle: String,
        grade: GradeLevel,
        onResult: (Result<KicdSubstrandContent>) -> Unit
    ) {
        val payload = JSONObject()
            .put("grade_id", grade.code.lowercase())
            .put("learning_area_id", learningAreaId)
            .put("strand_id", strandId)
            .put("sub_strand_id", subStrandId)
            .put("language", "en")

        SupabaseAuthRepository.invokeEdgeFunction(
            context = context,
            functionName = "generate-substrand-content",
            payload = payload
        ) { result ->
            onResult(result.mapCatching { response ->
                val content = response.optJSONObject("content")
                    ?: throw IllegalStateException("The server returned no lesson content.")
                fun strings(key: String): List<String> {
                    val array = content.optJSONArray(key) ?: org.json.JSONArray()
                    return buildList {
                        for (index in 0 until array.length()) {
                            array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                        }
                    }
                }
                val outcomes = strings("outcomes")
                val questions = strings("inquiry_questions")
                val activityRows = content.optJSONArray("learning_activities") ?: org.json.JSONArray()
                val activities = buildList {
                    for (index in 0 until activityRows.length()) {
                        val activity = activityRows.optJSONObject(index) ?: continue
                        val title = activity.optString("title").trim()
                        val instructions = activity.optString("instructions").trim()
                        if (title.isNotBlank() && instructions.isNotBlank()) add("$title: $instructions")
                    }
                }
                val digitalTask = content.optString("digital_task")
                if (outcomes.isEmpty() || questions.isEmpty() || activities.isEmpty()) {
                    throw IllegalStateException("The generated lesson pack is incomplete.")
                }
                KicdSubstrandContent(
                    specificLearningOutcomes = outcomes,
                    keyInquiryQuestions = questions,
                    suggestedExperiences = activities,
                    digitalTaskPrompt = buildString {
                        append("Suggested learning experiences:\n")
                        activities.forEachIndexed { index, activity ->
                            append(index + 1).append(". ").append(activity).append("\n")
                        }
                        append("\nPortfolio task: ").append(
                            digitalTask.ifBlank {
                                "Record evidence of your learning about $subStrandTitle in your portfolio."
                            }
                        )
                    },
                    kicdCitation = "AI-generated draft for ${grade.title} • $strandTitle → $subStrandTitle. Based on published Tusome curriculum records; pending curriculum review.",
                    fullLessonContentJson = content.toString()
                )
            })
        }
    }

    // AI Study Notes Generator strictly following KICD Curriculum Level Guidelines
    fun generateStrandStudyNotes(
        strandTitle: String,
        grade: GradeLevel,
        onResult: (String) -> Unit
    ) {
        val categoryText = when (grade.category) {
            GradeCategory.LOWER_PRIMARY -> "KICD LOWER PRIMARY LEVEL (Grade 1 - 3)"
            GradeCategory.UPPER_PRIMARY -> "KICD UPPER PRIMARY LEVEL (Grade 4 - 6)"
            GradeCategory.JUNIOR_SECONDARY -> "KICD JUNIOR SECONDARY LEVEL (Grade 7 - 9)"
            else -> "KICD CURRICULUM LEVEL (${grade.title})"
        }

        val notes = buildString {
            append("📚 KICD CURRICULUM AI STUDY NOTES\n")
            append("Level: ").append(categoryText).append(" • ").append(grade.title).append("\n")
            append("Strand: ").append(strandTitle).append("\n\n")

            when (grade.category) {
                GradeCategory.LOWER_PRIMARY -> {
                    append("1. FOUNDATIONAL CONCEPTS & OBSERVATION:\n")
                    append("• Identify items in the immediate school and home environment related to ").append(strandTitle).append(".\n")
                    append("• Develop foundational vocabulary through rhymes, pictures, and simple hands-on demonstration.\n\n")
                    append("2. PRACTICAL GUIDED ACTIVITIES:\n")
                    append("• Collect and sort local natural materials in small collaborative learning groups.\n")
                    append("• Practice personal safety, cleanliness, and care for living organisms.\n\n")
                    append("3. INTEGRATED KENYA VALUES:\n")
                    append("• Love, Respect, Sharing, and Care for the Environment.")
                }

                GradeCategory.JUNIOR_SECONDARY -> {
                    append("1. ADVANCED CONCEPTS & TECHNICAL SKILLS:\n")
                    append("• Analyze scientific, mathematical, or pre-technical principles governing ").append(strandTitle).append(".\n")
                    append("• Perform detailed problem-solving, data recording, and technical analysis.\n\n")
                    append("2. PRE-VOCATIONAL & COMMUNITY SERVICE PROJECTS:\n")
                    append("• Design sustainable community solutions and apply safety standards when using tools/equipment.\n")
                    append("• Present project findings through digital presentations and self-assessment rubrics.\n\n")
                    append("3. CORE COMPETENCIES & CITIZENSHIP:\n")
                    append("• Innovation, Self-Reliance, Social Justice, and National Values.")
                }

                else -> {
                    append("1. SPECIFIC LEARNING OUTCOMES (SLO):\n")
                    append("• Explain core principles of ").append(strandTitle).append(" in the Kenyan context.\n")
                    append("• Construct practical models (e.g., container gardens, check dams, place value charts).\n\n")
                    append("2. INQUIRY & EXPERIENTIAL LEARNING:\n")
                    append("• Conduct guided field walks around the school compound to observe real-world challenges.\n")
                    append("• Record digital evidence (photos/reflections) in learner CBC portfolios.\n\n")
                    append("3. CORE COMPETENCIES & VALUES:\n")
                    append("• Environmental Stewardship, Critical Thinking, Digital Literacy, and Integrity.")
                }
            }
        }

        onResult(notes)
    }

    // AI Practice Questions Generator strictly following KICD Curriculum Level
    fun generatePracticeQuestions(
        strandTitle: String,
        grade: GradeLevel,
        onResult: (List<QuizQuestion>) -> Unit
    ) {
        val questions = when (grade.category) {
            GradeCategory.LOWER_PRIMARY -> listOf(
                QuizQuestion(
                    id = "kicd_lp_q1_${System.currentTimeMillis()}",
                    questionText = "When doing practical activities in $strandTitle (${grade.title}), why should we wash our hands with soap afterwards?",
                    choices = listOf(
                        "To maintain hygiene and prevent the spread of germs",
                        "To make our hands wet only",
                        "To waste water",
                        "Because the teacher said so without reason"
                    ),
                    correctAnswerIndex = 0,
                    explanation = "KICD Lower Primary hygiene guidelines emphasize washing hands with clean running water and soap."
                ),
                QuizQuestion(
                    id = "kicd_lp_q2_${System.currentTimeMillis()}",
                    questionText = "Which of these local natural materials can we collect during our $strandTitle field walk?",
                    choices = listOf(
                        "Dry leaves, twigs, and small stones",
                        "Sharp broken glass bottles",
                        "Used rusty razor blades",
                        "Plastic waste"
                    ),
                    correctAnswerIndex = 0,
                    explanation = "In KICD foundational learning, safe natural materials like dry leaves and twigs are used for group tasks."
                )
            )

            GradeCategory.JUNIOR_SECONDARY -> listOf(
                QuizQuestion(
                    id = "kicd_jss_q1_${System.currentTimeMillis()}",
                    questionText = "In KICD Junior Secondary (${grade.title}) $strandTitle, what is the primary purpose of applying design thinking principles?",
                    choices = listOf(
                        "To identify community challenges, design innovative prototypes, and evaluate practical solutions",
                        "To avoid using technical tools",
                        "To eliminate mathematical calculations",
                        "To memorize theories without practical execution"
                    ),
                    correctAnswerIndex = 0,
                    explanation = "JSS Pre-Technical and Integrated Science curriculum emphasizes design thinking and practical innovation."
                ),
                QuizQuestion(
                    id = "kicd_jss_q2_${System.currentTimeMillis()}",
                    questionText = "Which safety protocol is mandatory when conducting practical technical tasks in $strandTitle?",
                    choices = listOf(
                        "Wearing appropriate protective gear (gloves/goggles) and inspecting tools before use",
                        "Operating equipment without teacher supervision",
                        "Leaving sharp tools scattered on the floor",
                        "Ignoring equipment load limits"
                    ),
                    correctAnswerIndex = 0,
                    explanation = "Safety standards in JSS require personal protective equipment (PPE) and tool inspection."
                )
            )

            else -> listOf(
                QuizQuestion(
                    id = "kicd_up_q1_${System.currentTimeMillis()}",
                    questionText = "In KICD ${grade.title} curriculum for $strandTitle, how does container gardening help families living in urban areas?",
                    choices = listOf(
                        "It allows families to grow fresh indigenous vegetables in recycled sacks or plastic tins",
                        "It requires buying expensive tractors",
                        "It stops plants from receiving rainwater",
                        "It works only in forest reserves"
                    ),
                    correctAnswerIndex = 0,
                    explanation = "Container gardening uses small spaces and recycled sacks/tins to grow fresh vegetables like sukuma wiki."
                ),
                QuizQuestion(
                    id = "kicd_up_q2_${System.currentTimeMillis()}",
                    questionText = "How do we record evidence for our CBC digital portfolio in $strandTitle?",
                    choices = listOf(
                        "By taking photographs/videos of our completed project and writing brief reflection notes",
                        "By deleting all our school work",
                        "By copying directly without doing the task",
                        "By refusing to upload task evidence"
                    ),
                    correctAnswerIndex = 0,
                    explanation = "Digital literacy in CBC involves capturing project photos/videos and uploading learner reflections."
                )
            )
        }

        onResult(questions)
    }
}