package com.example.tusome.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tusome.data.local.AssessmentLocalStore
import com.example.tusome.data.repository.AssessmentRepository
import com.example.tusome.data.repository.SupabaseAuthRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.Executors

class AssessmentPaperActivity : AppCompatActivity() {

    private lateinit var paper: JSONObject
    private lateinit var paperId: String
    private lateinit var userId: String
    private lateinit var attemptId: String
    private lateinit var answers: JSONObject
    private lateinit var status: TextView
    private lateinit var questionContainer: LinearLayout
    private val saveExecutor = Executors.newSingleThreadExecutor()
    private var startedAt = System.currentTimeMillis()
    private var submittedAt: Long? = null
    private var rendering = false
    private val answerControls = mutableListOf<ViewGroup>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        paperId = intent.getStringExtra("PAPER_ID").orEmpty()
        val version = intent.getIntExtra("PAPER_VERSION", 1)
        if (paperId.isBlank()) {
            finish()
            return
        }
        val session = SupabaseAuthRepository.currentSession(this)
        userId = session?.userId ?: "guest"
        val cachedJson = AssessmentRepository.getCachedPaper(this, paperId, version)
        paper = cachedJson ?: run {
            finish()
            return
        }

        val localStore = AssessmentLocalStore(this)
        val saved = localStore.getLatestAttemptForPaper(userId, paperId)
        attemptId = saved?.optString("attempt_id")?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        answers = saved?.optJSONObject("answers") ?: JSONObject()
        startedAt = saved?.optLong("started_at")?.takeIf { it > 0L } ?: System.currentTimeMillis()
        submittedAt = saved?.opt("submitted_at").let { value ->
            if (value == null || value == JSONObject.NULL) null else (value as? Number)?.toLong()
        }

