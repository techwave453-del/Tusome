package com.example.tusome.ui

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.tusome.R
import com.example.tusome.data.repository.SupabaseAuthRepository
import com.example.tusome.ui.fragments.AssessmentsFragment
import com.example.tusome.ui.fragments.CompetenciesFragment
import com.example.tusome.ui.fragments.HomeFragment
import com.example.tusome.ui.fragments.ProfileFragment
import com.example.tusome.ui.fragments.StrandsFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationDrawer: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SupabaseAuthRepository.currentSession(this) == null) {
            startActivity(android.content.Intent(this, AuthActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)

        // Rotate near-expiry access tokens in the background. A temporary network failure
        // must not prevent learners from opening the app's locally available screens.
        SupabaseAuthRepository.refreshSessionIfNeeded(this) { result ->
            result.onFailure {
                // Keep the encrypted cached session for offline use; retry on a later launch.
            }
        }

        toolbar = findViewById(R.id.toolbar)
        bottomNav = findViewById(R.id.bottomNavigationView)
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationDrawer = findViewById(R.id.navigationDrawer)
        val roles = SupabaseAuthRepository.currentSession(this)?.roles.orEmpty()
        navigationDrawer.menu.findItem(R.id.drawer_admin).isVisible =
            roles.any { it == "platform_admin" || it == "school_admin" }

        toolbar.navigationIcon = getDrawable(R.drawable.ic_menu)
        toolbar.navigationContentDescription = getString(R.string.open_navigation_menu)
        toolbar.setNavigationOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
        toolbar.title = getString(R.string.nav_home)
        toolbar.subtitle = null

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.navigation_home
            navigationDrawer.setCheckedItem(R.id.drawer_home)
            showDestination(R.id.navigation_home)
        } else {
            updateToolbarTitle(bottomNav.selectedItemId)
            navigationDrawer.setCheckedItem(drawerIdFor(bottomNav.selectedItemId))
        }

        bottomNav.setOnItemSelectedListener { item ->
            showDestination(item.itemId)
            navigationDrawer.setCheckedItem(drawerIdFor(item.itemId))
            true
        }

        // Reselecting the current tab should not reset its scroll position or form state.
        bottomNav.setOnItemReselectedListener { }

        navigationDrawer.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.drawer_home -> selectBottomDestination(R.id.navigation_home)
                R.id.drawer_strands -> selectBottomDestination(R.id.navigation_strands)
                R.id.drawer_assessments -> selectBottomDestination(R.id.navigation_assessments)
                R.id.drawer_competencies -> selectBottomDestination(R.id.navigation_competencies)
                R.id.drawer_profile -> selectBottomDestination(R.id.navigation_profile)
                R.id.drawer_admin -> startActivity(android.content.Intent(this, AdminActivity::class.java))
                R.id.drawer_about -> showAboutDialog()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    }
                    bottomNav.selectedItemId != R.id.navigation_home -> {
                        bottomNav.selectedItemId = R.id.navigation_home
                    }
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    private fun selectBottomDestination(itemId: Int) {
        if (bottomNav.selectedItemId == itemId) {
            navigationDrawer.setCheckedItem(drawerIdFor(itemId))
        } else {
            bottomNav.selectedItemId = itemId
        }
    }

    private fun showDestination(itemId: Int) {
        val tag = itemId.toString()
        val fragmentManager = supportFragmentManager
        val existingFragment = fragmentManager.findFragmentByTag(tag)
        val target = existingFragment ?: createDestination(itemId)

        updateToolbarTitle(itemId)

        // Apply tab changes immediately so rapid taps never queue stale destinations.
        if (fragmentManager.isStateSaved) return

        fragmentManager.beginTransaction().apply {
            setReorderingAllowed(true)
            fragmentManager.fragments.forEach { fragment ->
                if (fragment.isAdded && fragment != target) {
                    hide(fragment)
                }
            }
            if (existingFragment == null) {
                add(R.id.fragmentContainer, target, tag)
            } else {
                show(target)
            }
        }.commitNow()
    }

    private fun createDestination(itemId: Int): Fragment = when (itemId) {
        R.id.navigation_home -> HomeFragment()
        R.id.navigation_strands -> StrandsFragment()
        R.id.navigation_assessments -> AssessmentsFragment()
        R.id.navigation_competencies -> CompetenciesFragment()
        R.id.navigation_profile -> ProfileFragment()
        else -> HomeFragment()
    }

    private fun drawerIdFor(itemId: Int): Int = when (itemId) {
        R.id.navigation_home -> R.id.drawer_home
        R.id.navigation_strands -> R.id.drawer_strands
        R.id.navigation_assessments -> R.id.drawer_assessments
        R.id.navigation_competencies -> R.id.drawer_competencies
        R.id.navigation_profile -> R.id.drawer_profile
        else -> R.id.drawer_home
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

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(R.string.app_subtitle)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
