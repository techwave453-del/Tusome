package com.example.tusome.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tusome.R
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.repository.CbcRepository
import com.example.tusome.data.repository.SupabaseCurriculumRepository
import com.example.tusome.ui.StrandDetailActivity
import com.example.tusome.ui.adapters.GradeChipAdapter
import com.example.tusome.ui.adapters.LearningAreaAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeFragment : Fragment() {

    private var selectedGrade = GradeLevel.GRADE_4
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

        selectedGrade = GradeLevel.values().firstOrNull {
            it.code.equals(
                requireContext().getSharedPreferences("tusome_curriculum", android.content.Context.MODE_PRIVATE)
                    .getString("selected_grade", GradeLevel.GRADE_4.code),
                ignoreCase = true
            )
        } ?: GradeLevel.GRADE_4

        val rvGradeChips: RecyclerView = view.findViewById(R.id.rvGradeChips)
        learningAreasRecyclerView = view.findViewById(R.id.rvLearningAreas)
        learningAreasHeading = view.findViewById(R.id.tvLearningAreasHeading)
        curriculumSyncStatus = view.findViewById(R.id.tvCurriculumSyncStatus)
        val btnStartHeroActivity: Button = view.findViewById(R.id.btnStartHeroActivity)
        viewAllLearningAreas = view.findViewById(R.id.tvViewAllLearningAreas)

        val gradeAdapter = GradeChipAdapter(GradeLevel.values().toList(), selectedGrade) { grade ->
            selectedGrade = grade
            requireContext().getSharedPreferences("tusome_curriculum", android.content.Context.MODE_PRIVATE)
                .edit().putString("selected_grade", grade.code).apply()
            updateLearningAreasHeading()
            learningAreasRecyclerView?.let(::loadLearningAreas)
        }
        rvGradeChips.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvGradeChips.adapter = gradeAdapter

        updateLearningAreasHeading()
        learningAreasRecyclerView?.let(::loadLearningAreas)

        btnStartHeroActivity.setOnClickListener {
            SupabaseCurriculumRepository.fetchLearningAreaOverview(
                selectedGrade,
                "agriculture_nutrition"
            ) { result ->
                if (!isAdded) return@fetchLearningAreaOverview
                AlertDialog.Builder(requireContext())
                    .setTitle("Agriculture and Nutrition • ${selectedGrade.title}")
                    .setMessage(
                        result.getOrElse {
                            "Unable to load the live curriculum. Connect to the internet and try again."
                        }
                    )
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
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
        val requestedGrade = selectedGrade
        curriculumSyncStatus?.text = "Connecting to the live CBC catalogue…"

        SupabaseCurriculumRepository.fetchLearningAreas(requestedGrade) { result ->
            if (!isAdded || selectedGrade != requestedGrade || learningAreasRecyclerView !== recyclerView) {
                return@fetchLearningAreas
            }

            result.onSuccess { areas ->
                if (areas.isEmpty()) {
                    curriculumSyncStatus?.text =
                        "Connected to Tusome, but no published learning areas are listed for ${requestedGrade.title}."
                    renderLearningAreas(recyclerView, emptyList(), fromLiveCatalogue = true)
                } else {
                    curriculumSyncStatus?.text =
                        "Live Tusome curriculum connected • ${areas.size} learning areas"
                    renderLearningAreas(recyclerView, areas, fromLiveCatalogue = true)
                }
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
                    renderLearningAreas(recyclerView, emptyList(), fromLiveCatalogue = false)
                }
            }
        }
    }

    private fun renderLearningAreas(
        recyclerView: RecyclerView,
        areas: List<com.example.tusome.data.model.LearningArea>,
        fromLiveCatalogue: Boolean
    ) {
        viewAllLearningAreas?.text = "View All (${areas.size})"
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 1)
        recyclerView.adapter = LearningAreaAdapter(areas) { area ->
            if (fromLiveCatalogue) {
                curriculumSyncStatus?.text = "Loading ${area.title} from Supabase…"
                SupabaseCurriculumRepository.fetchLearningAreaOverview(area.gradeLevel, area.id) { result ->
                    if (!isAdded) return@fetchLearningAreaOverview
                    curriculumSyncStatus?.text = "Live Tusome curriculum connected"
                    AlertDialog.Builder(requireContext())
                        .setTitle(area.title)
                        .setMessage(
                            result.getOrElse {
                                "Unable to load curriculum details. Check your internet connection and try again."
                            }
                        )
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            } else if (CbcRepository.getStrandsForArea(area.id).isNotEmpty()) {
                val intent = Intent(requireContext(), StrandDetailActivity::class.java).apply {
                    putExtra("LEARNING_AREA_ID", area.id)
                }
                startActivity(intent)
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
        super.onDestroyView()
    }
}