        renderScreen()
        renderQuestions()
        if (submittedAt != null) renderMarkingGuide()
    }

    private fun renderScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val toolbar = MaterialToolbar(this).apply {
            title = paper.optString("title", intent.getStringExtra("PAPER_TITLE") ?: "Assessment")
            subtitle = intent.getStringExtra("PAPER_TITLE").orEmpty()
            setNavigationIcon(android.R.drawable.ic_media_previous)
            setNavigationOnClickListener { finish() }
        }
        root.addView(toolbar)

        status = TextView(this).apply {
            textSize = 13f
            setPadding(18, 12, 18, 12)
        }
        root.addView(status)

        val scroll = ScrollView(this)
        questionContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 4, 16, 16)
        }
        scroll.addView(questionContainer)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val submit = Button(this).apply {
            text = if (submittedAt == null) "Submit assessment" else "Submitted"
            isEnabled = submittedAt == null
            setOnClickListener { submitAssessment() }
        }
        root.addView(submit, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(16, 8, 16, 16) })

        setContentView(root)
        updateStatus()
    }

    private fun renderQuestions() {
        rendering = true
        questionContainer.removeAllViews()
        answerControls.clear()
        val questions = paper.optJSONArray("question_payload") ?: JSONArray()
        if (questions.length() == 0) {
            addMessage("This assessment has no questions yet. A curriculum editor must complete and publish its question set.")
            rendering = false
            return
        }

        for (index in 0 until questions.length()) {
            val question = questions.optJSONObject(index) ?: continue
            val questionId = question.optString("id").ifBlank { "question_${index + 1}" }
            val prompt = question.optString("question_text")
                .ifBlank { question.optString("prompt", question.optString("text")) }
            val marks = question.optDouble("marks", 0.0)
            val type = question.optString("question_type", question.optString("type", "short_answer"))
                .lowercase()
            val card = MaterialCardView(this).apply {
                radius = 16f
                cardElevation = 1f
                strokeWidth = 1
                setStrokeColor(com.google.android.material.color.MaterialColors.getColor(
                    this, com.google.android.material.R.attr.colorOutline
                ))
            }
            val column = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 14, 16, 14)
            }
            column.addView(TextView(this).apply {
                text = buildString {
                    append(index + 1).append(". ").append(prompt)
                    if (marks > 0) append(" (").append(marks).append(" marks)")
                }
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
            })

            val options = question.optJSONArray("options") ?: JSONArray()
            if ((type == "multiple_choice" || type == "true_false") && options.length() > 0) {
                val group = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
                for (optionIndex in 0 until options.length()) {
                    val optionText = options.optString(optionIndex)
                    val radio = RadioButton(this).apply {
                        text = optionText
                        isChecked = answers.optString(questionId) == optionText
                        isEnabled = submittedAt == null
                    }
                    group.addView(radio)
                }
                group.setOnCheckedChangeListener { checkedGroup, checkedId ->
                    if (rendering || submittedAt != null) return@setOnCheckedChangeListener
                    val selected = checkedGroup.findViewById<RadioButton>(checkedId)?.text?.toString().orEmpty()
                    answers.put(questionId, selected)
                    saveAttempt()
                }
                column.addView(group)
                answerControls.add(group)
            } else {
                val field = EditText(this).apply {
                    hint = "Write your answer here"
                    setText(answers.optString(questionId))
                    textSize = 14f
                    minLines = if (type in setOf("long_answer", "essay", "structured", "reflection")) 4 else 2
                    isSingleLine = type in setOf("short_answer", "numeric", "fill_blank")
                    gravity = android.view.Gravity.TOP
                    isEnabled = submittedAt == null
                    setPadding(12, 10, 12, 10)
                }
                field.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        if (rendering || submittedAt != null) return
                        answers.put(questionId, s?.toString().orEmpty())
                        saveAttempt()
                    }
                    override fun afterTextChanged(s: Editable?) = Unit
                })
                column.addView(field)
                answerControls.add(field)
            }
            card.addView(column)
            questionContainer.addView(card, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 })
        }
        rendering = false
    }

    private fun submitAssessment() {
        if (submittedAt != null) return
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Submit assessment?")
            .setMessage("You will not be able to change your answers after submission.")
            .setNegativeButton("Continue working", null)
            .setPositiveButton("Submit") { _, _ ->
                submittedAt = System.currentTimeMillis()
                saveAttempt()
                renderScreen()
                renderQuestions()
                renderMarkingGuide()
            }
            .show()
    }

    private fun renderMarkingGuide() {
        val marking = paper.optJSONArray("marking_payload") ?: JSONArray()
        val heading = TextView(this).apply {
            text = "Marking guide"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(16, 16, 16, 8)
        }
        questionContainer.addView(heading)
        if (marking.length() == 0) {
            addMessage("The marking guide has not been published for this assessment yet.")
            return
        }
        for (index in 0 until marking.length()) {
            val item = marking.optJSONObject(index) ?: continue
            val answer = item.optString("suggested_answer", item.optString("answer"))
            val notes = item.optString("marking_notes", item.optString("rubric"))
            if (answer.isBlank() && notes.isBlank()) continue
            addMessage(buildString {
                append(item.optString("question_id", "Question ${index + 1}"))
                if (answer.isNotBlank()) append("\nSuggested answer: ").append(answer)
                if (notes.isNotBlank()) append("\nMarking guidance: ").append(notes)
            })
        }
    }

    private fun addMessage(message: String) {
        questionContainer.addView(TextView(this).apply {
            text = message
            textSize = 13f
            setPadding(16, 12, 16, 12)
        })
    }

    private fun saveAttempt() {
        val submitted = submittedAt
        saveExecutor.execute {
            AssessmentRepository.saveAttemptLocally(
                context = applicationContext,
                attemptId = attemptId,
                userId = userId,
                paperId = paperId,
                paperVersion = paper.optInt("version", 1),
                answers = JSONObject(answers.toString()),
                startedAtMillis = startedAt,
                submittedAtMillis = submitted
            )
            if (submitted != null && userId != "guest") {
                AssessmentRepository.syncPendingAttempts(applicationContext)
            }
        }
        updateStatus()
    }

    private fun updateStatus() {
        if (!::status.isInitialized) return
        status.text = if (submittedAt == null) {
            "Your answers save automatically on this device. They will sync when you are online and signed in."
        } else {
            "Submitted • Saved on this device. Cloud synchronisation will retry when connected."
        }
    }

    override fun onPause() {
        if (::paper.isInitialized && ::answers.isInitialized) saveAttempt()
        super.onPause()
    }

    override fun onDestroy() {
        saveExecutor.shutdown()
        super.onDestroy()
    }
}
