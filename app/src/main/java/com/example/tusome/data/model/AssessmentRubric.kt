package com.example.tusome.data.model

enum class PerformanceLevel(val code: String, val label: String) {
    EXCEEDS("EE", "Exceeds Expectations"),
    MEETS("ME", "Meets Expectations"),
    APPROACHES("AE", "Approaches Expectations"),
    BELOW("BE", "Below Expectations")
}

data class AssessmentRubric(
    val id: String,
    val subStrandId: String,
    val criteria: String,
    val exceedsDescription: String,
    val meetsDescription: String,
    val approachesDescription: String,
    val belowDescription: String,
    val learnerLevel: PerformanceLevel? = PerformanceLevel.MEETS,
    val teacherComment: String = "Good effort shown in demonstrating practical skills during group activity."
)