package com.mobile.flow.models

data class TimerPreset(
    val name: String,
    val focusMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int
) 