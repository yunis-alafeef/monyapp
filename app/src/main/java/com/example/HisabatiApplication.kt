package com.example

import android.app.Application
import com.example.notifications.NotificationHelper

class HisabatiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // App Check is strictly initialized inside MainActivity.onCreate(savedInstanceState) with launch intent.
        NotificationHelper.createNotificationChannel(this)
    }
}
