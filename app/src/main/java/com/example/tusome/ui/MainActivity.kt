package com.example.tusome.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.tusome.R
import com.example.tusome.ui.fragments.AssessmentsFragment
import com.example.tusome.ui.fragments.CompetenciesFragment
import com.example.tusome.ui.fragments.HomeFragment
import com.example.tusome.ui.fragments.ProfileFragment
import com.example.tusome.ui.fragments.StrandsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.navigation_home -> HomeFragment()
                R.id.navigation_strands -> StrandsFragment()
                R.id.navigation_assessments -> AssessmentsFragment()
                R.id.navigation_competencies -> CompetenciesFragment()
                R.id.navigation_profile -> ProfileFragment()
                else -> HomeFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}