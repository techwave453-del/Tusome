package com.example.tusome.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
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
import java.net.HttpURLConnection
import java.net.URL
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
    private val imageExecutor = Executors.newCachedThreadPool()
    private var startedAt = System.currentTimeMillis()
    private var submittedAt: Long? = null
    private var rendering = false

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
        paper = AssessmentRepository.getCachedPaper(this, paperId, version) ?: run {
            finish()
            return
        }

        val localStore = AssessmentLocalStore(this)
        val saved = localStore.getLatestAttemptForPaper(userId, paperId)
        attemptId = saved?.optString("attempt_id")?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()
        answers = saved?.optJSONObject("answers") ?: JSONObject()
        startedAt = saved?.optLong("started_at")?.takeIf { it > 0L }
            ?: System.currentTimeMillis()
        submittedAt = saved?.opt("submitted_at").let { value ->
            if (value == null || value == JSONObject.NULL) null else (value as? Number)?.toLong()
        }

        renderScreen()
        renderQuestions()
        if (submittedAt != null) {
            loadMarkingGuide()
            renderPerformanceGrid()
        }
    }

    private fun renderScreen() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val toolbar = MaterialToolbar(this).apply {
            title = paper.optString("title", intent.getStringExtra("PAPER_TITLE") ?: "Assessment")
            subtitle = paper.optString("paper_number").takeIf { it.isNotBlank() }
                ?: intent.getStringExtra("GRADE_CODE").orEmpty()
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
        renderPaperHeader()

        val sections = paper.optJSONArray("sections_payload") ?: JSONArray()
        val sectionMap = mutableMapOf<String, JSONObject>()
        for (index in 0 until sections.length()) {
            val section = sections.optJSONObject(index) ?: continue
            sectionMap[section.optString("id", "section_${index + 1}")] = section
        }

        val questions = paper.optJSONArray("question_payload") ?: JSONArray()
        if (questions.length() == 0) {
            addMessage("This assessment has no questions yet. A curriculum editor must complete and publish its question set.")
            rendering = false
            return
        }

        var lastSectionId = ""
        for (index in 0 until questions.length()) {
            val question = questions.optJSONObject(index) ?: continue
            val sectionId = question.optString("section_id")
            if (sectionId.isNotBlank() && sectionId != lastSectionId) {
                sectionMap[sectionId]?.let { renderSectionHeader(it) }
                lastSectionId = sectionId
            }
            renderQuestion(question, index)
        }
        rendering = false
    }

    private fun renderPaperHeader() {
        val instructions = paper.optString("instructions")
        if (instructions.isNotBlank()) {
            addHeading("Instructions to learners")
            addMessage(instructions)
        }
        val learnerFields = paper.optJSONArray("learner_detail_fields") ?: JSONArray()
        if (learnerFields.length() > 0) {
            addHeading("Learner details")
            val labels = (0 until learnerFields.length()).map { learnerFields.optString(it) }
                .filter { it.isNotBlank() }
            if (labels.isNotEmpty()) addMessage(labels.joinToString("   •   "))
        }
        val metadata = paper.optJSONObject("paper_metadata") ?: JSONObject()
        val meta = listOf(
            paper.optString("pathway"),
            metadata.optString("assessment_series"),
            paper.optInt("term_number").takeIf { it in 1..3 }?.let { "Term $it" }.orEmpty(),
            paper.optInt("trial_number").takeIf { it > 0 }?.let { "Trial $it" }.orEmpty()
        ).filter { it.isNotBlank() }
        if (meta.isNotEmpty()) addMessage(meta.joinToString("   •   "))
    }

    private fun renderSectionHeader(section: JSONObject) {
        val title = section.optString("title")
        if (title.isNotBlank()) addHeading(title)
        val instructions = section.optString("instructions")
        if (instructions.isNotBlank()) addMessage(instructions)
        val marks = section.optDouble("total_marks")
        if (marks > 0) addMessage("Section total: ${marks.toInt()} marks")
    }

    private fun renderQuestion(question: JSONObject, index: Int) {
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
            val number = question.optString("question_number").ifBlank { (index + 1).toString() }
            text = buildString {
                append(number).append(". ").append(prompt)
                if (marks > 0) append(" (").append(marks).append(" marks)")
            }
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
        })

        val stimulus = question.optString("stimulus")
        if (stimulus.isNotBlank()) {
            column.addView(TextView(this).apply {
                text = stimulus
                textSize = 14f
                setPadding(0, 8, 0, 6)
            })
        }
        val imageUrl = question.optString("image_url")
        if (imageUrl.startsWith("https://")) {
            val image = ImageView(this).apply {
                contentDescription = question.optString("image_alt").ifBlank { "Assessment illustration" }
                adjustViewBounds = true
                maxHeight = 720
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            column.addView(image, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ))
            loadQuestionImage(imageUrl, image)
        }

        renderQuestionTable(column, question.optJSONObject("table_data"))

        val subQuestions = question.optJSONArray("sub_questions") ?: JSONArray()
        if (subQuestions.length() > 0) {
            for (subIndex in 0 until subQuestions.length()) {
                val sub = subQuestions.optJSONObject(subIndex) ?: continue
                val subId = sub.optString("id").ifBlank { "${questionId}_${subIndex + 1}" }
                val subPrompt = sub.optString("question_text")
                    .ifBlank { sub.optString("prompt", sub.optString("text")) }
                val subMarks = sub.optDouble("marks")
                column.addView(TextView(this).apply {
                    text = buildString {
                        append(sub.optString("label").ifBlank { "(${'a' + subIndex})" })
                        append(" ").append(subPrompt)
                        if (subMarks > 0) append(" (").append(subMarks).append(" marks)")
                    }
                    textSize = 14f
                    setPadding(0, 10, 0, 4)
                })
                addAnswerControl(
                    column,
                    subId,
                    sub.optString("question_type", type).lowercase(),
                    sub.optJSONArray("options") ?: question.optJSONArray("options") ?: JSONArray()
                )
            }
        } else {
            addAnswerControl(column, questionId, type, question.optJSONArray("options") ?: JSONArray())
        }

        card.addView(column)
        questionContainer.addView(card, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 12 })
    }

    private fun renderQuestionTable(container: LinearLayout, table: JSONObject?) {
        if (table == null) return
        val headers = table.optJSONArray("headers") ?: JSONArray()
        val rows = table.optJSONArray("rows") ?: JSONArray()
        if (headers.length() == 0 && rows.length() == 0) return
        addMessageTo(container, (0 until headers.length()).joinToString("   |   ") { headers.optString(it) })
        for (rowIndex in 0 until rows.length()) {
            val row = rows.optJSONArray(rowIndex) ?: continue
            addMessageTo(container, (0 until row.length()).joinToString("   |   ") { row.optString(it) })
        }
    }

    private fun addMessageTo(container: LinearLayout, message: String) {
        container.addView(TextView(this).apply {
            text = message
            textSize = 13f
            setPadding(0, 5, 0, 5)
        })
    }

    private fun addAnswerControl(
        container: LinearLayout,
        answerId: String,
        type: String,
        options: JSONArray
    ) {
        if ((type == "multiple_choice" || type == "true_false" || type == "single_choice") && options.length() > 0) {
            val group = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
            for (optionIndex in 0 until options.length()) {
                val optionText = options.optString(optionIndex)
                group.addView(RadioButton(this).apply {
                    text = optionText
                    isChecked = answers.optString(answerId) == optionText
                    isEnabled = submittedAt == null
                })
            }
            group.setOnCheckedChangeListener { checkedGroup, checkedId ->
                if (rendering || submittedAt != null) return@setOnCheckedChangeListener
                val selected = checkedGroup.findViewById<RadioButton>(checkedId)?.text?.toString().orEmpty()
                answers.put(answerId, selected)
                saveAttempt()
            }
            container.addView(group)
        } else {
            val field = EditText(this).apply {
                hint = when (type) {
                    "essay", "long_answer", "structured", "reflection", "practical" -> "Write your answer here"
                    "numeric" -> "Enter your answer"
                    else -> "Your answer"
                }
                setText(answers.optString(answerId))
                textSize = 14f
                minLines = if (type in setOf("long_answer", "essay", "structured", "reflection", "practical")) 4 else 2
                isSingleLine = type in setOf("short_answer", "numeric", "fill_blank")
                gravity = android.view.Gravity.TOP
                isEnabled = submittedAt == null
                setPadding(12, 10, 12, 10)
            }
            field.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (rendering || submittedAt != null) return
                    answers.put(answerId, s?.toString().orEmpty())
                    saveAttempt()
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
            container.addView(field)
        }
    }

    private fun loadQuestionImage(imageUrl: String, imageView: ImageView) {
        imageExecutor.execute {
            val bitmap = runCatching {
                val connection = URL(imageUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 15000
                connection.instanceFollowRedirects = true
                try {
                    connection.inputStream.use { BitmapFactory.decodeStream(it) }
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()
            if (bitmap != null) runOnUiThread {
                if (!isFinishing && !isDestroyed) imageView.setImageBitmap(bitmap)
            }
        }
    }

    private fun submitAssessment() {
        if (submittedAt != null) return
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Submit assessment?")
            .setMessage("You will not be able to change your answers after submission.")
            .setNegativeButton("Continue working", null)
            .setPositiveButton("Submit") { _, _ ->
                submittedAt = System.currentTimeMillis()
                renderScreen()
                renderQuestions()
                saveAttempt { loadMarkingGuide() }
                renderPerformanceGrid()
            }
            .show()
    }

    private fun loadMarkingGuide() {
        addHeading("Marking guide")
        addMessage("Verifying your submitted attempt before loading the marking guide…")
        AssessmentRepository.syncPendingAttempts(this) {
            AssessmentRepository.fetchMarkingGuide(
                this,
                paperId,
                paper.optInt("version", 1)
            ) { result ->
                if (isFinishing || isDestroyed) return@fetchMarkingGuide
                result.onSuccess(::renderMarkingGuide).onFailure { error ->
                    addMessage(
                        error.message
                            ?: "The marking guide will be available after this submitted attempt syncs while online."
                    )
                }
            }
        }
    }

    private fun renderMarkingGuide(marking: JSONArray) {
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
                append(item.optString("question_id", "Question \${index + 1}"))
                if (answer.isNotBlank()) append("\nSuggested answer: ").append(answer)
                if (notes.isNotBlank()) append("\nMarking guidance: ").append(notes)
            })
        }
    }

    private fun renderPerformanceGrid() {
        val grid = paper.optJSONArray("performance_level_grid") ?: JSONArray()
        if (grid.length() == 0) return
        addHeading("Performance levels")
        for (index in 0 until grid.length()) {
            val row = grid.optJSONObject(index) ?: continue
            val criterion = row.optString("criterion", row.optString("question", "Criterion ${index + 1}"))
            val bands = listOf(
                "EE" to row.optString("exceeds_expectation"),
                "ME" to row.optString("meets_expectation"),
                "AE" to row.optString("approaching_expectation"),
                "BE" to row.optString("below_expectation")
            ).filter { it.second.isNotBlank() }
            addMessage(criterion + if (bands.isEmpty()) "" else "\n" + bands.joinToString("\n") { "${it.first}: ${it.second}" })
        }
    }

    private fun addHeading(value: String) {
        questionContainer.addView(TextView(this).apply {
            text = value
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 14, 0, 6)
        })
    }

    private fun addMessage(message: String) {
        questionContainer.addView(TextView(this).apply {
            text = message
            textSize = 13f
            setPadding(16, 12, 16, 12)
        })
    }

    private fun saveAttempt(afterSaved: (() -> Unit)? = null) {
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
                AssessmentRepository.syncPendingAttempts(applicationContext) {
                    afterSaved?.invoke()
                }
            } else if (afterSaved != null) {
                runOnUiThread { afterSaved.invoke() }
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
        imageExecutor.shutdown()
        super.onDestroy()
    }
}
