package com.example.tusome.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.tusome.R
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.model.SbaExamPaper
import com.example.tusome.data.repository.SbaExamAttempt
import com.example.tusome.data.repository.SbaExamAttemptRepository
import com.example.tusome.data.repository.SbaExamRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.tabs.TabLayout

class SbaExamActivity : AppCompatActivity() {

    private lateinit var examPaper: SbaExamPaper
    private lateinit var containerExamContent: LinearLayout
    private lateinit var attempt: SbaExamAttempt
    private lateinit var tvExamTimeMarks: TextView
    private var showingMarkingScheme = false
    private var timer: CountDownTimer? = null
    private var rendering = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sba_exam)

        containerExamContent = findViewById(R.id.containerExamContent)
        tvExamTimeMarks = findViewById(R.id.tvExamTimeMarks)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarSba)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val subjectId = intent.getStringExtra("SUBJECT_ID")
        val gradeCode = intent.getStringExtra("GRADE_CODE")
        val grade = GradeLevel.values().firstOrNull {
            it.code.equals(gradeCode, ignoreCase = true)
        }
        if (subjectId.isNullOrBlank() || grade == null) {
            showUnavailable("The selected grade or learning area is invalid.")
            return
        }

        val subjectTitle = intent.getStringExtra("SUBJECT_TITLE").orEmpty()
        examPaper = SbaExamRepository.getSbaExamPaper(subjectId, grade, subjectTitle) ?: run {
            showUnavailable("A sample practice paper is not currently available for this learning area and grade.")
            return
        }
        attempt = SbaExamAttemptRepository.loadOrCreate(this, examPaper)

        findViewById<TextView>(R.id.tvExamHeaderTitle).text = examPaper.title
        findViewById<TextView>(R.id.tvExamSubject).text =
            "${examPaper.gradeLevel.title.uppercase()} – ${examPaper.subjectTitle}"
        findViewById<TextView>(R.id.tvExamTerm).text = examPaper.termAndYear

        val tabs = findViewById<TabLayout>(R.id.tabLayoutSba)
        tabs.getTabAt(1)?.view?.isEnabled = attempt.isSubmitted
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 1 && !attempt.isSubmitted) {
                    tabs.getTabAt(0)?.select()
                    return
                }
                showingMarkingScheme = tab?.position == 1
                renderExamView()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })

        renderExamView()
        if (!attempt.isSubmitted) startTimer()
    }

    private fun showUnavailable(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Practice paper unavailable")
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
            .show()
    }

    private fun answerKey(questionNumber: Int, label: String) = "${questionNumber}_${label}"

    private fun saveAnswer(key: String, value: String) {
        if (attempt.isSubmitted) return
        attempt = attempt.copy(answers = attempt.answers + (key to value))
        SbaExamAttemptRepository.save(this, examPaper, attempt)
    }

    private fun startTimer() {
        val remaining = (attempt.deadlineAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        if (remaining == 0L) {
            submitAttempt()
            return
        }
        timer?.cancel()
        timer = object : CountDownTimer(remaining, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = millisUntilFinished / 3_600_000L
                val minutes = (millisUntilFinished % 3_600_000L) / 60_000L
                val seconds = (millisUntilFinished % 60_000L) / 1000L
                tvExamTimeMarks.text = "Time remaining: %02d:%02d:%02d • Total: %d marks".format(
                    hours, minutes, seconds, examPaper.totalMarks
                )
            }
            override fun onFinish() {
                tvExamTimeMarks.text = "Time is up"
                submitAttempt()
            }
        }.start()
    }

    private fun renderExamView() {
        if (!::examPaper.isInitialized || !::attempt.isInitialized) return
        rendering = true
        containerExamContent.removeAllViews()
        val textPrimary = MaterialColors.getColor(containerExamContent, com.google.android.material.R.attr.colorOnSurface)
        val textSecondary = MaterialColors.getColor(containerExamContent, com.google.android.material.R.attr.colorOnSurfaceVariant)
        val primary = MaterialColors.getColor(containerExamContent, com.google.android.material.R.attr.colorPrimary)

        val notice = TextView(this).apply {
            text = if (attempt.isSubmitted) {
                "Submitted • Your written responses are saved on this device. They have not been sent to a teacher."
            } else {
                "Sample practice paper • Your answers are saved automatically on this device. Submit when you are finished."
            }
            setTextColor(textSecondary)
            textSize = 13f
            setPadding(0, 0, 0, 12)
        }
        containerExamContent.addView(notice)

        examPaper.sections.forEach { section ->
            containerExamContent.addView(TextView(this).apply {
                text = section.sectionTitle
                setTextColor(primary)
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 16, 0, 4)
            })
            containerExamContent.addView(TextView(this).apply {
                text = section.instructions
                setTextColor(textSecondary)
                textSize = 12f
                setPadding(0, 0, 0, 12)
            })

            section.questions.forEach { question ->
                val card = MaterialCardView(this).apply {
                    setCardBackgroundColor(MaterialColors.getColor(containerExamContent, com.google.android.material.R.attr.colorSurface))
                    radius = 16f
                    cardElevation = 1f
                    strokeWidth = 1
                    setStrokeColor(MaterialColors.getColor(containerExamContent, com.google.android.material.R.attr.colorOutline))
                }
                val content = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(20, 16, 20, 16)
                }
                content.addView(TextView(this).apply {
                    text = "Question ${question.questionNumber}. ${question.scenarioOrPrompt}"
                    setTextColor(textPrimary)
                    textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                })

                question.subQuestions.forEach { sub ->
                    val key = answerKey(question.questionNumber, sub.label)
                    content.addView(TextView(this).apply {
                        text = "${sub.label} ${sub.questionText} (${sub.allocatedMarks} marks)"
                        setTextColor(primary)
                        textSize = 13f
                        setPadding(0, 12, 0, 6)
                    })
                    if (showingMarkingScheme && attempt.isSubmitted) {
                        content.addView(TextView(this).apply {
                            text = "Suggested answer:\n${sub.markingSchemeAnswer}\n\nMarking guidance: ${sub.markBreakdown}"
                            setTextColor(textPrimary)
                            textSize = 12f
                        })
                    } else {
                        val answer = EditText(this).apply {
                            hint = "Write your answer here"
                            minLines = 3
                            gravity = android.view.Gravity.TOP or android.view.Gravity.START
                            setText(attempt.answers[key].orEmpty())
                            isEnabled = !attempt.isSubmitted
                            textSize = 14f
                            setPadding(12, 10, 12, 10)
                        }
                        if (!attempt.isSubmitted) {
                            answer.addTextChangedListener(object : TextWatcher {
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                    if (!rendering) saveAnswer(key, s?.toString().orEmpty())
                                }
                                override fun afterTextChanged(s: Editable?) = Unit
                            })
                        }
                        content.addView(answer)
                    }
                }
                card.addView(content)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 8, 0, 16) }
                card.layoutParams = params
                containerExamContent.addView(card)
            }
        }

        if (!attempt.isSubmitted && !showingMarkingScheme) {
            containerExamContent.addView(Button(this).apply {
                text = "Submit assessment"
                isAllCaps = false
                setOnClickListener {
                    AlertDialog.Builder(this@SbaExamActivity)
                        .setTitle("Submit practice assessment?")
                        .setMessage("After submission you will not be able to change your answers on this attempt. Your responses are saved locally only; they have not been sent to a teacher and are not automatically marked.")
                        .setNegativeButton("Continue answering", null)
                        .setPositiveButton("Submit") { _, _ -> submitAttempt() }
                        .show()
                }
            })
        }
        rendering = false
    }

    private fun submitAttempt() {
        if (!::attempt.isInitialized || attempt.isSubmitted) return
        timer?.cancel()
        attempt = attempt.copy(submittedAtMillis = System.currentTimeMillis())
        SbaExamAttemptRepository.save(this, examPaper, attempt)
        val tabs = findViewById<TabLayout>(R.id.tabLayoutSba)
        tabs.getTabAt(1)?.view?.isEnabled = true
        renderExamView()
        AlertDialog.Builder(this)
            .setTitle("Assessment submitted")
            .setMessage("Your answers have been saved on this device. This sample assessment does not automatically award marks. Your answers remain saved on this device and are not yet available to a teacher.")
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun onPause() {
        if (::attempt.isInitialized && !attempt.isSubmitted) {
            SbaExamAttemptRepository.save(this, examPaper, attempt)
        }
        super.onPause()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}
