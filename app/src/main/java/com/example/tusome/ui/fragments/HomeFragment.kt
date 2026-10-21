package com.example.tusome.ui.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tusome.R
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.model.LearningArea
import com.example.tusome.data.repository.CbcRepository
import com.example.tusome.data.repository.SupabaseCurriculumRepository
import com.example.tusome.ui.StrandDetailActivity
import com.example.tusome.ui.adapters.GradeChipAdapter
import com.example.tusome.ui.adapters.LearningAreaAdapter
import com.example.tusome.ui.dialogs.LearningAreaBottomSheetDialog
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeFragment : Fragment() {

    private var selectedGrade = GradeLevel.GRADE_4
    private var gradeAdapter: GradeChipAdapter? = null
    private var learningAreasRecyclerView: RecyclerView? = null
    private var learningAreasHeading: TextView? = null
    private var viewAllLearningAreas: TextView? = null
    private var curriculumSyncStatus: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SupabaseCurriculumRepository.initialize(requireContext())

        selectedGrade = readSavedGrade()

        val rvGradeChips: RecyclerView = view.findViewById(R.id.rvGradeChips)
        learningAreasRecyclerView = view.findViewById(R.id.rvLearningAreas)
        learningAreasHeading = view.findViewById(R.id.tvLearningAreasHeading)
        curriculumSyncStatus = view.findViewById(R.id.tvCurriculumSyncStatus)
        val btnStartHeroActivity: Button =
            view.findViewById(R.id.btnStartHeroActivity)
        viewAllLearningAreas = view.findViewById(R.id.tvViewAllLearningAreas)

        gradeAdapter = GradeChipAdapter(
            GradeLevel.values().toList(),
            selectedGrade
        ) { grade ->
            if (selectedGrade != grade) {
                selectedGrade = grade
                saveSelectedGrade(grade)
                updateLearningAreasHeading()
                learningAreasRecyclerView?.let(::loadLearningAreas)
            }
        }

        rvGradeChips.layoutManager =
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
        rvGradeChips.adapter = gradeAdapter

        updateLearningAreasHeading()
        learningAreasRecyclerView?.let(::loadLearningAreas)

        btnStartHeroActivity.setOnClickListener {
            showLearningAreaDialog(
                title = "Agriculture & Nutrition",
                kiswahiliTitle = "Kilimo na Lishe",
                areaId = "agriculture_nutrition",
                iconResId = R.drawable.ic_leaf
            )
        }

        viewAllLearningAreas?.setOnClickListener {
            activity
                ?.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                ?.selectedItemId = R.id.navigation_strands
        }
    }

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

        val savedGrade = readSavedGrade()
        if (savedGrade != selectedGrade) {
            selectedGrade = savedGrade
            gradeAdapter?.setSelectedGrade(savedGrade)
            updateLearningAreasHeading()
            learningAreasRecyclerView?.let(::loadLearningAreas)
        }
    }

    private fun showLearningAreaDialog(
        title: String,
        kiswahiliTitle: String,
        areaId: String,
        iconResId: Int
    ) {
        val requestedGrade = selectedGrade
        curriculumSyncStatus?.text = "Loading $title curriculum details…"

        SupabaseCurriculumRepository.fetchLearningAreaOverview(
            requestedGrade,
            areaId
        ) { result ->
            if (!isAdded || selectedGrade != requestedGrade) {
                return@fetchLearningAreaOverview
            }

            curriculumSyncStatus?.text = "Curriculum details loaded"

            val overviewText = result.getOrElse {
                "Unable to load the live curriculum details. Connect to the internet and try again."
            }

            LearningAreaBottomSheetDialog.newInstance(
                title = title,
                kiswahiliTitle = kiswahiliTitle,
                gradeTitle = requestedGrade.title,
                overviewText = overviewText,
                areaId = areaId,
                iconResId = iconResId
            ).show(childFragmentManager, "LearningAreaOverview")
        }
    }

    private fun updateLearningAreasHeading() {
        learningAreasHeading?.text = "${selectedGrade.title} Learning Areas"
    }

    private fun loadLearningAreas(recyclerView: RecyclerView) {
        val requestedGrade = selectedGrade
        curriculumSyncStatus?.text = "Loading CBC catalogue…"

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
                    "Connected to Tusome, but no published learning areas are listed for ${requestedGrade.title}."
                } else {
                    "Curriculum catalogue available • ${areas.size} learning areas"
                }
                renderLearningAreas(recyclerView, areas, fromLiveCatalogue = true)
            }.onFailure {
                if (requestedGrade == GradeLevel.GRADE_4) {
                    curriculumSyncStatus?.text =
                        "Offline • showing the bundled sample catalogue, not live KICD data"
                    renderLearningAreas(
                        recyclerView,
                        CbcRepository.getLearningAreas(requestedGrade),
                        fromLiveCatalogue = false
                    )
                } else {
                    curriculumSyncStatus?.text =
                        "Offline • no saved curriculum catalogue is available for ${requestedGrade.title} yet"
                    renderLearningAreas(
                        recyclerView,
                        emptyList(),
                        fromLiveCatalogue = false
                    )
                }
            }
        }
    }

    private fun renderLearningAreas(
        recyclerView: RecyclerView,
        areas: List<LearningArea>,
        fromLiveCatalogue: Boolean
    ) {
        viewAllLearningAreas?.text = "View All (${areas.size})"
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 1)
        recyclerView.adapter = LearningAreaAdapter(areas) { area ->
            if (fromLiveCatalogue) {
                val requestedGrade = area.gradeLevel
                curriculumSyncStatus?.text =
                    "Loading ${area.title} curriculum…"

                SupabaseCurriculumRepository.fetchLearningAreaOverview(
                    requestedGrade,
                    area.id
                ) { result ->
                    if (!isAdded || selectedGrade != requestedGrade) {
                        return@fetchLearningAreaOverview
                    }

                    curriculumSyncStatus?.text =
                        "Live Tusome curriculum connected"

                    LearningAreaBottomSheetDialog.newInstance(
                        title = area.title,
                        kiswahiliTitle = area.kiswahiliTitle,
                        gradeTitle = requestedGrade.title,
                        overviewText = result.getOrElse {
                            "Unable to load curriculum details. Check your internet connection and try again."
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
                    "Detailed learning content is not available in the offline catalogue yet.",
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
        gradeAdapter = null
        super.onDestroyView()
    }
}
