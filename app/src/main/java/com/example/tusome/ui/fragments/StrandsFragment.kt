package com.example.tusome.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tusome.R
import com.example.tusome.data.repository.CbcRepository
import com.example.tusome.ui.StrandDetailActivity
import com.example.tusome.ui.adapters.StrandAdapter

class StrandsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_strands, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvStrandsList: RecyclerView = view.findViewById(R.id.rvStrandsList)
        val strands = CbcRepository.getStrandsForArea("agri_nutr")

        val adapter = StrandAdapter(strands) { strand ->
            val intent = Intent(requireContext(), StrandDetailActivity::class.java).apply {
                putExtra("STRAND_ID", strand.id)
            }
            startActivity(intent)
        }

        rvStrandsList.layoutManager = LinearLayoutManager(requireContext())
        rvStrandsList.adapter = adapter
    }
}