package com.mobile.flow

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.mobile.flow.utils.StatsManager
import java.time.LocalDate

class StatsActivity : AppCompatActivity() {
    private lateinit var todayCard: CardView
    private lateinit var weekCard: CardView
    private lateinit var todayTxt: TextView
    private lateinit var weekTxt: TextView
    private lateinit var todayBg: RelativeLayout
    private lateinit var weekBg: RelativeLayout
    private lateinit var todayChart: BarChart
    private lateinit var weekChart: BarChart
    private lateinit var statsManager: StatsManager
    private lateinit var hrsTxt: TextView
    private lateinit var minsTxt: TextView
    private lateinit var backBtn: ImageView
    private lateinit var longestStreakTxt: TextView
    private lateinit var currentStreakTxt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_stats)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        statsManager = StatsManager(this)

        todayCard = findViewById(R.id.today_card)
        weekCard = findViewById(R.id.month_card)
        todayTxt = findViewById(R.id.today_txt)
        weekTxt = findViewById(R.id.month_txt)
        todayBg = findViewById(R.id.today_bg)
        weekBg = findViewById(R.id.month_bg)
        todayChart = findViewById(R.id.today_chart)
        weekChart = findViewById(R.id.month_chart)
        hrsTxt = findViewById(R.id.hrs_txt)
        minsTxt = findViewById(R.id.mins_txt)
        backBtn = findViewById(R.id.back_btn)
        longestStreakTxt = findViewById(R.id.longest_streak_txt)
        currentStreakTxt = findViewById(R.id.current_streak_txt)

        // Update stats display
        updateStatsDisplay()
        setupCharts()

        todayCard.setOnClickListener {
            vibrate()
            todayTxt.setTextColor(getResources().getColor(R.color.alt_light_red, null))
            todayBg.setBackgroundColor(getResources().getColor(R.color.deep_red, null))

            weekTxt.setTextColor(getResources().getColor(R.color.deep_red, null))
            weekBg.setBackgroundColor(getResources().getColor(R.color.alt_light_red, null))

            todayChart.visibility = View.VISIBLE
            weekChart.visibility = View.GONE
        }

        weekCard.setOnClickListener {
            vibrate()
            todayTxt.setTextColor(getResources().getColor(R.color.deep_red, null))
            todayBg.setBackgroundColor(getResources().getColor(R.color.alt_light_red, null))

            weekTxt.setTextColor(getResources().getColor(R.color.alt_light_red, null))
            weekBg.setBackgroundColor(getResources().getColor(R.color.deep_red, null))

            todayChart.visibility = View.GONE
            weekChart.visibility = View.VISIBLE
        }

        backBtn.setOnClickListener {
            vibrate()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh stats and charts when activity resumes
        updateStatsDisplay()
        refreshCharts()
    }

    private fun setupCharts() {
        setupTodayChart()
        setupWeekChart()
    }

    private fun setupTodayChart() {
        val minuteStats = statsManager.loadMinuteStats()
        val today = LocalDate.now().toString()
        val todayStats = minuteStats.filter { it.date == today }
        // Aggregate by hour
        val hourMap = mutableMapOf<Int, Int>()
        todayStats.forEach { stat ->
            val hour = stat.minuteOfDay / 60
            hourMap[hour] = (hourMap[hour] ?: 0) + stat.minutes
        }
        val nonZeroHours = hourMap.keys.sorted()
        val entries = nonZeroHours.mapIndexed { index, hour ->
            BarEntry(index.toFloat(), hourMap[hour]?.toFloat() ?: 0f)
        }
        val labels = nonZeroHours.map { hour ->
            val ampm = if (hour < 12) "am" else "pm"
            val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            String.format("%d:00 %s", hour12, ampm)
        }
        val dataSet = BarDataSet(entries, "Focus Time (minutes)")
        dataSet.color = getResources().getColor(R.color.deep_red, null)
        val data = BarData(dataSet)
        data.barWidth = 0.3f // reduced bar width for more spacing
        todayChart.apply {
            this.data = data
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawBorders(false)
            setBackgroundColor(Color.TRANSPARENT)
            axisLeft.apply {
                setDrawGridLines(false)
                axisMinimum = 0f
                textColor = getResources().getColor(R.color.deep_red, null)
            }
            axisRight.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = getResources().getColor(R.color.deep_red, null)
                valueFormatter = IndexAxisValueFormatter(labels)
                labelCount = labels.size
                setLabelRotationAngle(0f) // Make labels straight
            }
            setVisibleXRangeMaximum(5f) // Show 5 bars at a time
            isDragEnabled = true
            setScaleEnabled(false)
            animateY(1000)
            invalidate()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupWeekChart() {
        val dailyStats = statsManager.loadDailyStats()
        // Get last 7 days
        val today = LocalDate.now()
        val days = (0..6).map { today.minusDays((6 - it).toLong()) }
        val dayToMinutes = dailyStats.associateBy({ LocalDate.parse(it.date) }, { it.minutes })
        val entries = days.mapIndexed { index, date ->
            BarEntry(index.toFloat(), dayToMinutes[date]?.toFloat() ?: 0f)
        }
        val labels = days.map { date ->
            date.dayOfWeek.name.substring(0, 1) + date.dayOfWeek.name.substring(1,3).lowercase() // e.g. Mon, Tue
        }
        val dataSet = BarDataSet(entries, "Focus Time (minutes)")
        dataSet.color = getResources().getColor(R.color.deep_red, null)
        val data = BarData(dataSet)
        data.barWidth = 0.4f // narrower bars for more spacing
        weekChart.apply {
            this.data = data
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawBorders(false)
            setBackgroundColor(Color.TRANSPARENT)
            axisLeft.apply {
                setDrawGridLines(false)
                axisMinimum = 0f
                textColor = getResources().getColor(R.color.deep_red, null)
            }
            axisRight.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = getResources().getColor(R.color.deep_red, null)
                valueFormatter = IndexAxisValueFormatter(labels)
                labelCount = labels.size
                setLabelRotationAngle(0f)
            }
            setVisibleXRangeMaximum(5f) // Show 5 bars at a time
            isDragEnabled = true
            setScaleEnabled(false)
            animateY(1000)
            invalidate()
            // --- Focus on today ---
            val todayIndex = days.indexOf(today)
            if (todayIndex != -1) {
                moveViewToX(todayIndex.toFloat())
            }
        }
    }

    private fun updateStatsDisplay() {
        // Update total focus time
        val (hours, minutes) = statsManager.getFormattedTotalFocusTime()
        hrsTxt.text = hours.toString()
        minsTxt.text = minutes.toString()

        // Update streaks
        val stats = statsManager.loadStats()
        longestStreakTxt.text = stats.longestStreak.toString()
        currentStreakTxt.text = stats.currentStreak.toString()
    }

    @SuppressLint("NewApi")
    private fun refreshCharts() {
        setupTodayChart()
        setupWeekChart()
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(vibrationEffect)
            } else {
                vibrator.vibrate(50)
            }
        }
    }
}