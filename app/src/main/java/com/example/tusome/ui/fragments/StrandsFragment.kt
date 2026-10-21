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
import com.example.tusome.data.model.LearningArea
import com.example.tusome.data.model.Strand
import com.example.tusome.data.repository.CbcRepository
import com.example.tusome.data.repository.SupabaseCurriculumRepository
import com.example.tusome.ui.StrandDetailActivity
import com.example.tusome.ui.adapters.StrandAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.util.Locale

class StrandsFragment : Fragment() {

    private var selectedGrade: GradeLevel = GradeLevel.GRADE_4
    private var selectedLearningAreaId: String? = null
    private var searchQuery: String = ""
    private var usingLiveData: Boolean = true
    private var allStrands: List<Strand> = emptyList()
    private lateinit var rvStrandsList: RecyclerView
    private lateinit var chipGroupGrades: ChipGroup
    private lateinit var chipGroupSubjects: ChipGroup
    private lateinit var layoutEmptyState: View
    private lateinit var tvStrandsStatus: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_strands, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvStrandsList = view.findViewById(R.id.rvStrandsList)
        chipGroupGrades = view.findViewById(R.id.chipGroupGrades)
        chipGroupSubjects = view.findViewById(R.id.chipGroupSubjects)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        tvStrandsStatus = view.findViewById(R.id.tvStrandsStatus)
        val etSearchStrands: EditText = view.findViewById(R.id.etSearchStrands)

        rvStrandsList.layoutManager = LinearLayoutManager(requireContext())
        setupGradeFilters()

        etSearchStrands.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString().orEmpty()
                renderStrands()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        loadGradeContent()
    }

    private fun setupGradeFilters() {
        chipGroupGrades.removeAllViews()
        chipGroupGrades.isSingleSelection = true
        chipGroupGrades.isSelectionRequired = true

        GradeLevel.values().forEach { grade ->
            val chip = Chip(requireContext()).apply {
                text = grade.title
                isCheckable = true
                isChecked = grade == selectedGrade
                setOnClickListener {
                    if (selectedGrade != grade) {
                        selectedGrade = grade
                        selectedLearningAreaId = null
                        chipGroupSubjects.clearCheck()
                        setupGradeFilters()
                        loadGradeContent()
                    }
                }
            }
            chipGroupGrades.addView(chip)
        }
    }

    private fun loadGradeContent() {
        val requestedGrade = selectedGrade
        selectedLearningAreaId = null
        allStrands = emptyList()
        chipGroupSubjects.removeAllViews()
        tvStrandsStatus.text = "Loading published strands for ${requestedGrade.title}…"
        renderStrands()

        SupabaseCurriculumRepository.fetchLearningAreas(requestedGrade) { areasResult ->
            if (!isAdded || selectedGrade != requestedGrade) return@fetchLearningAreas

            areasResult.onSuccess { areas ->
                SupabaseCurriculumRepository.fetchStrands(requestedGrade) { strandsResult ->
                    if (!isAdded || selectedGrade != requestedGrade) return@fetchStrands

                    strandsResult.onSuccess { strands ->
                        usingLiveData = true
                        allStrands = strands
                        setupSubjectFilters(areas)
                        tvStrandsStatus.text = if (strands.isEmpty()) {
                            "Supabase connected • no published strand details are available for ${requestedGrade.title} yet."
                        } else {
                            "Live Supabase curriculum • ${strands.size} strands for ${requestedGrade.title}"
                        }
                        renderStrands()
                    }.onFailure {
                        showFallbackOrError(requestedGrade, it.message)
                    }
                }
            }.onFailure {
                showFallbackOrError(requestedGrade, it.message)
            }
        }
    }

    private fun showFallbackOrError(grade: GradeLevel, errorMessage: String?) {
        if (!isAdded || selectedGrade != grade) return
        if (grade == GradeLevel.GRADE_4) {
            val areas = CbcRepository.getLearningAreas(grade)
            usingLiveData = false
            allStrands = areas.flatMap { area -> CbcRepository.getStrandsForArea(area.id) }
            setupSubjectFilters(areas)
            tvStrandsStatus.text =
                "Offline sample • showing bundled Grade 4 content, not live Supabase curriculum."
        } else {
            usingLiveData = true
            allStrands = emptyList()
            chipGroupSubjects.removeAllViews()
            tvStrandsStatus.text =
                "Could not load ${grade.title} from Supabase. Check your connection and try again." +
                    (errorMessage?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: "")
        }
        renderStrands()
    }

    private fun setupSubjectFilters(areas: List<LearningArea>) {
        setupSubjectFilterItems(areas.map { it.id to it.title })
    }

    private fun setupSubjectFilterItems(areas: List<Pair<String, String>>) {
        chipGroupSubjects.removeAllViews()
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
        if (!::rvStrandsList.isInitialized || !::layoutEmptyState.isInitialized) return

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
                putExtra("GRADE_CODE", selectedGrade.code)
                putExtra("FROM_SUPABASE", usingLiveData)
            }
            startActivity(intent)
        }
    }
}
