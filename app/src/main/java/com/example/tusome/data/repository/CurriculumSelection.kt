package com.example.tusome.data.repository

import com.example.tusome.data.model.LearningArea

/**
 * Selects a sensible featured learning area from the published catalogue.
 * Agriculture/Nutrition is preferred when present; otherwise use the first
 * published area, and return null for an empty catalogue.
 */
internal fun selectFeaturedLearningArea(areas: List<LearningArea>): LearningArea? =
    areas.firstOrNull {
        it.title.contains("agriculture", ignoreCase = true) ||
            it.title.contains("nutrition", ignoreCase = true)
    } ?: areas.firstOrNull()
