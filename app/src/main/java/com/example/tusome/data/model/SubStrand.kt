package com.example.tusome.data.model

data class SubStrand(
    val id: String,
    val strandId: String,
    val subStrandNumber: String,
    val title: String,
    val specificLearningOutcomes: List<String>,
    val keyInquiryQuestions: List<String>,
    val suggestedExperiences: List<String>,
    val digitalTaskPrompt: String,
    val rubric: AssessmentRubric
)