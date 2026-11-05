package com.example.tusome.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.tusome.data.repository.AssessmentRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import org.json.JSONArray
import org.json.JSONObject

class AssessmentReviewActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(MaterialToolbar(this).apply {
            title = "Assessment review"
            setNavigationIcon(android.R.drawable.ic_media_previous)
            setNavigationOnClickListener { finish() }
        })
        status = TextView(this).apply {
            text = "Loading assessment review queue…"
            textSize = 14f
            setPadding(18, 14, 18, 14)
        }
        root.addView(status)
        val scroll = ScrollView(this)
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 4, 16, 24)
        }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
        loadQueue()
    }

    private fun loadQueue() {
        status.text = "Loading drafts awaiting review or publication…"
        AssessmentRepository.manageAssessmentPaper(this, "review_queue") { result ->
            if (isFinishing || isDestroyed) return@manageAssessmentPaper
            list.removeAllViews()
            result.onSuccess { response ->
                val papers = response.optJSONArray("papers") ?: JSONArray()
                status.text = "${papers.length()} assessment(s) in the review workflow"
                if (papers.length() == 0) {
                    addMessage("No assessments are currently awaiting review or publication.")
                }
                for (index in 0 until papers.length()) {
                    val paper = papers.optJSONObject(index) ?: continue
                    addPaperCard(paper)
                }
            }.onFailure { error ->
                status.text = "Could not load the review queue."
                addMessage(error.message ?: "Check your curriculum-editor permissions.")
            }
        }
    }

    private fun addPaperCard(paper: JSONObject) {
        val id = paper.optString("id")
        val title = paper.optString("title", "Assessment draft")
        val state = paper.optString("content_status", "in_review")
        val card = MaterialCardView(this).apply {
            radius = 16f
            cardElevation = 1f
            strokeWidth = 1
            setOnClickListener { previewPaper(id) }
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 14)
        }
        column.addView(TextView(this).apply {
            text = title
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        column.addView(TextView(this).apply {
            text = "${paper.optString("grade_id")} • ${paper.optString("assessment_type")} • ${paper.optDouble("total_marks").toInt()} marks • ${state.replace('_', ' ')}"
            textSize = 12f
            setPadding(0, 5, 0, 8)
        })
        column.addView(MaterialButton(this).apply {
            text = if (state == "approved") "Publish assessment" else "Review assessment"
            isAllCaps = false
            setOnClickListener { previewPaper(id) }
        })
        card.addView(column)
        list.addView(card, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 12 })
    }

    private fun previewPaper(paperId: String) {
        status.text = "Loading assessment draft…"
        AssessmentRepository.manageAssessmentPaper(this, "get_paper", paperId) { result ->
            if (isFinishing || isDestroyed) return@manageAssessmentPaper
            result.onSuccess { response ->
                val paper = response.optJSONObject("paper")
                if (paper == null) {
                    status.text = "Assessment draft could not be read."
                    return@onSuccess
                }
                showReviewDialog(paper)
            }.onFailure { error ->
                status.text = "Could not open assessment: ${error.message.orEmpty()}"
            }
        }
    }

    private fun showReviewDialog(paper: JSONObject) {
        val state = paper.optString("content_status")
        val questions = paper.optJSONArray("question_payload") ?: JSONArray()
        val preview = buildString {
            append(paper.optString("title")).append("\n")
            append(paper.optString("instructions")).append("\n\n")
            append("Duration: ").append(paper.optInt("duration_minutes")).append(" minutes")
                .append(" • Total: ").append(paper.optDouble("total_marks").toInt()).append(" marks\n\n")
            for (index in 0 until questions.length()) {
                val q = questions.optJSONObject(index) ?: continue
                append(q.optString("question_number", (index + 1).toString()))
                    .append(". ").append(q.optString("question_text"))
                    .append(" (").append(q.optDouble("marks").toInt()).append(" marks)\n")
                val subs = q.optJSONArray("sub_questions") ?: JSONArray()
                for (subIndex in 0 until subs.length()) {
                    val sub = subs.optJSONObject(subIndex) ?: continue
                    append("   ").append(sub.optString("label")).append(" ")
                        .append(sub.optString("question_text")).append("\n")
                }
                append("\n")
            }
            append("MARKING GUIDE\n")
            val guide = paper.optJSONArray("marking_payload") ?: JSONArray()
            for (index in 0 until guide.length()) {
                val item = guide.optJSONObject(index) ?: continue
                append(item.optString("question_id")).append(": ")
                    .append(item.optString("suggested_answer", item.optString("answer")))
                    .append("\n")
            }
        }
        val builder = AlertDialog.Builder(this)
            .setTitle("Review: ${paper.optString("title")}")
            .setMessage(preview)
            .setNegativeButton("Close", null)

        if (state == "in_review") {
            builder.setNeutralButton("Request changes") { _, _ -> requestChanges(paper.optString("id")) }
            builder.setPositiveButton("Approve") { _, _ -> applyDecision(paper.optString("id"), "approve") }
        } else if (state == "approved") {
            builder.setPositiveButton("Publish") { _, _ -> applyDecision(paper.optString("id"), "publish") }
        }
        builder.show()
    }

    private fun requestChanges(paperId: String) {
        val input = EditText(this).apply {
            hint = "Explain the changes required"
            minLines = 3
            setPadding(24, 16, 24, 16)
        }
        AlertDialog.Builder(this)
            .setTitle("Request changes")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Send feedback") { _, _ ->
                applyDecision(paperId, "request_changes", input.text.toString())
            }
            .show()
    }

    private fun applyDecision(paperId: String, action: String, feedback: String = "") {
        status.text = "Saving assessment decision…"
        AssessmentRepository.manageAssessmentPaper(this, action, paperId, feedback) { result ->
            if (isFinishing || isDestroyed) return@manageAssessmentPaper
            result.onSuccess {
                status.text = "Assessment workflow updated."
                loadQueue()
            }.onFailure { error ->
                status.text = "Could not update assessment: ${error.message.orEmpty()}"
            }
        }
    }

    private fun addMessage(message: String) {
        list.addView(TextView(this).apply {
            text = message
            textSize = 14f
            setPadding(12, 18, 12, 18)
        })
    }
}
