package com.example.tusome.data.model

data class LearningArea(
    val id: String,
    val title: String,
    val kiswahiliTitle: String,
    val code: String,
    val gradeLevel: GradeLevel,
    val strandsCount: Int,
    val subStrandsCount: Int,
    val progressPercent: Int,
    val iconResId: Int,
    val bgColorHex: String,
    val iconColorHex: String
)