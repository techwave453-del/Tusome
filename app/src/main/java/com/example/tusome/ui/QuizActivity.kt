package com.example.tusome.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.tusome.R
import com.example.tusome.data.model.PerformanceLevel
import com.example.tusome.data.model.Quiz
import com.example.tusome.data.model.QuizResult
import com.example.tusome.data.repository.QuizRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class QuizActivity : AppCompatActivity() {

    private lateinit var quiz: Quiz
    private var currentQuestionIndex = 0
    private var correctCount = 0
    private var selectedChoiceIndex: Int? = null

    private lateinit var tvQuizTitle: TextView
    private lateinit var tvQuestionCounter: TextView
    private lateinit var tvQuestionText: TextView
    private lateinit var progressQuiz: ProgressBar
    private lateinit var btnChoiceA: MaterialButton
    private lateinit var btnChoiceB: MaterialButton
    private lateinit var btnChoiceC: MaterialButton
    private lateinit var btnChoiceD: MaterialButton
    private lateinit var cardExplanation: MaterialCardView
    private lateinit var tvExplanationText: TextView
    private lateinit var btnNextQuestion: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarQuiz)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val strandId = intent.getStringExtra("STRAND_ID") ?: "agri_s1"
        quiz = QuizRepository.getQuizForStrand(strandId)

        tvQuizTitle = findViewById(R.id.tvQuizTitle)
        tvQuestionCounter = findViewById(R.id.tvQuestionCounter)
        tvQuestionText = findViewById(R.id.tvQuestionText)
        progressQuiz = findViewById(R.id.progressQuiz)
        btnChoiceA = findViewById(R.id.btnChoiceA)
        btnChoiceB = findViewById(R.id.btnChoiceB)
        btnChoiceC = findViewById(R.id.btnChoiceC)
        btnChoiceD = findViewById(R.id.btnChoiceD)
        cardExplanation = findViewById(R.id.cardExplanation)
        tvExplanationText = findViewById(R.id.tvExplanationText)
        btnNextQuestion = findViewById(R.id.btnNextQuestion)

        tvQuizTitle.text = quiz.title

        displayQuestion(currentQuestionIndex)

        btnNextQuestion.setOnClickListener {
            if (currentQuestionIndex < quiz.questions.size - 1) {
                currentQuestionIndex++
                displayQuestion(currentQuestionIndex)
            } else {
                finishQuiz()
            }
        }
    }

    private fun displayQuestion(index: Int) {
        val q = quiz.questions[index]
        selectedChoiceIndex = null

        tvQuestionCounter.text = "Question ${index + 1} of ${quiz.questions.size}"
        tvQuestionText.text = q.questionText
        progressQuiz.progress = ((index + 1) * 100) / quiz.questions.size

        cardExplanation.visibility = View.GONE
        btnNextQuestion.isEnabled = false
        btnNextQuestion.text = if (index == quiz.questions.size - 1) "Complete Quiz" else "Next Question"

        val choices = q.choices
        val choiceButtons = listOf(btnChoiceA, btnChoiceB, btnChoiceC, btnChoiceD)

        choiceButtons.forEachIndexed { i, btn ->
            if (i < choices.size) {
                btn.visibility = View.VISIBLE
                btn.text = "${('A' + i)}. ${choices[i]}"
                btn.isEnabled = true
                btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
                btn.setTextColor(ContextCompat.getColor(this, R.color.text_primary))

                btn.setOnClickListener {
                    onChoiceSelected(i, q.correctAnswerIndex, q.explanation, choiceButtons)
                }
            } else {
                btn.visibility = View.GONE
            }
        }
    }

    private fun onChoiceSelected(
        selectedIndex: Int,
        correctIndex: Int,
        explanation: String,
        choiceButtons: List<MaterialButton>
    ) {
        if (selectedChoiceIndex != null) return
        selectedChoiceIndex = selectedIndex

        choiceButtons.forEach { it.isEnabled = false }

        if (selectedIndex == correctIndex) {
            correctCount++
            choiceButtons[selectedIndex].setBackgroundColor(ContextCompat.getColor(this, R.color.rubric_ee_bg))
            choiceButtons[selectedIndex].setTextColor(ContextCompat.getColor(this, R.color.rubric_ee))
        } else {
            choiceButtons[selectedIndex].setBackgroundColor(ContextCompat.getColor(this, R.color.rubric_be_bg))
            choiceButtons[selectedIndex].setTextColor(ContextCompat.getColor(this, R.color.rubric_be))

            // Highlight correct answer
            choiceButtons[correctIndex].setBackgroundColor(ContextCompat.getColor(this, R.color.rubric_ee_bg))
            choiceButtons[correctIndex].setTextColor(ContextCompat.getColor(this, R.color.rubric_ee))
        }

        tvExplanationText.text = explanation
        cardExplanation.visibility = View.VISIBLE
        btnNextQuestion.isEnabled = true
    }

    private fun finishQuiz() {
        val total = quiz.questions.size
        val percent = (correctCount * 100) / total

        val level = when {
            percent >= 80 -> PerformanceLevel.EXCEEDS
            percent >= 60 -> PerformanceLevel.MEETS
            percent >= 40 -> PerformanceLevel.APPROACHES
            else -> PerformanceLevel.BELOW
        }

        val result = QuizResult(
            quizId = quiz.id,
            strandId = quiz.strandId,
            score = correctCount,
            totalQuestions = total,
            percentage = percent,
            performanceLevel = level,
            timestamp = System.currentTimeMillis()
        )

        QuizRepository.saveQuizResult(this, result)

        AlertDialog.Builder(this)
            .setTitle("Quiz Complete! 🎉")
            .setMessage("Score: $correctCount / $total ($percent%)\nPerformance Level: ${level.label}\n\nYour quiz result has been saved to your CBC portfolio!")
            .setPositiveButton("Return to Strand") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }
}