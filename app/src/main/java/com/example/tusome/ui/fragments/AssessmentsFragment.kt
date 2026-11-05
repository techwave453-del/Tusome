package com.example.tusome.ui.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tusome.R
import com.example.tusome.data.local.ActivityProgressManager
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.model.LearningArea
import com.example.tusome.data.model.PerformanceLevel
import com.example.tusome.data.repository.CbcRepository
import com.example.tusome.data.repository.SupabaseCurriculumRepository
import com.example.tusome.ui.SbaExamActivity
import com.example.tusome.ui.adapters.RubricAdapter
import com.google.android.material.button.MaterialButton

class AssessmentsFragment : Fragment() {

    private lateinit var progressManager: ActivityProgressManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_assessments, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressManager = ActivityProgressManager(requireContext())
        loadRubricsAndSummary(view)
        loadSbaPracticePapers(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadRubricsAndSummary(it) }
    }

    private fun selectedGrade(): GradeLevel {
        val code = requireContext()
            .getSharedPreferences("tusome_curriculum", Context.MODE_PRIVATE)
            .getString("selected_grade", GradeLevel.GRADE_4.code)
        return GradeLevel.values().firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?: GradeLevel.GRADE_4
    }

    private fun loadSbaPracticePapers(rootView: View) {
        val status = rootView.findViewById<TextView>(R.id.tvSbaStatus)
        val buttons = rootView.findViewById<LinearLayout>(R.id.layoutSbaSubjects)
        val grade = selectedGrade()
        buttons.removeAllViews()

        if (grade != GradeLevel.GRADE_10) {
            status.text = "Sample practice papers are currently available for Grade 10 only. Change your selected grade in Curriculum to view the available papers."
            return
        }

        status.text = "Loading available Grade 10 sample papers…"
        SupabaseCurriculumRepository.initialize(requireContext())
        SupabaseCurriculumRepository.fetchLearningAreas(grade) { result ->
            if (!isAdded || view !== rootView) return@fetchLearningAreas
            result.onSuccess { areas ->
                val supported = areas.filter { area ->
                    val value = (area.id + " " + area.title).lowercase()
                    value.contains("chemistry") ||
                        value.contains("mathematics") ||
                        value.contains("core math") ||
                        value.contains("agriculture")
                }
                buttons.removeAllViews()
                if (supported.isEmpty()) {
                    status.text = "No matching sample papers are available for the published Grade 10 learning areas yet."
                } else {
                    status.text = "Choose a learning area to open its sample practice paper. These are practice questions, not official examination papers."
                    supported.forEach { area -> addSbaButton(buttons, area) }
                }
            }.onFailure { error ->
                status.text = "Could not load Grade 10 learning areas. Connect to the internet and reopen this screen. " + error.message.orEmpty()
            }
        }
    }

    private fun addSbaButton(container: LinearLayout, area: LearningArea) {
        val button = MaterialButton(requireContext()).apply {
            text = area.title
            isAllCaps = false
            setOnClickListener {
                startActivity(Intent(requireContext(), SbaExamActivity::class.java).apply {
                    putExtra("SUBJECT_ID", area.id)
                    putExtra("GRADE_CODE", area.gradeLevel.code)
                })
            }
        }
        container.addView(button, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
    }

    private fun loadRubricsAndSummary(rootView: View) {
        val rawRubrics = CbcRepository.getAllRubrics()
        var eeCount = 0
        var meCount = 0
        var aeCount = 0
        var beCount = 0

        val updatedRubrics = rawRubrics.map { rubric ->
            val savedLevel = progressManager.getSelfEvaluation(rubric.subStrandId)
            val savedNote = progressManager.getLearnerNote(rubric.subStrandId)
            when (savedLevel) {
                PerformanceLevel.EXCEEDS -> eeCount++
                PerformanceLevel.MEETS -> meCount++
                PerformanceLevel.APPROACHES -> aeCount++
                PerformanceLevel.BELOW -> beCount++
            }
            rubric.copy(
                learnerLevel = savedLevel,
                teacherComment = if (savedNote.isNotBlank()) "Learner Note: $savedNote" else rubric.teacherComment
            )
        }

        rootView.findViewById<TextView>(R.id.tvCountEE)?.text = eeCount.toString()
        rootView.findViewById<TextView>(R.id.tvCountME)?.text = meCount.toString()
        rootView.findViewById<TextView>(R.id.tvCountAE)?.text = aeCount.toString()
        rootView.findViewById<TextView>(R.id.tvCountBE)?.text = beCount.toString()

        val list = rootView.findViewById<RecyclerView>(R.id.rvRubricsList)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = RubricAdapter(updatedRubrics)
    }
}
