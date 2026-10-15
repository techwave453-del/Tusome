package com.example.tusome.data.model

data class LearnerProfile(
    val name: String,
    val admissionNo: String,
    val schoolName: String,
    val subCounty: String,
    val county: String,
    val currentGrade: GradeLevel,
    val totalStrandsCompleted: Int,
    val overallPerformance: PerformanceLevel,
    val attendanceRatePercent: Int,
    val parentGuardianContact: String
)