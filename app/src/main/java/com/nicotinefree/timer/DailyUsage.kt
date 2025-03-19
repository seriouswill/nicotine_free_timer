package com.nicotinefree.timer

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Class to manage daily usage tracking for nicotine products
 */
class DailyUsage(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "DailyUsagePrefs"
        private const val KEY_CURRENT_COUNT = "current_count"
        private const val KEY_LAST_RESET_DATE = "last_reset_date"
        
        // Format for storing the date
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Increment the usage count by 1
     */
    fun incrementCount() {
        checkAndResetIfNeeded()
        val currentCount = getCurrentCount()
        prefs.edit().putInt(KEY_CURRENT_COUNT, currentCount + 1).apply()
    }
    
    /**
     * Reset the usage count to 0
     */
    fun reset() {
        prefs.edit()
            .putInt(KEY_CURRENT_COUNT, 0)
            .putString(KEY_LAST_RESET_DATE, getCurrentDateString())
            .apply()
    }
    
    /**
     * Get the current usage count for today
     */
    fun getCurrentCount(): Int {
        checkAndResetIfNeeded()
        return prefs.getInt(KEY_CURRENT_COUNT, 0)
    }
    
    /**
     * Get the daily target from preferences
     */
    fun getDailyTarget(): Int {
        val mainPrefs = context.getSharedPreferences(WelcomeActivity.PREFS_NAME, Context.MODE_PRIVATE)
        return mainPrefs.getInt(WelcomeActivity.KEY_DAILY_AMOUNT, 0)
    }
    
    /**
     * Get the nicotine type from preferences
     */
    fun getNicotineType(): String {
        val mainPrefs = context.getSharedPreferences(WelcomeActivity.PREFS_NAME, Context.MODE_PRIVATE)
        return mainPrefs.getString(WelcomeActivity.KEY_NICOTINE_TYPE, "Cigarettes") ?: "Cigarettes"
    }
    
    /**
     * Determine if current count is over the daily target
     */
    fun isOverTarget(): Boolean {
        val currentCount = getCurrentCount()
        val target = getDailyTarget()
        return target > 0 && currentCount >= target
    }
    
    /**
     * Format the usage message based on nicotine type and current count
     */
    fun getFormattedUsageMessage(): String {
        val count = getCurrentCount()
        val nicotineType = getNicotineType()
        val target = getDailyTarget()
        
        val basicMessage = when (nicotineType) {
            WelcomeActivity.TYPE_CIGARETTES -> "$count cigarettes today"
            WelcomeActivity.TYPE_VAPING -> "$count vaping sessions today"
            WelcomeActivity.TYPE_POUCHES -> "$count pouches today"
            WelcomeActivity.TYPE_GUM -> "$count pieces today"
            else -> "$count uses today"
        }
        
        return if (target > 0) {
            "$basicMessage (Target: $target)"
        } else {
            basicMessage
        }
    }
    
    /**
     * Check if we need to reset the counter (if it's a new day)
     */
    private fun checkAndResetIfNeeded() {
        val lastResetDate = getLastResetDate()
        val today = getCurrentDateString()
        
        if (lastResetDate != today) {
            // It's a new day, reset the counter
            prefs.edit()
                .putInt(KEY_CURRENT_COUNT, 0)
                .putString(KEY_LAST_RESET_DATE, today)
                .apply()
        }
    }
    
    /**
     * Get the date of the last counter reset
     */
    private fun getLastResetDate(): String {
        return prefs.getString(KEY_LAST_RESET_DATE, "") ?: ""
    }
    
    /**
     * Get the current date as a string
     */
    private fun getCurrentDateString(): String {
        return DATE_FORMAT.format(Date())
    }
} 