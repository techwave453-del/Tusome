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
import com.example.tusome.ui.StrandDetailActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors
import java.util.Locale

data class ParsedSubStrand(
    val title: String,
    val outcomes: List<String>
)

data class ParsedStrand(
    val numberStr: String,
    val title: String,
    val subStrands: List<ParsedSubStrand>
)

class LearningAreaBottomSheetDialog : BottomSheetDialogFragment() {

    private var areaTitle: String = ""
    private var kiswahiliTitle: String = ""
    private var gradeTitle: String = ""
    private var overviewText: String = ""
    private var areaId: String = ""
    private var iconResId: Int = R.drawable.ic_leaf

    private var parsedStrandsList: List<ParsedStrand> = emptyList()
    private var filterQuery: String = ""

    companion object {
        fun newInstance(
            title: String,
            kiswahiliTitle: String,
            gradeTitle: String,
            overviewText: String,
            areaId: String,
            iconResId: Int
        ): LearningAreaBottomSheetDialog {
            return LearningAreaBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putString("TITLE", title)
                    putString("KISWAHILI", kiswahiliTitle)
                    putString("GRADE", gradeTitle)
                    putString("OVERVIEW", overviewText)
                    putString("AREA_ID", areaId)
                    putInt("ICON", iconResId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            areaTitle = args.getString("TITLE", "Learning Area")
            kiswahiliTitle = args.getString("KISWAHILI", "")
            gradeTitle = args.getString("GRADE", "Grade 4")
            overviewText = args.getString("OVERVIEW", "")
            areaId = args.getString("AREA_ID", "agri_nutr")
            iconResId = args.getInt("ICON", R.drawable.ic_leaf)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_learning_area_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvDialogTitle).text = areaTitle
        view.findViewById<TextView>(R.id.tvDialogKiswahili).text = kiswahiliTitle.ifBlank { areaTitle }
        view.findViewById<TextView>(R.id.tvDialogGradeBadge).text = gradeTitle
        view.findViewById<ImageView>(R.id.ivDialogIcon).setImageResource(iconResId)

        val cgCompetencies = view.findViewById<ChipGroup>(R.id.cgDialogCompetencies)
        setupCompetencyChips(cgCompetencies)

        val etSearch = view.findViewById<EditText>(R.id.etDialogSearch)
        val rvStrands = view.findViewById<RecyclerView>(R.id.rvDialogStrands)
        rvStrands.layoutManager = LinearLayoutManager(requireContext())

        parsedStrandsList = parseCurriculumText(overviewText)
        renderStrandsList(rvStrands)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterQuery = s?.toString().orEmpty()
                renderStrandsList(rvStrands)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        view.findViewById<Button>(R.id.btnDialogClose).setOnClickListener {
            dismiss()
        }
    }

    private fun setupCompetencyChips(chipGroup: ChipGroup) {
        val competencies = listOf(
            "Critical Thinking",
            "Digital Literacy",
            "Environmental Citizenship",
            "Communication"
        )
        val chipTextColor = MaterialColors.getColor(chipGroup, androidx.appcompat.R.attr.colorPrimary)

        competencies.forEach { comp ->
            val chip = Chip(requireContext()).apply {
                text = comp
                setChipBackgroundColorResource(R.color.surface_variant)
                setTextColor(chipTextColor)
                textSize = 11f
            }
            chipGroup.addView(chip)
        }
    }

    private fun renderStrandsList(recyclerView: RecyclerView) {
        val query = filterQuery.trim().lowercase(Locale.ROOT)
        val filtered = if (query.isBlank()) {
            parsedStrandsList
        } else {
            parsedStrandsList.filter { strand ->
                strand.title.lowercase(Locale.ROOT).contains(query) ||
                strand.subStrands.any { sub ->
                    sub.title.lowercase(Locale.ROOT).contains(query) ||
                    sub.outcomes.any { it.lowercase(Locale.ROOT).contains(query) }
                }
            }
        }

        recyclerView.adapter = DialogStrandAdapter(filtered, areaId) {
            val intent = Intent(requireContext(), StrandDetailActivity::class.java).apply {
                putExtra("LEARNING_AREA_ID", areaId)
            }
            startActivity(intent)
            dismiss()
        }
    }

    private fun parseCurriculumText(text: String): List<ParsedStrand> {
        if (text.isBlank()) return emptyList()

        val lines = text.lines()
        val strands = mutableListOf<ParsedStrand>()

        var currentStrandNumber = ""
        var currentStrandTitle = ""
        var currentSubStrandTitle = ""
        val currentSubStrands = mutableListOf<ParsedSubStrand>()
        var currentOutcomes = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.first().isDigit() && (trimmed.contains(". ") || trimmed.contains("."))) {
                if (currentSubStrandTitle.isNotBlank()) {
                    currentSubStrands.add(ParsedSubStrand(currentSubStrandTitle, currentOutcomes.toList()))
                    currentOutcomes = mutableListOf()
                    currentSubStrandTitle = ""
                }
                if (currentStrandTitle.isNotBlank()) {
                    strands.add(ParsedStrand(currentStrandNumber, currentStrandTitle, currentSubStrands.toList()))
                    currentSubStrands.clear()
                }

                val parts = trimmed.split(".", limit = 2)
                currentStrandNumber = "Strand ${parts[0].trim()}"
                currentStrandTitle = parts.getOrNull(1)?.trim() ?: trimmed
            } else if (trimmed.startsWith("•") || trimmed.startsWith("*")) {
                if (currentSubStrandTitle.isNotBlank()) {
                    currentSubStrands.add(ParsedSubStrand(currentSubStrandTitle, currentOutcomes.toList()))
                    currentOutcomes = mutableListOf()
                }
                currentSubStrandTitle = trimmed.removePrefix("•").removePrefix("*").trim()
            } else if (trimmed.startsWith("–") || trimmed.startsWith("-")) {
                val outcomeText = trimmed.removePrefix("–").removePrefix("-").trim()
                currentOutcomes.add(outcomeText)
            }
        }

