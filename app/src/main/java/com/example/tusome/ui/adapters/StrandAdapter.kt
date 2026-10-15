package com.example.tusome.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tusome.R
import com.example.tusome.data.model.Strand
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class StrandAdapter(
    private val strands: List<Strand>,
    private val onStrandClick: (Strand) -> Unit
) : RecyclerView.Adapter<StrandAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStrandNumber: TextView = view.findViewById(R.id.tvStrandNumber)
        val tvSubStrandBadge: TextView = view.findViewById(R.id.tvSubStrandBadge)
        val tvStrandTitle: TextView = view.findViewById(R.id.tvStrandTitle)
        val tvStrandDesc: TextView = view.findViewById(R.id.tvStrandDesc)
        val chipGroupCompetencies: ChipGroup = view.findViewById(R.id.chipGroupCompetencies)
        val btnViewStrand: Button = view.findViewById(R.id.btnViewStrand)
        val ivStatusCheck: ImageView = view.findViewById(R.id.ivStatusCheck)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_strand, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val strand = strands[position]
        holder.tvStrandNumber.text = "Strand ${strand.strandNumber}"
        holder.tvSubStrandBadge.text = "${strand.subStrands.size} Sub-strands"
        holder.tvStrandTitle.text = strand.title
        holder.tvStrandDesc.text = strand.description

        holder.chipGroupCompetencies.removeAllViews()
        strand.coreCompetencies.forEach { comp ->
            val chip = Chip(holder.itemView.context).apply {
                text = comp
                setChipBackgroundColorResource(R.color.primary_container_green)
                setTextColor(context.getColor(R.color.primary_green))
                chipMinHeight = 28f
                textSize = 11f
            }
            holder.chipGroupCompetencies.addView(chip)
        }

        val actionListener = View.OnClickListener { onStrandClick(strand) }
        holder.btnViewStrand.setOnClickListener(actionListener)
        holder.itemView.setOnClickListener(actionListener)
    }

    override fun getItemCount(): Int = strands.size
}