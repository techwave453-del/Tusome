package com.example.tusome.ui

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tusome.data.repository.SupabaseAuthRepository
import org.json.JSONArray
import org.json.JSONObject

private data class SchoolOption(
    val id: String,
    val name: String,
    val active: Boolean
) {
    override fun toString(): String = if (active) name else "$name (inactive)"
}

class AdminActivity : AppCompatActivity() {
    private lateinit var schoolName: EditText
    private lateinit var county: EditText
    private lateinit var inviteName: EditText
    private lateinit var inviteEmail: EditText
    private lateinit var schoolSpinner: Spinner
    private lateinit var roleSpinner: Spinner
    private lateinit var createSchoolButton: Button
    private lateinit var inviteButton: Button
    private lateinit var usersContainer: LinearLayout
    private lateinit var auditContainer: LinearLayout
    private lateinit var refreshButton: Button
    private lateinit var accountSummary: TextView
    private lateinit var roleKeys: List<String>
    private val schools = mutableListOf<SchoolOption>()
    private var isPlatformAdmin = false
    private var currentUserId: String? = null
    private var ownSchoolId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = SupabaseAuthRepository.currentSession(this)
        if (session == null || session.roles.none { it == "platform_admin" || it == "school_admin" }) {
            Toast.makeText(this, "Your account does not have school-management permissions.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        isPlatformAdmin = "platform_admin" in session.roles
        currentUserId = session.userId
        ownSchoolId = session.schoolId
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
            setBackgroundColor(Color.rgb(246, 249, 246))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(page)
        }

        page.addView(TextView(this).apply {
            text = "School administration"
            textSize = 26f
            setTextColor(Color.rgb(27, 94, 32))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }, params())
        accountSummary = TextView(this).apply {
            text = "Signed in as ${session.displayName}\nRole: ${session.roles.joinToString()}"
            textSize = 14f
            setTextColor(Color.DKGRAY)
        }
        page.addView(accountSummary, margins(top = 6, bottom = 12))

        refreshButton = actionButton("Refresh users and activity")
        page.addView(refreshButton, margins(bottom = 18))
        refreshButton.setOnClickListener { refreshAll() }

        if (isPlatformAdmin) {
            section(page, "Create a school")
            schoolName = field("School name")
            county = field("County (optional)")
            page.addView(schoolName, margins(top = 8))
            page.addView(county, margins(top = 8))
            createSchoolButton = actionButton("Create school")
            page.addView(createSchoolButton, margins(top = 10, bottom = 22))
            createSchoolButton.setOnClickListener { createSchool() }
        }

        section(page, "Invite a user")
        inviteName = field("Full name")
        inviteEmail = field("Email address").apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        schoolSpinner = Spinner(this)
        roleSpinner = Spinner(this)
        roleKeys = if (isPlatformAdmin) {
            listOf("learner", "teacher", "school_admin", "curriculum_editor", "platform_admin")
        } else {
            listOf("learner", "teacher")
        }
        roleSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            roleKeys.map { it.replace('_', ' ').replaceFirstChar { ch -> ch.uppercase() } }
        )
        page.addView(inviteName, margins(top = 8))
        page.addView(inviteEmail, margins(top = 8))
        page.addView(TextView(this).apply {
            text = "School (select a school; the ID is handled securely)"
            textSize = 12f
            setTextColor(Color.DKGRAY)
        }, margins(top = 8))
        page.addView(schoolSpinner, margins(top = 2))
        page.addView(roleSpinner, margins(top = 8))
        page.addView(TextView(this).apply {
            text = if (isPlatformAdmin) {
                "Platform administrators can invite users to a selected school. Global roles do not require a school."
            } else {
                "You can invite learners and teachers to your assigned school only."
            }
            textSize = 12f
            setTextColor(Color.DKGRAY)
        }, margins(top = 8))
        inviteButton = actionButton("Send invitation")
        page.addView(inviteButton, margins(top = 12, bottom = 22))
        inviteButton.setOnClickListener { inviteUser() }

        section(page, "Users and role assignments")
        if (isPlatformAdmin) {
            page.addView(TextView(this).apply {
                text = "Your platform administrator role is protected. You can change your secondary role here without losing administrator access."
                textSize = 12f
                setTextColor(Color.DKGRAY)
            }, margins(top = 4, bottom = 6))
            page.addView(actionButton("Change my secondary role").apply {
                setOnClickListener { showRoleAssignmentDialog(currentUserId.orEmpty(), replaceOwn = true) }
            }, margins(bottom = 10))
        }
        usersContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        page.addView(usersContainer, margins(top = 6, bottom = 20))

        section(page, "Recent administration activity")
        auditContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        page.addView(auditContainer, margins(top = 6, bottom = 20))

