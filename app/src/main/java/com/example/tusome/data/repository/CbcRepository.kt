package com.example.tusome.data.repository

import com.example.tusome.R
import com.example.tusome.data.model.*

object CbcRepository {

    val learnerProfile = LearnerProfile(
        name = "Amani Muthoni",
        admissionNo = "NPS-2024-0482",
        schoolName = "Nairobi Primary School",
        subCounty = "Westlands",
        county = "Nairobi City County",
        currentGrade = GradeLevel.GRADE_4,
        totalStrandsCompleted = 18,
        overallPerformance = PerformanceLevel.EXCEEDS,
        attendanceRatePercent = 98,
        parentGuardianContact = "+254 712 345 678"
    )

    fun getLearningAreas(grade: GradeLevel): List<LearningArea> {
        return when (grade.category) {
            GradeCategory.LOWER_PRIMARY -> listOf(
                LearningArea(
                    id = "env_act",
                    title = "Environmental Activities",
                    kiswahiliTitle = "Shughuli za Mazingira",
                    code = "ENV${grade.code}",
                    gradeLevel = grade,
                    strandsCount = 3,
                    subStrandsCount = 9,
                    progressPercent = 80,
                    iconResId = R.drawable.ic_leaf,
                    bgColorHex = "#F1F8E9",
                    iconColorHex = "#558B2F"
                ),
                LearningArea(
                    id = "math_act",
                    title = "Mathematics Activities",
                    kiswahiliTitle = "Shughuli za Hisabati",
                    code = "MTH${grade.code}",
                    gradeLevel = grade,
                    strandsCount = 4,
                    subStrandsCount = 12,
                    progressPercent = 75,
                    iconResId = R.drawable.ic_award,
                    bgColorHex = "#E8F5E9",
                    iconColorHex = "#2E7D32"
                ),
                LearningArea(
                    id = "lang_act",
                    title = "Language Activities (English)",
                    kiswahiliTitle = "Shughuli za Lugha ya Kiingereza",
                    code = "ENG${grade.code}",
                    gradeLevel = grade,
                    strandsCount = 5,
                    subStrandsCount = 15,
                    progressPercent = 88,
                    iconResId = R.drawable.ic_book,
                    bgColorHex = "#E1F5FE",
                    iconColorHex = "#0288D1"
                ),
                LearningArea(
                    id = "kisw_act",
                    title = "Shughuli za Kiswahili",
                    kiswahiliTitle = "Kusoma na Kuandika",
                    code = "KIS${grade.code}",
                    gradeLevel = grade,
                    strandsCount = 4,
                    subStrandsCount = 10,
                    progressPercent = 82,
                    iconResId = R.drawable.ic_school,
                    bgColorHex = "#E0F2F1",
                    iconColorHex = "#00695C"
                )
            )

            GradeCategory.UPPER_PRIMARY -> listOf(
                LearningArea(
                    id = "agriculture_nutrition",
                    title = "Agriculture & Nutrition",
                    kiswahiliTitle = "Kilimo na Lishe",
                    code = "AGN${grade.code}",
                    gradeLevel = grade,
                    strandsCount = 4,
                    subStrandsCount = 13,
                    progressPercent = 75,
                    iconResId = R.drawable.ic_leaf,
                    bgColorHex = "#F1F8E9",
                    iconColorHex = "#558B2F"
                ),
                LearningArea(
                    id = "math_act",
                    title = "Mathematics Activities",
                    kiswahiliTitle = "Shughuli za Hisabati",
                    code = "MTH${grade.code}",
                    gradeLevel = grade,
                    strandsCount = 5,
                    subStrandsCount = 15,
                    progressPercent = 82,
                    iconResId = R.drawable.ic_award,
                    bgColorHex = "#E8F5E9",
                    iconColorHex = "#2E7D32"
                ),
                LearningArea(
                    id = "sci_tech",
                    title = "Science & Technology",
                    kiswahiliTitle = "Sayansi na Teknolojia",
                    code = "SCT${grade.code}",
                    gradeLevel = grade,
                    strandsCount = 4,
                    subStrandsCount = 10,
                    progressPercent = 60,
                    iconResId = R.drawable.ic_psychology,
                    bgColorHex = "#FFF3E0",
                    iconColorHex = "#EF6C00"
                ),
                LearningArea(
                    id = "eng_lang",
                    title = "English Language",
                    kiswahiliTitle = "Lugha ya Kiingereza",
                    code = "ENG${grade.code}",
                    gradeLevel = grade,
                    strandsCount = 6,
                    subStrandsCount = 18,
                    progressPercent = 90,
                    iconResId = R.drawable.ic_book,
                    bgColorHex = "#E1F5FE",
                    iconColorHex = "#0288D1"
                ),
                LearningArea(
                    id = "kisw_lugh",
                    title = "Kiswahili Lugha",
                    kiswahiliTitle = "Kiswahili na Sanaa ya Lugha",
                    code = "KIS${grade.code}",
                    gradeLevel = grade,
                    strandsCount = 5,
                    subStrandsCount = 14,
                    progressPercent = 85,
                    iconResId = R.drawable.ic_school,
                    bgColorHex = "#E0F2F1",
                    iconColorHex = "#00695C"
                ),
                LearningArea(
                    id = "creative_arts",
                    title = "Creative Arts & PHE",
                    kiswahiliTitle = "Sanaa za Ubunifu na Elimu ya Mwili",
                    code = "CAP${grade.code}",
                    gradeLevel = grade,
                    strandsCount = 4,
                    subStrandsCount = 11,
                    progressPercent = 70,
                    iconResId = R.drawable.ic_star,
                    bgColorHex = "#F3E5F5",
                    iconColorHex = "#7B1FA2"
                )
            )

            GradeCategory.JUNIOR_SECONDARY -> listOf(
                LearningArea(
                    id = "pre_tech",
                    title = "Pre-Technical Studies",
                    kiswahiliTitle = "Masomo ya Mwanzo ya Ufundi",
                    code = "PTS${grade.code}",
                    gradeLevel = grade,
                    strandsCount = 5,
                    subStrandsCount = 16,
                    progressPercent = 68,
                    iconResId = R.drawable.ic_psychology,
                    bgColorHex = "#FFF3E0",
                    iconColorHex = "#EF6C00"
                ),
                LearningArea(
                    id = "integrated_sci",
                    title = "Integrated Science",
                    kiswahiliTitle = "Sayansi Mseto",
                    code = "ISC${grade.code}",
                    gradeLevel = grade,
                    strandsCount = 6,
                    subStrandsCount = 18,
                    progressPercent = 72,
                    iconResId = R.drawable.ic_leaf,
                    bgColorHex = "#F1F8E9",
                    iconColorHex = "#558B2F"
                ),
                LearningArea(
                    id = "math_act",
                    title = "Mathematics",
                    kiswahiliTitle = "Hisabati ya Sekondari",
                    code = "MTH${grade.code}",
                    gradeLevel = grade,
                    strandsCount = 6,
                    subStrandsCount = 20,
                    progressPercent = 80,
                    iconResId = R.drawable.ic_award,
                    bgColorHex = "#E8F5E9",
                    iconColorHex = "#2E7D32"
                ),
                LearningArea(
                    id = "social_studies",
                    title = "Social Studies & RE",
                    kiswahiliTitle = "Elimu ya Jamii na Dini",
                    code = "SST${grade.code}",
                    gradeLevel = grade,
                    strandsCount = 5,
                    subStrandsCount = 15,
                    progressPercent = 85,
                    iconResId = R.drawable.ic_school,
                    bgColorHex = "#E0F2F1",
                    iconColorHex = "#00695C"
                )
            )

            else -> listOf(
                LearningArea(
                    id = "general_area",
                    title = "General Learning Area",
                    kiswahiliTitle = "Eneo la Kujifunza",
                    code = "GEN${grade.code}",
                    gradeLevel = grade,
                    strandsCount = 3,
                    subStrandsCount = 9,
                    progressPercent = 50,
                    iconResId = R.drawable.ic_book,
                    bgColorHex = "#E8F5E9",
                    iconColorHex = "#2E7D32"
                )
            )
        }
    }

