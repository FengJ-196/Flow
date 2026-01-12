package com.mobile.flow

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
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
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.mobile.flow.utils.AuthManager
import com.mobile.flow.utils.DailyStats
import com.mobile.flow.utils.FirebaseSyncManager
import com.mobile.flow.utils.Stats
import com.mobile.flow.utils.StatsManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageView
    private lateinit var googleSignInBtn: CardView
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginBtn: CardView
    private lateinit var signupTxt: TextView
    private lateinit var forgotPasswordTxt: TextView
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        authManager = AuthManager.getInstance()

        // Bind UI elements
        backBtn = findViewById(R.id.back_btn)
        googleSignInBtn = findViewById(R.id.google_sign_in_btn)
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

        googleSignInBtn.setOnClickListener {
            vibrate()
            signInWithGoogle()
        }

        loginBtn.setOnClickListener {
            vibrate()
            handleLogin()
        }

        signupTxt.setOnClickListener {
            vibrate()
            Toast.makeText(this, "Sign up feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        forgotPasswordTxt.setOnClickListener {
            vibrate()
            Toast.makeText(this, "Password recovery coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithGoogle() {
        val credentialManager = CredentialManager.create(this)
        
        // IMPORTANT: Replace this with your actual Web Client ID from the Firebase Console
        val webClientId = "240899912782-cpivrl34pocl9mgcafkio20b1spp8kno.apps.googleusercontent.com"
        
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity
                )
                handleGoogleSignIn(result)
            } catch (e: GetCredentialException) {
                Log.e("LoginActivity", "Error getting credential", e)
                Toast.makeText(this@LoginActivity, "Sign-in cancelled or failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleGoogleSignIn(result: GetCredentialResponse) {
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                firebaseAuthWithGoogle(idToken)
            } catch (e: GoogleIdTokenParsingException) {
                Log.e("LoginActivity", "Error parsing Google ID token", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this@LoginActivity, "Signed in successfully", Toast.LENGTH_SHORT).show()
                    
                    val syncManager = FirebaseSyncManager()
                    val statsManager = StatsManager(this@LoginActivity)
                    
                    syncManager.downloadStats { summary, daily ->
                        try {
                            if (summary != null && daily != null) {
                                statsManager.syncWithCloud(summary, daily)
                                Log.d("LoginActivity", "Cloud data synced successfully")
                            }
                        } catch (e: Exception) {
                            Log.e("LoginActivity", "Error syncing cloud data", e)
                        } finally {
                            finish()
                        }
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Firebase auth failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleLogin() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: add Firebase Email/Password authentication here
        Toast.makeText(this, "Email login coming soon!", Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()

        val sharedPreferences = getSharedPreferences("PomodoroSettings", MODE_PRIVATE)
        val darkMode = sharedPreferences.getBoolean("darkMode", false)
        val amoledMode = sharedPreferences.getBoolean("amoledMode", false)

        val mainLayout = findViewById<RelativeLayout>(R.id.container)

        if (amoledMode) {
            mainLayout.setBackgroundColor(Color.BLACK)
            emailInput.setTextColor(Color.WHITE)
            passwordInput.setTextColor(Color.WHITE)
        } else if (darkMode) {
            mainLayout.setBackgroundColor(Color.parseColor("#1C1C1E"))
            emailInput.setTextColor(Color.WHITE)
            passwordInput.setTextColor(Color.WHITE)
        } else {
            mainLayout.setBackgroundColor(Color.parseColor("#EDEDED"))
            emailInput.setTextColor(Color.BLACK)
            passwordInput.setTextColor(Color.BLACK)
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
