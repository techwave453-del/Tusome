package com.example.tusome.ui

import android.os.Bundle
import android.graphics.Typeface
import android.util.TypedValue
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.example.tusome.R
import com.example.tusome.data.local.ActivityProgressManager
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.model.LearningContentBlock
import com.example.tusome.data.model.LearningContentBlockType
import com.example.tusome.data.model.LearningResource
import com.example.tusome.data.model.PerformanceLevel
import com.example.tusome.data.model.Strand
import com.example.tusome.data.repository.CbcRepository
import com.example.tusome.data.repository.LearningResourceRepository
import com.example.tusome.data.repository.SupabaseCurriculumRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup

class StrandDetailActivity : AppCompatActivity() {

    private lateinit var progressManager: ActivityProgressManager
    private lateinit var currentStrands: List<Strand>
    private lateinit var containerSubstrands: LinearLayout
    private lateinit var scrollStrandDetail: NestedScrollView
    private lateinit var tvStrandPosition: TextView
    private lateinit var tvStrandProgress: TextView
    private lateinit var progressStrand: ProgressBar
    private lateinit var btnPreviousStrand: Button
    private lateinit var btnNextStrand: Button
    private var currentStrandIndex: Int = 0
    private var currentGrade: GradeLevel = GradeLevel.GRADE_4
    private var usesLiveCurriculum: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_strand_detail)

        progressManager = ActivityProgressManager(this)
        containerSubstrands = findViewById(R.id.containerSubstrands)
        scrollStrandDetail = findViewById(R.id.scrollStrandDetail)
        tvStrandPosition = findViewById(R.id.tvStrandPosition)
        tvStrandProgress = findViewById(R.id.tvStrandProgress)
        progressStrand = findViewById(R.id.progressStrand)
        btnPreviousStrand = findViewById(R.id.btnPreviousStrand)
        btnNextStrand = findViewById(R.id.btnNextStrand)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarDetail)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val requestedAreaId = intent.getStringExtra("LEARNING_AREA_ID")
        val requestedStrandId = intent.getStringExtra("STRAND_ID")
        val requestedGrade = GradeLevel.values().firstOrNull {
            it.code.equals(intent.getStringExtra("GRADE_CODE"), ignoreCase = true)
        } ?: GradeLevel.GRADE_4
        val fromSupabase = intent.getBooleanExtra("FROM_SUPABASE", false)
        currentGrade = requestedGrade
        usesLiveCurriculum = fromSupabase

        btnPreviousStrand.setOnClickListener { navigateStrand(-1) }
        btnNextStrand.setOnClickListener { navigateStrand(1) }

        if (fromSupabase) {
            findViewById<TextView>(R.id.tvDetailTitle).text = "Loading published curriculum…"
            findViewById<TextView>(R.id.tvDetailDesc).text =
                "Fetching ${requestedGrade.title} content from Supabase."
            btnPreviousStrand.isEnabled = false
            btnNextStrand.isEnabled = false

            SupabaseCurriculumRepository.fetchStrands(requestedGrade) { result ->
                if (isFinishing || isDestroyed) return@fetchStrands
                result.onFailure {
                    Toast.makeText(
                        this,
                        "Unable to load ${requestedGrade.title} curriculum. Check your connection and try again.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }.onSuccess { strands ->
                    currentStrands = strands.filter { strand ->
                        requestedAreaId == null || strand.learningAreaId == requestedAreaId
                    }
                    currentStrandIndex = currentStrands.indexOfFirst { it.id == requestedStrandId }
                    if (currentStrandIndex < 0 && requestedStrandId == null && currentStrands.isNotEmpty()) {
                        currentStrandIndex = 0
                    }
                    if (currentStrandIndex !in currentStrands.indices) {
                        Toast.makeText(
                            this,
                            "No published strand was found for ${requestedGrade.title}.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                        return@onSuccess
                    }
                    displayStrand(currentStrands[currentStrandIndex])
                }
            }
        } else {
            val strandInRequest = if (requestedAreaId != null) {
                CbcRepository.getStrandsForArea(requestedAreaId)
                    .firstOrNull { it.id == requestedStrandId }
                    ?: CbcRepository.getStrandsForArea(requestedAreaId).firstOrNull()
            } else {
                CbcRepository.getLearningAreas(GradeLevel.GRADE_4)
                    .flatMap { CbcRepository.getStrandsForArea(it.id) }
                    .firstOrNull { it.id == requestedStrandId }
            }

            if (strandInRequest == null) {
                Toast.makeText(this, "This learning strand could not be found.", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            currentStrands = CbcRepository.getStrandsForArea(strandInRequest.learningAreaId)
            currentStrandIndex = currentStrands.indexOfFirst { it.id == strandInRequest.id }
                .coerceAtLeast(0)
            displayStrand(currentStrands[currentStrandIndex])
        }
    }

    private fun displayStrand(strand: Strand) {
        findViewById<TextView>(R.id.tvDetailStrandNumber).text = "Strand ${strand.strandNumber}"
        findViewById<TextView>(R.id.tvDetailTitle).text = strand.title
        findViewById<TextView>(R.id.tvDetailDesc).text = strand.description
        supportActionBar?.title = strand.title

        tvStrandPosition.text = "Strand ${currentStrandIndex + 1} of ${currentStrands.size}"
        btnPreviousStrand.isEnabled = currentStrandIndex > 0
        btnNextStrand.isEnabled = currentStrandIndex < currentStrands.lastIndex
        btnPreviousStrand.alpha = if (btnPreviousStrand.isEnabled) 1f else 0.45f
        btnNextStrand.alpha = if (btnNextStrand.isEnabled) 1f else 0.45f

        renderSubStrandCards(strand)
        refreshProgressSummary(strand)
    }

    private fun navigateStrand(direction: Int) {
        val targetIndex = currentStrandIndex + direction
        if (targetIndex !in currentStrands.indices) return

        // Save reflections and self-evaluations before leaving this strand.
        saveDraftsForCurrentStrand()
        currentStrandIndex = targetIndex
        displayStrand(currentStrands[currentStrandIndex])
        scrollStrandDetail.scrollTo(0, 0)
    }

    override fun onPause() {
        if (::containerSubstrands.isInitialized && ::progressManager.isInitialized) {
            saveDraftsForCurrentStrand()
        }
        super.onPause()
    }

    private fun renderSubStrandCards(strand: Strand) {
        containerSubstrands.removeAllViews()
        val inflater = LayoutInflater.from(this)

        strand.subStrands.forEach { sub ->
            val subCard = inflater.inflate(
                R.layout.item_interactive_substrand,
                containerSubstrands,
                false
            ) as MaterialCardView
            subCard.tag = sub.id

            val tvSubstrandTitle = subCard.findViewById<TextView>(R.id.tvSubstrandTitle)
            val tvStatusBadge = subCard.findViewById<TextView>(R.id.tvStatusBadge)
            val tvLearningOutcomes = subCard.findViewById<TextView>(R.id.tvLearningOutcomes)
            val tvInquiryQuestions = subCard.findViewById<TextView>(R.id.tvInquiryQuestions)
            val tvDigitalTaskPrompt = subCard.findViewById<TextView>(R.id.tvDigitalTaskPrompt)
            val etLearnerReflection = subCard.findViewById<EditText>(R.id.etLearnerReflection)
            val cgSelfEvaluation = subCard.findViewById<ChipGroup>(R.id.cgSelfEvaluation)
            val btnSaveProgress = subCard.findViewById<Button>(R.id.btnSaveProgress)

            tvSubstrandTitle.text = "Sub-strand ${sub.subStrandNumber}: ${sub.title}"
            tvLearningOutcomes.text = formatBulletList(
                sub.specificLearningOutcomes,
                "Outcomes specified in KICD curriculum."
            )
            tvInquiryQuestions.text = formatBulletList(
                sub.keyInquiryQuestions,
                "Inquiry prompts for interactive group discussion."
            )
            tvDigitalTaskPrompt.text = sub.digitalTaskPrompt.ifBlank {
                "Complete practical activity and upload evidence."
            }

            val isCompleted = progressManager.isSubStrandCompleted(sub.id)
            etLearnerReflection.setText(progressManager.getLearnerNote(sub.id))
            updateSubstrandUiState(subCard, tvStatusBadge, btnSaveProgress, isCompleted)

            when (progressManager.getSelfEvaluation(sub.id)) {
                PerformanceLevel.EXCEEDS -> cgSelfEvaluation.check(R.id.chipEE)
                PerformanceLevel.MEETS -> cgSelfEvaluation.check(R.id.chipME)
                PerformanceLevel.APPROACHES -> cgSelfEvaluation.check(R.id.chipAE)
                PerformanceLevel.BELOW -> cgSelfEvaluation.check(R.id.chipBE)
            }

            btnSaveProgress.setOnClickListener {
                val newCompletedState = !progressManager.isSubStrandCompleted(sub.id)
                saveSubStrandInputs(
                    sub.id,
                    newCompletedState,
                    etLearnerReflection,
                    cgSelfEvaluation
                )
                updateSubstrandUiState(subCard, tvStatusBadge, btnSaveProgress, newCompletedState)
                refreshProgressSummary(strand)

                val msg = if (newCompletedState) {
                    "Sub-strand ${sub.subStrandNumber} marked as completed."
                } else {
                    "Progress updated."
                }
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }

            containerSubstrands.addView(subCard)
            loadPublishedResources(subCard, sub, strand)
        }
    }

    private fun loadPublishedResources(
        subCard: MaterialCardView,
        subStrand: com.example.tusome.data.model.SubStrand,
        strand: Strand
    ) {
        val status = subCard.findViewById<TextView>(R.id.tvLearningResourcesStatus)
        val container = subCard.findViewById<LinearLayout>(R.id.containerLearningResources)
        if (!usesLiveCurriculum) {
            status.text = "Published resources appear here when the live CBC curriculum is loaded."
            return
        }

        status.text = "Loading reviewed resources…"
        LearningResourceRepository.fetchPublishedForSubStrand(
            context = this,
            gradeCode = currentGrade.code,
            learningAreaId = strand.learningAreaId,
            strandId = strand.id,
            subStrandId = subStrand.id
        ) { result ->
            if (isFinishing || isDestroyed || !subCard.isAttachedToWindow) return@fetchPublishedForSubStrand
            container.removeAllViews()
            result.onSuccess { fetched ->
                val resources = fetched.resources
                status.text = when {
                    resources.isEmpty() -> "No reviewed supplementary resources have been published for this sub-strand yet."
                    fetched.fromCache -> "Saved resources • available offline"
                    else -> "${resources.size} reviewed resource${if (resources.size == 1) "" else "s"} available"
                }
                resources.forEach { resource -> renderLearningResource(container, resource) }
            }.onFailure {
                status.text = "Learning resources could not be loaded. Try again when connected."
            }
        }
    }

    private fun renderLearningResource(
        parent: LinearLayout,
        resource: LearningResource
    ) {
        val card = MaterialCardView(this).apply {
            radius = dp(14).toFloat()
            cardElevation = dp(1).toFloat()
            strokeWidth = dp(1)
            strokeColor = themedColor(android.R.attr.colorControlHighlight, R.color.surface_variant)
            setCardBackgroundColor(themedColor(android.R.attr.colorBackground, R.color.surface_variant))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        card.addView(content)

        addResourceText(
            content,
            "${resource.type.displayName}: ${resource.title}",
            14f,
            bold = true
        )
        if (resource.summary.isNotBlank()) {
            addResourceText(content, resource.summary, 12f)
        }
        resource.estimatedMinutes?.let {
            addResourceText(content, "Estimated learning time: $it minutes", 11f)
        }
        resource.learningOutcomes.forEachIndexed { index, outcome ->
            if (index == 0) addResourceText(content, "Learning outcomes", 12f, bold = true)
            addResourceText(content, "• $outcome", 12f)
        }
        resource.sections.forEach { section ->
            if (section.title.isNotBlank()) {
                addResourceText(content, section.title, 13f, bold = true)
            }
            if (section.description.isNotBlank()) {
                addResourceText(content, section.description, 12f)
            }
            section.blocks.forEach { block -> renderLearningBlock(content, block) }
        }
        parent.addView(card)
    }

    private fun renderLearningBlock(parent: LinearLayout, block: LearningContentBlock) {
        when (block.type) {
            LearningContentBlockType.BULLET_LIST -> {
                block.items.forEach { addResourceText(parent, "• $it", 12f) }
                if (block.items.isEmpty() && block.text.isNotBlank()) {
                    addResourceText(parent, block.text, 12f)
                }
            }
            LearningContentBlockType.NUMBERED_LIST -> {
                block.items.forEachIndexed { index, item ->
                    addResourceText(parent, "${index + 1}. $item", 12f)
                }
                if (block.items.isEmpty() && block.text.isNotBlank()) {
                    addResourceText(parent, block.text, 12f)
                }
            }
            LearningContentBlockType.HEADING ->
                addResourceText(parent, block.text, 13f, bold = true)
            LearningContentBlockType.EXAMPLE -> {
                addResourceText(parent, "Example", 12f, bold = true)
                if (block.text.isNotBlank()) addResourceText(parent, block.text, 12f)
                block.items.forEach { addResourceText(parent, "• $it", 12f) }
            }
            LearningContentBlockType.IMAGE,
            LearningContentBlockType.VIDEO -> {
                val label = if (block.type == LearningContentBlockType.IMAGE) "Image" else "Video"
                if (block.caption.isNullOrBlank().not()) {
                    addResourceText(parent, "$label: ${block.caption}", 12f)
                }
                block.mediaUrl?.let { addResourceText(parent, "Media: $it", 11f) }
            }
            LearningContentBlockType.TABLE -> {
                if (block.text.isNotBlank()) addResourceText(parent, block.text, 12f)
                block.items.forEach { addResourceText(parent, it, 12f) }
            }
            else -> {
                if (block.text.isNotBlank()) addResourceText(parent, block.text, 12f)
                block.items.forEach { addResourceText(parent, "• $it", 12f) }
            }
        }
    }

    private fun addResourceText(
        parent: LinearLayout,
        value: String,
        sizeSp: Float,
        bold: Boolean = false
    ) {
        if (value.isBlank()) return
        val text = TextView(this).apply {
            this.text = value
            textSize = sizeSp
            setTextColor(themedColor(android.R.attr.textColorPrimary, R.color.text_secondary))
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setLineSpacing(dp(2).toFloat(), 1f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4) }
        }
        parent.addView(text)
    }

    private fun themedColor(attribute: Int, fallbackResource: Int): Int {
        val value = TypedValue()
        return if (theme.resolveAttribute(attribute, value, true)) {
            if (value.resourceId != 0) ContextCompat.getColor(this, value.resourceId) else value.data
        } else {
            ContextCompat.getColor(this, fallbackResource)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun saveDraftsForCurrentStrand() {
        for (index in 0 until containerSubstrands.childCount) {
            val card = containerSubstrands.getChildAt(index) as? MaterialCardView ?: continue
            val subStrandId = card.tag as? String ?: continue
            val reflection = card.findViewById<EditText>(R.id.etLearnerReflection)
            val evaluation = card.findViewById<ChipGroup>(R.id.cgSelfEvaluation)
            saveSubStrandInputs(
                subStrandId,
                progressManager.isSubStrandCompleted(subStrandId),
                reflection,
                evaluation
            )
        }
    }

    private fun saveSubStrandInputs(
        subStrandId: String,
        isCompleted: Boolean,
        reflection: EditText,
        evaluation: ChipGroup
    ) {
        val selectedLevel = when (evaluation.checkedChipId) {
            R.id.chipEE -> PerformanceLevel.EXCEEDS
            R.id.chipAE -> PerformanceLevel.APPROACHES
            R.id.chipBE -> PerformanceLevel.BELOW
            else -> PerformanceLevel.MEETS
        }
        progressManager.saveSubStrandProgress(
            subStrandId = subStrandId,
            isCompleted = isCompleted,
            learnerNote = reflection.text.toString().trim(),
            evaluationLevel = selectedLevel
        )
    }

    private fun refreshProgressSummary(strand: Strand) {
        val total = strand.subStrands.size
        val completed = strand.subStrands.count { progressManager.isSubStrandCompleted(it.id) }
        tvStrandProgress.text = "$completed of $total activities completed"
        progressStrand.progress = if (total == 0) 0 else (completed * 100 / total)
    }

    private fun updateSubstrandUiState(
        card: MaterialCardView,
        statusBadge: TextView,
        btn: Button,
        isCompleted: Boolean
    ) {
        if (isCompleted) {
            statusBadge.text = "COMPLETED"
            statusBadge.setBackgroundResource(R.drawable.bg_badge_ee)
            statusBadge.setTextColor(ContextCompat.getColor(this, R.color.rubric_ee))
            btn.text = "Completed ✓ (Tap to Toggle)"
            btn.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_green_dark))
            card.setStrokeColor(ContextCompat.getColor(this, R.color.primary_green))
            card.strokeWidth = 3
        } else {
            statusBadge.text = "IN PROGRESS"
            statusBadge.setBackgroundResource(R.drawable.bg_chip_unselected)
            statusBadge.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            btn.text = "Mark Activity as Completed"
            btn.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_green))
            card.setStrokeColor(ContextCompat.getColor(this, R.color.surface_variant))
            card.strokeWidth = 1
        }
    }

    private fun formatBulletList(items: List<String>, emptyMessage: String): String {
        return if (items.isEmpty()) {
            emptyMessage
        } else {
            items.joinToString(separator = "\n") { "• $it" }
        }
    }
}
