package com.example.tusome.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tusome.R
import com.example.tusome.data.model.LearningArea
import com.google.android.material.color.MaterialColors

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
        holder.tvStrandsCount.text = holder.itemView.context.getString(R.string.explore_learning_area)
        holder.ivSubjectIcon.setImageResource(item.iconResId)

        val primaryColor = MaterialColors.getColor(
            holder.itemView,
            androidx.appcompat.R.attr.colorPrimary
        )
        holder.ivSubjectIcon.setColorFilter(primaryColor)

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}