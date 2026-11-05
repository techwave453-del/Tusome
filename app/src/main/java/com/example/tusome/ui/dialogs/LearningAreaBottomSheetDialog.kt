package com.example.tusome.ui.dialogs

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tusome.R
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.model.Strand
import com.example.tusome.data.repository.SupabaseCurriculumRepository
import com.example.tusome.ui.StrandDetailActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors
import java.util.Locale

class LearningAreaBottomSheetDialog : BottomSheetDialogFragment() {
    private var areaTitle = ""
    private var kiswahiliTitle = ""
    private var gradeTitle = ""
    private var gradeCode = ""
    private var areaId = ""
    private var iconResId = R.drawable.ic_leaf
    private var allStrands: List<Strand> = emptyList()
    private var filterQuery = ""
    private var rvStrands: RecyclerView? = null
    private var tvStatus: TextView? = null

    companion object {
        fun newInstance(title: String, kiswahiliTitle: String, gradeTitle: String,
            overviewText: String, areaId: String, iconResId: Int, gradeCode: String) =
            LearningAreaBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putString("TITLE", title)
                    putString("KISWAHILI", kiswahiliTitle)
                    putString("GRADE", gradeTitle)
                    putString("GRADE_CODE", gradeCode)
                    putString("AREA_ID", areaId)
                    putInt("ICON", iconResId)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            areaTitle = args.getString("TITLE", "Learning Area")
            kiswahiliTitle = args.getString("KISWAHILI", "")
            gradeTitle = args.getString("GRADE", "Grade 4")
            gradeCode = args.getString("GRADE_CODE", "")
            areaId = args.getString("AREA_ID", "")
            iconResId = args.getInt("ICON", R.drawable.ic_leaf)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.dialog_learning_area_overview, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SupabaseCurriculumRepository.initialize(requireContext())
        view.findViewById<TextView>(R.id.tvDialogTitle).text = areaTitle
        view.findViewById<TextView>(R.id.tvDialogKiswahili).text = kiswahiliTitle.ifBlank { areaTitle }
        view.findViewById<TextView>(R.id.tvDialogGradeBadge).text = gradeTitle
        view.findViewById<ImageView>(R.id.ivDialogIcon).setImageResource(iconResId)
        setupCompetencyChips(view.findViewById(R.id.cgDialogCompetencies))

        val search = view.findViewById<EditText>(R.id.etDialogSearch)
        rvStrands = view.findViewById(R.id.rvDialogStrands)
        tvStatus = view.findViewById(R.id.tvDialogCurriculumStatus)
        rvStrands?.layoutManager = LinearLayoutManager(requireContext())
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterQuery = s?.toString().orEmpty()
                renderStrandsList()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        view.findViewById<Button>(R.id.btnDialogClose).setOnClickListener { dismiss() }
        loadPublishedStrands()
    }

    private fun loadPublishedStrands() {
        val grade = GradeLevel.values().firstOrNull { it.code.equals(gradeCode, ignoreCase = true) }
        if (grade == null || areaId.isBlank()) {
            showStatus("The selected grade or learning area is missing. Reopen this learning area and try again.")
            return
        }
        showStatus("Loading published strands, sub-strands and KICD learning outcomes…")
        SupabaseCurriculumRepository.fetchStrands(grade) { result ->
            if (!isAdded) return@fetchStrands
            result.onSuccess { strands ->
                allStrands = strands.filter { it.learningAreaId == areaId }
                if (allStrands.isEmpty()) {
                    showStatus("No published strands are available for " + areaTitle + " in " + gradeTitle + " yet.")
                } else {
                    showStatus("Showing " + allStrands.size + " published strands and their linked curriculum records.")
                    renderStrandsList()
                }
            }.onFailure { error ->
                showStatus("Unable to load the published KICD curriculum. " + error.message.orEmpty())
            }
        }
    }

