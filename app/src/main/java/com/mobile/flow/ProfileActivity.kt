package com.mobile.flow

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mobile.flow.utils.StatsManager

class ProfileActivity : AppCompatActivity() {
    private lateinit var statsManager: StatsManager
    private lateinit var userNameTxt: TextView
    private lateinit var userEmailTxt: TextView
    private lateinit var totalFocusTimeTxt: TextView
    private lateinit var currentStreakTxt: TextView
    private lateinit var sessionsCompletedTxt: TextView
    private lateinit var statsCard: CardView

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

        // Initialize StatsManager
        statsManager = StatsManager(this)

        // Bind UI elements
        userNameTxt = findViewById(R.id.user_name_txt)
        userEmailTxt = findViewById(R.id.user_email_txt)
        totalFocusTimeTxt = findViewById(R.id.total_focus_time_txt)
        currentStreakTxt = findViewById(R.id.current_streak_txt)
        sessionsCompletedTxt = findViewById(R.id.sessions_completed_txt)
        statsCard = findViewById(R.id.stats_card)

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
    }

    private fun loadProfileData() {
        val sharedPreferences = getSharedPreferences("PomodoroSettings", MODE_PRIVATE)

        // Load user name (default to "User")
        val userName = sharedPreferences.getString("user_name", "User") ?: "User"
        userNameTxt.text = userName

        // Load user email (default to empty)
        val userEmail = sharedPreferences.getString("user_email", "user@example.com") ?: "user@example.com"
        userEmailTxt.text = userEmail
    }

    private fun loadStatsData() {
        // Load stats from StatsManager
        val stats = statsManager.loadStats()

        // Display total focus time
        val (hours, minutes) = statsManager.getFormattedTotalFocusTime()
        totalFocusTimeTxt.text = "${hours}h ${minutes}m"

        // Display current streak
        currentStreakTxt.text = "${stats.currentStreak}"

        // Calculate total sessions (assuming 25 minutes per session)
        val totalSessions = stats.totalFocusMinutes / 25
        sessionsCompletedTxt.text = "$totalSessions"
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
        // Refresh stats when returning to this screen
        loadStatsData()
    }
}

