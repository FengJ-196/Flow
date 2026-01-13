package com.mobile.flow.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.LocalDateTime
import com.google.gson.Gson
import com.mobile.flow.models.DailyStats
import com.mobile.flow.models.MinuteStats
import com.mobile.flow.models.Stats


class StatsManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("PomodoroStats", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val syncManager = FirebaseSyncManager()
    
    @RequiresApi(Build.VERSION_CODES.O)
    fun updateStats(focusMinutes: Int) {
        val currentDateTime = LocalDateTime.now()
        val currentDate = currentDateTime.toLocalDate().toString()
        val minuteOfDay = currentDateTime.hour * 60 + currentDateTime.minute
        val stats = loadStats()
        
        // Update total focus time
        val newTotalFocusMinutes = stats.totalFocusMinutes + focusMinutes
        
        // Update minute stats
        updateMinuteStats(currentDate, minuteOfDay, focusMinutes)
        
        // Update daily stats
        updateDailyStats(currentDate, focusMinutes)

        // Recalculate streak from history
        val dailyStats = loadDailyStats()
        val newCurrentStreak = calculateCurrentStreak(dailyStats)
        var newLongestStreak = stats.longestStreak
        
        if (newCurrentStreak > newLongestStreak) {
            newLongestStreak = newCurrentStreak
        }
        
        // Save updated stats
        val finalFocusMinutes = newTotalFocusMinutes
        val finalCurrentStreak = newCurrentStreak
        val finalLongestStreak = newLongestStreak
        
        sharedPreferences.edit().apply {
            putLong("totalFocusMinutes", finalFocusMinutes)
            putString("lastFocusDate", currentDate)
            putInt("currentStreak", finalCurrentStreak)
            putInt("longestStreak", finalLongestStreak)
            apply()
        }

        // Sync with Firebase
        if (AuthManager.getInstance().isSignedIn()) {
            syncManager.uploadStats(
                Stats(finalFocusMinutes, currentDate, finalCurrentStreak, finalLongestStreak),
                loadDailyStats()
            )
        }
    }

    fun syncWithCloud(cloudSummary: Stats, cloudDaily: List<DailyStats>) {
        // Overwrite local stats with cloud data directly
        val mergedDaily = cloudDaily.sortedBy { it.date }.takeLast(30)

        sharedPreferences.edit().apply {
            putLong("totalFocusMinutes", cloudSummary.totalFocusMinutes)
            putString("lastFocusDate", cloudSummary.lastFocusDate)
            putInt("currentStreak", cloudSummary.currentStreak)
            putInt("longestStreak", cloudSummary.longestStreak)
            putString("dailyStats", gson.toJson(mergedDaily))
            apply()
        }
    }
    
    private fun updateMinuteStats(date: String, minuteOfDay: Int, minutes: Int) {
        val minuteStats = loadMinuteStats().toMutableList()
        val existingStat = minuteStats.find { it.date == date && it.minuteOfDay == minuteOfDay }
        
        if (existingStat != null) {
            minuteStats.remove(existingStat)
            minuteStats.add(MinuteStats(date, minuteOfDay, existingStat.minutes + minutes))
        } else {
            minuteStats.add(MinuteStats(date, minuteOfDay, minutes))
        }
        
        // Sort by minuteOfDay
        minuteStats.sortWith(compareBy({ it.date }, { it.minuteOfDay }))
        
        // Keep only last 2 days for performance (today and yesterday)
        val today = LocalDate.now().toString()
        val yesterday = LocalDate.now().minusDays(1).toString()
        val filtered = minuteStats.filter { it.date == today || it.date == yesterday }
        
        // Save minute stats
        sharedPreferences.edit().putString("minuteStats", gson.toJson(filtered)).apply()
    }
    
    private fun updateDailyStats(date: String, minutes: Int) {
        val dailyStats = loadDailyStats().toMutableList()
        val existingStat = dailyStats.find { it.date == date }
        
        if (existingStat != null) {
            dailyStats.remove(existingStat)
            dailyStats.add(DailyStats(date, existingStat.minutes + minutes))
        } else {
            dailyStats.add(DailyStats(date, minutes))
        }
        
        // Sort by date
        dailyStats.sortBy { it.date }
        
        // Keep only last 365 days
        if (dailyStats.size > 365) {
            dailyStats.removeAt(0)
        }
        
        // Save daily stats
        sharedPreferences.edit().putString("dailyStats", gson.toJson(dailyStats)).apply()
    }
    
    fun loadStats(): Stats {
        val dailyStats = loadDailyStats()
        return Stats(
            totalFocusMinutes = sharedPreferences.getLong("totalFocusMinutes", 0L),
            lastFocusDate = sharedPreferences.getString("lastFocusDate", "") ?: "",
            currentStreak = calculateCurrentStreak(dailyStats),
            longestStreak = sharedPreferences.getInt("longestStreak", 0)
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateCurrentStreak(dailyStats: List<DailyStats>): Int {
        if (dailyStats.isEmpty()) return 0
        
        val sortedStats = dailyStats.sortedByDescending { it.date }
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        
        // Find the most recent focus date
        val lastDate = LocalDate.parse(sortedStats[0].date)
        
        if (lastDate != today && lastDate != yesterday) {
            // Streak broken - no focus today or yesterday
            return 0
        }
        
        // Count consecutive days backwards
        var streak = 0
        var expectedDate = lastDate
        
        for (stat in sortedStats) {
            val statDate = LocalDate.parse(stat.date)
            if (statDate == expectedDate) {
                streak++
                expectedDate = expectedDate.minusDays(1)
            } else if (statDate.isBefore(expectedDate)) {
                // Gap in history
                break
            }
        }
        
        return streak
    }


    
    fun getFormattedTotalFocusTime(): Pair<Int, Int> {
        val totalMinutes = loadStats().totalFocusMinutes
        val hours = (totalMinutes / 60).toInt()
        val minutes = (totalMinutes % 60).toInt()
        return Pair(hours, minutes)
    }
    
    fun loadMinuteStats(): List<MinuteStats> {
        val json = sharedPreferences.getString("minuteStats", "[]")
        val type = object : com.google.gson.reflect.TypeToken<List<MinuteStats>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    fun loadDailyStats(): List<DailyStats> {
        val json = sharedPreferences.getString("dailyStats", "[]")
        val type = object : com.google.gson.reflect.TypeToken<List<DailyStats>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    // Debug methods to verify data
    fun printStoredData() {
        println("Total Focus Minutes: ${loadStats().totalFocusMinutes}")
        println("Last Focus Date: ${loadStats().lastFocusDate}")
        println("Current Streak: ${loadStats().currentStreak}")
        println("Longest Streak: ${loadStats().longestStreak}")
        println("Minute Stats: ${loadMinuteStats()}")
        println("Daily Stats: ${loadDailyStats()}")
    }

    fun clearAllStats() {
        sharedPreferences.edit().clear().apply()
    }
} 