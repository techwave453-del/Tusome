package com.example.tusome.ui.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tusome.R
import com.example.tusome.data.local.ActivityProgressManager
import com.example.tusome.data.model.GradeCategory
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.model.LearningArea
import com.example.tusome.data.repository.CbcRepository
import com.example.tusome.data.repository.SupabaseCurriculumRepository
import com.example.tusome.ui.StrandDetailActivity
import com.example.tusome.ui.adapters.GradeChipAdapter
import com.example.tusome.ui.adapters.LearningAreaAdapter
import com.example.tusome.ui.dialogs.LearningAreaBottomSheetDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButtonToggleGroup

class HomeFragment : Fragment() {

    private var selectedGrade = GradeLevel.GRADE_4
    private var gradeAdapter: GradeChipAdapter? = null
    private var learningAreasRecyclerView: RecyclerView? = null
    private var learningAreasHeading: TextView? = null
    private var viewAllLearningAreas: TextView? = null
    private var curriculumSyncStatus: TextView? = null
    private var completedActivitiesCount: TextView? = null
    private var assessmentProgressCount: TextView? = null
    private var progressManager: ActivityProgressManager? = null
    private var gradeCategoryGroup: MaterialButtonToggleGroup? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SupabaseCurriculumRepository.initialize(requireContext())
        selectedGrade = readSavedGrade()
        progressManager = ActivityProgressManager(requireContext())

        val rvGradeChips: RecyclerView = view.findViewById(R.id.rvGradeChips)
        learningAreasRecyclerView = view.findViewById(R.id.rvLearningAreas)
        learningAreasHeading = view.findViewById(R.id.tvLearningAreasHeading)
        curriculumSyncStatus = view.findViewById(R.id.tvCurriculumSyncStatus)
        completedActivitiesCount = view.findViewById(R.id.tvCompletedActivitiesCount)
        assessmentProgressCount = view.findViewById(R.id.tvAssessmentProgressCount)
        viewAllLearningAreas = view.findViewById(R.id.tvViewAllLearningAreas)
        gradeCategoryGroup = view.findViewById(R.id.groupGradeCategory)

        gradeAdapter = GradeChipAdapter(
            gradesFor(selectedGrade.category),
            selectedGrade
        ) { grade ->
            if (selectedGrade != grade) {
                selectedGrade = grade
                saveSelectedGrade(grade)
                updateLearningAreasHeading()
                learningAreasRecyclerView?.let(::loadLearningAreas)
            }
        }

        rvGradeChips.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        rvGradeChips.adapter = gradeAdapter

