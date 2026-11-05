package com.example.tusome.data.model

data class LessonSection(
    val sectionHeading: String,
    val explanationText: String,
    val bulletPoints: List<String> = emptyList(),
    val localKenyanExample: String = ""
)

data class LessonContent(
    val id: String,
    val subStrandId: String,
    val lessonTitle: String,
    val subjectTitle: String,
    val gradeLevel: GradeLevel,
    val introduction: String,
    val keyVocabulary: Map<String, String>,
    val mainSections: List<LessonSection>,
    val stepByStepGuide: List<String>,
    val summaryTakeaways: List<String>
)