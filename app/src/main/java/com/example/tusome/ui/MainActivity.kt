package com.example.tusome.ui

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.tusome.R
import com.example.tusome.ui.fragments.AssessmentsFragment
import com.example.tusome.ui.fragments.CompetenciesFragment
import com.example.tusome.ui.fragments.HomeFragment
import com.example.tusome.ui.fragments.ProfileFragment
import com.example.tusome.ui.fragments.StrandsFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        bottomNav = findViewById(R.id.bottomNavigationView)

        // The toolbar reflects the current destination instead of keeping a static app title.
        toolbar.navigationIcon = null
        toolbar.title = getString(R.string.nav_home)
        toolbar.subtitle = null

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.navigation_home
            showDestination(R.id.navigation_home)
        } else {
            updateToolbarTitle(bottomNav.selectedItemId)
        }

        bottomNav.setOnItemSelectedListener { item ->
            showDestination(item.itemId)
            true
        }

        // Selecting the active tab should not recreate its fragment.
        bottomNav.setOnItemReselectedListener { /* Keep the current destination in place. */ }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (bottomNav.selectedItemId != R.id.navigation_home) {
                    bottomNav.selectedItemId = R.id.navigation_home
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun showDestination(itemId: Int) {
        val fragment: Fragment = when (itemId) {
            R.id.navigation_home -> HomeFragment()
            R.id.navigation_strands -> StrandsFragment()
            R.id.navigation_assessments -> AssessmentsFragment()
            R.id.navigation_competencies -> CompetenciesFragment()
            R.id.navigation_profile -> ProfileFragment()
            else -> HomeFragment()
        }

        updateToolbarTitle(itemId)
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragmentContainer, fragment, itemId.toString())
            .commit()
    }

    private fun updateToolbarTitle(itemId: Int) {
        toolbar.setTitle(
            when (itemId) {
                R.id.navigation_home -> R.string.nav_home
                R.id.navigation_strands -> R.string.nav_strands
                R.id.navigation_assessments -> R.string.nav_assessments
                R.id.navigation_competencies -> R.string.nav_competencies
                R.id.navigation_profile -> R.string.nav_profile
                else -> R.string.app_name
            }
        )
    }
}
