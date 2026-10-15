package com.example.tusome.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tusome.R
import com.example.tusome.data.model.PerformanceLevel
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

        val learningAreaId = intent.getStringExtra("LEARNING_AREA_ID") ?: "agri_nutr"
        val strands = CbcRepository.getStrandsForArea(learningAreaId)
        val strand = strands.firstOrNull() ?: return

        findViewById<TextView>(R.id.tvDetailStrandNumber).text = "Strand ${strand.strandNumber}"
        findViewById<TextView>(R.id.tvDetailTitle).text = strand.title
        findViewById<TextView>(R.id.tvDetailDesc).text = strand.description

        val containerSubstrands = findViewById<LinearLayout>(R.id.containerSubstrands)
        containerSubstrands.removeAllViews()

        val inflater = LayoutInflater.from(this)
        strand.subStrands.forEach { sub ->
            val subCard = inflater.inflate(R.layout.item_rubric, containerSubstrands, false) as MaterialCardView
            
            subCard.findViewById<TextView>(R.id.tvRubricSubstrand).text = "Sub-strand ${sub.subStrandNumber}: ${sub.title}"
            subCard.findViewById<TextView>(R.id.tvRubricCriteria).text = "Specific Outcomes: ${sub.specificLearningOutcomes.joinToString("\n• ", prefix = "• ")}"
            subCard.findViewById<TextView>(R.id.tvTeacherRemark).text = "Digital Literacy Task: ${sub.digitalTaskPrompt}"
            
            val badge = subCard.findViewById<TextView>(R.id.tvPerformanceLevelBadge)
            badge.text = sub.rubric.learnerLevel?.name ?: "ME"
            badge.setBackgroundResource(R.drawable.bg_badge_ee)
            badge.setTextColor(Color.parseColor("#2E7D32"))

            containerSubstrands.addView(subCard)
        }
    }
}