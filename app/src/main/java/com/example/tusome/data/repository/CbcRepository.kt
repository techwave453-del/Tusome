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
        return listOf(
            LearningArea(
                id = "agri_nutr",
                title = "Agriculture & Nutrition",
                kiswahiliTitle = "Kilimo na Lishe",
                code = "AGN4",
                gradeLevel = grade,
                strandsCount = 4,
                subStrandsCount = 12,
                progressPercent = 75,
                iconResId = R.drawable.ic_leaf,
                bgColorHex = "#F1F8E9",
                iconColorHex = "#558B2F"
            ),
            LearningArea(
                id = "math_act",
                title = "Mathematics Activities",
                kiswahiliTitle = "Shughuli za Hisabati",
                code = "MTH4",
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
                code = "SCT4",
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
                code = "ENG4",
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
                code = "KIS4",
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
                code = "CAP4",
                gradeLevel = grade,
                strandsCount = 4,
                subStrandsCount = 11,
                progressPercent = 70,
                iconResId = R.drawable.ic_star,
                bgColorHex = "#F3E5F5",
                iconColorHex = "#7B1FA2"
            )
        )
    }

    fun getStrandsForArea(learningAreaId: String): List<Strand> {
        return when (learningAreaId) {
            "agri_nutr" -> listOf(
                Strand(
                    id = "agri_s1",
                    learningAreaId = "agri_nutr",
                    strandNumber = 1,
                    title = "Conserving Our Environment",
                    description = "Methods of soil conservation, water retention techniques, and soil preparation for kitchen gardens in Kenyan schools.",
                    coreCompetencies = listOf("Critical Thinking", "Environmental Citizenship", "Digital Literacy"),
                    subStrands = listOf(
                        SubStrand(
                            id = "agri_s1_sub1",
                            strandId = "agri_s1",
                            subStrandNumber = "1.1",
                            title = "Soil Erosion Control Methods",
                            specificLearningOutcomes = listOf(
                                "Identify common types of soil erosion in the local school environment.",
                                "Construct simple terracing or mulch beds using locally available materials.",
                                "Appreciate the importance of soil protection for food security in Kenya."
                            ),
                            keyInquiryQuestions = listOf(
                                "How does running water wash away fertile topsoil in our school farm?",
                                "What natural materials can we use to build mulch covers?"
                            ),
                            suggestedExperiences = listOf(
                                "Learners walk around the school compound to observe erosion gullies after rainfall.",
                                "In small collaborative groups, learners build mini-check dams using stones and dry grasses."
                            ),
                            digitalTaskPrompt = "Record a short 1-minute video demonstration explaining how grass strips prevent soil erosion.",
                            rubric = AssessmentRubric(
                                id = "rub_agri_1",
                                subStrandId = "agri_s1_sub1",
                                criteria = "Ability to identify soil erosion and construct effective erosion barriers using local materials.",
                                exceedsDescription = "Accurately identifies all 3 types of erosion, constructs neat check dams independently, and explains soil moisture retention clearly.",
                                meetsDescription = "Correctly identifies soil erosion, constructs functional grass strips/check dams with minimal guidance.",
                                approachesDescription = "Identifies soil erosion with teacher assistance, requires help to build erosion barriers.",
                                belowDescription = "Struggles to identify erosion types and requires constant support during soil conservation activities.",
                                learnerLevel = PerformanceLevel.EXCEEDS,
                                teacherComment = "Amani demonstrated exemplary leadership during the school garden practical exercise!"
                            )
                        ),
                        SubStrand(
                            id = "agri_s1_sub2",
                            strandId = "agri_s1",
                            subStrandNumber = "1.2",
                            title = "Compost Manure Preparation",
                            specificLearningOutcomes = listOf(
                                "Distinguish organic waste from inorganic waste materials.",
                                "Prepare a simple compost pit or heap using kitchen waste and dry leaves.",
                                "Practice safety precautions when handling farm tools."
                            ),
                            keyInquiryQuestions = listOf(
                                "Why do kitchen wastes turn into fertile soil over time?",
                                "Which items should NEVER be thrown into a compost heap?"
                            ),
                            suggestedExperiences = listOf(
                                "Learners sort school lunchroom peels into organic bin.",
                                "Learners layer green leaves, dry grass, and wood ash in a 1m x 1m compost pit."
                            ),
                            digitalTaskPrompt = "Take photographs of your 3 compost pit layers and upload to your CBC digital portfolio.",
                            rubric = AssessmentRubric(
                                id = "rub_agri_2",
                                subStrandId = "agri_s1_sub2",
                                criteria = "Correct sorting of organic waste and execution of compost pit layering techniques.",
                                exceedsDescription = "Flawlessly sorts organic materials, explains nitrogen/carbon ratios in compost layering, and strictly observes tool safety.",
                                meetsDescription = "Accurately sorts organic waste and layers compost pit correctly.",
                                approachesDescription = "Sorts most waste correctly; needs guidance on proper compost layering sequence.",
                                belowDescription = "Inaccurately classifies waste; struggles with compost pit preparation.",
                                learnerLevel = PerformanceLevel.MEETS,
                                teacherComment = "Good understanding of organic decomposition. Keep up the active participation."
                            )
                        )
                    )
                ),
                Strand(
                    id = "agri_s2",
                    learningAreaId = "agri_nutr",
                    strandNumber = 2,
                    title = "Food Production Processes",
                    description = "Vegetable crop establishment, nursery management, transplanting, and organic pest control.",
                    coreCompetencies = listOf("Creativity and Imagination", "Self-efficacy", "Problem Solving"),
                    subStrands = listOf(
                        SubStrand(
                            id = "agri_s2_sub1",
                            strandId = "agri_s2",
                            subStrandNumber = "2.1",
                            title = "Container Gardening for Vegetables",
                            specificLearningOutcomes = listOf(
                                "Select suitable recycled containers (sacks, plastic tins, tires) for growing vegetables.",
                                "Plant sukuma wiki (kale) or spinach seedlings in container gardens.",
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
                                criteria = "Resourcefulness in sourcing containers and care for container-grown vegetable crops.",
                                exceedsDescription = "Designs creative multi-story sack gardens, maintains healthiest crops with complete logbook.",
                                meetsDescription = "Successfully prepares container garden and waters crops consistently.",
                                approachesDescription = "Prepares container garden with guidance; occasionally forgets watering schedule.",
                                belowDescription = "Requires continuous supervision to plant and maintain container crops.",
                                learnerLevel = PerformanceLevel.EXCEEDS,
                                teacherComment = "Excellent innovation with multi-story sack gardens!"
                            )
                        )
                    )
                )
            )
            "math_act" -> listOf(
                Strand(
                    id = "mth_s1",
                    learningAreaId = "math_act",
                    strandNumber = 1,
                    title = "Numbers & Operations",
                    description = "Place value up to 10,000, addition, subtraction, multiplication as repeated addition, and fractions.",
                    coreCompetencies = listOf("Critical Thinking", "Learning to Learn", "Digital Literacy"),
                    subStrands = listOf(
                        SubStrand(
                            id = "mth_s1_sub1",
                            strandId = "mth_s1",
                            subStrandNumber = "1.1",
                            title = "Place Value of Digits up to 10,000",
                            specificLearningOutcomes = listOf(
                                "Read and write numbers up to 10,000 in symbols and words.",
                                "Represent place values (Thousands, Hundreds, Tens, Ones) using abacus or place value charts."
                            ),
                            keyInquiryQuestions = listOf(
                                "How does changing a digit's position alter its total value in money transactions?"
                            ),
                            suggestedExperiences = listOf(
                                "Learners use bottle caps (chapas) grouped in 10s and 100s to count school tree saplings."
                            ),
                            digitalTaskPrompt = "Solve the interactive digital place value quiz on the tablet.",
                            rubric = AssessmentRubric(
                                id = "rub_mth_1",
                                subStrandId = "mth_s1_sub1",
                                criteria = "Accuracy in place value identification and representation on abacus.",
                                exceedsDescription = "Solves complex place value word problems with 100% accuracy and assists peers.",
                                meetsDescription = "Correctly identifies place value up to 10,000 in standard format.",
                                approachesDescription = "Makes minor errors in place value above 1,000.",
                                belowDescription = "Struggles with place value concept beyond Hundreds.",
                                learnerLevel = PerformanceLevel.MEETS,
                                teacherComment = "Solid grasp of number concepts up to 10,000."
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
                    title = "General CBC Competency Strand",
                    description = "Core learning outcomes and values integration according to KICD Kenya framework.",
                    coreCompetencies = listOf("Communication", "Collaboration", "Citizenship"),
                    subStrands = listOf(
                        SubStrand(
                            id = "gen_s1_sub1",
                            strandId = "gen_s1",
                            subStrandNumber = "1.1",
                            title = "Core Knowledge & Practical Application",
                            specificLearningOutcomes = listOf(
                                "Demonstrate understanding of key concepts.",
                                "Apply learned concepts in daily living."
                            ),
                            keyInquiryQuestions = listOf("How does this strand help in community building?"),
                            suggestedExperiences = listOf("Group inquiry and interactive poster creation."),
                            digitalTaskPrompt = "Upload summary project work.",
                            rubric = AssessmentRubric(
                                id = "rub_gen_1",
                                subStrandId = "gen_s1_sub1",
                                criteria = "Mastery of strand objectives.",
                                exceedsDescription = "Demonstrates complete mastery and creative application.",
                                meetsDescription = "Meets all required learning outcomes.",
                                approachesDescription = "Requires minor reinforcement.",
                                belowDescription = "Needs structured intervention.",
                                learnerLevel = PerformanceLevel.MEETS,
                                teacherComment = "Satisfactory progress."
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
            getStrandsForArea(area.id).flatMap { strand ->
                strand.subStrands.map { it.rubric }
            }
        }
    }
}