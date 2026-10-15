package com.example.tusome.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.tusome.R
import com.example.tusome.data.local.ActivityProgressManager
import com.example.tusome.data.repository.CbcRepository

class ProfileFragment : Fragment() {

    private lateinit var progressManager: ActivityProgressManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressManager = ActivityProgressManager(requireContext())
        loadProfileData(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadProfileData(it) }
    }

    private fun loadProfileData(view: View) {
        val profile = CbcRepository.learnerProfile
        val completedCount = progressManager.getCompletedSubStrandsCount()

        view.findViewById<TextView>(R.id.tvProfileName).text = profile.name
        view.findViewById<TextView>(R.id.tvProfileAdm).text =
            "Adm No: ${profile.admissionNo} • ${profile.currentGrade.title} • $completedCount Activities Completed"
        view.findViewById<TextView>(R.id.tvSchoolName).text = "School: ${profile.schoolName}"
        view.findViewById<TextView>(R.id.tvCountyName).text =
            "County: ${profile.county} (${profile.subCounty} Sub-County)"
        view.findViewById<TextView>(R.id.tvParentContact).text =
            "Guardian Contact: ${profile.parentGuardianContact}"
    }
}