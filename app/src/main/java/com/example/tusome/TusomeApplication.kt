package com.example.tusome

import android.app.Application
import com.example.tusome.data.repository.SupabaseCurriculumRepository
import com.google.android.material.color.DynamicColors

class TusomeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply system dynamic colors (phone wallpaper & system palette) to all activities
        DynamicColors.applyToActivitiesIfAvailable(this)

        // Initialize local curriculum offline cache repository
        SupabaseCurriculumRepository.initialize(this)
    }
}