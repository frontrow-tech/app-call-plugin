package com.hiennv.flutter_callkit_incoming

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.gson.Gson

class Utils {

    companion object {

        private var gson: Gson? = null

        fun getGsonInstance(): Gson {
            if(gson == null) gson = Gson()
            return gson!!
        }

        @JvmStatic
        fun dpToPx(dp: Float): Float {
            return dp * Resources.getSystem().displayMetrics.density
        }

        @JvmStatic
        fun pxToDp(px: Float): Float {
            return px / Resources.getSystem().displayMetrics.density
        }

        @JvmStatic
        fun getScreenWidth(): Int {
            return Resources.getSystem().displayMetrics.widthPixels
        }

        @JvmStatic
        fun getScreenHeight(): Int {
            return Resources.getSystem().displayMetrics.heightPixels
        }

        fun getNavigationBarHeight(context: Context): Int {
            val resources = context.resources
            val id = resources.getIdentifier(
                "navigation_bar_height", "dimen", "android"
            )
            return if (id > 0) {
                resources.getDimensionPixelSize(id)
            } else 0
        }

        fun getStatusBarHeight(context: Context): Int {
            val resources = context.resources
            val id: Int =
                resources.getIdentifier("status_bar_height", "dimen", "android")
            return if (id > 0) {
                resources.getDimensionPixelSize(id)
            } else 0
        }

        fun backToForeground(context: Context) {
            val packageName = context.packageName
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)?.cloneFilter()

            intent?.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)

            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

    }
}

fun Context.showNotificationWithFullScreenIntent(
    isLockScreen: Boolean = false,
    channelId: String = CHANNEL_ID,
    title: String = "Title",
    description: String = "Lorem ipsum dolor sit amet, consectetur adipiscing elit."

) {
    val builder = NotificationCompat.Builder(this, channelId)
        .setSmallIcon(android.R.drawable.arrow_up_float)
        .setContentTitle(title)
        .setContentText(description)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setFullScreenIntent(getFullScreenIntent(), true)


    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    with(notificationManager) {
        buildChannel()

        val notification = builder.build()

        notify(0, notification)
    }
}

private fun NotificationManager.buildChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Example Notification Channel"
        val descriptionText = "This is used to demonstrate the Full Screen Intent"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }

        createNotificationChannel(channel)
    }
}

private fun Context.getFullScreenIntent(): PendingIntent {
    val destination = CallkitIncomingActivity::class.java
    val intent = Intent(this, destination)

    // flags and request code are 0 for the purpose of demonstration
    return PendingIntent.getActivity(this, 0, intent, 0)
}

private const val CHANNEL_ID = "channelId"
