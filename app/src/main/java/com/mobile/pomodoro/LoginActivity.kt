package com.mobile.pomodoro

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
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
import com.mobile.pomodoro.utils.AuthManager
import com.mobile.pomodoro.utils.DailyStats
import com.mobile.pomodoro.utils.FirebaseSyncManager
import com.mobile.pomodoro.utils.Stats
import com.mobile.pomodoro.utils.StatsManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInBtn: CardView
    private lateinit var backBtn: TextView
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        authManager = AuthManager.getInstance()

        googleSignInBtn = findViewById(R.id.google_sign_in_btn)
        backBtn = findViewById(R.id.back_btn)

        googleSignInBtn.setOnClickListener {
            signInWithGoogle()
        }

        backBtn.setOnClickListener {
            finish()
        }
    }

    private fun signInWithGoogle() {
        val credentialManager = CredentialManager.create(this)
        
        // IMPORTANT: The user MUST replace this with their actual Web Client ID from the Firebase Console
        // See: Project Settings -> General -> Your apps -> Web Apps (or OAuth 2.0 Client IDs in Google Cloud Console)
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
}
