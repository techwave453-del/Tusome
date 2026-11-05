package com.example.tusome.ui

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.model.LearningContentGenerationRequest
import com.example.tusome.data.model.LearningContentBlock
import com.example.tusome.data.model.LearningContentBlockType
import com.example.tusome.data.model.LearningResourceSection
import com.example.tusome.data.model.LearningResourceReviewDecision
import com.example.tusome.data.model.LearningResourceType
import com.example.tusome.data.model.Strand
import com.example.tusome.data.model.LearningArea
import com.example.tusome.data.repository.LearningResourceWorkflowRepository
import com.example.tusome.data.repository.SupabaseAuthRepository
import com.example.tusome.data.repository.SupabaseCurriculumRepository
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject

private data class LearningLanguageOption(val label: String, val code: String) {
    override fun toString(): String = label
}

private data class EditableBlockInput(
    val id: String,
    val type: LearningContentBlockType,
    val textInput: EditText,
    val itemsInput: EditText?,
    val mediaUrl: String?,
    val caption: String?
)

private data class EditableSectionInput(
    val id: String,
    val titleInput: EditText,
    val descriptionInput: EditText,
    val blocks: List<EditableBlockInput>
)

/**
 * Teacher and curriculum-editor workspace for generating, reviewing and publishing
 * supplementary CBC resources. The AI service and all writes remain server-side.
 */
class LearningContentActivity : AppCompatActivity() {

    private lateinit var gradeSpinner: Spinner
    private lateinit var areaSpinner: Spinner
    private lateinit var strandSpinner: Spinner
    private lateinit var subStrandSpinner: Spinner
    private lateinit var typeSpinner: Spinner
    private lateinit var languageSpinner: Spinner
    private lateinit var minutesInput: EditText
    private lateinit var contextInput: EditText
    private lateinit var statusText: TextView
    private lateinit var generateButton: Button
    private lateinit var submitButton: Button
    private lateinit var previewContainer: LinearLayout
    private lateinit var myResourcesContainer: LinearLayout
    private lateinit var reviewQueueContainer: LinearLayout

    private val grades = GradeLevel.values().toList()
    private val resourceTypes = LearningResourceType.values().toList()
    private val languages = listOf(
        LearningLanguageOption("English", "en"),
        LearningLanguageOption("Kiswahili", "sw"),
        LearningLanguageOption("Kikamba", "kam")
    )
    private var learningAreas: List<LearningArea> = emptyList()
    private var allStrands: List<Strand> = emptyList()
    private var visibleStrands: List<Strand> = emptyList()
    private var isLoadingCurriculum = false
    private var currentDraftId: String? = null
    private var isCurriculumEditor = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = SupabaseAuthRepository.currentSession(this)
        val roles = session?.roles.orEmpty()
        val canManageContent = roles.any {
            it == "teacher" || it == "curriculum_editor" || it == "platform_admin"
        }
        if (session == null || !canManageContent) {
            Toast.makeText(this, "Your account does not have learning-content permissions.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        isCurriculumEditor = roles.any { it == "curriculum_editor" || it == "platform_admin" }

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(28))
            setBackgroundColor(Color.rgb(246, 249, 246))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(page)
        }

        page.addView(titleText("CBC learning content", 24f), params())
        page.addView(messageText(
            "Generate supplementary lessons and activities from published CBC outcomes. " +
                "Every generated resource remains a draft until a curriculum editor approves and publishes it."
        ), margins(top = 6, bottom = 16))

        section(page, "1. Choose the curriculum node")
        gradeSpinner = spinner()
        areaSpinner = spinner()
        strandSpinner = spinner()
        subStrandSpinner = spinner()
        addLabeledSpinner(page, "Grade", gradeSpinner)
        addLabeledSpinner(page, "Learning area", areaSpinner)
        addLabeledSpinner(page, "Strand", strandSpinner)
        addLabeledSpinner(page, "Sub-strand", subStrandSpinner)

