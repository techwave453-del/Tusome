package com.example.tusome.ui

import android.graphics.Color
import android.os.Bundle
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
import org.json.JSONObject

class AdminActivity : AppCompatActivity() {
    private lateinit var schoolName: EditText
    private lateinit var county: EditText
    private lateinit var inviteName: EditText
    private lateinit var inviteEmail: EditText
    private lateinit var schoolId: EditText
    private lateinit var roleSpinner: Spinner
    private lateinit var createSchoolButton: Button
    private lateinit var inviteButton: Button
    private lateinit var roleKeys: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = SupabaseAuthRepository.currentSession(this)
        if (session == null || session.roles.none { it == "platform_admin" || it == "school_admin" }) {
            Toast.makeText(this, "Your account does not have school-management permissions.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val isPlatformAdmin = "platform_admin" in session.roles
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
        page.addView(TextView(this).apply {
            text = "Signed in as ${session.displayName}\nRole: ${session.roles.joinToString()}"
            textSize = 14f
            setTextColor(Color.DKGRAY)
        }, margins(top = 6, bottom = 18))

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
        schoolId = field("School ID (UUID; required for school roles)")
        if (!isPlatformAdmin) schoolId.setText(session.schoolId.orEmpty())
        roleSpinner = Spinner(this)
        roleKeys = if (isPlatformAdmin) {
            listOf("learner", "teacher", "school_admin", "curriculum_editor", "platform_admin")
        } else {
            listOf("learner", "teacher")
        }
        val roleLabels = roleKeys.map { it.replace('_', ' ').replaceFirstChar { ch -> ch.uppercase() } }
        roleSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roleLabels)
        page.addView(inviteName, margins(top = 8))
        page.addView(inviteEmail, margins(top = 8))
        page.addView(roleSpinner, margins(top = 8))
        page.addView(schoolId, margins(top = 8))
        page.addView(TextView(this).apply {
            text = if (isPlatformAdmin) {
                "For teacher and school administrator roles, enter the school's UUID. Creating a school above fills this field automatically."
            } else {
                "You can invite learners and teachers to your assigned school only."
            }
            textSize = 12f
            setTextColor(Color.DKGRAY)
        }, margins(top = 8))
        inviteButton = actionButton("Send invitation")
        page.addView(inviteButton, margins(top = 14))
        inviteButton.setOnClickListener { inviteUser() }

        val backButton = Button(this).apply {
            text = "Back to learning"
            isAllCaps = false
            setOnClickListener { finish() }
        }
        page.addView(backButton, margins(top = 10))
        setContentView(scroll)
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
                if (id.isNotBlank()) schoolId.setText(id)
                Toast.makeText(
                    this,
                    if (id.isNotBlank()) "School created. Its ID has been filled into the invitation form." else "School created.",
                    Toast.LENGTH_LONG
                ).show()
            }.onFailure { error ->
                Toast.makeText(this, error.message ?: "Could not create school.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun inviteUser() {
        val name = inviteName.text.toString().trim()
        val email = inviteEmail.text.toString().trim()
        val role = roleKeys.getOrNull(roleSpinner.selectedItemPosition) ?: "learner"
        val school = schoolId.text.toString().trim()
        if (name.length < 2) {
            inviteName.error = "Enter the user's full name"
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inviteEmail.error = "Enter a valid email address"
            return
        }
        if (role in listOf("teacher", "school_admin") && school.isBlank()) {
            schoolId.error = "A school ID is required for this role"
            return
        }

        inviteButton.isEnabled = false
        val payload = JSONObject()
            .put("action", "invite_user")
            .put("display_name", name)
            .put("email", email)
            .put("role_key", role)
        if (school.isNotBlank()) payload.put("school_id", school)
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
            }.onFailure { error ->
                Toast.makeText(this, error.message ?: "Could not send invitation.", Toast.LENGTH_LONG).show()
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
