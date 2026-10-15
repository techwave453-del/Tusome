package com.example.tusome.data.model

data class CoreCompetency(
    val id: String,
    val name: String,
    val kiswahiliName: String,
    val description: String,
    val progressPercent: Int,
    val strandsAppliedCount: Int,
    val iconResId: Int
)