package com.example.tusome.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tusome.R
import com.example.tusome.data.model.LearningArea

class LearningAreaAdapter(
    private val items: List<LearningArea>,
    private val onItemClick: (LearningArea) -> Unit
) : RecyclerView.Adapter<LearningAreaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivSubjectIcon: ImageView = view.findViewById(R.id.ivSubjectIcon)
        val tvGradeBadge: TextView = view.findViewById(R.id.tvGradeBadge)
        val tvSubjectTitle: TextView = view.findViewById(R.id.tvSubjectTitle)
        val tvSubjectKiswahili: TextView = view.findViewById(R.id.tvSubjectKiswahili)
        val tvStrandsCount: TextView = view.findViewById(R.id.tvStrandsCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_learning_area, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvGradeBadge.text = "${item.gradeLevel.title} • CBC"
        holder.tvSubjectTitle.text = item.title
        holder.tvSubjectKiswahili.text = item.kiswahiliTitle
        holder.tvSubjectKiswahili.visibility =
            if (item.kiswahiliTitle.isBlank()) View.GONE else View.VISIBLE
        holder.tvStrandsCount.text = if (item.strandsCount > 0) {
            "${item.strandsCount} strands • ${item.subStrandsCount} sub-strands"
        } else {
            "Explore available curriculum"
        }
        holder.ivSubjectIcon.setImageResource(illustrationFor(item.title))
        holder.ivSubjectIcon.contentDescription = null
        holder.itemView.contentDescription =
            "${item.title}. ${item.kiswahiliTitle}. ${holder.tvStrandsCount.text}. Open learning area."

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    private fun illustrationFor(title: String): Int {
        val name = title.lowercase()
        return when {
            listOf("agriculture", "nutrition", "environment", "nature", "livestock").any(name::contains) ->
                R.drawable.illustration_learning_area_agriculture
            listOf("mathematics", "math", "numeracy").any(name::contains) ->
                R.drawable.illustration_learning_area_math
            listOf("english", "kiswahili", "language", "literacy", "communication").any(name::contains) ->
                R.drawable.illustration_learning_area_language
            listOf("science", "technology", "integrated science", "computer", "digital").any(name::contains) ->
                R.drawable.illustration_learning_area_science
            listOf("creative", "arts", "music", "sport", "physical", "health", "movement").any(name::contains) ->
                R.drawable.illustration_learning_area_creative
            else -> R.drawable.illustration_learning_area_general
        }
    }

    override fun getItemCount(): Int = items.size
}