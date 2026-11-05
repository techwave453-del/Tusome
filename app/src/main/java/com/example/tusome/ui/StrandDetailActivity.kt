package com.example.tusome.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.example.tusome.R
import com.example.tusome.data.local.ActivityProgressManager
import com.example.tusome.data.local.OfflineCurriculumCache
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.model.PerformanceLevel
import com.example.tusome.data.model.Strand
import com.example.tusome.data.repository.CbcRepository
import com.example.tusome.data.repository.QuizRepository
import com.example.tusome.data.repository.SupabaseCurriculumRepository
import com.example.tusome.data.service.CbcAiTutorService
import com.example.tusome.ui.dialogs.AiTutorBottomSheetDialog
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import org.json.JSONArray
import org.json.JSONObject

class StrandDetailActivity : AppCompatActivity() {

    private lateinit var progressManager: ActivityProgressManager
    private lateinit var offlineCache: OfflineCurriculumCache
    private lateinit var currentStrands: List<Strand>
    private lateinit var containerSubstrands: LinearLayout
    private lateinit var scrollStrandDetail: NestedScrollView
    private lateinit var tvStrandPosition: TextView
    private lateinit var tvStrandProgress: TextView
    private lateinit var progressStrand: ProgressBar
    private lateinit var btnPreviousStrand: Button
    private lateinit var btnNextStrand: Button
    private lateinit var btnTakeStrandQuiz: Button
    private lateinit var tvQuizCardStatus: TextView
    private lateinit var fabAskAiTutor: ExtendedFloatingActionButton

    private lateinit var btnGenerateAiNotes: Button
    private lateinit var btnGenerateAiQuestions: Button
    private lateinit var containerAiGeneratedContent: LinearLayout
    private lateinit var tvAiNotesDisplay: TextView

