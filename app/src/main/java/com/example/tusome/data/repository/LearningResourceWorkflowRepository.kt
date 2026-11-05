package com.example.tusome.data.repository

import android.content.Context
import com.example.tusome.data.model.LearningContentBlock
import com.example.tusome.data.model.LearningResourceDraft
import com.example.tusome.data.model.LearningResourceReviewDecision
import org.json.JSONArray
import org.json.JSONObject

/**
 * Client facade for the authenticated, server-authorized learning-resource workflow.
 * All permission checks and database writes happen in the Edge Function.
 */
object LearningResourceWorkflowRepository {

    /** Generates a CBC-aligned resource on the server and saves it as a reviewable draft. */
    fun generateDraft(
        context: Context,
        request: com.example.tusome.data.model.LearningContentGenerationRequest,
        onResult: (Result<JSONObject>) -> Unit
    ) {
        val payload = JSONObject()
            .put("grade_id", request.gradeCode.trim().lowercase())
            .put("learning_area_id", request.learningAreaId.trim())
            .put("strand_id", request.strandId.trim())
            .put("sub_strand_id", request.subStrandId.trim())
            .put("resource_type", request.resourceType.key)
            .put("language_code", request.languageCode.trim().lowercase())
            .put("estimated_minutes", request.estimatedMinutes ?: 30)
            .put("difficulty", request.difficulty ?: "age_appropriate")
            .put("local_context", request.localContext ?: "")
            .put("additional_instructions", request.additionalInstructions ?: "")
        SupabaseAuthRepository.generateLearningResource(context, payload, onResult)
    }

    fun createDraft(
        context: Context,
        draft: LearningResourceDraft,
        onResult: (Result<JSONObject>) -> Unit
    ) {
        val payload = runCatching {
            require(draft.gradeId.isNotBlank()) { "Select a grade." }
            require(draft.learningAreaId.isNotBlank()) { "Select a learning area." }
            require(draft.strandId.isNotBlank()) { "Select a curriculum strand." }
            require(draft.subStrandId.isNotBlank()) { "Select a curriculum sub-strand." }
            require(draft.title.trim().length in 2..180) { "The resource title must be 2–180 characters." }
            require(draft.languageCode.matches(Regex("^[a-z]{2,3}(-[A-Za-z0-9]{2,8})?$"))) {
                "Choose a valid language code."
            }
            require(draft.sections.isNotEmpty()) { "Add at least one content section." }
            draftToJson(draft)
        }.getOrElse {
            onResult(Result.failure(it))
            return
        }
        SupabaseAuthRepository.manageLearningResources(context, payload, onResult)
    }

    fun submitForReview(
        context: Context,
        resourceId: String,
        onResult: (Result<JSONObject>) -> Unit
    ) = sendAction(context, "submit_for_review", resourceId, onResult)

    fun review(
        context: Context,
        resourceId: String,
        decision: LearningResourceReviewDecision,
        feedback: String = "",
        onResult: (Result<JSONObject>) -> Unit
    ) {
        val payload = JSONObject()
            .put("action", "review")
            .put("resource_id", resourceId)
            .put("decision", decision.key)
            .put("feedback", feedback.trim())
        SupabaseAuthRepository.manageLearningResources(context, payload, onResult)
    }

    fun publish(
        context: Context,
        resourceId: String,
        onResult: (Result<JSONObject>) -> Unit
    ) = sendAction(context, "publish", resourceId, onResult)

    fun withdraw(
        context: Context,
        resourceId: String,
        onResult: (Result<JSONObject>) -> Unit
    ) = sendAction(context, "withdraw", resourceId, onResult)

    private fun sendAction(
        context: Context,
        action: String,
        resourceId: String,
        onResult: (Result<JSONObject>) -> Unit
    ) {
        if (resourceId.isBlank()) {
            onResult(Result.failure(IllegalArgumentException("A resource ID is required.")))
            return
        }
        val payload = JSONObject()
            .put("action", action)
            .put("resource_id", resourceId)
        SupabaseAuthRepository.manageLearningResources(context, payload, onResult)
    }

    private fun draftToJson(draft: LearningResourceDraft): JSONObject {
        val outcomes = JSONArray()
        draft.learningOutcomes.forEach { outcome ->
            if (outcome.isNotBlank()) outcomes.put(outcome.trim())
        }

        val contentSections = JSONArray()
        draft.sections.forEach { section ->
            val blocks = JSONArray()
            section.blocks.forEach { block -> blocks.put(blockToJson(block)) }
            contentSections.put(
                JSONObject()
                    .put("id", section.id)
                    .put("title", section.title)
                    .put("description", section.description)
                    .put("blocks", blocks)
            )
        }

        val content = JSONObject().put("sections", contentSections)
        return JSONObject()
            .put("action", "create_draft")
            .put("grade_id", draft.gradeId.trim().lowercase())
            .put("learning_area_id", draft.learningAreaId.trim())
            .put("strand_id", draft.strandId.trim())
            .put("sub_strand_id", draft.subStrandId.trim())
            .put("resource_type", draft.type.key)
            .put("title", draft.title.trim())
            .put("summary", draft.summary.trim())
            .put("language_code", draft.languageCode.trim().lowercase())
            .put("learning_outcomes", outcomes)
            .put("estimated_minutes", draft.estimatedMinutes)
            .put("source", draft.source.key)
            .put("content", content)
    }

    private fun blockToJson(block: LearningContentBlock): JSONObject {
        val items = JSONArray()
        block.items.forEach { item -> items.put(item) }
        return JSONObject()
            .put("id", block.id)
            .put("type", block.type.key)
            .put("text", block.text)
            .put("items", items)
            .put("media_url", block.mediaUrl)
            .put("caption", block.caption)
    }
}
