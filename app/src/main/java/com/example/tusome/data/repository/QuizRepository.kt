package com.example.tusome.data.repository

import android.content.Context
import com.example.tusome.data.model.*

object QuizRepository {

    fun getQuizForStrand(strandId: String, grade: GradeLevel = GradeLevel.GRADE_4): Quiz {
        return when (strandId) {
            "agri_s1" -> Quiz(
                id = "quiz_agri_s1",
                strandId = strandId,
                title = "Conservation of Resources Quiz",
                questions = listOf(
                    QuizQuestion(
                        id = "q1",
                        questionText = "Which of the following organic materials is best suited for building a compost heap?",
                        choices = listOf(
                            "Dry leaves, vegetable peels, and wood ash",
                            "Broken plastic bottles and glass containers",
                            "Used polythene bags and synthetic rubber",
                            "Metal scraps and iron nails"
                        ),
                        correctAnswerIndex = 0,
                        explanation = "Organic waste such as leaves, vegetable peels, and wood ash decompose naturally into fertile compost manure."
                    ),
                    QuizQuestion(
                        id = "q2",
                        questionText = "How does drip irrigation help conserve water in crop production?",
                        choices = listOf(
                            "It sprays water high into the air",
                            "It directs water directly to the roots, reducing evaporation",
                            "It floods the whole farm at once",
                            "It works only when it rains heavily"
                        ),
                        correctAnswerIndex = 1,
                        explanation = "Drip irrigation delivers water directly to plant root zones, minimizing water waste and evaporation."
                    ),
                    QuizQuestion(
                        id = "q3",
                        questionText = "Why is mulching important for soil conservation?",
                        choices = listOf(
                            "It prevents sunlight from ever reaching the farm",
                            "It retains soil moisture and reduces soil erosion caused by rain",
                            "It turns crops yellow",
                            "It replaces crop harvesting"
                        ),
                        correctAnswerIndex = 1,
                        explanation = "Mulching protects soil topsoil from heavy raindrops and retains soil moisture."
                    )
                )
            )

            "mth_s1" -> Quiz(
                id = "quiz_mth_s1",
                strandId = strandId,
                title = "Numbers & Place Value Quiz",
                questions = listOf(
                    QuizQuestion(
                        id = "q1_m",
                        questionText = "In the number 7,842, what is the place value of the digit 8?",
                        choices = listOf("Ones", "Tens", "Hundreds", "Thousands"),
                        correctAnswerIndex = 2,
                        explanation = "The digit 8 is in the Hundreds position (8 x 100 = 800)."
                    ),
                    QuizQuestion(
                        id = "q2_m",
                        questionText = "What is 4,500 + 3,200?",
                        choices = listOf("7,700", "7,500", "8,000", "6,700"),
                        correctAnswerIndex = 0,
                        explanation = "4,500 + 3,200 = 7,700."
                    )
                )
            )

            else -> Quiz(
                id = "quiz_gen",
                strandId = strandId,
                title = "${grade.title} CBC General Assessment Quiz",
                questions = listOf(
                    QuizQuestion(
                        id = "q_g1",
                        questionText = "Which of the following is one of the 7 Kenya CBC Core Competencies?",
                        choices = listOf(
                            "Critical Thinking & Problem Solving",
                            "Driving Motor Vehicles",
                            "Heavy Lifting",
                            "Watching Television"
                        ),
                        correctAnswerIndex = 0,
                        explanation = "Critical Thinking & Problem Solving is one of the 7 core competencies in Kenya CBC."
                    ),
                    QuizQuestion(
                        id = "q_g2",
                        questionText = "What is the primary goal of formative assessment in CBC?",
                        choices = listOf(
                            "To grade and punish learners",
                            "To track learner progress and offer constructive feedback",
                            "To end the school year early",
                            "To eliminate practical activities"
                        ),
                        correctAnswerIndex = 1,
                        explanation = "Formative assessment tracks continuous learner growth and supports learning to learn."
                    )
                )
            )
        }
    }

    fun saveQuizResult(context: Context, result: QuizResult) {
        val prefs = context.getSharedPreferences("tusome_quiz_results", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("score_${result.strandId}", result.score)
            putInt("total_${result.strandId}", result.totalQuestions)
            putInt("percent_${result.strandId}", result.percentage)
            putString("level_${result.strandId}", result.performanceLevel.name)
            putLong("time_${result.strandId}", result.timestamp)
            apply()
        }
    }

    fun getQuizResultForStrand(context: Context, strandId: String): QuizResult? {
        val prefs = context.getSharedPreferences("tusome_quiz_results", Context.MODE_PRIVATE)
        if (!prefs.contains("score_$strandId")) return null

        val score = prefs.getInt("score_$strandId", 0)
        val total = prefs.getInt("total_$strandId", 1)
        val percent = prefs.getInt("percent_$strandId", 0)
        val levelStr = prefs.getString("level_$strandId", PerformanceLevel.MEETS.name)
        val level = try { PerformanceLevel.valueOf(levelStr!!) } catch (_: Exception) { PerformanceLevel.MEETS }
        val time = prefs.getLong("time_$strandId", System.currentTimeMillis())

        return QuizResult(
            quizId = "quiz_$strandId",
            strandId = strandId,
            score = score,
            totalQuestions = total,
            percentage = percent,
            performanceLevel = level,
            timestamp = time
        )
    }
}