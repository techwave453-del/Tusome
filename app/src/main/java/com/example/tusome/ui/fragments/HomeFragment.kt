package com.example.tusome.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tusome.R
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.repository.CbcRepository
import com.example.tusome.ui.StrandDetailActivity
import com.example.tusome.ui.adapters.GradeChipAdapter
import com.example.tusome.ui.adapters.LearningAreaAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeFragment : Fragment() {

    private var selectedGrade = GradeLevel.GRADE_4
    private var learningAreasRecyclerView: RecyclerView? = null
    private var learningAreasHeading: TextView? = null
    private var viewAllLearningAreas: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvGradeChips: RecyclerView = view.findViewById(R.id.rvGradeChips)
        learningAreasRecyclerView = view.findViewById(R.id.rvLearningAreas)
        learningAreasHeading = view.findViewById(R.id.tvLearningAreasHeading)
        val btnStartHeroActivity: Button = view.findViewById(R.id.btnStartHeroActivity)
        viewAllLearningAreas = view.findViewById(R.id.tvViewAllLearningAreas)

        val gradeAdapter = GradeChipAdapter(GradeLevel.values().toList(), selectedGrade) { grade ->
            selectedGrade = grade
            updateLearningAreasHeading()
            learningAreasRecyclerView?.let(::loadLearningAreas)
        }
        rvGradeChips.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvGradeChips.adapter = gradeAdapter

        updateLearningAreasHeading()
        learningAreasRecyclerView?.let(::loadLearningAreas)

        btnStartHeroActivity.setOnClickListener {
            val intent = Intent(requireContext(), StrandDetailActivity::class.java).apply {
                putExtra("LEARNING_AREA_ID", "agri_nutr")
                putExtra("STRAND_ID", "agri_s1")
            }
            startActivity(intent)
        }

        viewAllLearningAreas?.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                ?.selectedItemId = R.id.navigation_strands
        }
    }

    private fun updateLearningAreasHeading() {
        learningAreasHeading?.text = "${selectedGrade.title} Learning Areas"
    }

    private fun loadLearningAreas(recyclerView: RecyclerView) {
        val learningAreas = CbcRepository.getLearningAreas(selectedGrade)
        viewAllLearningAreas?.text = "View All (${learningAreas.size})"
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 1)
        recyclerView.adapter = LearningAreaAdapter(learningAreas) { area ->
            val intent = Intent(requireContext(), StrandDetailActivity::class.java).apply {
                putExtra("LEARNING_AREA_ID", area.id)
            }
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        learningAreasRecyclerView = null
        learningAreasHeading = null
        viewAllLearningAreas = null
        super.onDestroyView()
    }
}