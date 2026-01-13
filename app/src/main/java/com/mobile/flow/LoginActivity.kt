package com.mobile.flow

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
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
import com.mobile.flow.utils.FirebaseSyncManager
import com.mobile.flow.utils.StatsManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var backBtn: ImageView
    private lateinit var googleSignInBtn: CardView
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        authManager = AuthManager.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        backBtn = findViewById(R.id.back_btn)
        googleSignInBtn = findViewById(R.id.google_sign_in_btn)

        backBtn.setOnClickListener {
            vibrate()
            finish()
        }

        googleSignInBtn.setOnClickListener {
            vibrate()
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        val credentialManager = CredentialManager.create(this)
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
                handleSignIn(result)
            } catch (e: GetCredentialException) {
                Log.e("LoginActivity", "Error getting credential", e)
                Toast.makeText(this@LoginActivity, "Sign-in cancelled or failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSignIn(result: GetCredentialResponse) {
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
                    onAuthSuccess()
                } else {
                    Toast.makeText(this@LoginActivity, "Firebase auth failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun onAuthSuccess() {
        val currentUser = AuthManager.getInstance().currentUser
        if (currentUser != null) {
            val statsPrefs = getSharedPreferences("PomodoroStats", MODE_PRIVATE)
            statsPrefs.edit().apply {
                putString("userName", currentUser.displayName)
                putString("userEmail", currentUser.email)
                apply()
            }
        }

        Toast.makeText(this, "Signed in successfully", Toast.LENGTH_SHORT).show()
        val syncManager = FirebaseSyncManager()
        val statsManager = StatsManager(this)

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
    }

    override fun onStart() {
        super.onStart()
        val sharedPreferences = getSharedPreferences("PomodoroSettings", MODE_PRIVATE)
        val darkMode = sharedPreferences.getBoolean("darkMode", false)
        val amoledMode = sharedPreferences.getBoolean("amoledMode", false)

        val mainLayout = findViewById<RelativeLayout>(R.id.container)

        if (amoledMode) {
            mainLayout.setBackgroundColor(Color.BLACK)
        } else if (darkMode) {
            mainLayout.setBackgroundColor(Color.parseColor("#1C1C1E"))
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
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        }
    }
}

