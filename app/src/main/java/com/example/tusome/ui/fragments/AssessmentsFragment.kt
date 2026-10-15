package com.example.tusome.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tusome.R
import com.example.tusome.data.local.ActivityProgressManager
import com.example.tusome.data.model.PerformanceLevel
import com.example.tusome.data.repository.CbcRepository
import com.example.tusome.ui.adapters.RubricAdapter

class AssessmentsFragment : Fragment() {

    private lateinit var progressManager: ActivityProgressManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_assessments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressManager = ActivityProgressManager(requireContext())
        loadRubricsAndSummary(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadRubricsAndSummary(it) }
    }

    private fun loadRubricsAndSummary(rootView: View) {
        val subStrandsById = CbcRepository.getLearningAreas(com.example.tusome.data.model.GradeLevel.GRADE_4)
            .flatMap { area -> CbcRepository.getStrandsForArea(area.id) }
            .flatMap { strand -> strand.subStrands }
            .associateBy { it.id }
        val rawRubrics = CbcRepository.getAllRubrics()

        var eeCount = 0
        var meCount = 0
        var aeCount = 0
        var beCount = 0

        val updatedRubrics = rawRubrics.map { r ->
            val savedLevel = progressManager.getSelfEvaluation(r.subStrandId)
            val savedNote = progressManager.getLearnerNote(r.subStrandId)

            when (savedLevel) {
                PerformanceLevel.EXCEEDS -> eeCount++
                PerformanceLevel.MEETS -> meCount++
                PerformanceLevel.APPROACHES -> aeCount++
                PerformanceLevel.BELOW -> beCount++
            }

            val subStrand = subStrandsById[r.subStrandId]
            r.copy(
                learnerLevel = savedLevel,
                teacherComment = if (savedNote.isNotBlank()) "Learner Note: $savedNote" else r.teacherComment,
                subStrandTitle = subStrand?.title.orEmpty(),
                keyInquiryQuestions = subStrand?.keyInquiryQuestions.orEmpty(),
                suggestedExperiences = subStrand?.suggestedExperiences.orEmpty()
            )
        }

        // Update counter badges
        rootView.findViewById<TextView>(R.id.tvCountEE)?.text = eeCount.toString()
        rootView.findViewById<TextView>(R.id.tvCountME)?.text = meCount.toString()
        rootView.findViewById<TextView>(R.id.tvCountAE)?.text = aeCount.toString()
        rootView.findViewById<TextView>(R.id.tvCountBE)?.text = beCount.toString()

        val rvRubricsList: RecyclerView = rootView.findViewById(R.id.rvRubricsList)
        rvRubricsList.layoutManager = LinearLayoutManager(requireContext())
        rvRubricsList.adapter = RubricAdapter(updatedRubrics)
    }
}