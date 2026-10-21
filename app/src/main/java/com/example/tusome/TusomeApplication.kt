package com.example.tusome

import android.app.Application
import com.google.android.material.color.DynamicColors

class TusomeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply system dynamic colors (phone wallpaper & system palette) to all activities
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}