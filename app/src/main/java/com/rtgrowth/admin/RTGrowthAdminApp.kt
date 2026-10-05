package com.rtgrowth.admin

import android.app.Application
import android.content.Intent
import android.os.Build
import com.rtgrowth.admin.services.AdminLiveSyncService
import com.rtgrowth.admin.utils.NotificationHelper

class RTGrowthAdminApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // নোটিফিকেশন চ্যানেল তৈরি
        NotificationHelper.createNotificationChannels(this)

        // অ্যাপ ওপেন হওয়া মাত্রই ব্যাকগ্রাউন্ড লাইভ সিঙ্ক ইঞ্জিন চালু হবে
        val serviceIntent = Intent(this, AdminLiveSyncService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}
