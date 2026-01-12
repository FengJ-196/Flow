package com.mobile.pomodoro.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp

class FirebaseSyncManager {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun uploadStats(stats: Stats, dailyStats: List<DailyStats>) {
        val user = auth.currentUser ?: return
        val userId = user.uid

        // 1. Upload Summary Stats (Directly on User Document)
        val userRef = db.collection("users").document(userId)
        
        val summaryData = mapOf(
            "totalFocusMinutes" to stats.totalFocusMinutes,
            "lastFocusDate" to stats.lastFocusDate,
            "currentStreak" to stats.currentStreak,
            "longestStreak" to stats.longestStreak,
            "updatedAt" to Timestamp.now()
        )

        userRef.set(summaryData, SetOptions.merge())
            .addOnSuccessListener { Log.d("Sync", "User summary stats uploaded") }
            .addOnFailureListener { e -> Log.e("Sync", "Failed to upload summary", e) }

        // 2. Upload Daily Stats
        val dailyRef = db.collection("users").document(userId)
            .collection("daily_stats")

        for (day in dailyStats) {
            val dayData = mapOf(
                "date" to day.date,
                "minutes" to day.minutes
            )
            dailyRef.document(day.date).set(dayData, SetOptions.merge())
                .addOnFailureListener { e -> Log.e("Sync", "Failed to upload daily stat for ${day.date}", e) }
        }
    }

    fun downloadStats(onComplete: (Stats?, List<DailyStats>?) -> Unit) {
        val user = auth.currentUser ?: run {
            onComplete(null, null)
            return
        }
        val userId = user.uid

        // Download Summary (From User Document)
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val summary = if (document.exists() && document.contains("totalFocusMinutes")) {
                    Stats(
                        totalFocusMinutes = document.getLong("totalFocusMinutes") ?: 0L,
                        lastFocusDate = document.getString("lastFocusDate") ?: "",
                        currentStreak = document.getLong("currentStreak")?.toInt() ?: 0,
                        longestStreak = document.getLong("longestStreak")?.toInt() ?: 0
                    )
                } else null

                // Download Daily Stats
                db.collection("users").document(userId)
                    .collection("daily_stats")
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val daily = querySnapshot.documents.mapNotNull { doc ->
                            val date = doc.getString("date")
                            val minutes = doc.getLong("minutes")?.toInt()
                            if (date != null && minutes != null) {
                                DailyStats(date, minutes)
                            } else null
                        }
                        onComplete(summary, daily)
                    }
                    .addOnFailureListener {
                        onComplete(summary, null)
                    }
            }
            .addOnFailureListener {
                onComplete(null, null)
            }
    }
}
