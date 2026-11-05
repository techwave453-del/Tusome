package com.example.tusome.data.repository

import com.example.tusome.data.model.*

object SbaExamRepository {

    fun getSbaExamPaper(
        subjectId: String,
        grade: GradeLevel = GradeLevel.GRADE_4
    ): SbaExamPaper? {
        if (grade != GradeLevel.GRADE_10) return null
        val supportedSubject = subjectId.contains("chemistry", ignoreCase = true) ||
            subjectId.contains("math", ignoreCase = true) ||
            subjectId.contains("agriculture", ignoreCase = true)
        if (!supportedSubject) return null
        val gradeTitle = if (grade.code.equals("G10", ignoreCase = true)) "GRADE 10" else grade.title.uppercase()

        return when {
            subjectId.contains("chemistry", ignoreCase = true) -> SbaExamPaper(
                id = "sba_chem_$gradeTitle",
                title = "SAMPLE PRACTICE ASSESSMENT",
                subjectTitle = "CHEMISTRY",
                gradeLevel = grade,
                termAndYear = "Grade 10 • Sample practice paper",
                timeAllowedMinutes = 120,
                totalMarks = 10,
                sections = listOf(
                    ExamSection(
                        sectionTitle = "SECTION A",
                        instructions = "Answer ALL questions directly.",
                        questions = listOf(
                            ExamQuestion(
                                questionNumber = 1,
                                scenarioOrPrompt = "A Grade 10 learner investigated the reaction between dilute hydrochloric acid and calcium carbonate chips.",
                                totalMarks = 6,
                                subQuestions = listOf(
                                    SubQuestion(
                                        label = "(a)",
                                        questionText = "State TWO observable changes during the reaction.",
                                        allocatedMarks = 2,
                                        markingSchemeAnswer = "(i) Effervescence / bubbling of a colorless gas.\n(ii) Decrease in mass of the reaction mixture as carbon dioxide escapes.",
                                        markBreakdown = "1 mark for effervescence; 1 mark for mass decrease (Total: 2 marks)"
                                    ),
                                    SubQuestion(
                                        label = "(b)",
                                        questionText = "Write a balanced chemical equation for the reaction.",
                                        allocatedMarks = 2,
                                        markingSchemeAnswer = "CaCO₃(s) + 2HCl(aq) → CaCl₂(aq) + H₂O(l) + CO₂(g)",
                                        markBreakdown = "1 mark for correct chemical formulas; 1 mark for correct balancing and state symbols (Total: 2 marks)"
                                    ),
                                    SubQuestion(
                                        label = "(c)",
                                        questionText = "Describe a test to confirm the identity of the gas produced.",
                                        allocatedMarks = 2,
                                        markingSchemeAnswer = "Bubble the gas into lime water (calcium hydroxide solution). The lime water turns milky/cloudy due to the formation of insoluble calcium carbonate.",
                                        markBreakdown = "1 mark for bubbling into lime water; 1 mark for observing white precipitate/milky color (Total: 2 marks)"
                                    )
                                )
                            )
                        )
                    ),
                    ExamSection(
                        sectionTitle = "SECTION B",
                        instructions = "Answer ALL structured questions.",
                        questions = listOf(
                            ExamQuestion(
                                questionNumber = 2,
                                scenarioOrPrompt = "The table below shows information about ions T⁺ and Z²⁻:",
                                totalMarks = 8,
                                subQuestions = listOf(
                                    SubQuestion(
                                        label = "(a)",
                                        questionText = "Determine the atomic number of element T and element Z.",
                                        allocatedMarks = 2,
                                        markingSchemeAnswer = "Element T: Atomic Number = 11 (Ion T⁺ has 10 electrons [2.8], so neutral atom has 11 protons/electrons).\nElement Z: Atomic Number = 16 (Ion Z²⁻ has 18 electrons [2.8.8], so neutral atom has 16 protons).",
                                        markBreakdown = "1 mark for T = 11; 1 mark for Z = 16 (Total: 2 marks)"
                                    ),
                                    SubQuestion(
                                        label = "(b)",
                                        questionText = "Write the formula of the compound formed between T and Z.",
                                        allocatedMarks = 2,
                                        markingSchemeAnswer = "Formula: T₂Z (Valency of T is 1, valency of Z is 2).",
                                        markBreakdown = "2 marks for correct formula T₂Z (Total: 2 marks)"
                                    )
                                )
                            )
                        )
                    )
                )
            )

            subjectId.contains("math", ignoreCase = true) -> SbaExamPaper(
                id = "sba_mth_$gradeTitle",
                title = "SAMPLE PRACTICE ASSESSMENT",
                subjectTitle = "CORE MATHEMATICS",
                gradeLevel = grade,
                termAndYear = "Grade 10 • Sample practice paper",
                timeAllowedMinutes = 120,
                totalMarks = 10,
                sections = listOf(
                    ExamSection(
                        sectionTitle = "SECTION A",
                        instructions = "Answer ALL questions directly.",
                        questions = listOf(
                            ExamQuestion(
                                questionNumber = 1,
                                scenarioOrPrompt = "Evaluate using logarithm tables correct to 4 significant figures:\n (1.24 × 10⁻¹·⁰⁴⁵⁶) / 3.82",
                                totalMarks = 4,
                                subQuestions = listOf(
                                    SubQuestion(
                                        label = "(a)",
                                        questionText = "Show step-by-step log simplification and final answer.",
                                        allocatedMarks = 4,
                                        markingSchemeAnswer = "Log Table Working:\nlog(1.24) = 0.0934\nlog(10⁻¹·⁰⁴⁵⁶) = -1.0456\nNumerator log = 0.0934 - 1.0456 = -0.9522\nlog(3.82) = 0.5821\nSubtract denominator log: -0.9522 - 0.5821 = -1.5343\nAntilog(-1.5343) = 0.02922",
                                        markBreakdown = "1 mark for correct log lookup; 1 mark for log subtraction; 1 mark for antilog lookup; 1 mark for 4 sig. fig. answer (Total: 4 marks)"
                                    )
                                )
                            )
                        )
                    ),
                    ExamSection(
                        sectionTitle = "SECTION B",
                        instructions = "Answer ALL questions.",
                        questions = listOf(
                            ExamQuestion(
                                questionNumber = 2,
                                scenarioOrPrompt = "A sector of a circle with radius r = 12 cm subtends an angle θ = 120° at the center O.",
                                totalMarks = 6,
                                subQuestions = listOf(
                                    SubQuestion(
                                        label = "(a)",
                                        questionText = "Calculate the area of the sector. (Take π = 3.142)",
                                        allocatedMarks = 3,
                                        markingSchemeAnswer = "Area = (θ / 360°) × π × r²\nArea = (120 / 360) × 3.142 × 12²\nArea = (1 / 3) × 3.142 × 144 = 150.816 cm²",
                                        markBreakdown = "1 mark for formula substitution; 1 mark for evaluation; 1 mark for correct unit cm² (Total: 3 marks)"
                                    ),
                                    SubQuestion(
                                        label = "(b)",
                                        questionText = "Calculate the arc length of the sector.",
                                        allocatedMarks = 3,
                                        markingSchemeAnswer = "Arc Length = (θ / 360°) × 2 × π × r\nArc Length = (120 / 360) × 2 × 3.142 × 12\nArc Length = (1 / 3) × 75.408 = 25.136 cm",
                                        markBreakdown = "1 mark for formula; 1 mark for substitution; 1 mark for answer with units (Total: 3 marks)"
                                    )
                                )
                            )
                        )
                    )
                )
            )

            subjectId.contains("agriculture", ignoreCase = true) -> SbaExamPaper(
                id = "sba_agri_$gradeTitle",
                title = "SAMPLE PRACTICE ASSESSMENT",
                subjectTitle = "AGRICULTURE & NUTRITION",
                gradeLevel = grade,
                termAndYear = "Grade 10 • Sample practice paper",
                timeAllowedMinutes = 120,
                totalMarks = 11,
                sections = listOf(
                    ExamSection(
                        sectionTitle = "SECTION A",
                        instructions = "Answer ALL questions in this section directly.",
                        questions = listOf(
                            ExamQuestion(
                                questionNumber = 1,
                                scenarioOrPrompt = "At NEP Girls Senior School, Grade 10 learners conducted a practical field study on soil conservation techniques.",
                                totalMarks = 6,
                                subQuestions = listOf(
                                    SubQuestion(
                                        label = "(a)",
                                        questionText = "Identify FOUR organic waste materials suitable for constructing a compost pit.",
                                        allocatedMarks = 2,
                                        markingSchemeAnswer = "(i) Fresh green grass clippings / weeds\n(ii) Dry maize stalks / wheat straw\n(iii) Vegetable & fruit peels from school lunchroom\n(iv) Wood ash / topsoil",
                                        markBreakdown = "0.5 marks for each valid organic material identified (Total: 2 marks)"
                                    ),
                                    SubQuestion(
                                        label = "(b)",
                                        questionText = "Explain TWO benefits of mulching in crop production.",
                                        allocatedMarks = 2,
                                        markingSchemeAnswer = "(i) Retains soil moisture by reducing evaporation from bare soil surface.\n(ii) Suppresses weed growth by blocking direct sunlight to weed seeds.",
                                        markBreakdown = "1 mark for moisture retention; 1 mark for weed suppression (Total: 2 marks)"
                                    ),
                                    SubQuestion(
                                        label = "(c)",
                                        questionText = "State TWO qualities of a good container used in container gardening.",
                                        allocatedMarks = 2,
                                        markingSchemeAnswer = "(i) Must have bottom drainage holes to prevent waterlogging.\n(ii) Must be durable and non-toxic (e.g., recycled sugar sacks, plastic tubs).",
                                        markBreakdown = "1 mark for drainage holes; 1 mark for durability (Total: 2 marks)"
                                    )
                                )
                            )
                        )
                    ),
                    ExamSection(
                        sectionTitle = "SECTION B",
                        instructions = "Answer ALL structured questions.",
                        questions = listOf(
                            ExamQuestion(
                                questionNumber = 2,
                                scenarioOrPrompt = "A farmer in Kitui County wishes to establish a bottle drip irrigation system for growing sukuma wiki during dry seasons.",
                                totalMarks = 8,
                                subQuestions = listOf(
                                    SubQuestion(
                                        label = "(a)",
                                        questionText = "Describe THREE steps involved in assembling a bottle drip irrigation system.",
                                        allocatedMarks = 3,
                                        markingSchemeAnswer = "1. Clean a recycled 1.5L plastic bottle and make 2-3 small pinholes near the base or in the cap.\n2. Invert or bury the bottle partially beside the crop root zone.\n3. Fill the bottle with clean water and cover to reduce evaporation.",
                                        markBreakdown = "1 mark for pinhole preparation; 1 mark for root placement; 1 mark for water filling (Total: 3 marks)"
                                    ),
                                    SubQuestion(
                                        label = "(b)",
                                        questionText = "Outline TWO reasons why drip irrigation is superior to overhead sprinkler irrigation in dry areas.",
                                        allocatedMarks = 2,
                                        markingSchemeAnswer = "(i) Reduces water loss through wind drift and direct evaporation.\n(ii) Delivers water precisely to root zones, reducing weed germination between rows.",
                                        markBreakdown = "1 mark for evaporation reduction; 1 mark for targeted delivery (Total: 2 marks)"
                                    )
                                )
                            )
                        )
                    )
                )
            )
            else -> null
        }
    }
}