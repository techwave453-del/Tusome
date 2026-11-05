package com.example.tusome.data.model

data class QuizQuestion(
    val id: String,
    val questionText: String,
    val choices: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String
)

data class Quiz(
    val id: String,
    val strandId: String,
    val title: String,
    val questions: List<QuizQuestion>
)

data class QuizResult(
    val quizId: String,
    val strandId: String,
    val score: Int,
    val totalQuestions: Int,
    val percentage: Int,
    val performanceLevel: PerformanceLevel,
    val timestamp: Long
)