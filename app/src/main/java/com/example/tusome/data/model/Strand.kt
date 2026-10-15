package com.example.tusome.data.model

data class Strand(
    val id: String,
    val learningAreaId: String,
    val strandNumber: Int,
    val title: String,
    val description: String,
    val coreCompetencies: List<String>,
    val subStrands: List<SubStrand>,
    val isCompleted: Boolean = false
)