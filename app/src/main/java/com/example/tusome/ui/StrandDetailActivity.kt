package com.example.tusome.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_strand_detail)

        progressManager = ActivityProgressManager(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarDetail)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val requestedAreaId = intent.getStringExtra("LEARNING_AREA_ID")
        val requestedStrandId = intent.getStringExtra("STRAND_ID")

        val strand: Strand? = if (requestedAreaId != null) {
            val areaStrands = CbcRepository.getStrandsForArea(requestedAreaId)
            if (requestedStrandId == null) areaStrands.firstOrNull()
            else areaStrands.firstOrNull { it.id == requestedStrandId }
        } else {
            val allStrands = CbcRepository.getLearningAreas(GradeLevel.GRADE_4)
                .flatMap { area -> CbcRepository.getStrandsForArea(area.id) }
            if (requestedStrandId == null) allStrands.firstOrNull()
            else allStrands.firstOrNull { it.id == requestedStrandId }
        }

        if (strand == null) {
            Toast.makeText(this, "This learning strand could not be found.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.tvDetailStrandNumber).text = "Strand ${strand.strandNumber}"
        findViewById<TextView>(R.id.tvDetailTitle).text = strand.title
        findViewById<TextView>(R.id.tvDetailDesc).text = strand.description

        renderSubStrandCards(strand)
    }

    private fun renderSubStrandCards(strand: Strand) {
        val containerSubstrands = findViewById<LinearLayout>(R.id.containerSubstrands)
        containerSubstrands.removeAllViews()

        val inflater = LayoutInflater.from(this)

        strand.subStrands.forEach { sub ->
            val subCard = inflater.inflate(R.layout.item_interactive_substrand, containerSubstrands, false) as MaterialCardView

            val tvSubstrandTitle = subCard.findViewById<TextView>(R.id.tvSubstrandTitle)
            val tvStatusBadge = subCard.findViewById<TextView>(R.id.tvStatusBadge)
            val tvLearningOutcomes = subCard.findViewById<TextView>(R.id.tvLearningOutcomes)
            val tvInquiryQuestions = subCard.findViewById<TextView>(R.id.tvInquiryQuestions)
            val tvDigitalTaskPrompt = subCard.findViewById<TextView>(R.id.tvDigitalTaskPrompt)
            val etLearnerReflection = subCard.findViewById<EditText>(R.id.etLearnerReflection)
            val cgSelfEvaluation = subCard.findViewById<ChipGroup>(R.id.cgSelfEvaluation)
            val btnSaveProgress = subCard.findViewById<Button>(R.id.btnSaveProgress)

            tvSubstrandTitle.text = "Sub-strand ${sub.subStrandNumber}: ${sub.title}"
            tvLearningOutcomes.text = formatBulletList(sub.specificLearningOutcomes, "Outcomes specified in KICD curriculum.")
            tvInquiryQuestions.text = formatBulletList(sub.keyInquiryQuestions, "Inquiry prompts for interactive group discussion.")
            tvDigitalTaskPrompt.text = sub.digitalTaskPrompt.ifBlank { "Complete practical activity and upload evidence." }

            // Load saved state
            val isCompleted = progressManager.isSubStrandCompleted(sub.id)
            val savedNote = progressManager.getLearnerNote(sub.id)
            val savedLevel = progressManager.getSelfEvaluation(sub.id)

            etLearnerReflection.setText(savedNote)
            updateSubstrandUiState(subCard, tvStatusBadge, btnSaveProgress, isCompleted)

            when (savedLevel) {
                PerformanceLevel.EXCEEDS -> cgSelfEvaluation.check(R.id.chipEE)
                PerformanceLevel.MEETS -> cgSelfEvaluation.check(R.id.chipME)
                PerformanceLevel.APPROACHES -> cgSelfEvaluation.check(R.id.chipAE)
                PerformanceLevel.BELOW -> cgSelfEvaluation.check(R.id.chipBE)
            }

            btnSaveProgress.setOnClickListener {
                val newCompletedState = !progressManager.isSubStrandCompleted(sub.id)
                val reflectionText = etLearnerReflection.text.toString().trim()

                val selectedLevel = when (cgSelfEvaluation.checkedChipId) {
                    R.id.chipEE -> PerformanceLevel.EXCEEDS
                    R.id.chipAE -> PerformanceLevel.APPROACHES
                    R.id.chipBE -> PerformanceLevel.BELOW
                    else -> PerformanceLevel.MEETS
                }

                progressManager.saveSubStrandProgress(
                    subStrandId = sub.id,
                    isCompleted = newCompletedState,
                    learnerNote = reflectionText,
                    evaluationLevel = selectedLevel
                )

                updateSubstrandUiState(subCard, tvStatusBadge, btnSaveProgress, newCompletedState)

                val msg = if (newCompletedState) {
                    "Sub-strand ${sub.subStrandNumber} marked as COMPLETED!"
                } else {
                    "Progress updated."
                }
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }

            containerSubstrands.addView(subCard)
        }
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