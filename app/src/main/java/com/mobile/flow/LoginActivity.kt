package com.mobile.flow

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LoginActivity : AppCompatActivity() {
    private lateinit var backBtn: ImageView
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginBtn: CardView
    private lateinit var signupTxt: TextView
    private lateinit var forgotPasswordTxt: TextView
    private lateinit var guestModeBtn: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Bind UI elements
        backBtn = findViewById(R.id.back_btn)
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        loginBtn = findViewById(R.id.login_btn)
        signupTxt = findViewById(R.id.signup_txt)
        forgotPasswordTxt = findViewById(R.id.forgot_password_txt)

        // Setup click listeners
        backBtn.setOnClickListener {
            vibrate()
            finish()
        }

        loginBtn.setOnClickListener {
            vibrate()
            handleLogin()
        }

        // I think we dont need to implement this right now
        signupTxt.setOnClickListener {
            vibrate()
            Toast.makeText(this, "Sign up feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        // I think we dont need to implement this right now
        forgotPasswordTxt.setOnClickListener {
            vibrate()
            Toast.makeText(this, "Password recovery coming soon!", Toast.LENGTH_SHORT).show()
        }

    }

    private fun handleLogin() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        // Basic validation
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
            return
        }

        // Email validation
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: add Firebase authentication here

    }



    override fun onStart() {
        super.onStart()

        val sharedPreferences = getSharedPreferences("PomodoroSettings", MODE_PRIVATE)
        val darkMode = sharedPreferences.getBoolean("darkMode", false)
        val amoledMode = sharedPreferences.getBoolean("amoledMode", false)

        val mainLayout = findViewById<RelativeLayout>(R.id.container)

        if (amoledMode) {
            mainLayout.setBackgroundColor(Color.BLACK)
            // Update text colors for dark mode
            findViewById<TextView>(R.id.email_input).setTextColor(Color.WHITE)
            findViewById<TextView>(R.id.password_input).setTextColor(Color.WHITE)
        } else if (darkMode) {
            mainLayout.setBackgroundColor(Color.parseColor("#1C1C1E"))
            // Update text colors for dark mode
            findViewById<TextView>(R.id.email_input).setTextColor(Color.WHITE)
            findViewById<TextView>(R.id.password_input).setTextColor(Color.WHITE)
        } else {
            mainLayout.setBackgroundColor(Color.parseColor("#EDEDED"))
        }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun vibrate() {
        val sharedPreferences = getSharedPreferences("PomodoroSettings", MODE_PRIVATE)
        if (!sharedPreferences.getBoolean("hapticFeedback", true)) return

        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        }
    }
}

