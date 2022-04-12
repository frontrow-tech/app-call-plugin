package com.hiennv.flutter_callkit_incoming

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver.Companion.EXTRA_CALLKIT_ACTION_COLOR
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver.Companion.EXTRA_CALLKIT_AVATAR
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver.Companion.EXTRA_CALLKIT_DURATION
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver.Companion.EXTRA_CALLKIT_HANDLE
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver.Companion.EXTRA_CALLKIT_ID
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver.Companion.EXTRA_CALLKIT_IS_CUSTOM_NOTIFICATION
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver.Companion.EXTRA_CALLKIT_NAME_CALLER
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver.Companion.EXTRA_CALLKIT_TYPE
import com.hiennv.flutter_callkit_incoming.widgets.CircleTransform
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit



class CallkitNotificationManager(private val context: Context) {

    companion object {

        const val EXTRA_TIME_START_CALL = "EXTRA_TIME_START_CALL"
    }

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var notificationViews

    : RemoteViews? = null
    private var notificationId: Int = 9696

    private var targetLoadAvatarDefault = object : Target {
        override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
            notificationBuilder.setLargeIcon(bitmap)
            getNotificationManager().notify(notificationId, notificationBuilder.build())
        }

        override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {}

        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
    }

    private var targetLoadAvatarCustomize = object : Target {
        override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
            notificationViews?.setImageViewBitmap(R.id.ivAvatar, bitmap)
            notificationViews?.setViewVisibility(R.id.ivAvatar, View.VISIBLE)
            getNotificationManager().notify(notificationId, notificationBuilder.build())
        }

        override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {}

        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
        }
    }


    fun showIncomingNotification(data: Bundle) {
        data.putLong(EXTRA_TIME_START_CALL, System.currentTimeMillis())
        notificationId = data.getString(EXTRA_CALLKIT_ID, "callkit_incoming").hashCode()
        createNotificationChanel()

        notificationBuilder = NotificationCompat.Builder(context, "callkit_incoming_channel_id").setPriority(NotificationCompat.PRIORITY_MAX)
        notificationBuilder.setAutoCancel(false)
        notificationBuilder.setChannelId("callkit_incoming_channel_id")
        notificationBuilder.setDefaults(NotificationCompat.DEFAULT_VIBRATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setCategory(NotificationCompat.CATEGORY_CALL)
            notificationBuilder.priority = NotificationCompat.PRIORITY_HIGH
        }
        notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        notificationBuilder.setOngoing(true)
        notificationBuilder.setWhen(0)
        notificationBuilder.setTimeoutAfter(data.getLong(EXTRA_CALLKIT_DURATION, 0L))
        notificationBuilder.setOnlyAlertOnce(true)


        this.context.scheduleNotification(getActivityPendingIntent(notificationId, data))
        notificationBuilder.setFullScreenIntent(
                getActivityPendingIntent(notificationId, data), true
        )
        val smallIcon = context.applicationInfo.icon
        notificationBuilder.setSmallIcon(smallIcon)
        notificationBuilder.setChannelId("callkit_incoming_channel_id")
        notificationBuilder.priority = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationManager.IMPORTANCE_HIGH
        } else {
            Notification.PRIORITY_MAX
        }
            notificationViews =
                    RemoteViews(context.packageName, R.layout.layout_custom_notification)
            notificationViews?.setTextViewText(
                    R.id.tvNameCaller,
                    data.getString(EXTRA_CALLKIT_NAME_CALLER, "")
            )
            notificationViews?.setTextViewText(
                    R.id.tvNumber,
                    data.getString(EXTRA_CALLKIT_HANDLE, "")
            )
            notificationViews?.setOnClickPendingIntent(
                    R.id.llDecline,
                    getDeclinePendingIntent(notificationId, data)
            )
            notificationViews?.setOnClickPendingIntent(
                    R.id.llAccept,
                    getAcceptPendingIntent(notificationId, data)
            )
            val avatarUrl = data.getString(EXTRA_CALLKIT_AVATAR, "")
            if (avatarUrl != null && avatarUrl.isNotEmpty()) {
                val headers =
                        data.getSerializable(CallkitIncomingBroadcastReceiver.EXTRA_CALLKIT_HEADERS) as HashMap<String, Any?>
                getPicassoInstance(context, headers).load(avatarUrl)
                        .transform(CircleTransform())
                        .into(targetLoadAvatarCustomize)
            }
            notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(""))
            notificationViews!!.setViewPadding(0,0,0,0,0)
            notificationBuilder.setCustomContentView(notificationViews)
            notificationBuilder.setCustomBigContentView(notificationViews)
            notificationBuilder.setCustomHeadsUpContentView(notificationViews)
        val notification = notificationBuilder.build()
        notification.flags = Notification.FLAG_INSISTENT
        getNotificationManager().notify(notificationId, notification)
    }


    fun clearIncomingNotification(data: Bundle) {
        context.sendBroadcast(CallkitIncomingActivity.getIntentEnded())
        notificationId = data.getString(EXTRA_CALLKIT_ID, "callkit_incoming").hashCode()
        getNotificationManager().cancel(notificationId)
    }

    private fun createNotificationChanel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelCall = NotificationChannel(
                    "callkit_incoming_channel_id",
                    "Incoming Call",
                    NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = ""
                vibrationPattern =
                        longArrayOf(0, 1000, 500, 1000, 500)
                lightColor = Color.RED
                enableLights(true)
                enableVibration(true)
            }
            getNotificationManager().createNotificationChannel(channelCall)

            val channelMissedCall = NotificationChannel(
                    "callkit_missed_channel_id",
                    "Missed Call",
                    NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = ""
                vibrationPattern = longArrayOf(0, 1000)
                vibrationPattern = longArrayOf(0, 1000)
                lightColor = Color.RED
                enableLights(true)
                enableVibration(true)
            }
            getNotificationManager().createNotificationChannel(channelMissedCall)
        }
    }

    private fun getAcceptPendingIntent(id: Int, data: Bundle): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.cloneFilter()
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent != null) {
            val intentTransparent = TransparentActivity.getIntentAccept(context, data)
            intentTransparent.putExtra(CallkitIncomingBroadcastReceiver.EXTRA_CALLKIT_INCOMING_DATA, data)
            return PendingIntent.getActivities(
                    context,
                    id,
                    arrayOf(intent, intentTransparent),
                    getFlagPendingIntent()
            )
        } else {
            val acceptIntent = CallkitIncomingBroadcastReceiver.getIntentAccept(context, data)
            acceptIntent.putExtra(CallkitIncomingBroadcastReceiver.EXTRA_CALLKIT_INCOMING_DATA, data)
            return PendingIntent.getBroadcast(
                    context,
                    id,
                    acceptIntent,
                    getFlagPendingIntent()
            )
        }
    }

    private fun getDeclinePendingIntent(id: Int, data: Bundle): PendingIntent {
        val declineIntent = CallkitIncomingBroadcastReceiver.getIntentDecline(context, data)
        declineIntent.putExtra(CallkitIncomingBroadcastReceiver.EXTRA_CALLKIT_INCOMING_DATA, data)
        return PendingIntent.getBroadcast(
                context,
                id,
                declineIntent,
                getFlagPendingIntent()
        )
    }

    private fun getCallbackPendingIntent(id: Int, data: Bundle): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.cloneFilter()
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent != null) {
            val intentTransparent = TransparentActivity.getIntentCallback(context, data)
            return PendingIntent.getActivities(
                    context,
                    id,
                    arrayOf(intent, intentTransparent),
                    getFlagPendingIntent()
            )
        } else {
            val acceptIntent = CallkitIncomingBroadcastReceiver.getIntentCallback(context, data)
            return PendingIntent.getBroadcast(
                    context,
                    id,
                    acceptIntent,
                    getFlagPendingIntent()
            )
        }
    }

    private fun getActivityPendingIntent(id: Int, data: Bundle): PendingIntent {
        val intent = CallkitIncomingActivity.getIntent(data)
        intent.putExtra(CallkitIncomingBroadcastReceiver.EXTRA_CALLKIT_INCOMING_DATA, data)
        return PendingIntent.getActivity(context, id, intent, getFlagPendingIntent())
    }

    private fun getAppPendingIntent(id: Int, data: Bundle): PendingIntent {
        val intent: Intent? = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.putExtra(CallkitIncomingBroadcastReceiver.EXTRA_CALLKIT_INCOMING_DATA, data)
        return PendingIntent.getActivity(context, id, intent, getFlagPendingIntent())
    }

    private fun getFlagPendingIntent(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }

    private fun getNotificationManager(): NotificationManagerCompat {
        return NotificationManagerCompat.from(context)
    }


    private fun getPicassoInstance(context: Context, headers: HashMap<String, Any?>): Picasso {
        val client = OkHttpClient.Builder()
                .addNetworkInterceptor { chain ->
                    val newRequestBuilder: okhttp3.Request.Builder = chain.request().newBuilder()
                    for ((key, value) in headers) {
                        newRequestBuilder.addHeader(key, value.toString())
                    }
                    chain.proceed(newRequestBuilder.build())
                }
                .build()
        return Picasso.Builder(context)
                .downloader(OkHttp3Downloader(client))
                .build()
    }


}

fun Context.scheduleNotification(intent: PendingIntent) {
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val timeInMillis = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(SCHEDULE_TIME)


    with(alarmManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setExact(AlarmManager.RTC_WAKEUP, timeInMillis, intent)
        }
    }


}

private const val SCHEDULE_TIME = 0L