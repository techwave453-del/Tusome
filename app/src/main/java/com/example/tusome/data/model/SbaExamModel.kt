package com.example.tusome.data.model

data class SubQuestion(
    val label: String, // e.g., "a)", "(i)"
    val questionText: String,
    val allocatedMarks: Int,
    val markingSchemeAnswer: String,
    val markBreakdown: String
)

data class ExamQuestion(
    val questionNumber: Int,
    val scenarioOrPrompt: String,
    val totalMarks: Int,
    val subQuestions: List<SubQuestion>
)

data class ExamSection(
    val sectionTitle: String, // e.g., "SECTION A: THEORY (30 Marks)"
    val instructions: String,
    val questions: List<ExamQuestion>
)

data class SbaExamPaper(
    val id: String,
    val title: String, // e.g., "THE 47 COUNTIES C1 SCHOOLS' JOINT NATIONAL SBA ASSESSMENT"
    val subjectTitle: String,
    val gradeLevel: GradeLevel,
    val termAndYear: String, // e.g., "TERM 3 ENDTERM ASSESSMENT 2026"
    val timeAllowedMinutes: Int,
    val totalMarks: Int,
    val sections: List<ExamSection>
)