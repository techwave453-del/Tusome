package com.example.tusome.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tusome.R
import com.example.tusome.data.model.GradeLevel

class GradeChipAdapter(
    private val grades: List<GradeLevel>,
    private var selectedGrade: GradeLevel,
    private val onGradeSelected: (GradeLevel) -> Unit
) : RecyclerView.Adapter<GradeChipAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvGradeChip: TextView = view.findViewById(R.id.tvGradeChip)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grade_chip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val grade = grades[position]
        holder.tvGradeChip.text = grade.title

        val isSelected = grade == selectedGrade
        holder.tvGradeChip.setBackgroundResource(
            if (isSelected) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected
        )
        holder.tvGradeChip.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (isSelected) R.color.surface_white else R.color.primary_green
            )
        )

        holder.itemView.setOnClickListener {
            selectedGrade = grade
            notifyDataSetChanged()
            onGradeSelected(grade)
        }
    }

    override fun getItemCount(): Int = grades.size
}