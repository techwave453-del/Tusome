package com.example.tusome.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tusome.R
import com.example.tusome.data.model.AssessmentRubric
import com.example.tusome.data.model.PerformanceLevel

class RubricAdapter(
    private val rubrics: List<AssessmentRubric>
) : RecyclerView.Adapter<RubricAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRubricSubstrand: TextView = view.findViewById(R.id.tvRubricSubstrand)
        val tvPerformanceLevelBadge: TextView = view.findViewById(R.id.tvPerformanceLevelBadge)
        val tvRubricCriteria: TextView = view.findViewById(R.id.tvRubricCriteria)
        val tvTeacherRemark: TextView = view.findViewById(R.id.tvTeacherRemark)
        val tvKeyInquiryQuestions: TextView = view.findViewById(R.id.tvKeyInquiryQuestions)
        val tvSuggestedExperiences: TextView = view.findViewById(R.id.tvSuggestedExperiences)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rubric, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rubric = rubrics[position]
        holder.tvRubricCriteria.text = "Criteria: ${rubric.criteria}"
        holder.tvTeacherRemark.text = rubric.teacherComment
        holder.tvKeyInquiryQuestions.text = rubric.keyInquiryQuestions
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "\n") { "• $it" }
            ?.let { "Key Inquiry Questions\n$it" }
            .orEmpty()
        holder.tvKeyInquiryQuestions.visibility =
            if (rubric.keyInquiryQuestions.isEmpty()) View.GONE else View.VISIBLE
        holder.tvSuggestedExperiences.text = rubric.suggestedExperiences
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "\n") { "• $it" }
            ?.let { "Suggested Experiences\n$it" }
            .orEmpty()
        holder.tvSuggestedExperiences.visibility =
            if (rubric.suggestedExperiences.isEmpty()) View.GONE else View.VISIBLE

        val context = holder.itemView.context
        val (bgRes, textColorRes, textLabel) = when (rubric.learnerLevel) {
            PerformanceLevel.EXCEEDS -> Triple(R.drawable.bg_badge_ee, R.color.rubric_ee, "EE - Exceeds Expectations")
            PerformanceLevel.MEETS -> Triple(R.drawable.bg_badge_me, R.color.rubric_me, "ME - Meets Expectations")
            PerformanceLevel.APPROACHES -> Triple(R.drawable.bg_badge_ae, R.color.rubric_ae, "AE - Approaches Expectations")
            PerformanceLevel.BELOW -> Triple(R.drawable.bg_badge_be, R.color.rubric_be, "BE - Below Expectations")
            null -> Triple(R.drawable.bg_badge_me, R.color.rubric_me, "Not evaluated")
        }

        holder.tvPerformanceLevelBadge.setBackgroundResource(bgRes)
        holder.tvPerformanceLevelBadge.setTextColor(ContextCompat.getColor(context, textColorRes))
        holder.tvPerformanceLevelBadge.text = textLabel
        holder.tvRubricSubstrand.text = rubric.subStrandTitle
            .takeIf { it.isNotBlank() }
            ?: "Sub-strand: ${rubric.subStrandId}"
    }

    override fun getItemCount(): Int = rubrics.size
}