    fun getStrandsForArea(learningAreaId: String, grade: GradeLevel = GradeLevel.GRADE_4): List<Strand> {
        return when {
            learningAreaId.contains("agriculture") || learningAreaId == "agri_nutr" -> listOf(
                Strand(
                    id = "agri_s1",
                    learningAreaId = learningAreaId,
                    strandNumber = 1,
                    title = "Conservation of Resources",
                    description = "Soil conservation, water retention, fuel conservation, and wild animal management in Kenyan farms.",
                    coreCompetencies = listOf("Critical Thinking", "Environmental Citizenship", "Digital Literacy"),
                    subStrands = listOf(
                        SubStrand(
                            id = "agri_s1_sub1",
                            strandId = "agri_s1",
                            subStrandNumber = "1.1",
                            title = "Soil Conservation Methods",
                            specificLearningOutcomes = listOf(
                                "Identify locally available organic materials suitable for composting.",
                                "Prepare a simple compost heap using suitable organic materials.",
                                "Explain how compost contributes to healthy soil and farming in Kenya."
                            ),
                            keyInquiryQuestions = listOf(
                                "How do organic waste materials turn into fertile soil?",
                                "What natural materials preserve soil moisture in hot seasons?"
                            ),
                            suggestedExperiences = listOf(
                                "Learners collect dry leaves, crop stalks, and ash to build a school garden compost heap.",
                                "Learners observe soil texture before and after applying compost manure."
                            ),
                            digitalTaskPrompt = "Record a 1-minute video demonstration explaining compost heap layering.",
                            rubric = AssessmentRubric(
                                id = "rub_agri_1",
                                subStrandId = "agri_s1_sub1",
                                criteria = "Ability to select materials and prepare functional compost manure.",
                                exceedsDescription = "Flawlessly selects materials, builds neat compost layers, and clearly explains soil fertility.",
                                meetsDescription = "Correctly builds compost heap with minimal guidance.",
                                approachesDescription = "Requires help to identify organic waste and sequence compost layers.",
                                belowDescription = "Struggles with waste sorting and compost preparation.",
                                learnerLevel = PerformanceLevel.EXCEEDS,
                                teacherComment = "Exemplary teamwork during compost pit practical exercise!"
                            )
                        ),
                        SubStrand(
                            id = "agri_s1_sub2",
                            strandId = "agri_s1",
                            subStrandNumber = "1.2",
                            title = "Water Conservation & Drip Irrigation",
                            specificLearningOutcomes = listOf(
                                "Describe how drip irrigation reduces water wastage in crop production.",
                                "Assemble and demonstrate a simple drip-watering method using plastic bottles.",
                                "Explain why conserving water matters in growing crops."
                            ),
                            keyInquiryQuestions = listOf(
                                "How can bottle drip irrigation save water in arid regions of Kenya?",
                                "Why should water be targeted directly at plant roots?"
                            ),
                            suggestedExperiences = listOf(
                                "Learners punch small holes in recycled plastic bottles and bottle caps.",
                                "Learners install bottle drip irrigation next to kale (sukuma wiki) seedlings."
                            ),
                            digitalTaskPrompt = "Take photographs of your drip irrigation setup and upload to CBC digital portfolio.",
                            rubric = AssessmentRubric(
                                id = "rub_agri_2",
                                subStrandId = "agri_s1_sub2",
                                criteria = "Assembly and demonstration of bottle drip irrigation system.",
                                exceedsDescription = "Assembles leak-proof drip system and measures daily root water absorption.",
                                meetsDescription = "Assembles functional bottle drip irrigation system.",
                                approachesDescription = "Assembles drip bottle with assistance; needs help regulating drip rate.",
                                belowDescription = "Struggles to construct drip irrigation apparatus.",
                                learnerLevel = PerformanceLevel.MEETS,
                                teacherComment = "Good understanding of water retention techniques."
                            )
                        )
                    )
                ),
                Strand(
                    id = "agri_s2",
                    learningAreaId = learningAreaId,
                    strandNumber = 2,
                    title = "Food Production Processes",
                    description = "Vegetable crop establishment, nursery management, container gardening, and indigenous vegetables.",
                    coreCompetencies = listOf("Creativity & Imagination", "Self-Efficacy", "Problem Solving"),
                    subStrands = listOf(
                        SubStrand(
                            id = "agri_s2_sub1",
                            strandId = "agri_s2",
                            subStrandNumber = "2.1",
                            title = "Container Gardening for Indigenous Vegetables",
                            specificLearningOutcomes = listOf(
                                "Select suitable recycled containers (sacks, plastic tins, tires) for growing vegetables.",
                                "Plant sukuma wiki, spinach, or amaranth (terere) seedlings in container gardens.",
                                "Demonstrate regular watering and caring for growing crops."
                            ),
                            keyInquiryQuestions = listOf(
                                "How can urban families without big farms grow fresh indigenous vegetables?",
                                "Why is container gardening sustainable and eco-friendly?"
                            ),
                            suggestedExperiences = listOf(
                                "Learners bring discarded sugar sacks or plastic tubs from home.",
                                "Learners fill containers with compost soil mixture and transplant kale seedlings."
                            ),
                            digitalTaskPrompt = "Create a weekly growth chart showing seedling height measurements in centimeters.",
                            rubric = AssessmentRubric(
                                id = "rub_agri_3",
                                subStrandId = "agri_s2_sub1",
                                criteria = "Resourcefulness in container selection and vegetable crop care.",
                                exceedsDescription = "Designs creative multi-story sack gardens and maintains complete growth logbook.",
                                meetsDescription = "Successfully plants container garden and waters crops consistently.",
                                approachesDescription = "Prepares container garden with guidance; occasionally forgets watering schedule.",
                                belowDescription = "Requires continuous supervision during planting.",
                                learnerLevel = PerformanceLevel.EXCEEDS,
                                teacherComment = "Creative multi-story sack garden design!"
                            )
                        )
                    )
                )
            )

            learningAreaId.contains("math") || learningAreaId == "math_act" -> listOf(
                Strand(
                    id = "mth_s1",
                    learningAreaId = learningAreaId,
                    strandNumber = 1,
                    title = "Numbers & Place Value",
                    description = "Place value, whole numbers, addition, subtraction, multiplication, and fraction concepts.",
                    coreCompetencies = listOf("Critical Thinking", "Learning to Learn", "Digital Literacy"),
                    subStrands = listOf(
                        SubStrand(
                            id = "mth_s1_sub1",
                            strandId = "mth_s1",
                            subStrandNumber = "1.1",
                            title = if (grade.category == GradeCategory.LOWER_PRIMARY) "Numbers up to 1,000" else "Place Value up to 100,000",
                            specificLearningOutcomes = listOf(
                                "Read and write numbers in symbols and words.",
                                "Represent place values (Thousands, Hundreds, Tens, Ones) using place value charts or abacus.",
                                "Apply number sense in daily shopping and budgeting."
                            ),
                            keyInquiryQuestions = listOf(
                                "How does changing a digit's position alter its total value in money transactions?"
                            ),
                            suggestedExperiences = listOf(
                                "Learners group bottle caps (chapas) in 10s and 100s to count items."
                            ),
                            digitalTaskPrompt = "Complete the interactive place value math quiz on the tablet.",
                            rubric = AssessmentRubric(
                                id = "rub_mth_1",
                                subStrandId = "mth_s1_sub1",
                                criteria = "Accuracy in place value identification and number representation.",
                                exceedsDescription = "Solves complex place value word problems with 100% accuracy.",
                                meetsDescription = "Correctly identifies place value in standard format.",
                                approachesDescription = "Makes minor place value errors.",
                                belowDescription = "Struggles with place value concept beyond Hundreds.",
                                learnerLevel = PerformanceLevel.MEETS,
                                teacherComment = "Solid grasp of place value concepts."
                            )
                        )
                    )
                )
            )

            else -> listOf(
                Strand(
                    id = "gen_s1",
                    learningAreaId = learningAreaId,
                    strandNumber = 1,
                    title = "${grade.title} Core Concepts Strand",
                    description = "Foundational competencies, inquiry skills, and KICD core values integration.",
                    coreCompetencies = listOf("Communication", "Collaboration", "Citizenship"),
                    subStrands = listOf(
                        SubStrand(
                            id = "gen_s1_sub1",
                            strandId = "gen_s1",
                            subStrandNumber = "1.1",
                            title = "Practical Application & Inquiry",
                            specificLearningOutcomes = listOf(
                                "Demonstrate understanding of key concepts in ${grade.title}.",
                                "Apply learned concepts in school and community projects."
                            ),
                            keyInquiryQuestions = listOf("How does this learning outcome help solve local community challenges?"),
                            suggestedExperiences = listOf("Group collaborative project and interactive digital poster presentation."),
                            digitalTaskPrompt = "Upload a summary report or digital evidence to your portfolio.",
                            rubric = AssessmentRubric(
                                id = "rub_gen_1",
                                subStrandId = "gen_s1_sub1",
                                criteria = "Mastery of grade-level strand learning outcomes.",
                                exceedsDescription = "Demonstrates complete mastery and creative practical application.",
                                meetsDescription = "Meets all required learning outcomes.",
                                approachesDescription = "Requires minor practical application.",
                                belowDescription = "Needs structured intervention.",
                                learnerLevel = PerformanceLevel.MEETS,
                                teacherComment = "Good effort and satisfactory progress."
                            )
                        )
                    )
                )
            )
        }
    }

