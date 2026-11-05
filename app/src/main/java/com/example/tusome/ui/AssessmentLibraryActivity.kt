package com.example.tusome.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.repository.AssessmentRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton
import org.json.JSONObject

class AssessmentLibraryActivity : AppCompatActivity() {

    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private lateinit var grade: GradeLevel
    private lateinit var areaId: String
    private lateinit var areaTitle: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gradeCode = intent.getStringExtra("GRADE_CODE").orEmpty()
        grade = GradeLevel.values().firstOrNull { it.code.equals(gradeCode, true) }
            ?: GradeLevel.GRADE_4
        areaId = intent.getStringExtra("SUBJECT_ID").orEmpty()
        areaTitle = intent.getStringExtra("SUBJECT_TITLE").orEmpty()
        if (areaId.isBlank() || areaTitle.isBlank()) {
            finish()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(com.google.android.material.color.MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorSurface
            ))
        }
        val toolbar = MaterialToolbar(this).apply {
            title = areaTitle
            subtitle = grade.title
            setNavigationIcon(com.google.android.material.R.drawable.material_ic_arrow_back)
            setNavigationOnClickListener { finish() }
        }
        root.addView(toolbar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        status = TextView(this).apply {
            text = "Loading published assessments…"
            textSize = 14f
            setPadding(20, 16, 20, 12)
        }
        root.addView(status)

        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 4, 16, 24)
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)

        loadAssessments()
    }

    private fun loadAssessments() {
        status.text = "Loading published assessments…"
        AssessmentRepository.fetchPublishedPapers(this, grade.code.lowercase(), areaId) { result ->
            if (isFinishing || isDestroyed) return@fetchPublishedPapers
            content.removeAllViews()
            result.onSuccess { papers ->
                if (papers.isEmpty()) {
                    status.text = "No published assessments are available for this learning area yet."
                    addMessage("New papers will appear here after a curriculum editor has created, reviewed and published them. This screen does not display invented placeholder questions.")
                } else {
                    status.text = "${papers.size} published assessment(s) • Saved on this device for offline access"
                    papers.forEach(::addPaperCard)
                }
            }.onFailure { error ->
                status.text = "Assessments could not be loaded."
                addMessage(error.message ?: "Connect to the internet once to download assessments for this learning area.")
            }
        }
    }

    private fun addPaperCard(paper: JSONObject) {
        val id = paper.optString("id")
        val version = paper.optInt("version", 1)
        if (id.isBlank()) return
        val title = paper.optString("title", "Assessment")
        val type = paper.optString("assessment_type", "practice")
            .replaceFirstChar { it.uppercase() }
        val duration = paper.optInt("duration_minutes")
        val marks = paper.optDouble("total_marks")

        val card = MaterialCardView(this).apply {
            radius = 18f
            cardElevation = 1f
            strokeWidth = 1
            setStrokeColor(com.google.android.material.color.MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOutline
            ))
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 16, 18, 16)
        }
        column.addView(TextView(this).apply {
            text = title
            textSize = 17f
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        column.addView(TextView(this).apply {
            text = buildString {
                append(type)
                if (duration > 0) append(" • ").append(duration).append(" min")
                if (marks > 0) append(" • ").append(marks.toInt()).append(" marks")
                append(" • v").append(version)
            }
            textSize = 12f
            setPadding(0, 5, 0, 8)
        })
        val description = paper.optString("description")
        if (description.isNotBlank()) {
            column.addView(TextView(this).apply {
                text = description
                textSize = 13f
            })
        }
        column.addView(MaterialButton(this).apply {
            text = "Open assessment"
            isAllCaps = false
            setOnClickListener {
                startActivity(Intent(this@AssessmentLibraryActivity, AssessmentPaperActivity::class.java).apply {
                    putExtra("PAPER_ID", id)
                    putExtra("PAPER_VERSION", version)
                    putExtra("PAPER_TITLE", title)
                    putExtra("GRADE_CODE", grade.code)
                    putExtra("LEARNING_AREA_ID", areaId)
                })
            }
        })
        card.addView(column)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 12 }
        content.addView(card, params)
    }

    private fun addMessage(message: String) {
        content.addView(TextView(this).apply {
            text = message
            textSize = 14f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 16, 12, 16)
        })
    }
}