        gradeCategoryGroup?.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) onCategorySelected(checkedId)
        }
        gradeCategoryGroup?.check(categoryButtonFor(selectedGrade.category))

        updateLearningAreasHeading()
        refreshProgressSummary()
        learningAreasRecyclerView?.let(::loadLearningAreas)

        viewAllLearningAreas?.setOnClickListener {
            activity
                ?.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                ?.selectedItemId = R.id.navigation_strands
        }
    }

    private fun onCategorySelected(buttonId: Int) {
        val category = when (buttonId) {
            R.id.btnPrePrimary -> GradeCategory.PRE_PRIMARY
            R.id.btnLowerPrimary -> GradeCategory.LOWER_PRIMARY
            R.id.btnUpperPrimary -> GradeCategory.UPPER_PRIMARY
            R.id.btnJuniorSchool -> GradeCategory.JUNIOR_SECONDARY
            R.id.btnSeniorSchool -> GradeCategory.SENIOR_SECONDARY
            else -> return
        }

        val grades = gradesFor(category)
        gradeAdapter?.setGrades(grades)
        if (selectedGrade.category != category) {
            selectedGrade = grades.first()
            gradeAdapter?.setSelectedGrade(selectedGrade)
            saveSelectedGrade(selectedGrade)
            updateLearningAreasHeading()
            learningAreasRecyclerView?.let(::loadLearningAreas)
        } else {
            gradeAdapter?.setSelectedGrade(selectedGrade)
        }
    }

    private fun categoryButtonFor(category: GradeCategory): Int = when (category) {
        GradeCategory.PRE_PRIMARY -> R.id.btnPrePrimary
        GradeCategory.LOWER_PRIMARY -> R.id.btnLowerPrimary
        GradeCategory.UPPER_PRIMARY -> R.id.btnUpperPrimary
        GradeCategory.JUNIOR_SECONDARY -> R.id.btnJuniorSchool
        GradeCategory.SENIOR_SECONDARY -> R.id.btnSeniorSchool
    }

    private fun gradesFor(category: GradeCategory): List<GradeLevel> =
        GradeLevel.values().filter { it.category == category }

    private fun readSavedGrade(): GradeLevel {
        val savedCode = requireContext()
            .getSharedPreferences("tusome_curriculum", Context.MODE_PRIVATE)
            .getString("selected_grade", GradeLevel.GRADE_4.code)

        return GradeLevel.values().firstOrNull {
            it.code.equals(savedCode, ignoreCase = true)
        } ?: GradeLevel.GRADE_4
    }

    private fun saveSelectedGrade(grade: GradeLevel) {
        requireContext()
            .getSharedPreferences("tusome_curriculum", Context.MODE_PRIVATE)
            .edit()
            .putString("selected_grade", grade.code)
            .apply()
    }

    override fun onResume() {
        super.onResume()
        if (!isAdded || learningAreasRecyclerView == null) return

        refreshProgressSummary()
        val savedGrade = readSavedGrade()
        if (savedGrade != selectedGrade) {
            selectedGrade = savedGrade
            gradeAdapter?.setGrades(gradesFor(savedGrade.category))
            gradeAdapter?.setSelectedGrade(savedGrade)
            gradeCategoryGroup?.check(categoryButtonFor(savedGrade.category))
            updateLearningAreasHeading()
            learningAreasRecyclerView?.let(::loadLearningAreas)
        }
    }

    private fun refreshProgressSummary() {
        val manager = progressManager ?: return
        completedActivitiesCount?.text = manager.getCompletedSubStrandsCount().toString()
        assessmentProgressCount?.text = manager.getSavedSelfEvaluationsCount().toString()
    }

    private fun updateLearningAreasHeading() {
        learningAreasHeading?.text = "${selectedGrade.title} learning areas"
    }

    private fun loadLearningAreas(recyclerView: RecyclerView) {
        val requestedGrade = selectedGrade
        curriculumSyncStatus?.text = "Loading CBC curriculum…"

        SupabaseCurriculumRepository.fetchLearningAreas(requestedGrade) { result ->
            if (
                !isAdded ||
                selectedGrade != requestedGrade ||
                learningAreasRecyclerView !== recyclerView
            ) {
                return@fetchLearningAreas
            }

            result.onSuccess { areas ->
                curriculumSyncStatus?.text = if (areas.isEmpty()) {
                    "No published learning areas are currently listed for ${requestedGrade.title}."
                } else {
                    "Curriculum available • ${areas.size} learning areas"
                }
                renderLearningAreas(recyclerView, areas, fromRepositoryCatalogue = true)
            }.onFailure {
                if (requestedGrade == GradeLevel.GRADE_4) {
                    curriculumSyncStatus?.text =
                        "Offline • showing the bundled sample catalogue, not live KICD data"
                    renderLearningAreas(
                        recyclerView,
                        CbcRepository.getLearningAreas(requestedGrade),
                        fromRepositoryCatalogue = false
                    )
                } else {
                    curriculumSyncStatus?.text =
                        "Offline • no bundled sample catalogue is available for ${requestedGrade.title}"
                    renderLearningAreas(recyclerView, emptyList(), fromRepositoryCatalogue = false)
                }
            }
        }
    }

    private fun renderLearningAreas(
        recyclerView: RecyclerView,
        areas: List<LearningArea>,
        fromRepositoryCatalogue: Boolean
    ) {
        viewAllLearningAreas?.text = "View all (${areas.size})"
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = LearningAreaAdapter(areas) { area ->
            if (fromRepositoryCatalogue) {
                val requestedGrade = area.gradeLevel
                curriculumSyncStatus?.text = "Loading ${area.title} curriculum…"

                SupabaseCurriculumRepository.fetchLearningAreaOverview(
                    requestedGrade,
                    area.id
                ) { result ->
                    if (!isAdded || selectedGrade != requestedGrade) {
                        return@fetchLearningAreaOverview
                    }

                    curriculumSyncStatus?.text = "Curriculum details available"
                    LearningAreaBottomSheetDialog.newInstance(
                        title = area.title,
                        kiswahiliTitle = area.kiswahiliTitle,
                        gradeTitle = requestedGrade.title,
                        overviewText = result.getOrElse {
                            "Curriculum details could not be loaded right now. If you are offline, reconnect and try again."
                        },
                        areaId = area.id,
                        iconResId = area.iconResId
                    ).show(childFragmentManager, "LearningAreaOverview")
                }
            } else if (CbcRepository.getStrandsForArea(area.id).isNotEmpty()) {
                startActivity(
                    Intent(requireContext(), StrandDetailActivity::class.java).apply {
                        putExtra("LEARNING_AREA_ID", area.id)
                    }
                )
            } else {
                Toast.makeText(
                    requireContext(),
                    "Detailed learning content is not available in the bundled offline sample yet.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        learningAreasRecyclerView = null
        learningAreasHeading = null
        viewAllLearningAreas = null
        curriculumSyncStatus = null
        completedActivitiesCount = null
        assessmentProgressCount = null
        progressManager = null
        gradeAdapter = null
        gradeCategoryGroup = null
        super.onDestroyView()
    }
}
