package com.example.tusome.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tusome.R
import com.example.tusome.data.model.GradeLevel
import com.example.tusome.data.repository.SupabaseAuthRepository
import com.example.tusome.ui.AuthActivity

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.btnSignOut).setOnClickListener {
            SupabaseAuthRepository.signOut(requireContext())
            Toast.makeText(requireContext(), "You have signed out.", Toast.LENGTH_SHORT).show()
            startActivity(
                Intent(requireContext(), AuthActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            )
            requireActivity().finish()
        }
        loadProfileData(view)
    }

    override fun onResume() {
        super.onResume()
        val root = view ?: return
        loadProfileData(root) // Show cached profile immediately, including while offline.
        root.findViewById<TextView>(R.id.tvProfileSyncStatus).text = "Refreshing profile from Supabase…"
        SupabaseAuthRepository.refreshCurrentUserData(requireContext()) { result ->
            if (!isAdded) return@refreshCurrentUserData
            val currentView = view ?: return@refreshCurrentUserData
            result.onSuccess {
                loadProfileData(currentView)
                currentView.findViewById<TextView>(R.id.tvProfileSyncStatus).text =
                    "Profile and role assignments refreshed from Supabase."
            }.onFailure { error ->
                // Keep cached values visible, but make a failed refresh observable instead of silently
                // leaving the user to assume the stale profile was saved successfully.
                currentView.findViewById<TextView>(R.id.tvProfileSyncStatus).text =
                    "Showing saved profile. Refresh failed: ${error.message ?: "Check your connection and try again."}"
            }
        }
    }

    private fun loadProfileData(view: View) {
        val session = SupabaseAuthRepository.currentSession(requireContext()) ?: return
        val roleLabels = session.roles.map { role ->
            when (role) {
                "learner" -> "Learner"
                "teacher" -> "Teacher"
                "school_admin" -> "School Administrator"
                "curriculum_editor" -> "Curriculum Editor"
                "platform_admin" -> "Platform Administrator"
                else -> role.replace('_', ' ').replaceFirstChar { it.uppercase() }
            }
        }
        val grade = session.gradeId?.let { code ->
            GradeLevel.values().firstOrNull { it.code.equals(code, ignoreCase = true) }?.title ?: code
        } ?: "Grade not assigned"
        val roleText = roleLabels.joinToString(", ").ifBlank { "No role assigned" }

        view.findViewById<TextView>(R.id.tvProfileName).text = session.displayName
        view.findViewById<TextView>(R.id.tvProfileAdm).text = "${session.email} • $roleText"
        view.findViewById<TextView>(R.id.tvSchoolName).text =
            "School: ${if (session.schoolId == null) "Not assigned yet" else "School linked"}"
        view.findViewById<TextView>(R.id.tvCountyName).text = "Learning level: $grade"
        view.findViewById<TextView>(R.id.tvParentContact).text =
            "Account ID: ${session.userId.take(8)}…"
    }
}
