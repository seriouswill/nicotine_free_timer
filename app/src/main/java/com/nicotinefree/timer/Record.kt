package com.nicotinefree.timer

import java.io.Serializable

data class Record(
    val timestamp: Long,
    val durationSeconds: Int,
    val userName: String = "User",
    val nicotineType: String = "Cigarettes"
) : Serializable, Comparable<Record> {
    
    override fun compareTo(other: Record): Int {
        // Compare by duration in descending order
        return other.durationSeconds.compareTo(this.durationSeconds)
    }
} 