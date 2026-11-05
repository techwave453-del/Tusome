package com.example.tusome.data.service

import com.example.tusome.data.model.GradeLevel

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
}