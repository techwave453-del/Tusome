package com.example.tusome.ui

import android.content.Intent
import android.graphics.Color
import android.net.Uri
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
import com.example.tusome.data.repository.SupabaseAuthRepository
import com.google.android.material.card.MaterialCardView

class AuthActivity : AppCompatActivity() {
    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var submitButton: Button
    private lateinit var modeButton: Button
    private lateinit var forgotButton: Button
    private lateinit var helperText: TextView
    private var createAccount = false
    private var recoveryCallback: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRecoveryLink(intent.data)) {
            recoveryCallback = intent.data
            renderPasswordReset()
            return
        }
        if (SupabaseAuthRepository.currentSession(this) != null) {
            openApp()
            return
        }
        renderForm()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isRecoveryLink(intent.data)) {
            recoveryCallback = intent.data
            renderPasswordReset()
        }
    }

    private fun isRecoveryLink(uri: Uri?): Boolean =
        uri?.scheme == "com.example.tusome" && uri.host == "auth" && uri.path == "/reset-password"

    private fun renderForm() {
        val page = pageContainer()
        addBrand(page)
        val card = cardContainer()
        val form = formContainer()
        card.addView(form)
        page.addView(card, LinearLayout.LayoutParams(-1, -2))
        form.addView(heading("Sign in to your account"), matchWrap())

        nameInput = field("Full name", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        emailInput = field("Email address", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        passwordInput = field("Password", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        passwordInput.filters = arrayOf(android.text.InputFilter.LengthFilter(128))
        form.addView(nameInput, marginParams(top = 16))
        form.addView(emailInput, marginParams(top = 12))
        form.addView(passwordInput, marginParams(top = 12))

        helperText = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.rgb(82, 96, 83))
        }
        form.addView(helperText, marginParams(top = 12))
        submitButton = primaryButton("Sign in") { submit() }
        form.addView(submitButton, marginParams(top = 20))
        forgotButton = secondaryButton("Forgot password?") { renderRecoveryRequest() }
        form.addView(forgotButton, marginParams(top = 2))
        modeButton = secondaryButton("Create a new account") {
            createAccount = !createAccount
            updateMode()
        }
        form.addView(modeButton, marginParams(top = 2))
        addFooter(page)
        setContentView(wrapPage(page))
        updateMode()
    }

    private fun renderRecoveryRequest() {
        val page = pageContainer()
        addBrand(page)
        val card = cardContainer()
        val form = formContainer()
        card.addView(form)
        page.addView(card, LinearLayout.LayoutParams(-1, -2))
        form.addView(heading("Reset your password"), matchWrap())
        form.addView(TextView(this).apply {
            text = "Enter the email address linked to your Tusome account. If an account exists, we will send a secure recovery link."
            textSize = 14f
            setTextColor(Color.rgb(82, 96, 83))
        }, marginParams(top = 10))
        val recoveryEmail = field("Email address", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
            .apply { setText(emailInput.text?.toString().orEmpty()) }
        form.addView(recoveryEmail, marginParams(top = 18))
        form.addView(primaryButton("Send recovery link") { button ->
            val email = recoveryEmail.text?.toString()?.trim().orEmpty()
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                recoveryEmail.error = "Enter a valid email address"
                return@primaryButton
            }
            button.isEnabled = false
            button.text = "Sending…"
            SupabaseAuthRepository.requestPasswordRecovery(this, email) { result ->
                button.isEnabled = true
                button.text = "Send recovery link"
                result.onSuccess {
                    Toast.makeText(this, "If an account exists for that email, a password recovery link has been sent.", Toast.LENGTH_LONG).show()
                    emailInput.setText(email)
                    renderForm()
                }.onFailure { error ->
                    Toast.makeText(this, error.message ?: "Could not send the recovery email. Try again.", Toast.LENGTH_LONG).show()
                }
            }
        }, marginParams(top = 20))
        form.addView(secondaryButton("Back to sign in") { renderForm() }, marginParams(top = 6))
        addFooter(page)
        setContentView(wrapPage(page))
    }

    private fun renderPasswordReset() {
        val page = pageContainer()
        addBrand(page)
        val card = cardContainer()
        val form = formContainer()
        card.addView(form)
        page.addView(card, LinearLayout.LayoutParams(-1, -2))
        form.addView(heading("Choose a new password"), matchWrap())
        form.addView(TextView(this).apply {
            text = "Use at least 8 characters. Your new password will replace the old one."
            textSize = 14f
            setTextColor(Color.rgb(82, 96, 83))
        }, marginParams(top = 10))
        val newPassword = field("New password", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        val confirmPassword = field("Confirm new password", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        newPassword.filters = arrayOf(android.text.InputFilter.LengthFilter(128))
        confirmPassword.filters = arrayOf(android.text.InputFilter.LengthFilter(128))
        form.addView(newPassword, marginParams(top = 18))
        form.addView(confirmPassword, marginParams(top = 12))
        form.addView(primaryButton("Update password") { button ->
            val password = newPassword.text?.toString().orEmpty()
            val confirmation = confirmPassword.text?.toString().orEmpty()
            if (password.length < 8) {
                newPassword.error = "Use at least 8 characters"
                return@primaryButton
            }
            if (password != confirmation) {
                confirmPassword.error = "Passwords do not match"
                return@primaryButton
            }
            val callback = recoveryCallback
            if (callback == null) {
                Toast.makeText(this, "The recovery link is missing. Request a new one.", Toast.LENGTH_LONG).show()
                renderForm()
                return@primaryButton
            }
            button.isEnabled = false
            button.text = "Updating…"
            SupabaseAuthRepository.completePasswordRecovery(this, callback, password) { result ->
                button.isEnabled = true
                button.text = "Update password"
                result.onSuccess {
                    recoveryCallback = null
                    Toast.makeText(this, "Your password has been updated.", Toast.LENGTH_LONG).show()
                    openApp()
                }.onFailure { error ->
                    Toast.makeText(this, error.message ?: "The recovery link may have expired. Request a new one.", Toast.LENGTH_LONG).show()
                }
            }
        }, marginParams(top = 20))
        form.addView(secondaryButton("Back to sign in") {
            recoveryCallback = null
            renderForm()
        }, marginParams(top = 6))
        addFooter(page)
        setContentView(wrapPage(page))
    }

    private fun pageContainer() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(24), dp(24), dp(24), dp(24))
        setBackgroundColor(Color.rgb(246, 249, 246))
    }

    private fun addBrand(page: LinearLayout) {
        page.addView(TextView(this).apply {
            text = "Tusome"
            textSize = 34f
            setTextColor(Color.rgb(27, 94, 32))
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }, matchWrap())
        page.addView(TextView(this).apply {
            text = "Kenyan CBC learning platform"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(82, 96, 83))
        }, marginParams(top = 4, bottom = 24))
    }

    private fun addFooter(page: LinearLayout) {
        page.addView(TextView(this).apply {
            text = "Your account and permissions are managed securely by Tusome."
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(100, 112, 101))
        }, marginParams(top = 18))
    }

    private fun cardContainer() = MaterialCardView(this).apply {
        radius = dp(20).toFloat()
        cardElevation = dp(3).toFloat()
        setCardBackgroundColor(Color.WHITE)
        strokeWidth = dp(1)
        strokeColor = Color.rgb(226, 234, 226)
    }

    private fun formContainer() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(20), dp(20), dp(20))
    }

    private fun heading(value: String) = TextView(this).apply {
        text = value
        textSize = 22f
        setTextColor(Color.rgb(30, 55, 32))
        setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun wrapPage(page: LinearLayout) = ScrollView(this).apply {
        isFillViewport = true
        addView(page)
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

    private fun primaryButton(label: String, action: (Button) -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(46, 125, 50))
        setOnClickListener { action(this) }
    }

    private fun secondaryButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { action() }
    }

    private fun updateMode() {
        nameInput.visibility = if (createAccount) View.VISIBLE else View.GONE
        forgotButton.visibility = if (createAccount) View.GONE else View.VISIBLE
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
                    if (signup.session != null) openApp() else {
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
                result.onSuccess { openApp() }.onFailure { error ->
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
