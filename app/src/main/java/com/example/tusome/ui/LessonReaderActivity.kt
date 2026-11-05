package com.example.tusome.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tusome.R
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.repository.LessonNotesRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView

class LessonReaderActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson_reader)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarLesson)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val subStrandId = intent.getStringExtra("SUBSTRAND_ID") ?: "agri_s1_sub1"
        val subStrandTitle = intent.getStringExtra("SUBSTRAND_TITLE") ?: "Soil Erosion Control"
        val strandId = intent.getStringExtra("STRAND_ID") ?: "agri_s1"
        val gradeCode = intent.getStringExtra("GRADE_CODE") ?: "G4"

        val grade = GradeLevel.values().firstOrNull {
            it.code.equals(gradeCode, ignoreCase = true)
        } ?: GradeLevel.GRADE_4

        val lesson = LessonNotesRepository.getDetailedLessonNotes(subStrandId, subStrandTitle, grade)

        findViewById<TextView>(R.id.tvLessonSubjectGrade).text = "${lesson.subjectTitle.uppercase()} • ${lesson.gradeLevel.title.uppercase()}"
        findViewById<TextView>(R.id.tvLessonTitle).text = lesson.lessonTitle
        findViewById<TextView>(R.id.tvLessonIntro).text = lesson.introduction

        // Render Key Vocabulary
        val containerVocabulary = findViewById<LinearLayout>(R.id.containerVocabulary)
        containerVocabulary.removeAllViews()
        val inflater = LayoutInflater.from(this)

        lesson.keyVocabulary.forEach { (term, definition) ->
            val vocabCard = inflater.inflate(R.layout.item_interactive_substrand, containerVocabulary, false) as MaterialCardView
            vocabCard.removeAllViews()

            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 16, 24, 16)
            }
            val tvTerm = TextView(this).apply {
                text = "📌 $term"
                setTextColor(getColor(R.color.text_primary))
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
            }
            val tvDef = TextView(this).apply {
                text = definition
                setTextColor(getColor(R.color.text_secondary))
                textSize = 12f
                setPadding(0, 4, 0, 0)
            }
            layout.addView(tvTerm)
            layout.addView(tvDef)
            vocabCard.addView(layout)
            containerVocabulary.addView(vocabCard)
        }

        // Render Main Content Sections
        val containerMainSections = findViewById<LinearLayout>(R.id.containerMainSections)
        containerMainSections.removeAllViews()

        lesson.mainSections.forEach { section ->
            val sectionCard = inflater.inflate(R.layout.item_interactive_substrand, containerMainSections, false) as MaterialCardView
            sectionCard.removeAllViews()

            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 20, 24, 20)
            }

            val tvHeading = TextView(this).apply {
                text = section.sectionHeading
                setTextColor(getColor(R.color.text_primary))
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
            }
            val tvExplanation = TextView(this).apply {
                text = section.explanationText
                setTextColor(getColor(R.color.text_secondary))
                textSize = 13f
                setPadding(0, 8, 0, 8)
            }
            layout.addView(tvHeading)
            layout.addView(tvExplanation)

            section.bulletPoints.forEach { point ->
                val tvBullet = TextView(this).apply {
                    text = "• $point"
                    setTextColor(getColor(R.color.text_primary))
                    textSize = 13f
                    setPadding(0, 4, 0, 4)
                }
                layout.addView(tvBullet)
            }

            if (section.localKenyanExample.isNotBlank()) {
                val exCard = MaterialCardView(this).apply {
                    setCardBackgroundColor(getColor(R.color.surface_variant))
                    radius = 12f
                    cardElevation = 0f
                    val exLayout = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(16, 12, 16, 12)
                        val tvEx = TextView(context).apply {
                            text = "🇰🇪 ${section.localKenyanExample}"
                            setTextColor(getColor(R.color.text_primary))
                            textSize = 12f
                            setTypeface(null, Typeface.BOLD)
                        }
                        addView(tvEx)
                    }
                    addView(exLayout)
                }
                layout.addView(exCard)
            }

            sectionCard.addView(layout)
            containerMainSections.addView(sectionCard)
        }

        // Render Step-by-Step Guide
        findViewById<TextView>(R.id.tvStepByStepText).text = lesson.stepByStepGuide.joinToString("\n\n")

        // Render Summary Takeaways
        findViewById<TextView>(R.id.tvSummaryTakeawaysText).text = lesson.summaryTakeaways.joinToString("\n• ", prefix = "• ")

        // Quiz Launcher
        findViewById<Button>(R.id.btnTakeLessonQuiz).setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java).apply {
                putExtra("STRAND_ID", strandId)
            }
            startActivity(intent)
        }
    }
}