package com.nicotinefree.timer

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.ObjectInputStream
import java.util.concurrent.TimeUnit

class TimerService : Service() {
    private val binder = TimerBinder()
    private var isTimerRunning = false
    private var startTimeMillis = 0L
    private var elapsedSeconds = 0
    private var personalBest = 0
    private var recordNotificationSent = false
    
    companion object {
        const val NOTIFICATION_ID = 1
        const val RECORD_NOTIFICATION_ID = 2
        const val CHANNEL_ID = "TimerChannel"
        const val RECORD_CHANNEL_ID = "RecordChannel"
        const val ACTION_START = "com.nicotinefree.timer.START"
        const val ACTION_STOP = "com.nicotinefree.timer.STOP"
    }
    
    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        loadPersonalBest()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTimer()
            ACTION_STOP -> stopTimer()
        }
        return START_STICKY
    }
    
    fun startTimer() {
        if (!isTimerRunning) {
            isTimerRunning = true
            startTimeMillis = System.currentTimeMillis()
            recordNotificationSent = false
            
            // Start foreground service with notification
            startForeground(NOTIFICATION_ID, buildNotification(0))
            
            // Start timer thread
            Thread {
                while (isTimerRunning) {
                    elapsedSeconds = ((System.currentTimeMillis() - startTimeMillis) / 1000).toInt()
                    updateNotification(elapsedSeconds)
                    
                    // Check if user has surpassed their personal best
                    if (personalBest > 0 && elapsedSeconds > personalBest && !recordNotificationSent) {
                        sendRecordNotification()
                        recordNotificationSent = true
                    }
                    
                    try {
                        Thread.sleep(1000)
                    } catch (e: InterruptedException) {
                        break
                    }
                }
            }.start()
        }
    }
    
    fun stopTimer(): Int {
        isTimerRunning = false
        val seconds = elapsedSeconds
        
        // Use the newer STOP_FOREGROUND_REMOVE constant instead of boolean
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        
        stopSelf()
        return seconds
    }
    
    fun getElapsedSeconds(): Int {
        return if (isTimerRunning) {
            ((System.currentTimeMillis() - startTimeMillis) / 1000).toInt()
        } else {
            elapsedSeconds
        }
    }
    
    fun isRunning(): Boolean = isTimerRunning
    
    private fun loadPersonalBest() {
        try {
            val file = File(filesDir, "timer_records.dat")
            if (file.exists()) {
                ObjectInputStream(file.inputStream()).use { input ->
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val records = input.readObject() as MutableList<Record>
                        personalBest = records.maxByOrNull { it.durationSeconds }?.durationSeconds ?: 0
                    } catch (e: ClassNotFoundException) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Update the personal best from MainActivity
    fun updatePersonalBest(newBest: Int) {
        personalBest = newBest
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Timer channel
            val timerChannel = NotificationChannel(
                CHANNEL_ID,
                "Timer Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background timer service"
            }
            
            // Record notification channel
            val recordChannel = NotificationChannel(
                RECORD_CHANNEL_ID,
                "Record Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for when you beat your record"
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(timerChannel)
            notificationManager.createNotificationChannel(recordChannel)
        }
    }
    
    private fun buildNotification(seconds: Int): Notification {
        // Create an intent to open the main activity when notification is tapped
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Nicotine-Free Timer")
            .setContentText("Time: ${formatTime(seconds)}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(seconds: Int) {
        val notification = buildNotification(seconds)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun sendRecordNotification() {
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 1, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }
        
        val notification = NotificationCompat.Builder(this, RECORD_CHANNEL_ID)
            .setContentTitle("New Record!")
            .setContentText("Congratulations! You've beaten your previous nicotine-free record!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .build()
            
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(RECORD_NOTIFICATION_ID, notification)
    }
    
    private fun formatTime(totalSeconds: Int): String {
        val hours = TimeUnit.SECONDS.toHours(totalSeconds.toLong())
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds.toLong()) % 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
} 