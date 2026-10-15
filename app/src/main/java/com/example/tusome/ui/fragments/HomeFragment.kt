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

class HomeFragment : Fragment() {

    private var selectedGrade = GradeLevel.GRADE_4

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvGradeChips: RecyclerView = view.findViewById(R.id.rvGradeChips)
        val rvLearningAreas: RecyclerView = view.findViewById(R.id.rvLearningAreas)
        val btnStartHeroActivity: Button = view.findViewById(R.id.btnStartHeroActivity)

        // Setup Grade Selector Chips
        val gradeAdapter = GradeChipAdapter(GradeLevel.values().toList(), selectedGrade) { grade ->
            selectedGrade = grade
            loadLearningAreas(rvLearningAreas)
        }
        rvGradeChips.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvGradeChips.adapter = gradeAdapter

        // Setup Learning Areas Grid
        loadLearningAreas(rvLearningAreas)

        // Hero Activity Button
        btnStartHeroActivity.setOnClickListener {
            val intent = Intent(requireContext(), StrandDetailActivity::class.java).apply {
                putExtra("LEARNING_AREA_ID", "agri_nutr")
                putExtra("STRAND_ID", "agri_s1")
            }
            startActivity(intent)
        }
    }

    private fun loadLearningAreas(recyclerView: RecyclerView) {
        val learningAreas = CbcRepository.getLearningAreas(selectedGrade)
        val adapter = LearningAreaAdapter(learningAreas) { area ->
            val intent = Intent(requireContext(), StrandDetailActivity::class.java).apply {
                putExtra("LEARNING_AREA_ID", area.id)
            }
            startActivity(intent)
        }
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 1)
        recyclerView.adapter = adapter
    }
}