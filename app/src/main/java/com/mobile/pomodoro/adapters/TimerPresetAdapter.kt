package com.mobile.pomodoro.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mobile.pomodoro.R
import com.mobile.pomodoro.models.TimerPreset

class TimerPresetAdapter(
    private var presets: List<TimerPreset>,
    private val onPresetSelected: (TimerPreset) -> Unit
) : RecyclerView.Adapter<TimerPresetAdapter.PresetViewHolder>() {

    private var onLongClickListener: ((TimerPreset) -> Unit)? = null

    fun setOnLongClickListener(listener: (TimerPreset) -> Unit) {
        onLongClickListener = listener
    }

    fun updatePresets(newPresets: List<TimerPreset>) {
        presets = newPresets
        notifyDataSetChanged()
    }

    inner class PresetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val presetNameText: TextView = itemView.findViewById(R.id.preset_name)
        val focusTimeText: TextView = itemView.findViewById(R.id.preset_focus_time)
        val shortBreakTimeText: TextView = itemView.findViewById(R.id.preset_short_break_time)
        val longBreakTimeText: TextView = itemView.findViewById(R.id.preset_long_break_time)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPresetSelected(presets[position])
                }
            }

            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onLongClickListener?.invoke(presets[position])
                }
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timer_preset, parent, false)
        return PresetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PresetViewHolder, position: Int) {
        val preset = presets[position]
        holder.presetNameText.text = preset.name
        holder.focusTimeText.text = "Focus: ${preset.focusMinutes} min"
        holder.shortBreakTimeText.text = "Short Break: ${preset.shortBreakMinutes} min"
        holder.longBreakTimeText.text = "Long Break: ${preset.longBreakMinutes} min"
    }

    override fun getItemCount() = presets.size
} 