    private var currentStrandIndex: Int = 0
    private var selectedGrade: GradeLevel = GradeLevel.GRADE_4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_strand_detail)

        progressManager = ActivityProgressManager(this)
        offlineCache = OfflineCurriculumCache(this)

        containerSubstrands = findViewById(R.id.containerSubstrands)
        scrollStrandDetail = findViewById(R.id.scrollStrandDetail)
        tvStrandPosition = findViewById(R.id.tvStrandPosition)
        tvStrandProgress = findViewById(R.id.tvStrandProgress)
        progressStrand = findViewById(R.id.progressStrand)
        btnPreviousStrand = findViewById(R.id.btnPreviousStrand)
        btnNextStrand = findViewById(R.id.btnNextStrand)
        btnTakeStrandQuiz = findViewById(R.id.btnTakeStrandQuiz)
        tvQuizCardStatus = findViewById(R.id.tvQuizCardStatus)
        fabAskAiTutor = findViewById(R.id.fabAskAiTutor)

        btnGenerateAiNotes = findViewById(R.id.btnGenerateAiNotes)
        btnGenerateAiQuestions = findViewById(R.id.btnGenerateAiQuestions)
        containerAiGeneratedContent = findViewById(R.id.containerAiGeneratedContent)
        tvAiNotesDisplay = findViewById(R.id.tvAiNotesDisplay)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarDetail)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val requestedAreaId = intent.getStringExtra("LEARNING_AREA_ID")
        val requestedStrandId = intent.getStringExtra("STRAND_ID")
        val fromSupabase = intent.getBooleanExtra("FROM_SUPABASE", false)
        val gradeCode = intent.getStringExtra("GRADE_CODE")
        val requestedGrade = GradeLevel.values().firstOrNull {
            it.code.equals(gradeCode, ignoreCase = true)
        } ?: if (fromSupabase) {
            Toast.makeText(this, "The selected grade was not supplied. Return and select a grade again.", Toast.LENGTH_LONG).show()
            finish()
            return
        } else {
            GradeLevel.GRADE_4
        }
        selectedGrade = requestedGrade

        btnPreviousStrand.setOnClickListener { navigateStrand(-1) }
        btnNextStrand.setOnClickListener { navigateStrand(1) }

        if (fromSupabase) {
            if (requestedAreaId.isNullOrBlank() || requestedStrandId.isNullOrBlank()) {
                Toast.makeText(this, "The selected learning area and strand were not supplied. Please reopen the selected strand.", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            findViewById<TextView>(R.id.tvDetailTitle).text = "Loading published curriculum…"
            findViewById<TextView>(R.id.tvDetailDesc).text =
                "Fetching ${requestedGrade.title} content from Supabase."
            btnPreviousStrand.isEnabled = false
            btnNextStrand.isEnabled = false

            SupabaseCurriculumRepository.fetchStrands(requestedGrade) { result ->
                if (isFinishing || isDestroyed) return@fetchStrands
                result.onFailure {
                    Toast.makeText(
                        this,
                        "Unable to load ${requestedGrade.title} curriculum. Check your connection and try again.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }.onSuccess { strands ->
                    // Keep navigation scoped to the exact selected learning area and grade.
                    currentStrands = strands.filter { strand ->
                        strand.learningAreaId == requestedAreaId
                    }
                    currentStrandIndex = currentStrands.indexOfFirst { it.id == requestedStrandId }
                    if (currentStrandIndex < 0 && requestedStrandId == null && currentStrands.isNotEmpty()) {
                        currentStrandIndex = 0
                    }
                    if (currentStrandIndex !in currentStrands.indices) {
                        Toast.makeText(
                            this,
                            "No published strand was found for ${requestedGrade.title}.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                        return@onSuccess
                    }
                    displayStrand(currentStrands[currentStrandIndex])
                }
            }
        } else {
            val strandInRequest = if (!requestedAreaId.isNullOrBlank()) {
                CbcRepository.getStrandsForArea(requestedAreaId)
                    .firstOrNull { it.id == requestedStrandId }
            } else {
                CbcRepository.getLearningAreas(requestedGrade)
                    .flatMap { CbcRepository.getStrandsForArea(it.id) }
                    .firstOrNull { it.id == requestedStrandId }
            }

            if (strandInRequest == null) {
                Toast.makeText(this, "This learning strand could not be found.", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            currentStrands = CbcRepository.getStrandsForArea(strandInRequest.learningAreaId)
            currentStrandIndex = currentStrands.indexOfFirst { it.id == strandInRequest.id }
                .coerceAtLeast(0)
            displayStrand(currentStrands[currentStrandIndex])
        }
    }

    override fun onResume() {
        super.onResume()
        if (::currentStrands.isInitialized && currentStrandIndex in currentStrands.indices) {
            refreshQuizCardStatus(currentStrands[currentStrandIndex])
        }
    }

    private fun displayStrand(strand: Strand) {
        findViewById<TextView>(R.id.tvDetailStrandNumber).text = "Strand ${strand.strandNumber}"
        findViewById<TextView>(R.id.tvDetailTitle).text = strand.title
        findViewById<TextView>(R.id.tvDetailDesc).text = strand.description
        supportActionBar?.title = strand.title

        tvStrandPosition.text = "Strand ${currentStrandIndex + 1} of ${currentStrands.size}"
        btnPreviousStrand.isEnabled = currentStrandIndex > 0
        btnNextStrand.isEnabled = currentStrandIndex < currentStrands.lastIndex
        btnPreviousStrand.alpha = if (btnPreviousStrand.isEnabled) 1f else 0.45f
        btnNextStrand.alpha = if (btnNextStrand.isEnabled) 1f else 0.45f

        btnTakeStrandQuiz.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java).apply {
                putExtra("STRAND_ID", strand.id)
            }
            startActivity(intent)
        }

        fabAskAiTutor.setOnClickListener {
            AiTutorBottomSheetDialog.newInstance("Can you explain ${strand.title}?")
                .show(supportFragmentManager, "AiTutor")
        }

        // Setup AI Content Generation & Persistence
        setupAiContentGenerator(strand)

        refreshQuizCardStatus(strand)
        renderSubStrandCards(strand)
        refreshProgressSummary(strand)
    }

    private fun setupAiContentGenerator(strand: Strand) {
        // Load saved offline AI study notes if available
        val savedNotes = offlineCache.getGeneratedAiStudyNotes(strand.id)
        val savedQuestions = offlineCache.getGeneratedAiPracticeQuestions(strand.id)

        if (!savedNotes.isNullOrBlank()) {
            tvAiNotesDisplay.text = savedNotes
            containerAiGeneratedContent.visibility = View.VISIBLE
        } else if (!savedQuestions.isNullOrBlank()) {
            tvAiNotesDisplay.text = savedQuestions
            containerAiGeneratedContent.visibility = View.VISIBLE
        } else {
            containerAiGeneratedContent.visibility = View.GONE
        }

        btnGenerateAiNotes.setOnClickListener {
            CbcAiTutorService.generateStrandStudyNotes(
                strandTitle = strand.title,
                grade = selectedGrade
            ) { notes ->
                tvAiNotesDisplay.text = notes
                containerAiGeneratedContent.visibility = View.VISIBLE
                offlineCache.saveGeneratedAiStudyNotes(strand.id, notes)
                Toast.makeText(this, "AI Study Notes generated & saved locally for offline study!", Toast.LENGTH_SHORT).show()
            }
        }

        btnGenerateAiQuestions.setOnClickListener {
            CbcAiTutorService.generatePracticeQuestions(
                strandTitle = strand.title,
                grade = selectedGrade
            ) { questions ->
                val formattedQuestions = buildString {
                    append("❓ AI GENERATED PRACTICE QUESTIONS:\n\n")
                    questions.forEachIndexed { i, q ->
                        append(i + 1).append(". ").append(q.questionText).append("\n")
                        q.choices.forEachIndexed { j, c ->
                            append("   ").append(('A' + j)).append(". ").append(c).append("\n")
                        }
                        append("   💡 Correct Answer: ").append(('A' + q.correctAnswerIndex)).append(" (").append(q.explanation).append(")\n\n")
                    }
                }
                tvAiNotesDisplay.text = formattedQuestions
                containerAiGeneratedContent.visibility = View.VISIBLE
                offlineCache.saveGeneratedAiPracticeQuestions(strand.id, formattedQuestions)
                Toast.makeText(this, "AI Practice Questions generated & saved locally for offline study!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refreshQuizCardStatus(strand: Strand) {
        val quizResult = QuizRepository.getQuizResultForStrand(this, strand.id)
        if (quizResult != null) {
            tvQuizCardStatus.text = "Latest Score: ${quizResult.score}/${quizResult.totalQuestions} (${quizResult.percentage}%) • Level: ${quizResult.performanceLevel.code}"
        } else {
            tvQuizCardStatus.text = "Test your understanding of this strand"
        }
    }

    private fun navigateStrand(direction: Int) {
        val targetIndex = currentStrandIndex + direction
        if (targetIndex !in currentStrands.indices) return

        saveDraftsForCurrentStrand()
        currentStrandIndex = targetIndex
        displayStrand(currentStrands[currentStrandIndex])
        scrollStrandDetail.scrollTo(0, 0)
    }

    override fun onPause() {
        if (::containerSubstrands.isInitialized && ::progressManager.isInitialized) {
            saveDraftsForCurrentStrand()
        }
        super.onPause()
    }

    private fun renderSubStrandCards(strand: Strand) {
        containerSubstrands.removeAllViews()
        val inflater = LayoutInflater.from(this)

        if (strand.subStrands.isEmpty()) {
            val emptyState = TextView(this).apply {
                text = "No published sub-strands or Specific Learning Outcomes are available for this strand yet. The app will not generate lesson content until the KICD curriculum records are published."
                setTextColor(ContextCompat.getColor(this@StrandDetailActivity, R.color.text_secondary))
                textSize = 14f
                setPadding(20, 24, 20, 24)
            }
            containerSubstrands.addView(emptyState)
            return
        }

        strand.subStrands.forEach { sub ->
            val subCard = inflater.inflate(
                R.layout.item_interactive_substrand,
                containerSubstrands,
                false
            ) as MaterialCardView
            subCard.tag = sub.id

            val tvSubstrandTitle = subCard.findViewById<TextView>(R.id.tvSubstrandTitle)
            val tvStatusBadge = subCard.findViewById<TextView>(R.id.tvStatusBadge)
            val tvLearningOutcomes = subCard.findViewById<TextView>(R.id.tvLearningOutcomes)
            val tvInquiryQuestions = subCard.findViewById<TextView>(R.id.tvInquiryQuestions)
            val tvDigitalTaskPrompt = subCard.findViewById<TextView>(R.id.tvDigitalTaskPrompt)
            val tvSubstrandKicdCitation = subCard.findViewById<TextView>(R.id.tvSubstrandKicdCitation)
            val btnReadFullLesson = subCard.findViewById<Button>(R.id.btnReadFullLesson)
            val btnGenerateSubstrandAi = subCard.findViewById<Button>(R.id.btnGenerateSubstrandAi)
            val etLearnerReflection = subCard.findViewById<EditText>(R.id.etLearnerReflection)
            val cgSelfEvaluation = subCard.findViewById<ChipGroup>(R.id.cgSelfEvaluation)
            val btnSaveProgress = subCard.findViewById<Button>(R.id.btnSaveProgress)

            tvSubstrandTitle.text = "Sub-strand ${sub.subStrandNumber}: ${sub.title}"
            val hasPublishedSlos = sub.specificLearningOutcomes.isNotEmpty()
            btnGenerateSubstrandAi.isEnabled = hasPublishedSlos
            btnGenerateSubstrandAi.text = if (hasPublishedSlos) "Generate with AI" else "Awaiting published KICD SLOs"
            if (!hasPublishedSlos) {
                tvLearningOutcomes.text = "No published Specific Learning Outcomes are linked to this sub-strand yet."
                tvSubstrandKicdCitation.text = "AI generation is disabled until the verified KICD outcomes are published."
            }

            btnReadFullLesson.setOnClickListener {
                val intent = Intent(this, LessonReaderActivity::class.java).apply {
                    putExtra("SUBSTRAND_ID", sub.id)
                    putExtra("SUBSTRAND_TITLE", sub.title)
                    putExtra("STRAND_ID", strand.id)
                    putExtra("GRADE_CODE", selectedGrade.code)
                }
                startActivity(intent)
            }

            // Check if AI generated KICD content exists in local offline cache
            val cachedKicdJson = offlineCache.getSubstrandKicdContent(sub.id)
            if (!cachedKicdJson.isNullOrBlank()) {
                try {
                    val json = JSONObject(cachedKicdJson)
                    val outcomes = json.getJSONArray("outcomes")
                    val questions = json.getJSONArray("questions")
                    val task = json.getString("task")
                    val citation = json.getString("citation")

                    tvLearningOutcomes.text = (0 until outcomes.length()).joinToString("\n") { "• ${outcomes.getString(it)}" }
                    tvInquiryQuestions.text = (0 until questions.length()).joinToString("\n") { "• ${questions.getString(it)}" }
                    tvDigitalTaskPrompt.text = task
                    tvSubstrandKicdCitation.text = citation
                } catch (_: Exception) {
                    tvLearningOutcomes.text = formatBulletList(sub.specificLearningOutcomes, "Outcomes specified in KICD curriculum.")
                    tvInquiryQuestions.text = formatBulletList(sub.keyInquiryQuestions, "Inquiry prompts for interactive group discussion.")
                    tvDigitalTaskPrompt.text = sub.digitalTaskPrompt.ifBlank { "Complete practical activity and upload evidence." }
                }
            } else {
                tvLearningOutcomes.text = formatBulletList(sub.specificLearningOutcomes, "Outcomes specified in KICD curriculum.")
                tvInquiryQuestions.text = formatBulletList(sub.keyInquiryQuestions, "Inquiry prompts for interactive group discussion.")
                tvDigitalTaskPrompt.text = sub.digitalTaskPrompt.ifBlank { "Complete practical activity and upload evidence." }
            }

            btnGenerateSubstrandAi.setOnClickListener {
                if (!hasPublishedSlos) {
                    Toast.makeText(this, "Publish the verified KICD Specific Learning Outcomes for this sub-strand before generating content.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                btnGenerateSubstrandAi.isEnabled = false
                btnGenerateSubstrandAi.text = "Generating…"
                CbcAiTutorService.generateSubStrandKicdContent(
                    context = this,
                    learningAreaId = strand.learningAreaId,
                    strandId = strand.id,
                    subStrandId = sub.id,
                    subStrandTitle = sub.title,
                    strandTitle = strand.title,
                    grade = selectedGrade
                ) { result ->
                    btnGenerateSubstrandAi.isEnabled = true
                    btnGenerateSubstrandAi.text = "Generate with AI"
                    result.onFailure { error ->
                        Toast.makeText(
                            this,
                            error.message ?: "Unable to generate this sub-strand lesson. Check your connection and sign-in.",
                            Toast.LENGTH_LONG
                        ).show()
                    }.onSuccess { kicdContent ->
                        tvLearningOutcomes.text = kicdContent.specificLearningOutcomes.joinToString("\n") { "• $it" }
                        tvInquiryQuestions.text = kicdContent.keyInquiryQuestions.joinToString("\n") { "• $it" }
                        tvDigitalTaskPrompt.text = kicdContent.digitalTaskPrompt
                        tvSubstrandKicdCitation.text = kicdContent.kicdCitation

                        val saveObj = JSONObject().apply {
                            put("outcomes", JSONArray(kicdContent.specificLearningOutcomes))
                            put("questions", JSONArray(kicdContent.keyInquiryQuestions))
                            put("experiences", JSONArray(kicdContent.suggestedExperiences))
                            put("task", kicdContent.digitalTaskPrompt)
                            put("citation", kicdContent.kicdCitation)
                            put("full_content", kicdContent.fullLessonContentJson)
                        }
                        offlineCache.saveSubstrandKicdContent(sub.id, saveObj.toString())
                        Toast.makeText(
                            this,
                            "AI lesson draft generated from this sub-strand and saved offline.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            val isCompleted = progressManager.isSubStrandCompleted(sub.id)
            etLearnerReflection.setText(progressManager.getLearnerNote(sub.id))
            updateSubstrandUiState(subCard, tvStatusBadge, btnSaveProgress, isCompleted)

            when (progressManager.getSelfEvaluation(sub.id)) {
                PerformanceLevel.EXCEEDS -> cgSelfEvaluation.check(R.id.chipEE)
                PerformanceLevel.MEETS -> cgSelfEvaluation.check(R.id.chipME)
                PerformanceLevel.APPROACHES -> cgSelfEvaluation.check(R.id.chipAE)
                PerformanceLevel.BELOW -> cgSelfEvaluation.check(R.id.chipBE)
            }

            btnSaveProgress.setOnClickListener {
                val newCompletedState = !progressManager.isSubStrandCompleted(sub.id)
                saveSubStrandInputs(
                    sub.id,
                    newCompletedState,
                    etLearnerReflection,
                    cgSelfEvaluation
                )
                updateSubstrandUiState(subCard, tvStatusBadge, btnSaveProgress, newCompletedState)
                refreshProgressSummary(strand)

                val msg = if (newCompletedState) {
                    "Sub-strand ${sub.subStrandNumber} marked as completed."
                } else {
                    "Progress updated."
                }
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }

            containerSubstrands.addView(subCard)
        }
    }

    private fun saveDraftsForCurrentStrand() {
        for (index in 0 until containerSubstrands.childCount) {
            val card = containerSubstrands.getChildAt(index) as? MaterialCardView ?: continue
            val subStrandId = card.tag as? String ?: continue
            val reflection = card.findViewById<EditText>(R.id.etLearnerReflection)
            val evaluation = card.findViewById<ChipGroup>(R.id.cgSelfEvaluation)
            saveSubStrandInputs(
                subStrandId,
                progressManager.isSubStrandCompleted(subStrandId),
                reflection,
                evaluation
            )
        }
    }

    private fun saveSubStrandInputs(
        subStrandId: String,
        isCompleted: Boolean,
        reflection: EditText,
        evaluation: ChipGroup
    ) {
        val selectedLevel = when (evaluation.checkedChipId) {
            R.id.chipEE -> PerformanceLevel.EXCEEDS
            R.id.chipAE -> PerformanceLevel.APPROACHES
            R.id.chipBE -> PerformanceLevel.BELOW
            else -> PerformanceLevel.MEETS
        }
        progressManager.saveSubStrandProgress(
            subStrandId = subStrandId,
            isCompleted = isCompleted,
            learnerNote = reflection.text.toString().trim(),
            evaluationLevel = selectedLevel
        )
    }

    private fun refreshProgressSummary(strand: Strand) {
        val total = strand.subStrands.size
        val completed = strand.subStrands.count { progressManager.isSubStrandCompleted(it.id) }
        tvStrandProgress.text = "$completed of $total activities completed"
        progressStrand.progress = if (total == 0) 0 else (completed * 100 / total)
    }

    private fun updateSubstrandUiState(
        card: MaterialCardView,
        statusBadge: TextView,
        btn: Button,
        isCompleted: Boolean
    ) {
        if (isCompleted) {
            statusBadge.text = "COMPLETED"
            statusBadge.setBackgroundResource(R.drawable.bg_badge_ee)
            statusBadge.setTextColor(ContextCompat.getColor(this, R.color.rubric_ee))
            btn.text = "Completed ✓ (Tap to Toggle)"
            btn.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_green_dark))
            card.setStrokeColor(ContextCompat.getColor(this, R.color.primary_green))
            card.strokeWidth = 3
        } else {
            statusBadge.text = "IN PROGRESS"
            statusBadge.setBackgroundResource(R.drawable.bg_chip_unselected)
            statusBadge.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            btn.text = "Mark Activity as Completed"
            btn.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_green))
            card.setStrokeColor(ContextCompat.getColor(this, R.color.surface_variant))
            card.strokeWidth = 1
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