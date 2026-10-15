package com.example.tusome

import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.model.LearningArea
import com.example.tusome.data.repository.selectFeaturedLearningArea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurriculumSelectionTest {
    private fun area(id: String, title: String) = LearningArea(
        id = id,
        title = title,
        kiswahiliTitle = title,
        code = id.uppercase(),
        gradeLevel = GradeLevel.GRADE_4,
        strandsCount = 0,
        subStrandsCount = 0,
        progressPercent = 0,
        iconResId = 0,
        bgColorHex = "#FFFFFF",
        iconColorHex = "#000000"
    )

    @Test
    fun prefersAgricultureOrNutritionWhenPublished() {
        val mathematics = area("mathematics", "Mathematics")
        val agriculture = area("agriculture_nutrition", "Agriculture and Nutrition")

        assertEquals(agriculture, selectFeaturedLearningArea(listOf(mathematics, agriculture)))
    }

    @Test
    fun fallsBackToFirstPublishedAreaWhenAgricultureIsMissing() {
        val first = area("english", "English")
        val second = area("science", "Science and Technology")

        assertEquals(first, selectFeaturedLearningArea(listOf(first, second)))
    }

    @Test
    fun returnsNullWhenNoPublishedAreasExist() {
        assertNull(selectFeaturedLearningArea(emptyList()))
    }
}
