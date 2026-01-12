package com.mobile.flow.utils

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController

object UltraFocusManager {
    private var originalOrientation: Int = 0
    private var originalDndMode: Int = NotificationManager.INTERRUPTION_FILTER_ALL
    private var originalSystemUiVisibility: Int = 0

    fun enableUltraFocusMode(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Save current DND mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    val currentFilter = notificationManager.currentInterruptionFilter
                    // Only save if it's a valid filter value
                    if (isValidInterruptionFilter(currentFilter)) {
                        originalDndMode = currentFilter
                    }
                    // Set DND mode to priority only
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                } else {
                    throw SecurityException("Notification policy access not granted")
                }
            }

            // Disable notifications
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = notificationManager.getNotificationChannel("timer_notifications")
                channel?.let {
                    it.enableLights(false)
                    it.enableVibration(false)
                    it.setSound(null, null)
                }
            }

            // Hide notification bar and set fullscreen
            if (context is Activity) {
                originalSystemUiVisibility = context.window.decorView.systemUiVisibility
                context.window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
                // Hide status bar for Android 11+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    context.window.insetsController?.let { controller ->
                        controller.hide(WindowInsets.Type.statusBars())
                        controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                } else {
                    // For older versions, use legacy flags
                    context.window.decorView.systemUiVisibility =
                        View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Error enabling ultra focus mode: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun disableUltraFocusMode(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Turn off DND mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    // Always set to ALL to ensure DND is turned off
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
            }

            // Re-enable notifications
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = notificationManager.getNotificationChannel("timer_notifications")
                channel?.let {
                    it.enableLights(true)
                    it.enableVibration(true)
                    it.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build())
                }
            }

            // Show notification bar and exit fullscreen
            if (context is Activity) {
                context.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    context.window.insetsController?.show(WindowInsets.Type.statusBars())
                } else {
                    context.window.decorView.systemUiVisibility = originalSystemUiVisibility
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Error disabling ultra focus mode: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun setOrientation(activity: Activity, isUltraFocusMode: Boolean) {
        if (isUltraFocusMode) {
            originalOrientation = activity.requestedOrientation
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            // Lock to portrait when not in ultra focus mode
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun isValidInterruptionFilter(filter: Int): Boolean {
        return filter in arrayOf(
            NotificationManager.INTERRUPTION_FILTER_ALL,
            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            NotificationManager.INTERRUPTION_FILTER_NONE,
            NotificationManager.INTERRUPTION_FILTER_ALARMS,
            NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        )
    }
} 