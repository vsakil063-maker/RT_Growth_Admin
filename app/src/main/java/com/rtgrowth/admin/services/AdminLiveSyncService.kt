package com.rtgrowth.admin.services

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.firebase.database.*
import com.rtgrowth.admin.R
import com.rtgrowth.admin.utils.NotificationHelper

class AdminLiveSyncService : Service() {

    private lateinit var db: DatabaseReference
    private var isInitialLoaded = false
    private val knownEvents = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        db = FirebaseDatabase.getInstance().reference
        NotificationHelper.createNotificationChannels(this)

        val serviceNotif = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_SERVICE)
            .setContentTitle("RT Growth Engine Live")
            .setContentText("24/7 background sync connected to database...")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1001, serviceNotif)
        startFirebaseListeners()
    }

    private fun startFirebaseListeners() {
        // ১. ট্রানজ্যাকশন ও টাস্ক লিসেনার
        db.child("users").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                processUserSnapshot(snapshot)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                processUserSnapshot(snapshot)
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })

        // ২. লাইভ সাপোর্ট চ্যাট লিসেনার
        db.child("chats").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                processChatSnapshot(snapshot)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                processChatSnapshot(snapshot)
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })

        Handler(Looper.getMainLooper()).postDelayed({ isInitialLoaded = true }, 4000)
    }

    private fun processUserSnapshot(snapshot: DataSnapshot) {
        val phone = snapshot.key ?: return

        // ডিপোজিট
        snapshot.child("deposits").children.forEach { depSnap ->
            val id = depSnap.key ?: return@forEach
            val status = depSnap.child("status").value?.toString() ?: "Pending"
            val amt = depSnap.child("amount").value?.toString() ?: "0"
            val key = "${phone}_dep_$id"

            if (status == "Pending" && !knownEvents.contains(key)) {
                if (isInitialLoaded) {
                    NotificationHelper.showNotification(
                        this,
                        "💰 নতুন ডিপোজিট রিকোয়েস্ট!",
                        "$phone নম্বর থেকে ৳$amt অ্যাড করার অনুরোধ এসেছে।"
                    )
                }
                knownEvents.add(key)
            }
        }

        // উইথড্রয়াল
        snapshot.child("withdrawals").children.forEach { wdSnap ->
            val id = wdSnap.key ?: return@forEach
            val status = wdSnap.child("status").value?.toString() ?: "Pending"
            val amt = wdSnap.child("amount").value?.toString() ?: "0"
            val key = "${phone}_wd_$id"

            if (status == "Pending" && !knownEvents.contains(key)) {
                if (isInitialLoaded) {
                    NotificationHelper.showNotification(
                        this,
                        "💸 নতুন ক্যাশ আউট রিকোয়েস্ট!",
                        "$phone নম্বর ৳$amt উইথড্র করতে চায়।"
                    )
                }
                knownEvents.add(key)
            }
        }

        // টাইপিং টাস্ক
        snapshot.child("paragraph_jobs").children.forEach { taskSnap ->
            val id = taskSnap.key ?: return@forEach
            val status = taskSnap.child("status").value?.toString() ?: "Pending"
            val key = "${phone}_task_$id"

            if (status == "Pending" && !knownEvents.contains(key)) {
                if (isInitialLoaded) {
                    NotificationHelper.showNotification(
                        this,
                        "📝 নতুন টাইপিং টাস্ক জমা!",
                        "$phone একটি টাইপিং কাজ জমা দিয়েছে।"
                    )
                }
                knownEvents.add(key)
            }
        }
    }

    private fun processChatSnapshot(snapshot: DataSnapshot) {
        val phone = snapshot.key ?: return
        snapshot.child("messages").children.forEach { msgSnap ->
            val msgId = msgSnap.key ?: return@forEach
            val sender = msgSnap.child("sender").value?.toString() ?: ""
            val seen = msgSnap.child("seen").value as? Boolean ?: false
            val text = msgSnap.child("text").value?.toString() ?: ""
            val key = "${phone}_msg_$msgId"

            if (sender == "user" && !seen && !knownEvents.contains(key)) {
                if (isInitialLoaded) {
                    NotificationHelper.showNotification(
                        this,
                        "💬 মেসেজ: $phone",
                        text,
                        isChat = true
                    )
                }
                knownEvents.add(key)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
