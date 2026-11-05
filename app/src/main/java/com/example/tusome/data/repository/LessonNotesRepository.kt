package com.example.tusome.data.repository

import com.example.tusome.data.model.*

object LessonNotesRepository {

    fun getDetailedLessonNotes(
        subStrandId: String,
        subStrandTitle: String,
        grade: GradeLevel
    ): LessonContent {
        return when {
            subStrandId == "agri_s1_sub1" || subStrandTitle.contains("Soil Erosion", ignoreCase = true) -> LessonContent(
                id = "lesson_$subStrandId",
                subStrandId = subStrandId,
                lessonTitle = "Soil Erosion Control Methods in Kenyan Farms",
                subjectTitle = "Agriculture & Nutrition",
                gradeLevel = grade,
                introduction = "Soil erosion is the detachment and carrying away of fertile topsoil by water, wind, or human activities. Topsoil is the most valuable layer of soil because it contains essential nutrients and organic humus needed for healthy plant growth.",
                keyVocabulary = mapOf(
                    "Topsoil" to "The dark, nutrient-rich upper layer of soil essential for crop roots.",
                    "Mulching" to "Covering bare soil with dry grass, straw, or maize stalks to prevent splash erosion.",
                    "Check Dam" to "A small barrier constructed across a gully to slow down flowing rainwater.",
                    "Fanya Juu Terrace" to "A Kenyan terracing technique where soil is thrown uphill to form embankment ridges across slopes."
                ),
                mainSections = listOf(
                    LessonSection(
                        sectionHeading = "1. Types of Soil Erosion in Kenya",
                        explanationText = "Soil erosion occurs in four distinct stages depending on rain intensity and slope steepness:",
                        bulletPoints = listOf(
                            "Splash Erosion: Raindrops strike bare soil directly, dislodging soil particles into the air.",
                            "Sheet Erosion: Rainwater flows evenly over flat bare land, removing a uniform thin layer of topsoil.",
                            "Rill Erosion: Flowing water forms small shallow finger-like channels on sloping farm land.",
                            "Gully Erosion: Uncontrolled rill erosion deepens and widens into deep trenches that trap farm equipment."
                        ),
                        localKenyanExample = "Example: Gully erosion is frequently observed on unvegetated roadside slopes in Kitui and Machakos counties after heavy April rains."
                    ),
                    LessonSection(
                        sectionHeading = "2. Methods of Soil Conservation",
                        explanationText = "Kenyan farmers use effective physical and biological conservation measures:",
                        bulletPoints = listOf(
                            "Cover Cropping: Planting spreading leguminous crops like sweet potatoes, beans, and desmodium to protect topsoil.",
                            "Grass Strips: Planting Napier grass or vetiver grass along contour lines to trap silty soil.",
                            "Contour Farming: Plowing across hillsides rather than up and down to create water barriers.",
                            "Gabions: Wire mesh boxes packed with stones placed in large erosion gullies."
                        ),
                        localKenyanExample = "Example: In Murang'a and Nyeri counties, Napier grass strips along tea farm contours hold soil firmly on steep hillsides."
                    )
                ),
                stepByStepGuide = listOf(
                    "Step 1: Inspect the school farm or kitchen garden for bare soil patches and water channels after rainfall.",
                    "Step 2: Collect dry grass, maize stalks, or banana leaves from the surrounding environment.",
                    "Step 3: Lay a 5cm thick layer of mulch around kale (sukuma wiki) and tomato seedlings, leaving a 2cm gap around plant stems.",
                    "Step 4: Construct mini check dams in small water paths using stones and dry tree branches.",
                    "Step 5: Record before-and-after observations in your CBC digital learning portfolio."
                ),
                summaryTakeaways = listOf(
                    "Topsoil takes hundreds of years to form but can be washed away in a single heavy rainstorm.",
                    "Mulching conserves soil moisture, suppresses weeds, and stops splash erosion.",
                    "Fanya Juu terraces and grass strips protect sloping Kenyan farms from severe soil degradation."
                )
            )

            subStrandId == "agri_s1_sub2" || subStrandTitle.contains("Compost", ignoreCase = true) -> LessonContent(
                id = "lesson_$subStrandId",
                subStrandId = subStrandId,
                lessonTitle = "Step-by-Step Compost Manure Preparation",
                subjectTitle = "Agriculture & Nutrition",
                gradeLevel = grade,
                introduction = "Compost manure is a natural, eco-friendly soil fertilizer created through the biological decomposition of organic plant and kitchen waste by micro-organisms.",
                keyVocabulary = mapOf(
                    "Organic Waste" to "Materials derived from living organisms, such as fruit peels, dry leaves, and cow dung.",
                    "Decomposition" to "The natural process where micro-organisms break down organic waste into fertile humus.",
                    "Humus" to "Dark organic material formed from fully rotted plant matter that retains soil moisture."
                ),
                mainSections = listOf(
                    LessonSection(
                        sectionHeading = "1. Selecting Materials for Compost",
                        explanationText = "A healthy compost heap requires a balanced ratio of nitrogen-rich green waste and carbon-rich dry waste:",
                        bulletPoints = listOf(
                            "Green Materials (Nitrogen Source): Fresh grass clippings, green vegetable peels, weeds, and leguminous leaves.",
                            "Dry Materials (Carbon Source): Dry leaves, straw, maize stalks, wood shavings, and dry grass.",
                            "Accelerators: Cow dung, chicken manure, wood ash, and topsoil containing decomposing bacteria.",
                            "DO NOT USE: Plastics, glass, synthetic rubber, meat scraps (which attract rodents), or diseased plants."
                        ),
                        localKenyanExample = "Example: School lunchroom fruit peels and dry maize stalks from school farms make excellent compost material."
                    ),
                    LessonSection(
                        sectionHeading = "2. The Layering Sequence in a Compost Heap",
                        explanationText = "Construct a 1m x 1m compost heap or pit in a shaded area sheltered from direct sunlight:",
                        bulletPoints = listOf(
                            "Base Layer (15cm): Coarse materials like maize stalks and small twigs to facilitate bottom aeration.",
                            "Second Layer (15cm): Dry carbon-rich materials (dry grass and leaves).",
                            "Third Layer (15cm): Fresh nitrogen-rich green waste and kitchen scraps.",
                            "Fourth Layer (5cm): Animal manure or wood ash layer to supply micro-organisms and potassium.",
                            "Top Cover: Soil and grass cover to prevent nutrient evaporation and retain heat."
                        )
                    )
                ),
                stepByStepGuide = listOf(
                    "Step 1: Dig a 1m deep pit or build a surface heap in a cool shaded area near the school garden.",
                    "Step 2: Lay coarse maize stalks at the bottom for drainage and air circulation.",
                    "Step 3: Add alternating 15cm layers of green waste and dry leaves.",
                    "Step 4: Sprinkle water lightly over each layer so the heap remains moist like a wrung-out sponge.",
                    "Step 5: Insert a sharp dry stick into the center. Check the stick after 2 weeks; if hot, decomposition is actively taking place!",
                    "Step 6: Turn the compost pit every 3 weeks to supply oxygen. The compost will be ready for crop planting in 8-12 weeks."
                ),
                summaryTakeaways = listOf(
                    "Compost manure improves soil structure, water holding capacity, and plant nutrient supply without chemical pollution.",
                    "Maintaining proper moisture and turning the heap regularly speeds up decomposition."
                )
            )

            subStrandTitle.contains("Place Value", ignoreCase = true) || subStrandTitle.contains("Math", ignoreCase = true) -> LessonContent(
                id = "lesson_$subStrandId",
                subStrandId = subStrandId,
                lessonTitle = "Place Value & Large Numbers Masterclass",
                subjectTitle = "Mathematics Activities",
                gradeLevel = grade,
                introduction = "Place value is the numeric value that a digit has based on its position within a number. Understanding place value allows us to read, write, compare, and perform mathematical operations accurately.",
                keyVocabulary = mapOf(
                    "Digit" to "Any individual symbol from 0 to 9 used to form numbers.",
                    "Place Value" to "The positional value of a digit (Ones, Tens, Hundreds, Thousands, Ten Thousands).",
                    "Total Value" to "The exact product of a digit multiplied by its place value."
                ),
                mainSections = listOf(
                    LessonSection(
                        sectionHeading = "1. Understanding Place Value Positions",
                        explanationText = "Starting from the right-hand side, place values increase by a power of 10 for each position:",
                        bulletPoints = listOf(
                            "Ones (O): Position 1 from right (x1)",
                            "Tens (T): Position 2 from right (x10)",
                            "Hundreds (H): Position 3 from right (x100)",
                            "Thousands (Th): Position 4 from right (x1,000)",
                            "Ten Thousands (TTh): Position 5 from right (x10,000)"
                        ),
                        localKenyanExample = "Example in Kenya Shillings: In KES 47,852, the digit 7 represents KES 7,000."
                    ),
                    LessonSection(
                        sectionHeading = "2. Calculating Total Value",
                        explanationText = "To find total value, multiply the digit by its place value:",
                        bulletPoints = listOf(
                            "Number: 58,314",
                            "Digit 5 is in Ten Thousands position -> Total Value = 5 x 10,000 = 50,000",
                            "Digit 8 is in Thousands position -> Total Value = 8 x 1,000 = 8,000",
                            "Digit 3 is in Hundreds position -> Total Value = 3 x 100 = 300",
                            "Digit 1 is in Tens position -> Total Value = 1 x 10 = 10",
                            "Digit 4 is in Ones position -> Total Value = 4 x 1 = 4"
                        )
                    )
                ),
                stepByStepGuide = listOf(
                    "Step 1: Write down the number clearly.",
                    "Step 2: Draw a place value chart with columns: TTh | Th | H | T | O.",
                    "Step 3: Fill the digits into the chart starting from the rightmost digit into the Ones column.",
                    "Step 4: Multiply each digit by its place value column heading to verify total value."
                ),
                summaryTakeaways = listOf(
                    "Changing a digit's position alters its total value completely.",
                    "Zero (0) serves as a vital placeholder in place value numbers."
                )
            )

            else -> LessonContent(
                id = "lesson_$subStrandId",
                subStrandId = subStrandId,
                lessonTitle = "KICD Detailed Lesson Notes: $subStrandTitle",
                subjectTitle = "CBC ${grade.title} Learning Area",
                gradeLevel = grade,
                introduction = "This detailed lesson guide covers core concepts, facts, real-world examples, and practical activities for $subStrandTitle according to the official Kenya Institute of Curriculum Development (KICD) design for ${grade.title}.",
                keyVocabulary = mapOf(
                    "Core Concept" to "The fundamental principle taught in this KICD sub-strand.",
                    "Practical Application" to "Applying acquired knowledge to solve real-world problems in Kenyan homes and schools.",
                    "Inquiry Prompt" to "A thought-provoking question that stimulates critical thinking and active group discussion."
                ),
                mainSections = listOf(
                    LessonSection(
                        sectionHeading = "1. Core Subject Explanations & Key Principles",
                        explanationText = "Key facts and theoretical principles required for $subStrandTitle:",
                        bulletPoints = listOf(
                            "Mastery of foundational vocabulary and core subject rules.",
                            "Observation of physical models, diagrams, and digital learning materials.",
                            "Integration of 7 Kenya CBC Core Competencies (Critical Thinking, Digital Literacy, Communication)."
                        ),
                        localKenyanExample = "Local Context: Applying $subStrandTitle concepts in everyday community activities across Kenya."
                    ),
                    LessonSection(
                        sectionHeading = "2. Practical Experiential Tasks & Safety Protocols",
                        explanationText = "Hands-on guided activities for individual and group learning:",
                        bulletPoints = listOf(
                            "Participating in collaborative group inquiry tasks.",
                            "Observing safety standards, tool care, and hygiene guidelines.",
                            "Recording digital reflections and photo/video evidence in learner CBC portfolios."
                        )
                    )
                ),
                stepByStepGuide = listOf(
                    "Step 1: Read the lesson introduction and key vocabulary definitions carefully.",
                    "Step 2: Study the main content sections and local Kenyan examples.",
                    "Step 3: Carry out the hands-on practical activity with group members.",
                    "Step 4: Take the interactive knowledge check quiz to test your understanding."
                ),
                summaryTakeaways = listOf(
                    "Understanding core principles enables practical problem solving in real life.",
                    "Continuous practice and portfolio evidence submission ensures steady learning progress."
                )
            )
        }
    }
}