        section(page, "2. Configure the resource")
        typeSpinner = spinner()
        typeSpinner.adapter = adapter(resourceTypes.map { it.displayName })
        addLabeledSpinner(page, "Resource type", typeSpinner)

        languageSpinner = spinner()
        languageSpinner.adapter = adapter(languages.map { it.label })
        addLabeledSpinner(page, "Language", languageSpinner)

        minutesInput = field("Estimated learning time (minutes)").apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText("30")
        }
        contextInput = field("Local context or teaching instructions (optional)", multiline = true)
        page.addView(minutesInput, margins(top = 8))
        page.addView(contextInput, margins(top = 8))

        generateButton = actionButton("Generate learning resource")
        page.addView(generateButton, margins(top = 12))
        statusText = messageText("Loading published CBC curriculum…")
        page.addView(statusText, margins(top = 10, bottom = 10))

        section(page, "Generated draft")
        previewContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(previewContainer, margins(top = 6))
        submitButton = actionButton("Submit draft for review").apply {
            visibility = View.GONE
        }
        page.addView(submitButton, margins(top = 10, bottom = 18))

        section(page, "My resources")
        myResourcesContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(myResourcesContainer, margins(top = 6, bottom = 18))

        if (isCurriculumEditor) {
            section(page, "Resources awaiting review")
            reviewQueueContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            page.addView(reviewQueueContainer, margins(top = 6, bottom = 18))
        } else {
            reviewQueueContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        }

        page.addView(Button(this).apply {
            text = "Back to learning"
            isAllCaps = false
            setOnClickListener { finish() }
        }, margins(top = 12))

        setContentView(scroll)
        setupSelectors(session.gradeId)
        generateButton.setOnClickListener { generateResource() }
        submitButton.setOnClickListener { submitCurrentDraft() }
        loadMyResources()
        if (isCurriculumEditor) loadReviewQueue()
    }

    private fun setupSelectors(profileGradeId: String?) {
        gradeSpinner.adapter = adapter(grades.map { it.title })
        val profileIndex = grades.indexOfFirst {
            it.code.equals(profileGradeId, ignoreCase = true)
        }
        gradeSpinner.setSelection(profileIndex.takeIf { it >= 0 } ?: grades.indexOf(GradeLevel.GRADE_4))

        gradeSpinner.onItemSelectedListener = selectionListener { position ->
            if (!isLoadingCurriculum) {
                loadCurriculum(grades.getOrNull(position) ?: GradeLevel.GRADE_4)
            }
        }
        areaSpinner.onItemSelectedListener = selectionListener {
            if (!isLoadingCurriculum) updateStrandsForSelectedArea()
        }
        strandSpinner.onItemSelectedListener = selectionListener {
            if (!isLoadingCurriculum) updateSubStrandsForSelectedStrand()
        }
        loadCurriculum(grades.getOrNull(gradeSpinner.selectedItemPosition) ?: GradeLevel.GRADE_4)
    }

    private fun loadCurriculum(grade: GradeLevel) {
        isLoadingCurriculum = true
        statusText.text = "Loading ${grade.title} curriculum…"
        learningAreas = emptyList()
        allStrands = emptyList()
        visibleStrands = emptyList()
        setSpinnerItems(areaSpinner, emptyList())
        setSpinnerItems(strandSpinner, emptyList())
        setSpinnerItems(subStrandSpinner, emptyList())

        SupabaseCurriculumRepository.fetchLearningAreas(grade) { areasResult ->
            if (isFinishing || isDestroyed || gradeSpinner.selectedItemPosition < 0 ||
                grades.getOrNull(gradeSpinner.selectedItemPosition) != grade) return@fetchLearningAreas

            areasResult.onFailure { error ->
                isLoadingCurriculum = false
                statusText.text = error.message ?: "Could not load published learning areas."
            }.onSuccess { areas ->
                learningAreas = areas
                setSpinnerItems(areaSpinner, areas.map { it.title })
                SupabaseCurriculumRepository.fetchStrands(grade) { strandsResult ->
                    if (isFinishing || isDestroyed ||
                        grades.getOrNull(gradeSpinner.selectedItemPosition) != grade) return@fetchStrands
                    isLoadingCurriculum = false
                    strandsResult.onFailure { error ->
                        statusText.text = error.message ?: "Could not load published strands."
                    }.onSuccess { strands ->
                        allStrands = strands
                        statusText.text = if (areas.isEmpty()) {
                            "No published learning areas are available for ${grade.title}."
                        } else {
                            "Choose a sub-strand to generate a resource aligned to its published learning outcomes."
                        }
                        updateStrandsForSelectedArea()
                    }
                }
            }
        }
    }

    private fun updateStrandsForSelectedArea() {
        val area = learningAreas.getOrNull(areaSpinner.selectedItemPosition)
        visibleStrands = if (area == null) emptyList() else
            allStrands.filter { it.learningAreaId == area.id }
        setSpinnerItems(strandSpinner, visibleStrands.map { it.title })
        updateSubStrandsForSelectedStrand()
    }

    private fun updateSubStrandsForSelectedStrand() {
        val strand = visibleStrands.getOrNull(strandSpinner.selectedItemPosition)
        val subStrands = strand?.subStrands.orEmpty()
        setSpinnerItems(subStrandSpinner, subStrands.map { it.title })
    }

    private fun generateResource() {
        val grade = grades.getOrNull(gradeSpinner.selectedItemPosition)
        val area = learningAreas.getOrNull(areaSpinner.selectedItemPosition)
        val strand = visibleStrands.getOrNull(strandSpinner.selectedItemPosition)
        val subStrand = strand?.subStrands?.getOrNull(subStrandSpinner.selectedItemPosition)
        val type = resourceTypes.getOrNull(typeSpinner.selectedItemPosition)
        val language = languages.getOrNull(languageSpinner.selectedItemPosition)
        val minutes = minutesInput.text.toString().toIntOrNull()

        if (grade == null || area == null || strand == null || subStrand == null || type == null || language == null) {
            statusText.text = "Select a complete grade, learning area, strand and sub-strand first."
            return
        }
        if (subStrand.specificLearningOutcomes.isEmpty()) {
            statusText.text = "This sub-strand has no published learning outcomes. The curriculum editor must add them first."
            return
        }
        if (minutes == null || minutes !in 5..240) {
            minutesInput.error = "Enter a duration between 5 and 240 minutes."
            return
        }

        val request = LearningContentGenerationRequest(
            gradeCode = grade.code,
            learningAreaId = area.id,
            strandId = strand.id,
            subStrandId = subStrand.id,
            resourceType = type,
            languageCode = language.code,
            learningOutcomes = subStrand.specificLearningOutcomes,
            learnerAgeRange = grade.title,
            estimatedMinutes = minutes,
            difficulty = "age_appropriate",
            localContext = contextInput.text.toString().trim().ifBlank { null }
        )

        generateButton.isEnabled = false
        submitButton.visibility = View.GONE
        currentDraftId = null
        previewContainer.removeAllViews()
        statusText.text = "Generating a CBC-aligned draft. This may take a short while…"

        LearningResourceWorkflowRepository.generateDraft(this, request) { result ->
            generateButton.isEnabled = true
            result.onSuccess { response ->
                val resource = response.optJSONObject("resource")
                if (resource == null) {
                    statusText.text = "The server did not return the generated draft."
                    return@onSuccess
                }
                currentDraftId = resource.optString("id").takeIf { it.isNotBlank() }
                statusText.text = "Draft generated successfully. It is not visible to learners until reviewed and published."
                renderResourcePreview(resource)
                submitButton.visibility = if (currentDraftId != null) View.VISIBLE else View.GONE
                loadMyResources()
            }.onFailure { error ->
                statusText.text = error.message ?: "Resource generation failed. Check your connection and try again."
            }
        }
    }

    private fun submitCurrentDraft() {
        val resourceId = currentDraftId ?: return
        submitButton.isEnabled = false
        LearningResourceWorkflowRepository.submitForReview(this, resourceId) { result ->
            submitButton.isEnabled = true
            result.onSuccess {
                statusText.text = "Submitted for curriculum review. Learners cannot see it yet."
                submitButton.visibility = View.GONE
                loadMyResources()
                if (isCurriculumEditor) loadReviewQueue()
            }.onFailure { error ->
                statusText.text = error.message ?: "The draft could not be submitted for review."
            }
        }
    }

    private fun loadMyResources() {
        myResourcesContainer.removeAllViews()
        myResourcesContainer.addView(messageText("Loading your resources…"))
        LearningResourceWorkflowRepository.listMyResources(this) { result ->
            myResourcesContainer.removeAllViews()
            result.onSuccess { response ->
                val rows = response.optJSONArray("resources") ?: JSONArray()
                if (rows.length() == 0) {
                    myResourcesContainer.addView(messageText("You have not generated or created any resources yet."))
                    return@onSuccess
                }
                for (index in 0 until rows.length()) {
                    val resource = rows.optJSONObject(index) ?: continue
                    val card = resourceCard(resource)
                    myResourcesContainer.addView(card, margins(bottom = 8))
                }
            }.onFailure { error ->
                myResourcesContainer.addView(messageText(error.message ?: "Could not load your resources."))
            }
        }
    }

    private fun loadReviewQueue() {
        reviewQueueContainer.removeAllViews()
        reviewQueueContainer.addView(messageText("Loading review queue…"))
        LearningResourceWorkflowRepository.listReviewQueue(this) { result ->
            reviewQueueContainer.removeAllViews()
            result.onSuccess { response ->
                val rows = response.optJSONArray("resources") ?: JSONArray()
                if (rows.length() == 0) {
                    reviewQueueContainer.addView(messageText("There are no resources awaiting review."))
                    return@onSuccess
                }
                for (index in 0 until rows.length()) {
                    val resource = rows.optJSONObject(index) ?: continue
                    val card = resourceCard(resource, showReview = true)
                    reviewQueueContainer.addView(card, margins(bottom = 8))
                }
            }.onFailure { error ->
                reviewQueueContainer.addView(messageText(error.message ?: "Could not load the review queue."))
            }
        }
    }

    private fun resourceCard(resource: JSONObject, showReview: Boolean = false): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.WHITE)
        }
        val title = resource.optString("title").ifBlank { "Untitled resource" }
        val status = resource.optString("status").replace('_', ' ')
        val type = resource.optString("resource_type").replace('_', ' ')
        card.addView(titleText(title, 16f), params())
        card.addView(messageText("$type • $status • v${resource.optInt("version", 1)}"), margins(top = 4))
        val feedback = resource.optString("review_feedback")
        if (feedback.isNotBlank()) card.addView(messageText("Review feedback: $feedback"), margins(top = 4))
        val isApproved = status == "approved"
        val openButton = actionButton(
            when {
                showReview && isApproved -> "Publish approved resource"
                showReview -> "Review resource"
                else -> "View resource"
            }
        ).apply {
            setOnClickListener {
                if (showReview && isApproved) {
                    publishApprovedResource(resource.optString("id"))
                } else if (showReview) {
                    showReviewDialog(resource)
                } else {
                    currentDraftId = resource.optString("id").takeIf { it.isNotBlank() }
                    renderResourcePreview(resource)
                    statusText.text = "Resource status: $status"
                    submitButton.visibility = if (status == "draft" || status == "rejected") View.VISIBLE else View.GONE
                }
            }
        }
        card.addView(openButton, margins(top = 8))
        if (!showReview && (status == "draft" || status == "rejected")) {
            card.addView(actionButton("Edit draft").apply {
                setOnClickListener {
                    currentDraftId = resource.optString("id").takeIf { it.isNotBlank() }
                    showEditDialog(resource)
                }
            }, margins(top = 6))
        }
        return card
    }

    private fun showEditDialog(resource: JSONObject) {
        val resourceId = resource.optString("id")
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        val titleInput = field("Resource title").apply { setText(resource.optString("title")) }
        val summaryInput = field("Resource summary", multiline = true).apply {
            setText(resource.optString("summary"))
        }
        form.addView(messageText("Title"), margins(bottom = 2))
        form.addView(titleInput, margins(bottom = 8))
        form.addView(messageText("Summary"), margins(bottom = 2))
        form.addView(summaryInput, margins(bottom = 10))

        val editableSections = mutableListOf<EditableSectionInput>()
        val sections = resource.optJSONObject("content")?.optJSONArray("sections") ?: JSONArray()
        for (sectionIndex in 0 until sections.length()) {
            val section = sections.optJSONObject(sectionIndex) ?: continue
            form.addView(titleText("Section ${sectionIndex + 1}", 15f), margins(top = 8, bottom = 4))
            val sectionTitle = field("Section title").apply { setText(section.optString("title")) }
            val sectionDescription = field("Section introduction", multiline = true).apply {
                setText(section.optString("description"))
            }
            form.addView(sectionTitle, margins(bottom = 6))
            form.addView(sectionDescription, margins(bottom = 8))

            val editableBlocks = mutableListOf<EditableBlockInput>()
            val blocks = section.optJSONArray("blocks") ?: JSONArray()
            for (blockIndex in 0 until blocks.length()) {
                val block = blocks.optJSONObject(blockIndex) ?: continue
                val blockType = LearningContentBlockType.fromKey(block.optString("type"))
                    ?: LearningContentBlockType.PARAGRAPH
                form.addView(messageText("Content block: ${blockType.key.replace('_', ' ')}"), margins(top = 4))
                val blockText = field("Block text", multiline = true).apply {
                    setText(block.optString("text"))
                }
                form.addView(blockText, margins(top = 3))
                val blockItems = block.optJSONArray("items") ?: JSONArray()
                val itemsInput = if (blockItems.length() > 0) {
                    val itemsField = field("List items (one per line)", multiline = true).apply {
                        setText((0 until blockItems.length()).joinToString("\n") { blockItems.optString(it) })
                    }
                    form.addView(itemsField, margins(top = 4))
                    itemsField
                } else null
                editableBlocks.add(
                    EditableBlockInput(
                        id = block.optString("id").ifBlank { "block-${sectionIndex + 1}-${blockIndex + 1}" },
                        type = blockType,
                        textInput = blockText,
                        itemsInput = itemsInput,
                        mediaUrl = block.optString("media_url").takeIf { it.isNotBlank() && it != "null" },
                        caption = block.optString("caption").takeIf { it.isNotBlank() && it != "null" }
                    )
                )
            }
            editableSections.add(
                EditableSectionInput(
                    id = section.optString("id").ifBlank { "section-${sectionIndex + 1}" },
                    titleInput = sectionTitle,
                    descriptionInput = sectionDescription,
                    blocks = editableBlocks
                )
            )
        }

        val scroll = ScrollView(this).apply {
            addView(form)
            isFillViewport = false
        }
        AlertDialog.Builder(this)
            .setTitle("Edit learning resource")
            .setView(scroll)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save draft") { _, _ ->
                val sectionsToSave = editableSections.map { section ->
                    LearningResourceSection(
                        id = section.id,
                        title = section.titleInput.text.toString().trim(),
                        description = section.descriptionInput.text.toString().trim(),
                        blocks = section.blocks.map { block ->
                            LearningContentBlock(
                                id = block.id,
                                type = block.type,
                                text = block.textInput.text.toString().trim(),
                                items = block.itemsInput?.text?.toString()?.lines()
                                    ?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty(),
                                mediaUrl = block.mediaUrl,
                                caption = block.caption
                            )
                        }
                    )
                }
                LearningResourceWorkflowRepository.updateDraft(
                    context = this,
                    resourceId = resourceId,
                    title = titleInput.text.toString(),
                    summary = summaryInput.text.toString(),
                    sections = sectionsToSave,
                    changeSummary = "Author revised the draft"
                ) { result ->
                    result.onSuccess { response ->
                        val updated = response.optJSONObject("resource")
                        if (updated != null) {
                            currentDraftId = resourceId
                            renderResourcePreview(updated)
                            submitButton.visibility = View.VISIBLE
                            statusText.text = "Draft updated. Submit it again when it is ready for review."
                        }
                        loadMyResources()
                    }.onFailure { error ->
                        Toast.makeText(this, error.message ?: "Draft could not be updated.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()
    }

    private fun publishApprovedResource(resourceId: String) {
        LearningResourceWorkflowRepository.publish(this, resourceId) { result ->
            result.onSuccess {
                Toast.makeText(this, "Resource published.", Toast.LENGTH_LONG).show()
                loadReviewQueue()
            }.onFailure { error ->
                Toast.makeText(this, error.message ?: "Resource could not be published.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showReviewDialog(resource: JSONObject) {
        val resourceId = resource.optString("id")
        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        details.addView(messageText(resource.optString("summary")), margins(bottom = 8))
        details.addView(messageText("Grade: ${resource.optString("grade_id")} • Language: ${resource.optString("language_code")}"), margins(bottom = 8))
        val sections = resource.optJSONObject("content")?.optJSONArray("sections") ?: JSONArray()
        for (sectionIndex in 0 until sections.length()) {
            val section = sections.optJSONObject(sectionIndex) ?: continue
            details.addView(titleText(section.optString("title"), 14f), margins(top = 8))
            val blocks = section.optJSONArray("blocks") ?: JSONArray()
            for (blockIndex in 0 until blocks.length()) {
                val block = blocks.optJSONObject(blockIndex) ?: continue
                val blockText = block.optString("text")
                if (blockText.isNotBlank()) details.addView(messageText(blockText), margins(top = 3))
                val items = block.optJSONArray("items") ?: JSONArray()
                for (itemIndex in 0 until items.length()) {
                    details.addView(messageText("• ${items.optString(itemIndex)}"), margins(top = 2))
                }
            }
        }
        val feedbackInput = EditText(this).apply {
            hint = "Review feedback (required when requesting changes or rejecting)"
            minLines = 3
            gravity = android.view.Gravity.TOP
            setText(resource.optString("review_feedback"))
        }
        details.addView(feedbackInput, params())
        val reviewScroll = ScrollView(this).apply { addView(details) }

        AlertDialog.Builder(this)
            .setTitle(resource.optString("title", "Review learning resource"))
            .setView(reviewScroll)
            .setNegativeButton("Reject") { _, _ ->
                reviewResource(resourceId, LearningResourceReviewDecision.REJECTED, feedbackInput.text.toString())
            }
            .setNeutralButton("Request changes") { _, _ ->
                reviewResource(resourceId, LearningResourceReviewDecision.CHANGES_REQUESTED, feedbackInput.text.toString())
            }
            .setPositiveButton("Approve & publish") { _, _ ->
                reviewResource(resourceId, LearningResourceReviewDecision.APPROVED, feedbackInput.text.toString(), publishAfterApproval = true)
            }
            .show()
    }

    private fun reviewResource(
        resourceId: String,
        decision: LearningResourceReviewDecision,
        feedback: String,
        publishAfterApproval: Boolean = false
    ) {
        LearningResourceWorkflowRepository.review(this, resourceId, decision, feedback) { result ->
            result.onSuccess {
                if (publishAfterApproval) {
                    LearningResourceWorkflowRepository.publish(this, resourceId) { publishResult ->
                        publishResult.onSuccess {
                            Toast.makeText(this, "Resource approved and published.", Toast.LENGTH_LONG).show()
                            loadReviewQueue()
                        }.onFailure { error ->
                            Toast.makeText(this, error.message ?: "Approved, but publishing failed.", Toast.LENGTH_LONG).show()
                            loadReviewQueue()
                        }
                    }
                } else {
                    val message = if (decision == LearningResourceReviewDecision.REJECTED) {
                        "Resource rejected."
                    } else {
                        "Changes requested. The author can revise the draft."
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    loadReviewQueue()
                }
            }.onFailure { error ->
                Toast.makeText(this, error.message ?: "The review decision could not be saved.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun renderResourcePreview(resource: JSONObject) {
        previewContainer.removeAllViews()
        previewContainer.addView(titleText(resource.optString("title"), 17f), margins(bottom = 4))
        previewContainer.addView(messageText(resource.optString("summary")), margins(bottom = 8))
        val content = resource.optJSONObject("content") ?: JSONObject()
        val sections = content.optJSONArray("sections") ?: JSONArray()
        for (sectionIndex in 0 until sections.length()) {
            val section = sections.optJSONObject(sectionIndex) ?: continue
            previewContainer.addView(titleText(section.optString("title"), 14f), margins(top = 8))
            val blocks = section.optJSONArray("blocks") ?: JSONArray()
            for (blockIndex in 0 until blocks.length()) {
                val block = blocks.optJSONObject(blockIndex) ?: continue
                val text = block.optString("text")
                if (text.isNotBlank()) previewContainer.addView(messageText(text), margins(top = 4))
                val items = block.optJSONArray("items") ?: JSONArray()
                for (itemIndex in 0 until items.length()) {
                    previewContainer.addView(messageText("• ${items.optString(itemIndex)}"), margins(top = 2))
                }
            }
        }
    }

    private fun section(parent: LinearLayout, label: String) {
        parent.addView(titleText(label, 19f), margins(top = 12, bottom = 4))
    }

    private fun addLabeledSpinner(parent: LinearLayout, label: String, spinner: Spinner) {
        parent.addView(messageText(label), margins(top = 8, bottom = 2))
        parent.addView(spinner, params())
    }

    private fun spinner() = Spinner(this).apply {
        this.adapter = this@LearningContentActivity.adapter(emptyList())
    }

    private fun adapter(items: List<String>) =
        ArrayAdapter(this, android.R.layout.simple_spinner_item, items).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

    private fun setSpinnerItems(spinner: Spinner, items: List<String>) {
        spinner.adapter = adapter(items)
    }

    private fun selectionListener(onSelected: (Int) -> Unit) =
        object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) = onSelected(position)

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

    private fun field(hintText: String, multiline: Boolean = false) = TextInputEditText(this).apply {
        hint = hintText
        textSize = 14f
        setTextColor(Color.rgb(35, 45, 36))
        setHintTextColor(Color.GRAY)
        setPadding(dp(12), dp(10), dp(12), dp(10))
        if (multiline) {
            minLines = 3
            gravity = android.view.Gravity.TOP
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        } else {
            setSingleLine(true)
        }
    }

    private fun titleText(value: String, size: Float) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(Color.rgb(39, 75, 41))
        setTypeface(typeface, Typeface.BOLD)
    }

    private fun messageText(value: String) = TextView(this).apply {
        text = value
        textSize = 13f
        setTextColor(Color.DKGRAY)
    }

    private fun actionButton(label: String) = Button(this).apply {
        text = label
        isAllCaps = false
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(46, 125, 50))
    }

    private fun params() = LinearLayout.LayoutParams(-1, -2)

    private fun margins(top: Int = 0, bottom: Int = 0) =
        LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(top)
            bottomMargin = dp(bottom)
        }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
