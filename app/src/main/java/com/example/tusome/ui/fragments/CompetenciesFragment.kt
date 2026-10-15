package com.example.tusome.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tusome.R
import com.example.tusome.data.repository.CbcRepository
import com.example.tusome.ui.adapters.CompetencyAdapter

class CompetenciesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_competencies, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvCompetenciesList: RecyclerView = view.findViewById(R.id.rvCompetenciesList)
        val competencies = CbcRepository.getCoreCompetencies()

        val adapter = CompetencyAdapter(competencies)
        rvCompetenciesList.layoutManager = LinearLayoutManager(requireContext())
        rvCompetenciesList.adapter = adapter
    }
}