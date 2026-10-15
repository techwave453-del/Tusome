package com.example.tusome.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tusome.R
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.model.Strand
import com.example.tusome.data.repository.CbcRepository
import com.example.tusome.ui.StrandDetailActivity
import com.example.tusome.ui.adapters.StrandAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.util.Locale

class StrandsFragment : Fragment() {

    private var selectedLearningAreaId: String? = null
    private var searchQuery: String = ""
    private lateinit var allStrands: List<Strand>
    private lateinit var rvStrandsList: RecyclerView
    private lateinit var chipGroupSubjects: ChipGroup
    private lateinit var layoutEmptyState: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_strands, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvStrandsList = view.findViewById(R.id.rvStrandsList)
        chipGroupSubjects = view.findViewById(R.id.chipGroupSubjects)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        val etSearchStrands: EditText = view.findViewById(R.id.etSearchStrands)

        val learningAreas = CbcRepository.getLearningAreas(GradeLevel.GRADE_4)
        allStrands = learningAreas.flatMap { area ->
            CbcRepository.getStrandsForArea(area.id)
        }

        rvStrandsList.layoutManager = LinearLayoutManager(requireContext())
        setupSubjectFilters(learningAreas.map { it.id to it.title })
        etSearchStrands.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString().orEmpty()
                renderStrands()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        renderStrands()
    }

    private fun setupSubjectFilters(areas: List<Pair<String, String>>) {
        chipGroupSubjects.isSingleSelection = true
        chipGroupSubjects.isSelectionRequired = true

        val allChip = Chip(requireContext()).apply {
            text = "All subjects"
            isCheckable = true
            isChecked = true
            setOnClickListener {
                selectedLearningAreaId = null
                renderStrands()
            }
        }
        chipGroupSubjects.addView(allChip)

        areas.forEach { (areaId, title) ->
            val chip = Chip(requireContext()).apply {
                text = title
                isCheckable = true
                setOnClickListener {
                    selectedLearningAreaId = areaId
                    renderStrands()
                }
            }
            chipGroupSubjects.addView(chip)
        }
    }

    private fun renderStrands() {
        if (!::rvStrandsList.isInitialized || !::allStrands.isInitialized) return

        val query = searchQuery.trim().lowercase(Locale.ROOT)
        val filtered = allStrands.filter { strand ->
            val matchesArea = selectedLearningAreaId == null ||
                strand.learningAreaId == selectedLearningAreaId
            val matchesQuery = query.isBlank() ||
                strand.title.lowercase(Locale.ROOT).contains(query) ||
                strand.description.lowercase(Locale.ROOT).contains(query) ||
                strand.coreCompetencies.any { it.lowercase(Locale.ROOT).contains(query) } ||
                strand.subStrands.any { sub ->
                    sub.title.lowercase(Locale.ROOT).contains(query) ||
                        sub.specificLearningOutcomes.any { it.lowercase(Locale.ROOT).contains(query) }
                }
            matchesArea && matchesQuery
        }

        rvStrandsList.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        layoutEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        rvStrandsList.adapter = StrandAdapter(filtered) { strand ->
            val intent = Intent(requireContext(), StrandDetailActivity::class.java).apply {
                putExtra("LEARNING_AREA_ID", strand.learningAreaId)
                putExtra("STRAND_ID", strand.id)
            }
            startActivity(intent)
        }
    }
}