    fun getCoreCompetencies(): List<CoreCompetency> {
        return listOf(
            CoreCompetency(
                id = "cc_comm",
                name = "Communication & Collaboration",
                kiswahiliName = "Mawasiliano na Ushirikiano",
                description = "Expressing ideas clearly through oral, written, and artistic mediums while working effectively in teams.",
                progressPercent = 88,
                strandsAppliedCount = 14,
                iconResId = R.drawable.ic_person
            ),
            CoreCompetency(
                id = "cc_crit",
                name = "Critical Thinking & Problem Solving",
                kiswahiliName = "Fikra Tunduizi na Kutatua Matatizo",
                description = "Analyzing real-world challenges, evaluating evidence, and formulating creative solutions.",
                progressPercent = 82,
                strandsAppliedCount = 12,
                iconResId = R.drawable.ic_psychology
            ),
            CoreCompetency(
                id = "cc_crea",
                name = "Creativity & Imagination",
                kiswahiliName = "Ubunifu na Mawazo",
                description = "Generating novel ideas, artistic expression, and resourceful problem solving.",
                progressPercent = 92,
                strandsAppliedCount = 10,
                iconResId = R.drawable.ic_star
            ),
            CoreCompetency(
                id = "cc_citi",
                name = "Citizenship & Kenya Values",
                kiswahiliName = "Uraia na Maadili ya Kenya",
                description = "Understanding civic responsibility, patriotism, respect for diversity, integrity, and social justice.",
                progressPercent = 95,
                strandsAppliedCount = 16,
                iconResId = R.drawable.ic_school
            ),
            CoreCompetency(
                id = "cc_digi",
                name = "Digital Literacy",
                kiswahiliName = "Ujuzi wa Kiteknolojia / Kidijitali",
                description = "Using digital tools safely, navigating educational apps, and engaging in online research responsibly.",
                progressPercent = 78,
                strandsAppliedCount = 8,
                iconResId = R.drawable.ic_book
            ),
            CoreCompetency(
                id = "cc_learn",
                name = "Learning to Learn",
                kiswahiliName = "Kujifunza Kutokana na Kujifunza",
                description = "Developing curiosity, self-directed learning habits, and reflecting on personal cognitive growth.",
                progressPercent = 85,
                strandsAppliedCount = 11,
                iconResId = R.drawable.ic_check_circle
            ),
            CoreCompetency(
                id = "cc_self",
                name = "Self-Efficacy",
                kiswahiliName = "Kujiamini na Kujitawala",
                description = "Building confidence, emotional regulation, resilience, and personal goal setting.",
                progressPercent = 90,
                strandsAppliedCount = 13,
                iconResId = R.drawable.ic_award
            )
        )
    }

    fun getAllRubrics(): List<AssessmentRubric> {
        return getLearningAreas(GradeLevel.GRADE_4).flatMap { area ->
            getStrandsForArea(area.id, GradeLevel.GRADE_4).flatMap { strand ->
                strand.subStrands.map { it.rubric }
            }
        }
    }
}