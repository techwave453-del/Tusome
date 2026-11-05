package com.example.tusome.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningResourceTest {

    @Test
    fun resourceTypesMapToStableApiKeys() {
        assertEquals(
            LearningResourceType.LESSON_NOTES,
            LearningResourceType.fromKey("lesson_notes")
        )
        assertEquals(
            LearningResourceType.MARKING_SCHEME,
            LearningResourceType.fromKey("MARKING_SCHEME")
        )
        assertNull(LearningResourceType.fromKey("invented_type"))
    }

    @Test
    fun onlyPublishedResourcesAreAvailableToLearners() {
        val draft = resource(status = LearningResourceStatus.DRAFT)
        val published = resource(status = LearningResourceStatus.PUBLISHED)
        val teacherOnly = resource(
            status = LearningResourceStatus.PUBLISHED,
            type = LearningResourceType.MARKING_SCHEME
        )

        assertFalse(draft.isAvailableToLearners)
        assertTrue(published.isAvailableToLearners)
        assertFalse(teacherOnly.isAvailableToLearners)
    }

    @Test
    fun usableContentRequiresAtLeastOneNonEmptyBlock() {
        val empty = resource(
            sections = listOf(
                LearningResourceSection(
                    id = "section-1",
                    title = "Introduction",
                    blocks = listOf(LearningContentBlock(id = "block-1", type = LearningContentBlockType.PARAGRAPH))
                )
            )
        )
        val populated = resource(
            sections = listOf(
                LearningResourceSection(
                    id = "section-1",
                    title = "Introduction",
                    blocks = listOf(
                        LearningContentBlock(
                            id = "block-1",
                            type = LearningContentBlockType.PARAGRAPH,
                            text = "Plants need water and sunlight."
                        )
                    )
                )
            )
        )

        assertFalse(empty.hasUsableContent)
        assertTrue(populated.hasUsableContent)
    }

    private fun resource(
        status: LearningResourceStatus = LearningResourceStatus.DRAFT,
        type: LearningResourceType = LearningResourceType.LESSON_NOTES,
        sections: List<LearningResourceSection> = listOf(
            LearningResourceSection(
                id = "section-1",
                title = "Introduction",
                blocks = listOf(
                    LearningContentBlock(
                        id = "block-1",
                        type = LearningContentBlockType.PARAGRAPH,
                        text = "A sample learning paragraph."
                    )
                )
            )
        )
    ) = LearningResource(
        id = "resource-1",
        curriculumNodeId = "sub-strand-1",
        gradeCode = "G4",
        learningAreaId = "learning-area-1",
        strandId = "strand-1",
        subStrandId = "sub-strand-1",
        type = type,
        title = "Sample lesson",
        summary = "A short sample lesson.",
        languageCode = "en",
        status = status,
        source = LearningResourceSource.AI_GENERATED,
        version = 1,
        estimatedMinutes = 20,
        learningOutcomes = listOf("Describe the topic."),
        sections = sections,
        createdAt = null,
        updatedAt = null
    )
}