    private fun showStatus(message: String) {
        tvStatus?.text = message
        tvStatus?.visibility = View.VISIBLE
        rvStrands?.visibility = if (allStrands.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun setupCompetencyChips(chipGroup: ChipGroup) {
        listOf("Critical Thinking", "Digital Literacy", "Environmental Citizenship", "Communication").forEach { comp ->
            chipGroup.addView(Chip(requireContext()).apply {
                text = comp
                setChipBackgroundColorResource(R.color.surface_variant)
                setTextColor(MaterialColors.getColor(chipGroup, androidx.appcompat.R.attr.colorPrimary))
                textSize = 11f
            })
        }
    }

    private fun renderStrandsList() {
        val recycler = rvStrands ?: return
        val query = filterQuery.trim().lowercase(Locale.ROOT)
        val filtered = allStrands.filter { strand ->
            query.isBlank() || strand.title.lowercase(Locale.ROOT).contains(query) ||
                strand.subStrands.any { sub ->
                    sub.title.lowercase(Locale.ROOT).contains(query) ||
                        sub.specificLearningOutcomes.any { it.lowercase(Locale.ROOT).contains(query) }
                }
        }
        recycler.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        recycler.adapter = DialogStrandAdapter(filtered) { strand ->
            startActivity(Intent(requireContext(), StrandDetailActivity::class.java).apply {
                putExtra("LEARNING_AREA_ID", areaId)
                putExtra("STRAND_ID", strand.id)
                putExtra("GRADE_CODE", gradeCode)
                putExtra("FROM_SUPABASE", true)
            })
            dismiss()
        }
    }

    private class DialogStrandAdapter(
        private val items: List<Strand>,
        private val onStrandClick: (Strand) -> Unit
    ) : RecyclerView.Adapter<DialogStrandAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val badge: TextView = view.findViewById(R.id.tvDialogStrandBadge)
            val title: TextView = view.findViewById(R.id.tvDialogStrandTitle)
            val subContainer: LinearLayout = view.findViewById(R.id.containerDialogSubstrands)
            val action: Button = view.findViewById(R.id.btnDialogStartActivity)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_dialog_strand_card, parent, false)
        )

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val strand = items[position]
            holder.badge.text = "Strand " + strand.strandNumber
            holder.title.text = strand.title
            holder.subContainer.removeAllViews()
            val titleColor = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOnSurface)
            val textColor = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOnSurfaceVariant)

            if (strand.subStrands.isEmpty()) {
                holder.subContainer.addView(TextView(holder.itemView.context).apply {
                    text = "No published sub-strands are linked to this strand yet."
                    setTextColor(textColor)
                    textSize = 12f
                })
            }
            strand.subStrands.forEach { sub ->
                val subView = LinearLayout(holder.itemView.context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, 8, 0, 8)
                }
                subView.addView(TextView(holder.itemView.context).apply {
                    text = "• " + sub.subStrandNumber + " " + sub.title
                    setTextColor(titleColor)
                    textSize = 13f
                    setTypeface(null, Typeface.BOLD)
                })
                if (sub.specificLearningOutcomes.isEmpty()) {
                    subView.addView(TextView(holder.itemView.context).apply {
                        text = "   No published Specific Learning Outcomes"
                        setTextColor(textColor)
                        textSize = 12f
                    })
                } else {
                    sub.specificLearningOutcomes.forEach { outcome ->
                        subView.addView(TextView(holder.itemView.context).apply {
                            text = "   – " + outcome
                            setTextColor(textColor)
                            textSize = 12f
                        })
                    }
                }
                holder.subContainer.addView(subView)
            }
            holder.action.text = "Explore Sub-strands & SLOs"
            holder.action.setOnClickListener { onStrandClick(strand) }
        }

        override fun getItemCount() = items.size
    }

    override fun onDestroyView() {
        rvStrands = null
        tvStatus = null
        super.onDestroyView()
    }
}
