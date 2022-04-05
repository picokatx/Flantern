package com.picobyte.flantern.api

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.NotificationChannel

import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import android.media.AudioAttributes
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.net.Uri


class FlanternNotifications(val context: Context) {
    fun init() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val CHANNEL_ID = "com.picobyte.flantern.default"
        val name: CharSequence = "Flantern"
        val Description = "Flantern Notifications will arrive here"
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            val soundAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            mChannel.description = Description
            mChannel.enableLights(true)
            mChannel.lightColor = Color.RED
            mChannel.enableVibration(true)
            mChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            mChannel.setShowBadge(false)
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    private fun getUriForSoundName(context: Context, soundName: String): Uri {
        return Uri.parse(
            ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.packageName
                    + "/raw/" + soundName
        )
    }

    fun test(iconID: Int) {
        val SUMMARY_ID = 5960410
        val GROUP_KEY_WORK_EMAIL = "com.picobyte.flantern.group.messages"

        val soundAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()

        val newMessageNotification1 =
            NotificationCompat.Builder(context, "com.picobyte.flantern.test1")
                .setContentTitle("test")
                .setSmallIcon(iconID)
                .setContentText("You will not believe...")
                .setGroup(GROUP_KEY_WORK_EMAIL)
                .build()

        val newMessageNotification2 =
            NotificationCompat.Builder(context, "com.picobyte.flantern.test1")
                .setContentTitle("test1")
                .setSmallIcon(iconID)
                .setContentText("Please join us to celebrate the...")
                .setGroup(GROUP_KEY_WORK_EMAIL)
                .build()
        val newMessageNotification3 =
            NotificationCompat.Builder(context, "com.picobyte.flantern.test1")
                .setContentTitle("test2")
                .setSmallIcon(iconID)
                .setContentText("Flantern is a social media application that...")
                .setGroup(GROUP_KEY_WORK_EMAIL)
                .build()

        val summaryNotification = NotificationCompat.Builder(context, "com.picobyte.flantern.test1")
            .setContentTitle("Flantern")
            //set content text to support devices running API level < 24
            .setContentText("Two new messages")
            .setSmallIcon(iconID)
            //build summary info into InboxStyle template
            .setStyle(
                NotificationCompat.InboxStyle()
                    .addLine("Alex Faarborg Check this out")
                    .addLine("Jeff Chang Launch Party")
                    .setBigContentTitle("2 new messages")
                    .setSummaryText("3 Unread Messages")
            )
            //specify which group this notification belongs to
            .setGroup(GROUP_KEY_WORK_EMAIL)
            //set this notification as the summary for the group
            .setGroupSummary(true)
            .build()

        NotificationManagerCompat.from(context).apply {
            notify(SUMMARY_ID, summaryNotification)
            notify(295879, newMessageNotification1)
            notify(236492, newMessageNotification2)
            notify(260444, newMessageNotification3)
        }
    }
}