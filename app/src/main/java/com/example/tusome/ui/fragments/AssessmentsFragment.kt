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
import com.example.tusome.ui.adapters.RubricAdapter

class AssessmentsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_assessments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvRubricsList: RecyclerView = view.findViewById(R.id.rvRubricsList)
        val rubrics = CbcRepository.getAllRubrics()

        val adapter = RubricAdapter(rubrics)
        rvRubricsList.layoutManager = LinearLayoutManager(requireContext())
        rvRubricsList.adapter = adapter
    }
}