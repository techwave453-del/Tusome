package com.example.tusome.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tusome.R
import com.example.tusome.data.local.OfflineCurriculumCache
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.model.SbaExamPaper
import com.example.tusome.data.repository.SbaExamRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.tabs.TabLayout

class SbaExamActivity : AppCompatActivity() {

    private lateinit var examPaper: SbaExamPaper
    private lateinit var containerExamContent: LinearLayout
    private lateinit var offlineCache: OfflineCurriculumCache
    private var showingMarkingScheme = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sba_exam)

        offlineCache = OfflineCurriculumCache(this)
        containerExamContent = findViewById(R.id.containerExamContent)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarSba)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val subjectId = intent.getStringExtra("SUBJECT_ID") ?: "chemistry"
        val gradeCode = intent.getStringExtra("GRADE_CODE") ?: "G10"
        val grade = GradeLevel.values().firstOrNull {
            it.code.equals(gradeCode, ignoreCase = true)
        } ?: GradeLevel.GRADE_4

        examPaper = SbaExamRepository.getSbaExamPaper(subjectId, grade)

        findViewById<TextView>(R.id.tvExamHeaderTitle).text = examPaper.title
        findViewById<TextView>(R.id.tvExamSubject).text = "${examPaper.gradeLevel.title.uppercase()} – ${examPaper.subjectTitle} ASSESSMENT"
        findViewById<TextView>(R.id.tvExamTimeMarks).text = "Time: ${examPaper.timeAllowedMinutes / 60} Hours • Total: ${examPaper.totalMarks} Marks"
        findViewById<TextView>(R.id.tvExamTerm).text = examPaper.termAndYear

        val tabLayout = findViewById<TabLayout>(R.id.tabLayoutSba)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                showingMarkingScheme = tab?.position == 1
                renderExamView()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })

        renderExamView()
    }

    private fun renderExamView() {
        containerExamContent.removeAllViews()
        val textColorPrimary = MaterialColors.getColor(containerExamContent, com.google.android.material.R.attr.colorOnSurface)
        val textColorSecondary = MaterialColors.getColor(containerExamContent, com.google.android.material.R.attr.colorOnSurfaceVariant)
        val primaryColor = MaterialColors.getColor(containerExamContent, com.google.android.material.R.attr.colorPrimary)

        examPaper.sections.forEach { section ->
            // Section Header
            val sectionHeader = TextView(this).apply {
                text = section.sectionTitle
                setTextColor(primaryColor)
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setPadding(0, 16, 0, 4)
            }
            containerExamContent.addView(sectionHeader)

            val sectionInstructions = TextView(this).apply {
                text = section.instructions
                setTextColor(textColorSecondary)
                textSize = 12f
                setPadding(0, 0, 0, 12)
            }
            containerExamContent.addView(sectionInstructions)

            // Render Questions / Marking Scheme
            section.questions.forEach { question ->
                val card = MaterialCardView(this).apply {
                    setCardBackgroundColor(MaterialColors.getColor(containerExamContent, com.google.android.material.R.attr.colorSurface))
                    radius = 16f
                    cardElevation = 2f
                    strokeWidth = 1
                    setStrokeColor(MaterialColors.getColor(containerExamContent, com.google.android.material.R.attr.colorOutline))

                    val layout = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(20, 16, 20, 16)

                        // Question Prompt/Scenario
                        val tvPrompt = TextView(context).apply {
                            text = "Question ${question.questionNumber}. ${question.scenarioOrPrompt}"
                            setTextColor(textColorPrimary)
                            textSize = 14f
                            setTypeface(null, Typeface.BOLD)
                        }
                        addView(tvPrompt)

                        // Sub-questions or Marking Scheme
                        question.subQuestions.forEach { sub ->
                            val subLayout = LinearLayout(context).apply {
                                orientation = LinearLayout.VERTICAL
                                setPadding(0, 10, 0, 10)

                                if (!showingMarkingScheme) {
                                    // Assessment Question Mode
                                    val tvSubQuestion = TextView(context).apply {
                                        text = "${sub.label} ${sub.questionText} (${sub.allocatedMarks} marks)"
                                        setTextColor(textColorPrimary)
                                        textSize = 13f
                                    }
                                    addView(tvSubQuestion)
                                } else {
                                    // Marking Scheme Mode
                                    val tvSubLabel = TextView(context).apply {
                                        text = "${sub.label} ${sub.questionText} [${sub.allocatedMarks} Marks]"
                                        setTextColor(primaryColor)
                                        textSize = 13f
                                        setTypeface(null, Typeface.BOLD)
                                    }
                                    addView(tvSubLabel)

                                    val tvAnswer = TextView(context).apply {
                                        text = "✅ Marking Scheme Answer:\n${sub.markingSchemeAnswer}"
                                        setTextColor(textColorPrimary)
                                        textSize = 12f
                                        setPadding(0, 4, 0, 2)
                                    }
                                    addView(tvAnswer)

                                    val tvBreakdown = TextView(context).apply {
                                        text = "📍 Mark Allocation: ${sub.markBreakdown}"
                                        setTextColor(textColorSecondary)
                                        textSize = 11f
                                        setTypeface(null, Typeface.ITALIC)
                                    }
                                    addView(tvBreakdown)
                                }
                            }
                            addView(subLayout)
                        }
                    }
                    addView(layout)
                }

                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 16)
                }
                card.layoutParams = layoutParams
                containerExamContent.addView(card)
            }
        }
    }
}