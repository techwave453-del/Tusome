package com.example.tusome.ui

import android.os.Bundle
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
import com.example.tusome.R
import com.example.tusome.data.local.ActivityProgressManager
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.model.PerformanceLevel
import com.example.tusome.data.model.Strand
import com.example.tusome.data.repository.CbcRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup

class StrandDetailActivity : AppCompatActivity() {

    private lateinit var progressManager: ActivityProgressManager
    private lateinit var currentStrands: List<Strand>
    private lateinit var containerSubstrands: LinearLayout
    private lateinit var tvStrandPosition: TextView
    private lateinit var tvStrandProgress: TextView
    private lateinit var progressStrand: ProgressBar
    private lateinit var btnPreviousStrand: Button
    private lateinit var btnNextStrand: Button
    private var currentStrandIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_strand_detail)

        progressManager = ActivityProgressManager(this)
        containerSubstrands = findViewById(R.id.containerSubstrands)
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

        btnPreviousStrand.setOnClickListener { navigateStrand(-1) }
        btnNextStrand.setOnClickListener { navigateStrand(1) }

        displayStrand(currentStrands[currentStrandIndex])
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
        findViewById<androidx.core.widget.NestedScrollView>(android.R.id.content)
        window.decorView.findViewById<View>(R.id.containerSubstrands)?.let { container ->
            container.parent?.requestChildFocus(container, container)
        }
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
                null -> Unit
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
        }
    }

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