        if (currentSubStrandTitle.isNotBlank()) {
            currentSubStrands.add(ParsedSubStrand(currentSubStrandTitle, currentOutcomes.toList()))
        }
        if (currentStrandTitle.isNotBlank()) {
            strands.add(ParsedStrand(currentStrandNumber, currentStrandTitle, currentSubStrands.toList()))
        }

        return strands.ifEmpty {
            listOf(
                ParsedStrand(
                    numberStr = "Strand 1",
                    title = areaTitle,
                    subStrands = listOf(
                        ParsedSubStrand(
                            title = "Core Learning Outcomes",
                            outcomes = text.lines().filter { it.isNotBlank() }.take(6)
                        )
                    )
                )
            )
        }
    }

    private class DialogStrandAdapter(
        private val items: List<ParsedStrand>,
        private val areaId: String,
        private val onStartActivityClick: () -> Unit
    ) : RecyclerView.Adapter<DialogStrandAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDialogStrandBadge: TextView = view.findViewById(R.id.tvDialogStrandBadge)
            val tvDialogStrandTitle: TextView = view.findViewById(R.id.tvDialogStrandTitle)
            val containerDialogSubstrands: LinearLayout = view.findViewById(R.id.containerDialogSubstrands)
            val btnDialogStartActivity: Button = view.findViewById(R.id.btnDialogStartActivity)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_dialog_strand_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvDialogStrandBadge.text = item.numberStr.ifBlank { "Strand ${position + 1}" }
            holder.tvDialogStrandTitle.text = item.title

            holder.containerDialogSubstrands.removeAllViews()

            val titleColor = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOnSurface)
            val textColor = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOnSurfaceVariant)

            item.subStrands.forEach { sub ->
                val subView = LinearLayout(holder.itemView.context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, 8, 0, 8)
                }

                val subTitleView = TextView(holder.itemView.context).apply {
                    text = "• ${sub.title}"
                    setTextColor(titleColor)
                    textSize = 13f
                    setTypeface(null, Typeface.BOLD)
                }
                subView.addView(subTitleView)

                sub.outcomes.forEach { outcome ->
                    val outcomeView = TextView(holder.itemView.context).apply {
                        text = "   – $outcome"
                        setTextColor(textColor)
                        textSize = 12f
                    }
                    subView.addView(outcomeView)
                }

                holder.containerDialogSubstrands.addView(subView)
            }

            holder.btnDialogStartActivity.setOnClickListener {
                onStartActivityClick()
            }
        }

        override fun getItemCount(): Int = items.size
    }
}