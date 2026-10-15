package com.example.tusome.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tusome.R
import com.example.tusome.data.model.CoreCompetency
import com.google.android.material.progressindicator.LinearProgressIndicator

class CompetencyAdapter(
    private val items: List<CoreCompetency>
) : RecyclerView.Adapter<CompetencyAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCompIcon: ImageView = view.findViewById(R.id.ivCompIcon)
        val tvCompName: TextView = view.findViewById(R.id.tvCompName)
        val tvCompKiswahili: TextView = view.findViewById(R.id.tvCompKiswahili)
        val tvCompPercent: TextView = view.findViewById(R.id.tvCompPercent)
        val progressComp: LinearProgressIndicator = view.findViewById(R.id.progressComp)
        val tvCompDesc: TextView = view.findViewById(R.id.tvCompDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_competency, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comp = items[position]
        holder.tvCompName.text = comp.name
        holder.tvCompKiswahili.text = comp.kiswahiliName
        holder.tvCompPercent.text = "${comp.progressPercent}%"
        holder.progressComp.progress = comp.progressPercent
        holder.tvCompDesc.text = comp.description
        holder.ivCompIcon.setImageResource(comp.iconResId)
    }

    override fun getItemCount(): Int = items.size
}