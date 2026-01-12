package com.mobile.pomodoro.utils

data class Stats(
    val totalFocusMinutes: Long = 0L,
    val lastFocusDate: String = "",
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)

data class MinuteStats(
    val date: String, // e.g. "2024-06-09"
    val minuteOfDay: Int, // 0-1439
    val minutes: Int
)

data class DailyStats(
    val date: String,
    val minutes: Int
)
