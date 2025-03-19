package com.nicotinefree.timer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.TimeUnit

class HighScoreAdapter(
    private var records: List<Record>,
    private val freeFromText: String
) : RecyclerView.Adapter<HighScoreAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rankText: TextView = view.findViewById(R.id.rank_text)
        val timeText: TextView = view.findViewById(R.id.time_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.high_score_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        holder.rankText.text = "#${position + 1}"
        
        // Format as: "00:00:00 - Username (free from Cigarettes)"
        holder.timeText.text = "${formatTime(record.durationSeconds)} - ${record.userName} ($freeFromText ${record.nicotineType})"
    }

    override fun getItemCount() = records.size

    fun updateRecords(newRecords: List<Record>) {
        records = newRecords
        notifyDataSetChanged()
    }

    private fun formatTime(totalSeconds: Int): String {
        val hours = TimeUnit.SECONDS.toHours(totalSeconds.toLong())
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds.toLong()) % 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
} 