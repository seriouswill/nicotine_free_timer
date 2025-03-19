package com.nicotinefree.timer

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var timerButton: Button
    private lateinit var timerText: TextView
    private lateinit var recordText: TextView
    private lateinit var progressText: TextView
    private lateinit var usageText: TextView
    private lateinit var highScoreRecyclerView: RecyclerView
    private lateinit var highScoreAdapter: HighScoreAdapter
    
    private val handler = Handler(Looper.getMainLooper())
    private var timerService: TimerService? = null
    private var bound = false
    private var records = mutableListOf<Record>()
    private lateinit var dailyUsage: DailyUsage

    companion object {
        private const val RECORDS_FILENAME = "timer_records.dat"
        private const val MAX_RECORDS = 10
        private const val PREFS_NAME = "TimerAppPrefs"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_NICOTINE_TYPE = "nicotine_type"
    }
    
    // Service connection
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            bound = true
            
            // Update UI if service is already running
            if (timerService?.isRunning() == true) {
                timerButton.text = "Stop"
                startUpdatingUI()
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
            timerService = null
        }
    }

    // For checking date change while app is running
    private val midnightChecker = object : Runnable {
        override fun run() {
            // Check if the date changed and update the UI if needed
            dailyUsage.getCurrentCount() // This triggers checkAndResetIfNeeded
            updateUsageText()
            
            // Schedule next check in 1 minute
            handler.postDelayed(this, 60 * 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timerButton = findViewById(R.id.timer_button)
        timerText = findViewById(R.id.timer_text)
        recordText = findViewById(R.id.record_text)
        progressText = findViewById(R.id.progress_text)
        usageText = findViewById(R.id.usage_text)
        highScoreRecyclerView = findViewById(R.id.high_scores_recycler_view)
        
        // Initialize daily usage
        dailyUsage = DailyUsage(this)
        updateUsageText()
        
        // Start the midnight checker to periodically check for date changes
        handler.postDelayed(midnightChecker, 60 * 1000)
        
        // Reset button functionality
        val resetButton = findViewById<Button>(R.id.reset_button)
        resetButton.setOnClickListener {
            // Clear preferences
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
            
            // Reset daily usage counter
            dailyUsage.reset()
            
            // Delete records file
            val file = File(filesDir, RECORDS_FILENAME)
            if (file.exists()) {
                file.delete()
            }
            
            // Stop service if running
            if (timerService?.isRunning() == true) {
                stopTimerService()
            }
            
            // Go back to welcome screen
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Get username
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userName = sharedPref.getString(KEY_USER_NAME, "User") ?: "User"
        val nicotineType = sharedPref.getString(KEY_NICOTINE_TYPE, "Cigarettes") ?: "Cigarettes"
        
        // Update title or user display if needed
        val nameText = findViewById<TextView>(R.id.name_text)
        if (nameText != null) {
            nameText.text = "Hello, $userName! Free from $nicotineType"
        }

        loadRecords()
        updateRecordText()

        highScoreAdapter = HighScoreAdapter(records, getString(R.string.free_from))
        highScoreRecyclerView.layoutManager = LinearLayoutManager(this)
        highScoreRecyclerView.adapter = highScoreAdapter

        timerButton.setOnClickListener {
            if (timerService?.isRunning() == true) {
                stopTimerService()
            } else {
                startTimerService()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Force a check for date change and refresh counter display
        dailyUsage.getCurrentCount() // This will trigger checkAndResetIfNeeded()
        
        // Update the usage text to reflect any changes
        updateUsageText()
    }
    
    override fun onStart() {
        super.onStart()
        // Bind to the timer service
        Intent(this, TimerService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        
        // Ensure the service is created even if it's not running yet
        startService(Intent(this, TimerService::class.java))
    }
    
    override fun onStop() {
        super.onStop()
        // Unbind from the service
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Remove all callbacks to prevent memory leaks
        handler.removeCallbacksAndMessages(null)
    }
    
    private fun updateUsageText() {
        usageText.text = dailyUsage.getFormattedUsageMessage()
        
        // Change color based on daily target
        if (dailyUsage.isOverTarget()) {
            // Red color if over target
            usageText.setTextColor(resources.getColor(android.R.color.holo_red_dark, theme))
        } else {
            // Green color if under target
            usageText.setTextColor(resources.getColor(android.R.color.holo_green_dark, theme))
        }
    }
    
    private fun startTimerService() {
        // For Android 12+, check for notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != 
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
                return
            }
        }

        // Use explicit foreground start for Android 10+
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_START
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        timerButton.text = "Stop"
        
        // Set initial button color
        if (records.isNotEmpty()) {
            timerButton.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark, theme))
        }
        
        startUpdatingUI()
    }
    
    private fun stopTimerService() {
        if (bound && timerService != null) {
            val elapsedSeconds = timerService!!.stopTimer()
            
            // Get username and nicotine type for the record
            val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val userName = sharedPref.getString(KEY_USER_NAME, "User") ?: "User"
            val nicotineType = sharedPref.getString(KEY_NICOTINE_TYPE, "Cigarettes") ?: "Cigarettes"
            
            // Save record
            val record = Record(System.currentTimeMillis(), elapsedSeconds, userName, nicotineType)
            addRecord(record)
            
            // Increment daily usage counter
            dailyUsage.incrementCount()
            updateUsageText()
        }
        
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        stopService(intent)
        
        timerButton.text = "Start"
        timerButton.setBackgroundColor(resources.getColor(android.R.color.darker_gray, theme))
        
        // Reset timer display
        handler.removeCallbacksAndMessages(null)
        timerText.text = "00:00:00"
        progressText.text = ""
    }
    
    private fun startUpdatingUI() {
        handler.post(object : Runnable {
            override fun run() {
                if (bound && timerService?.isRunning() == true) {
                    updateUI()
                    handler.postDelayed(this, 1000)
                }
            }
        })
    }
    
    private fun updateUI() {
        val elapsedSeconds = timerService?.getElapsedSeconds() ?: 0
        timerText.text = formatTime(elapsedSeconds)
        updateProgressText(elapsedSeconds)
        updateButtonColor(elapsedSeconds)
    }
    
    private fun updateButtonColor(elapsedSeconds: Int) {
        if (records.isNotEmpty()) {
            val personalBest = records.maxByOrNull { it.durationSeconds }?.durationSeconds ?: 0
            
            when {
                elapsedSeconds < personalBest * 0.5 -> {
                    timerButton.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark, theme))
                }
                elapsedSeconds < personalBest -> {
                    timerButton.setBackgroundColor(resources.getColor(android.R.color.holo_orange_dark, theme))
                }
                elapsedSeconds >= personalBest -> {
                    timerButton.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark, theme))
                }
            }
        }
    }
    
    private fun updateProgressText(elapsedSeconds: Int) {
        if (records.isNotEmpty()) {
            val personalBest = records.maxByOrNull { it.durationSeconds }?.durationSeconds ?: 0
            val progress = if (elapsedSeconds < personalBest) {
                "You're ${formatTime(personalBest - elapsedSeconds)} behind your best nicotine-free record."
            } else {
                "You're ${formatTime(elapsedSeconds - personalBest)} ahead of your previous record!"
            }
            progressText.text = progress
        }
    }

    private fun updateRecordText() {
        if (records.isNotEmpty()) {
            val personalBest = records.maxByOrNull { it.durationSeconds }?.durationSeconds ?: 0
            recordText.text = "Your best nicotine-free time: ${formatTime(personalBest)}"
            
            // Update service if bound
            if (bound && timerService != null) {
                timerService?.updatePersonalBest(personalBest)
            }
        } else {
            recordText.text = "No nicotine-free records yet"
        }
    }

    private fun addRecord(newRecord: Record) {
        records.add(newRecord)
        records.sort()
        
        // Keep only the top records
        if (records.size > MAX_RECORDS) {
            records = records.subList(0, MAX_RECORDS)
        }
        
        saveRecords()
        updateRecordText()
        highScoreAdapter.updateRecords(records)
    }

    private fun formatTime(totalSeconds: Int): String {
        val hours = TimeUnit.SECONDS.toHours(totalSeconds.toLong())
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds.toLong()) % 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun saveRecords() {
        try {
            val file = File(filesDir, RECORDS_FILENAME)
            ObjectOutputStream(file.outputStream()).use { 
                it.writeObject(records) 
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadRecords() {
        try {
            val file = File(filesDir, RECORDS_FILENAME)
            if (file.exists()) {
                ObjectInputStream(file.inputStream()).use {
                    records = (it.readObject() as MutableList<Record>).toMutableList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            records = mutableListOf()
        }
    }
} 