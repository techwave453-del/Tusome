package com.example.tusome.ui

import android.graphics.Color
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
            val subCard = inflater.inflate(R.layout.item_rubric, containerSubstrands, false) as MaterialCardView

            subCard.findViewById<TextView>(R.id.tvRubricSubstrand).text =
                "Sub-strand ${sub.subStrandNumber}: ${sub.title}"
            subCard.findViewById<TextView>(R.id.tvRubricCriteria).text =
                "Specific Outcomes: ${sub.specificLearningOutcomes.joinToString("\n• ", prefix = "• ")}"
            subCard.findViewById<TextView>(R.id.tvTeacherRemark).text =
                "Digital Literacy Task: ${sub.digitalTaskPrompt}"

            val badge = subCard.findViewById<TextView>(R.id.tvPerformanceLevelBadge)
            badge.text = sub.rubric.learnerLevel?.name ?: "ME"
            badge.setBackgroundResource(R.drawable.bg_badge_ee)
            badge.setTextColor(Color.parseColor("#2E7D32"))

            containerSubstrands.addView(subCard)
        }
    }
}