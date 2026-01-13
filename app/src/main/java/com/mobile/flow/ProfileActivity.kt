package com.mobile.flow

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mobile.flow.utils.StatsManager
import com.mobile.flow.utils.AuthManager
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity(), android.content.SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var statsManager: StatsManager
    private lateinit var userNameTxt: TextView
    private lateinit var userEmailTxt: TextView
    private lateinit var totalFocusTimeTxt: TextView
    private lateinit var currentStreakTxt: TextView
    private lateinit var longestStreakTxt: TextView
    private lateinit var statsCard: CardView
    private lateinit var loginCard: CardView
    private lateinit var logoutCard: CardView
    private lateinit var clearDataCard: CardView
    private lateinit var statsPrefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTheme()
        enableEdgeToEdge()
        setContentView(R.layout.profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize StatsManager and SharedPreferences
        statsManager = StatsManager(this)
        statsPrefs = getSharedPreferences("PomodoroStats", MODE_PRIVATE)

        // Bind UI elements
        userNameTxt = findViewById(R.id.user_name_txt)
        userEmailTxt = findViewById(R.id.user_email_txt)
        totalFocusTimeTxt = findViewById(R.id.total_focus_time_txt)
        currentStreakTxt = findViewById(R.id.current_streak_txt)
        longestStreakTxt = findViewById(R.id.longest_streak_txt)
        statsCard = findViewById(R.id.stats_card)
        loginCard = findViewById(R.id.login_card)
        logoutCard = findViewById(R.id.logout_card)
        clearDataCard = findViewById(R.id.clear_data_card)

        // Load user profile data
        loadProfileData()

        // Load stats data
        loadStatsData()

        // Setup click listeners
        statsCard.setOnClickListener {
            vibrate()
            val intent = Intent(this, StatsActivity::class.java)
            startActivity(intent)
        }

        loginCard.setOnClickListener {
            vibrate()
            if (!AuthManager.getInstance().isSignedIn()) {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }

        logoutCard.setOnClickListener {
            vibrate()
            AuthManager.getInstance().signOut()
            
            // Clear all local data (stats and profile)
            statsManager.clearAllStats()

            // Refresh UI immediately
            loadProfileData()
            loadStatsData()

            android.widget.Toast.makeText(this, "Signed out and data cleared", android.widget.Toast.LENGTH_SHORT).show()
        }
        
        clearDataCard.setOnClickListener {
            vibrate()
            showClearDataConfirmation()
        }
    }

    private fun showClearDataConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Clear All Local Data")
            .setMessage("Are you sure you want to clear all your local statistics? This action cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                statsManager.clearAllStats()
                loadStatsData()
                android.widget.Toast.makeText(this, "Local data cleared", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadProfileData() {
        val userName = statsPrefs.getString("userName", null)
        val userEmail = statsPrefs.getString("userEmail", null)
        val isSignedIn = AuthManager.getInstance().isSignedIn()

        if (isSignedIn || userName != null) {
            // Load from Preferences (or Firebase as fallback)
            val displayName = userName ?: AuthManager.getInstance().currentUser?.displayName ?: "User"
            val email = userEmail ?: AuthManager.getInstance().currentUser?.email ?: ""
            
            userNameTxt.text = displayName
            userEmailTxt.text = email
            loginCard.visibility = android.view.View.GONE
            logoutCard.visibility = android.view.View.VISIBLE
        } else {
            // Guest mode
            userNameTxt.text = "Guest"
            userEmailTxt.text = "Sign in to sync your data"
            findViewById<TextView>(R.id.login_status_txt).text = "Login / Sign Up"
            loginCard.visibility = android.view.View.VISIBLE
            logoutCard.visibility = android.view.View.GONE
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: android.content.SharedPreferences?, key: String?) {
        if (key == "userName" || key == "userEmail" || key == "totalFocusMinutes" || key == "currentStreak" || key == "longestStreak" || key == "dailyStats") {
            loadProfileData()
            loadStatsData()
        }
    }

    private fun loadStatsData() {
        // Load stats from StatsManager
        val stats = statsManager.loadStats()

        // Display total focus time
        val (hours, minutes) = statsManager.getFormattedTotalFocusTime()
        totalFocusTimeTxt.text = "${hours}h ${minutes}m"

        // Display current streak
        currentStreakTxt.text = "${stats.currentStreak}"

        // Display longest streak
        longestStreakTxt.text = "${stats.longestStreak}"
    }

    private fun applyTheme() {
        // Theme will be applied in onStart after layout is inflated
    }

    override fun onStart() {
        super.onStart()

        val sharedPreferences = getSharedPreferences("PomodoroSettings", MODE_PRIVATE)
        val darkMode = sharedPreferences.getBoolean("darkMode", false)
        val amoledMode = sharedPreferences.getBoolean("amoledMode", false)

        val mainLayout = findViewById<RelativeLayout>(R.id.container)

        if (amoledMode) {
            mainLayout.setBackgroundColor(Color.BLACK)
            findViewById<TextView>(R.id.settings_txt).setTextColor(Color.WHITE)
            findViewById<TextView>(R.id.stats_txt).setTextColor(Color.WHITE)
        } else if (darkMode) {
            mainLayout.setBackgroundColor(Color.parseColor("#1C1C1E"))
            findViewById<TextView>(R.id.settings_txt).setTextColor(Color.WHITE)
            findViewById<TextView>(R.id.stats_txt).setTextColor(Color.WHITE)
        } else {
            mainLayout.setBackgroundColor(Color.parseColor("#EDEDED"))
            findViewById<TextView>(R.id.settings_txt).setTextColor(Color.BLACK)
            findViewById<TextView>(R.id.stats_txt).setTextColor(Color.BLACK)
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

    override fun onResume() {
        super.onResume()
        // Refresh stats and profile
        loadStatsData()
        loadProfileData()
        // Register listener for reactive updates
        statsPrefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        // Unregister listener to prevent memory leaks
        statsPrefs.unregisterOnSharedPreferenceChangeListener(this)
    }
}

