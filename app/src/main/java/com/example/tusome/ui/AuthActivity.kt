package com.example.tusome.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import com.example.tusome.data.repository.SupabaseAuthRepository
import com.google.android.material.card.MaterialCardView

class AuthActivity : AppCompatActivity() {

    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var submitButton: Button
    private lateinit var modeButton: Button
    private lateinit var helperText: TextView
    private var createAccount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SupabaseAuthRepository.currentSession(this) != null) {
            openApp()
            return
        }
        renderForm()
    }

    private fun renderForm() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24))
            setBackgroundColor(Color.rgb(246, 249, 246))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(page)
        }

        val brand = TextView(this).apply {
            text = "Tusome"
            textSize = 34f
            setTextColor(Color.rgb(27, 94, 32))
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        page.addView(brand, matchWrap())
        page.addView(TextView(this).apply {
            text = "Kenyan CBC learning platform"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(82, 96, 83))
        }, marginParams(top = 4, bottom = 24))

        val card = MaterialCardView(this).apply {
            radius = dp(20).toFloat()
            cardElevation = dp(3).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeWidth = dp(1)
            strokeColor = Color.rgb(226, 234, 226)
        }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20))
        }
        card.addView(form)
        page.addView(card, LinearLayout.LayoutParams(-1, -2))

        form.addView(TextView(this).apply {
            text = "Sign in to your account"
            textSize = 22f
            setTextColor(Color.rgb(30, 55, 32))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }, matchWrap())

        nameInput = field("Full name", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        emailInput = field("Email address", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        passwordInput = field("Password", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        passwordInput.setSingleLine(true)
        passwordInput.filters = arrayOf(android.text.InputFilter.LengthFilter(128))

        form.addView(nameInput, marginParams(top = 16))
        form.addView(emailInput, marginParams(top = 12))
        form.addView(passwordInput, marginParams(top = 12))

        helperText = TextView(this).apply {
            text = "New accounts start with learner access. School and platform roles must be assigned by an authorized administrator."
            textSize = 12f
            setTextColor(Color.rgb(82, 96, 83))
        }
        form.addView(helperText, marginParams(top = 12))

        submitButton = Button(this).apply {
            text = "Sign in"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(46, 125, 50))
            setOnClickListener { submit() }
        }
        form.addView(submitButton, marginParams(top = 20))

        modeButton = Button(this).apply {
            text = "Create a new account"
            isAllCaps = false
            setOnClickListener {
                createAccount = !createAccount
                updateMode()
            }
        }
        form.addView(modeButton, marginParams(top = 6))

        page.addView(TextView(this).apply {
            text = "Your account and permissions are managed securely by Tusome."
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(100, 112, 101))
        }, marginParams(top = 18))

        setContentView(scroll)
        updateMode()
    }

    private fun field(hintText: String, inputTypeValue: Int): EditText =
        EditText(this).apply {
            hint = hintText
            inputType = inputTypeValue
            setSingleLine(true)
            textSize = 16f
            setTextColor(Color.rgb(35, 45, 36))
            setHintTextColor(Color.rgb(125, 135, 126))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = getDrawable(android.R.drawable.edit_text)
        }

    private fun updateMode() {
        nameInput.visibility = if (createAccount) View.VISIBLE else View.GONE
        submitButton.text = if (createAccount) "Create account" else "Sign in"
        modeButton.text = if (createAccount) "I already have an account" else "Create a new account"
        helperText.text = if (createAccount) {
            "Your account will start with learner access. A school or platform administrator must assign any elevated role."
        } else {
            "Sign in to continue learning and access the features permitted for your account."
        }
    }

    private fun submit() {
        val email = emailInput.text?.toString()?.trim().orEmpty()
        val password = passwordInput.text?.toString().orEmpty()
        val displayName = nameInput.text?.toString()?.trim().orEmpty()

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Enter a valid email address"
            return
        }
        if (password.length < 8) {
            passwordInput.error = "Use at least 8 characters"
            return
        }
        if (createAccount && displayName.length < 2) {
            nameInput.error = "Enter your name"
            return
        }

        submitButton.isEnabled = false
        submitButton.text = "Please wait…"
        if (createAccount) {
            SupabaseAuthRepository.signUp(this, displayName, email, password) { result ->
                submitButton.isEnabled = true
                updateMode()
                result.onSuccess { signup ->
                    if (signup.session != null) {
                        openApp()
                    } else {
                        createAccount = false
                        updateMode()
                        Toast.makeText(this, signup.message, Toast.LENGTH_LONG).show()
                    }
                }.onFailure { error ->
                    Toast.makeText(this, error.message ?: "Could not create account.", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            SupabaseAuthRepository.signIn(this, email, password) { result ->
                submitButton.isEnabled = true
                updateMode()
                result.onSuccess { openApp() }
                    .onFailure { error ->
                        Toast.makeText(this, error.message ?: "Sign in failed.", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }

    private fun openApp() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2)

    private fun marginParams(top: Int = 0, bottom: Int = 0) =
        LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(top)
            bottomMargin = dp(bottom)
        }
}
