package com.example.tusome.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tusome.R
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.model.Strand
import com.example.tusome.data.repository.CbcRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView

class StrandDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_strand_detail)

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

        val containerSubstrands = findViewById<LinearLayout>(R.id.containerSubstrands)
        containerSubstrands.removeAllViews()

        val inflater = LayoutInflater.from(this)
        strand.subStrands.forEach { sub ->
            val subCard = inflater.inflate(R.layout.item_substrand_content, containerSubstrands, false) as MaterialCardView

            subCard.findViewById<TextView>(R.id.tvRubricSubstrand).text =
                "Sub-strand ${sub.subStrandNumber}: ${sub.title}"
            subCard.findViewById<TextView>(R.id.tvRubricCriteria).text =
                formatBulletList(sub.specificLearningOutcomes, "Learning outcomes will be added for this sub-strand.")
            subCard.findViewById<TextView>(R.id.tvKeyInquiryQuestions).text =
                formatBulletList(sub.keyInquiryQuestions, "No inquiry questions have been added yet.")
            subCard.findViewById<TextView>(R.id.tvSuggestedExperiences).text =
                formatBulletList(sub.suggestedExperiences, "No suggested activities have been added yet.")
            subCard.findViewById<TextView>(R.id.tvTeacherRemark).text =
                sub.digitalTaskPrompt.ifBlank { "No digital learning task has been added yet." }

            containerSubstrands.addView(subCard)
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