        page.addView(Button(this).apply {
            text = "Back to learning"
            isAllCaps = false
            setOnClickListener { finish() }
        }, margins(top = 10))
        setContentView(scroll)
        refreshAll()
    }

    private fun refreshAll() {
        refreshButton.isEnabled = false
        loadSchools()
        loadUsers()
        loadAuditLog()
        refreshButton.postDelayed({ refreshButton.isEnabled = true }, 800)
    }

    /**
     * Re-fetch the signed-in user's profile and role assignments after a successful
     * admin write, then reload all visible admin data. Cached data stays usable offline.
     */
    private fun refreshAfterMutation(selectSchoolId: String? = null) {
        SupabaseAuthRepository.refreshCurrentUserData(this) { result ->
            if (isFinishing || isDestroyed) return@refreshCurrentUserData
            result.onSuccess { session ->
                currentUserId = session.userId
                ownSchoolId = session.schoolId
                isPlatformAdmin = "platform_admin" in session.roles
                accountSummary.text =
                    "Signed in as ${session.displayName}\nRole: ${session.roles.joinToString()}"
            }
            if (selectSchoolId == null) {
                refreshAll()
            } else {
                loadSchools(selectSchoolId)
                loadUsers()
                loadAuditLog()
            }
        }
    }

    private fun loadSchools(selectId: String? = null) {
        SupabaseAuthRepository.manageSchoolUsers(
            this,
            JSONObject().put("action", "list_schools")
        ) { result ->
            result.onSuccess { response ->
                schools.clear()
                val rows = response.optJSONArray("schools") ?: JSONArray()
                for (index in 0 until rows.length()) {
                    val row = rows.optJSONObject(index) ?: continue
                    val id = row.optString("id")
                    if (id.isBlank()) continue
                    schools.add(
                        SchoolOption(
                            id = id,
                            name = row.optString("name", "Unnamed school"),
                            active = row.optBoolean("is_active", true)
                        )
                    )
                }
                schoolSpinner.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    schools
                )
                val preferredId = selectId ?: ownSchoolId
                val preferredIndex = schools.indexOfFirst { it.id == preferredId }
                if (preferredIndex >= 0) schoolSpinner.setSelection(preferredIndex)
                schoolSpinner.isEnabled = isPlatformAdmin && schools.size > 1
                if (schools.isEmpty()) {
                    Toast.makeText(this, "No schools are available yet. A platform administrator can create one.", Toast.LENGTH_LONG).show()
                }
            }.onFailure { error ->
                Toast.makeText(this, error.message ?: "Could not load schools.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createSchool() {
        val name = schoolName.text.toString().trim()
        if (name.length < 2) {
            schoolName.error = "Enter a school name"
            return
        }
        createSchoolButton.isEnabled = false
        SupabaseAuthRepository.manageSchoolUsers(
            this,
            JSONObject()
                .put("action", "create_school")
                .put("name", name)
                .put("county", county.text.toString().trim())
        ) { result ->
            createSchoolButton.isEnabled = true
            result.onSuccess { response ->
                val school = response.optJSONObject("school")
                val id = school?.optString("id").orEmpty()
                Toast.makeText(this, "School created.", Toast.LENGTH_LONG).show()
                schoolName.text.clear()
                county.text.clear()
                refreshAfterMutation(id.takeIf { it.isNotBlank() })
            }.onFailure { error ->
                Toast.makeText(this, error.message ?: "Could not create school.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun inviteUser() {
        val name = inviteName.text.toString().trim()
        val email = inviteEmail.text.toString().trim()
        val role = roleKeys.getOrNull(roleSpinner.selectedItemPosition) ?: "learner"
        val selectedSchool = schools.getOrNull(schoolSpinner.selectedItemPosition)?.id
        val school = selectedSchool.takeIf { role in listOf("learner", "teacher", "school_admin") }
        if (name.length < 2) {
            inviteName.error = "Enter the user's full name"
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inviteEmail.error = "Enter a valid email address"
            return
        }
        if (role in listOf("teacher", "school_admin") && school == null) {
            Toast.makeText(this, "Select a school for this role.", Toast.LENGTH_LONG).show()
            return
        }

        inviteButton.isEnabled = false
        val payload = JSONObject()
            .put("action", "invite_user")
            .put("display_name", name)
            .put("email", email)
            .put("role_key", role)
        if (school != null) payload.put("school_id", school)
        SupabaseAuthRepository.manageSchoolUsers(this, payload) { result ->
            inviteButton.isEnabled = true
            result.onSuccess { response ->
                Toast.makeText(
                    this,
                    response.optString("message", "Invitation sent."),
                    Toast.LENGTH_LONG
                ).show()
                inviteName.text.clear()
                inviteEmail.text.clear()
                refreshAfterMutation()
            }.onFailure { error ->
                Toast.makeText(this, error.message ?: "Could not send invitation.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadUsers() {
        usersContainer.removeAllViews()
        usersContainer.addView(messageText("Loading users…"))
        SupabaseAuthRepository.manageSchoolUsers(
            this,
            JSONObject().put("action", "list_users")
        ) { result ->
            usersContainer.removeAllViews()
            result.onSuccess { response ->
                val rows = response.optJSONArray("users") ?: JSONArray()
                if (rows.length() == 0) {
                    usersContainer.addView(messageText("No users found in this administration scope."))
                    return@onSuccess
                }
                for (index in 0 until rows.length()) {
                    val user = rows.optJSONObject(index) ?: continue
                    val userId = user.optString("user_id")
                    val displayName = user.optString("display_name").ifBlank { "Unnamed user" }
                    val email = user.optString("email").ifBlank { "Email unavailable" }
                    val schoolName = user.optString("school_name")
                    val roles = user.optJSONArray("roles") ?: JSONArray()
                    val card = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(12), dp(10), dp(12), dp(10))
                        setBackgroundColor(Color.WHITE)
                    }
                    card.addView(TextView(this).apply {
                        text = displayName
                        textSize = 16f
                        setTextColor(Color.rgb(32, 63, 34))
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    }, params())
                    card.addView(messageText(email + if (schoolName.isNotBlank()) "\nSchool: $schoolName" else ""), margins(top = 3))
                    if (roles.length() == 0) {
                        card.addView(messageText("No role assignments returned."), margins(top = 4))
                    }
                    for (roleIndex in 0 until roles.length()) {
                        val role = roles.optJSONObject(roleIndex) ?: continue
                        val roleKey = role.optString("role_key")
                        val roleSchoolId = role.optString("school_id").takeIf { it.isNotBlank() && it != "null" }
                        val roleLabel = roleKey.replace('_', ' ')
                        card.addView(messageText("Role: $roleLabel"), margins(top = 4))
                        val canRevoke = userId != currentUserId && roleKey != "platform_admin" &&
                            (isPlatformAdmin || roleKey == "teacher")
                        if (canRevoke) {
                            val revokeButton = Button(this).apply {
                                text = "Revoke $roleLabel"
                                isAllCaps = false
                                setTextColor(Color.rgb(150, 35, 35))
                                setOnClickListener {
                                    confirmRevoke(userId, displayName, roleKey, roleSchoolId)
                                }
                            }
                            card.addView(revokeButton, margins(top = 3))
                        }
                    }
                    if (isPlatformAdmin && userId != currentUserId) {
                        card.addView(Button(this).apply {
                            text = "Assign additional role"
                            isAllCaps = false
                            setOnClickListener {
                                showRoleAssignmentDialog(userId, replaceOwn = false)
                            }
                        }, margins(top = 6))
                    }
                    usersContainer.addView(card, margins(bottom = 8))
                    val divider = View(this).apply { setBackgroundColor(Color.rgb(220, 226, 220)) }
                    usersContainer.addView(divider, LinearLayout.LayoutParams(-1, dp(1)).apply {
                        bottomMargin = dp(8)
                    })
                }
            }.onFailure { error ->
                usersContainer.removeAllViews()
                usersContainer.addView(messageText(error.message ?: "Could not load users."))
            }
        }
    }

    private fun showRoleAssignmentDialog(targetUserId: String, replaceOwn: Boolean) {
        if (!isPlatformAdmin) {
            Toast.makeText(this, "Only the super user can manage additional roles.", Toast.LENGTH_LONG).show()
            return
        }
        if (targetUserId.isBlank()) return

        val availableRoleKeys = listOf("learner", "teacher", "school_admin", "curriculum_editor")
        val dialogContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        dialogContent.addView(TextView(this).apply {
            text = if (replaceOwn) {
                "Choose one secondary role. Your platform administrator role will remain unchanged."
            } else {
                "This explicitly assigns an additional role to the selected user."
            }
            textSize = 13f
            setTextColor(Color.DKGRAY)
        }, margins(bottom = 8))

        val roleChoice = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@AdminActivity,
                android.R.layout.simple_spinner_dropdown_item,
                availableRoleKeys.map { it.replace('_', ' ').replaceFirstChar { ch -> ch.uppercase() } }
            )
        }
        val schoolChoice = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@AdminActivity,
                android.R.layout.simple_spinner_dropdown_item,
                schools
            )
            isEnabled = schools.isNotEmpty()
        }
        dialogContent.addView(TextView(this).apply {
            text = "Role"
            setTextColor(Color.DKGRAY)
            textSize = 12f
        })
        dialogContent.addView(roleChoice, margins(bottom = 8))
        dialogContent.addView(TextView(this).apply {
            text = "School (required for Teacher and School Administrator)"
            setTextColor(Color.DKGRAY)
            textSize = 12f
        })
        dialogContent.addView(schoolChoice, margins(bottom = 8))

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (replaceOwn) "Change my secondary role" else "Assign an additional role")
            .setView(dialogContent)
            .setNegativeButton("Cancel", null)
            .setPositiveButton(if (replaceOwn) "Change role" else "Assign role", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val roleKey = availableRoleKeys.getOrNull(roleChoice.selectedItemPosition) ?: "learner"
                val selectedSchool = schools.getOrNull(schoolChoice.selectedItemPosition)?.id
                val needsSchool = roleKey == "teacher" || roleKey == "school_admin"
                if (needsSchool && selectedSchool == null) {
                    Toast.makeText(this, "Create or select an active school first.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val payload = JSONObject()
                    .put("action", if (replaceOwn) "replace_secondary_role" else "assign_role")
                    .put("user_id", targetUserId)
                    .put("role_key", roleKey)
                if (needsSchool && selectedSchool != null) payload.put("school_id", selectedSchool)

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                SupabaseAuthRepository.manageSchoolUsers(this, payload) { result ->
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    result.onSuccess { response ->
                        Toast.makeText(
                            this,
                            response.optString("message", "Role assignment updated."),
                            Toast.LENGTH_LONG
                        ).show()
                        dialog.dismiss()
                        refreshAfterMutation()
                    }.onFailure { error ->
                        Toast.makeText(
                            this,
                            error.message ?: "Role assignment could not be updated.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun confirmRevoke(userId: String, displayName: String, roleKey: String, schoolId: String?) {
        AlertDialog.Builder(this)
            .setTitle("Revoke role?")
            .setMessage("Remove the $roleKey role from $displayName? This does not delete the user's account or learning progress.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Revoke") { _, _ ->
                val payload = JSONObject()
                    .put("action", "revoke_role")
                    .put("user_id", userId)
                    .put("role_key", roleKey)
                if (schoolId != null) payload.put("school_id", schoolId)
                SupabaseAuthRepository.manageSchoolUsers(this, payload) { result ->
                    result.onSuccess { response ->
                        Toast.makeText(this, response.optString("message", "Role revoked."), Toast.LENGTH_LONG).show()
                        refreshAfterMutation()
                    }.onFailure { error ->
                        Toast.makeText(this, error.message ?: "Could not revoke role.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()
    }

    private fun loadAuditLog() {
        auditContainer.removeAllViews()
        auditContainer.addView(messageText("Loading recent activity…"))
        SupabaseAuthRepository.manageSchoolUsers(
            this,
            JSONObject().put("action", "list_audit_logs")
        ) { result ->
            auditContainer.removeAllViews()
            result.onSuccess { response ->
                val rows = response.optJSONArray("events") ?: JSONArray()
                if (rows.length() == 0) {
                    auditContainer.addView(messageText("No administrative activity has been recorded yet."))
                    return@onSuccess
                }
                for (index in 0 until rows.length()) {
                    val event = rows.optJSONObject(index) ?: continue
                    val details = event.optJSONObject("details") ?: JSONObject()
                    val action = event.optString("action_key").replace('.', ' ').replace('_', ' ')
                    val target = details.optString("email").ifBlank {
                        details.optString("name").ifBlank { event.optString("target_user_id") }
                    }
                    val line = if (target.isBlank()) action else "$action — $target"
                    auditContainer.addView(messageText("$line\n${event.optString("created_at")}"), margins(bottom = 8))
                }
            }.onFailure { error ->
                auditContainer.removeAllViews()
                auditContainer.addView(messageText(error.message ?: "Could not load administration activity."))
            }
        }
    }

    private fun section(parent: LinearLayout, title: String) {
        parent.addView(TextView(this).apply {
            text = title
            textSize = 19f
            setTextColor(Color.rgb(39, 75, 41))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }, margins(top = 8, bottom = 2))
    }

    private fun field(hintText: String) = EditText(this).apply {
        hint = hintText
        setSingleLine(true)
        textSize = 15f
        setTextColor(Color.rgb(35, 45, 36))
        setHintTextColor(Color.GRAY)
        setPadding(dp(12), dp(10), dp(12), dp(10))
    }

    private fun messageText(value: String) = TextView(this).apply {
        text = value
        textSize = 13f
        setTextColor(Color.DKGRAY)
    }

    private fun actionButton(label: String) = Button(this).apply {
        text = label
        isAllCaps = false
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(46, 125, 50))
    }

    private fun params() = LinearLayout.LayoutParams(-1, -2)

    private fun margins(top: Int = 0, bottom: Int = 0) =
        LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(top)
            bottomMargin = dp(bottom)
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
