package com.example.tusome.data.service

import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.model.QuizQuestion

object CbcAiTutorService {

    fun generateTutorResponse(
        userQuery: String,
        grade: GradeLevel,
        subjectContext: String? = null,
        onResponse: (String) -> Unit
    ) {
        val query = userQuery.trim().lowercase()

        val answer = when {
            query.contains("compost") || query.contains("manure") ->
                "Great question! In ${grade.title} Agriculture, a compost heap is built by layering organic materials: dry grass (carbon), fresh green leaves/kitchen peels (nitrogen), and ash/topsoil. Keep it moist, and in 6 to 8 weeks, it turns into rich soil manure!"

            query.contains("drip") || query.contains("irrigation") || query.contains("water") ->
                "Water conservation is key in Kenya! Drip irrigation uses plastic bottles with small pinholes to release water droplet by droplet directly to crop roots (like sukuma wiki). This reduces evaporation by over 50%!"

            query.contains("place value") || query.contains("number") || query.contains("digits") ->
                "In ${grade.title} Mathematics, place value shows what a digit is worth based on its position. For example, in 7,842:\n• 7 is in the Thousands place (7,000)\n• 8 is in the Hundreds place (800)\n• 4 is in the Tens place (40)\n• 2 is in the Ones place (2)!"

            query.contains("kiswahili") || query.contains("kilimo") || query.contains("lishe") ->
                "Jambo! Katika Somo la Kilimo na Lishe (${grade.title}), tunajifunza kuhusu kuhifadhi udongo na maji. Drip irrigation kwa Kiswahili ni 'Unyunyiziaji wa Matone'."

            query.contains("erosion") || query.contains("soil") ->
                "Soil erosion happens when rain or wind washes away topsoil. In CBC Agriculture, we prevent soil erosion by planting grass strips, building check dams, and mulching garden beds!"

            query.contains("competency") || query.contains("cbc") ->
                "The 7 Kenya CBC Core Competencies are:\n1. Communication & Collaboration\n2. Critical Thinking & Problem Solving\n3. Creativity & Imagination\n4. Citizenship\n5. Digital Literacy\n6. Learning to Learn\n7. Self-Efficacy!"

            else ->
                "Hello! I am your Tusome AI Learning Assistant for ${grade.title}. I can help you with Agriculture, Mathematics, Science & Tech, English, and Kiswahili. What concept would you like to explore today?"
        }

        onResponse(answer)
    }

    // AI Study Notes Generator for CBC Learning Areas
    fun generateStrandStudyNotes(
        strandTitle: String,
        grade: GradeLevel,
        onResult: (String) -> Unit
    ) {
        val notes = buildString {
            append("📚 AI GENERATED STUDY NOTES • ").append(grade.title).append("\n")
            append("Strand: ").append(strandTitle).append("\n\n")
            append("1. KEY CONCEPTS & DEFINITIONS:\n")
            append("• Practical application of KICD curriculum outcomes for ").append(strandTitle).append(".\n")
            append("• Focus on local Kenyan context, community problem solving, and eco-friendly farming practices.\n\n")
            append("2. SUMMARY OF PRACTICAL ACTIVITIES:\n")
            append("• Group inquiry, hands-on demonstration, and digital portfolio evidence recording.\n")
            append("• Observe safety precautions when handling farm tools or digital devices.\n\n")
            append("3. CORE VALUES & COMPETENCIES INTEGRATED:\n")
            append("• Environmental Citizenship, Critical Thinking, and Digital Literacy.\n")
            append("• Collaboration with peers and community service learning.")
        }
        onResult(notes)
    }

    // AI Practice Questions Generator for CBC Learning Areas
    fun generatePracticeQuestions(
        strandTitle: String,
        grade: GradeLevel,
        onResult: (List<QuizQuestion>) -> Unit
    ) {
        val questions = listOf(
            QuizQuestion(
                id = "ai_q1_${System.currentTimeMillis()}",
                questionText = "Which of the following best demonstrates practical learning in $strandTitle for ${grade.title}?",
                choices = listOf(
                    "Participating in hands-on group tasks and recording observations",
                    "Ignoring safety guidelines in the farm",
                    "Memorizing without understanding concepts",
                    "Refusing to work with peers"
                ),
                correctAnswerIndex = 0,
                explanation = "CBC emphasizes active learning, peer collaboration, and practical application of knowledge."
            ),
            QuizQuestion(
                id = "ai_q2_${System.currentTimeMillis()}",
                questionText = "Why is digital literacy important when carrying out learning activities in $strandTitle?",
                choices = listOf(
                    "It allows learners to record video evidence and research educational concepts online safely",
                    "It replaces all practical field work completely",
                    "It is only used for playing non-educational video games",
                    "It prevents team collaboration"
                ),
                correctAnswerIndex = 0,
                explanation = "Digital literacy enables learners to use technology responsibly for research and portfolio evidence submission."
            )
        )
        onResult(questions)
    }
}