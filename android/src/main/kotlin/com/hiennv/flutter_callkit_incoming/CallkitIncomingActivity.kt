package com.hiennv.flutter_callkit_incoming

import android.animation.ArgbEvaluator
import android.animation.TimeAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.KeyguardManager.KeyguardLock
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver.Companion.ACTION_CALL_INCOMING
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver.Companion.EXTRA_CALLKIT_AVATAR
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver.Companion.EXTRA_CALLKIT_BACKGROUND_URL
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver.Companion.EXTRA_CALLKIT_BACKGROUND_COLOR
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver.Companion.EXTRA_CALLKIT_DURATION
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver.Companion.EXTRA_CALLKIT_INCOMING_DATA
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver.Companion.EXTRA_CALLKIT_NAME_CALLER
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver.Companion.EXTRA_CALLKIT_HANDLE
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver.Companion.EXTRA_CALLKIT_HEADERS
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver.Companion.EXTRA_CALLKIT_IS_SHOW_LOGO
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver.Companion.EXTRA_CALLKIT_TYPE
import com.hiennv.flutter_callkit_incoming.widgets.RippleRelativeLayout
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import kotlin.math.abs
import okhttp3.OkHttpClient
import com.squareup.picasso.OkHttp3Downloader
import android.view.ViewGroup.MarginLayoutParams
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.widget.RelativeLayout
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver.Companion.EXTRA_CALLKIT_ACCEPT_BUTTON_IMAGE
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver.Companion.EXTRA_CALLKIT_ACCEPT_BUTTON_TEXT
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver.Companion.EXTRA_CALLKIT_DECLINE_BUTTON_IMAGE
import java.util.concurrent.TimeUnit

private const val SCHEDULE_TIME = 1L
class CallkitIncomingActivity : Activity() {

    companion object {

        const val ACTION_ENDED_CALL_INCOMING =
            "com.hiennv.flutter_callkit_incoming.ACTION_ENDED_CALL_INCOMING"

        fun getIntent(data: Bundle) = Intent(ACTION_CALL_INCOMING).apply {
            action = ACTION_CALL_INCOMING
            putExtra(EXTRA_CALLKIT_INCOMING_DATA, data)
            flags =
                Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }

        fun getIntentEnded() =
            Intent(ACTION_ENDED_CALL_INCOMING)

    }

    inner class EndedCallkitIncomingBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!isFinishing) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    finishAndRemoveTask()
                } else {
                    finish()
                }
            }
        }
    }

    private var endedCallkitIncomingBroadcastReceiver = EndedCallkitIncomingBroadcastReceiver()

    private lateinit var ivBackground: LinearLayout
    private lateinit var llBackgroundAnimation: LinearLayout

    private lateinit var tvNameCaller: TextView
    private lateinit var tvNumber: TextView
    private lateinit var ivAvatar: CircleImageView

    private lateinit var tvCardSubText: TextView
    private lateinit var tvAcceptCallText: TextView
    private lateinit var tvDeclineCallText: TextView

    private lateinit var llAction: LinearLayout
    private lateinit var ivAcceptCall: ImageView
    private lateinit var ivDeclineCall: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transparentStatusAndNavigation()

        setContentView(R.layout.activity_callkit_incoming)
        turnScreenOnAndKeyguardOff()
        initView()
        incomingData(intent)
        registerReceiver(
            endedCallkitIncomingBroadcastReceiver,
            IntentFilter(ACTION_ENDED_CALL_INCOMING)
        )
    }

    private fun getReceiver(lockScreen: Any): PendingIntent? {
        TODO("Not yet implemented")
    }

    private fun wakeLockRequest(duration: Long) {

        val pm = applicationContext.getSystemService(POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Callkit:PowerManager"
        )
        wakeLock.acquire(duration)
    }

    private fun transparentStatusAndNavigation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            setWindowFlag(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                        or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, true
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setWindowFlag(
                (WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                        or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION), false
            )
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
    }

    private fun setWindowFlag(bits: Int, on: Boolean) {
        val win: Window = window
        val winParams: WindowManager.LayoutParams = win.attributes
        if (on) {
            winParams.flags = winParams.flags or bits
        } else {
            winParams.flags = winParams.flags and bits.inv()
        }
        win.attributes = winParams
    }


    private fun incomingData(intent: Intent) {
        val data = intent.extras?.getBundle(EXTRA_CALLKIT_INCOMING_DATA)
        if (data == null) finish()


        tvNumber.text = data?.getString(EXTRA_CALLKIT_HANDLE, "")
        val instructorMessage = "Your instructor " + data?.getString(EXTRA_CALLKIT_NAME_CALLER, "") + " is asking you to join now"
        tvCardSubText.text = instructorMessage

        val avatarUrl = data?.getString(EXTRA_CALLKIT_AVATAR, "")
        if (avatarUrl != null && avatarUrl.isNotEmpty()) {
            ivAvatar.visibility = View.VISIBLE
            val headers = data.getSerializable(EXTRA_CALLKIT_HEADERS) as HashMap<String, Any?>
            getPicassoInstance(this@CallkitIncomingActivity, headers)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(ivAvatar)
        }

        val acceptUrl = data?.getString(EXTRA_CALLKIT_ACCEPT_BUTTON_IMAGE, "")
        val declineUrl = data?.getString(EXTRA_CALLKIT_DECLINE_BUTTON_IMAGE, "")

        if(acceptUrl != null && acceptUrl.isNotEmpty()){
            val headers = data.getSerializable(EXTRA_CALLKIT_HEADERS) as HashMap<String, Any?>
            getPicassoInstance(this@CallkitIncomingActivity, headers)
                .load(acceptUrl)
                .placeholder(R.drawable.ic_accept)
                .error(R.drawable.ic_accept)
                .into(ivAcceptCall)
        }

        if(declineUrl != null && declineUrl.isNotEmpty()){
            val headers = data.getSerializable(EXTRA_CALLKIT_HEADERS) as HashMap<String, Any?>
            getPicassoInstance(this@CallkitIncomingActivity, headers)
                .load(acceptUrl)
                .placeholder(R.drawable.ic_decline)
                .error(R.drawable.ic_decline)
                .into(ivDeclineCall)
        }

        val callType = data?.getInt(EXTRA_CALLKIT_TYPE, 0) ?: 0
        if (callType > 0) {
            ivAcceptCall.setImageResource(R.drawable.ic_video)
        }
        val duration = data?.getLong(EXTRA_CALLKIT_DURATION, 0L) ?: 0L
        wakeLockRequest(duration)

        finishTimeout(data, duration)

        startAnimation()
    }

    private fun startAnimation(){
        val start = Color.parseColor("#534C54")
        val mid = Color.parseColor("#382E43")


        val bg = llAction.background as GradientDrawable
        llAction.background.setDither(true)
        ivBackground.background.setDither(true)

        val evaluator = ArgbEvaluator()
        val animator = TimeAnimator.ofFloat(0.0f, 1.0f)
        animator.duration = 3000
        animator.repeatCount = ValueAnimator.INFINITE
        animator.repeatMode = ValueAnimator.REVERSE
        animator.addUpdateListener {
            val fraction = it.animatedFraction
            val newStart = evaluator.evaluate(fraction, start, mid) as Int
            val newMid = evaluator.evaluate(fraction, mid, start) as Int
            val newEnd = evaluator.evaluate(fraction, start, mid) as Int

            bg.colors = intArrayOf(newStart, newMid, newEnd)
        }

        animator.start()

    }

    private fun finishTimeout(data: Bundle?, duration: Long) {
        val currentSystemTime = System.currentTimeMillis()
        val timeStartCall =
            data?.getLong(CallkitNotificationManager.EXTRA_TIME_START_CALL, currentSystemTime)
                ?: currentSystemTime

        val timeOut = duration - abs(currentSystemTime - timeStartCall)
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    finishAndRemoveTask()
                } else {
                    finish()
                }
            }
        }, timeOut)
    }

    private fun initView() {

        tvNameCaller = findViewById(R.id.tvNameCaller)
        tvNumber = findViewById(R.id.tvNumber)
        ivAvatar = findViewById(R.id.ivCircularImage)
        tvAcceptCallText = findViewById(R.id.tvAccept)
        tvDeclineCallText = findViewById(R.id.tvDecline)
        tvCardSubText = findViewById(R.id.tvCardData)
        ivBackground = findViewById(R.id.ivBackground)


        llAction = findViewById(R.id.llAction)

        val params = llAction.layoutParams as MarginLayoutParams
        params.setMargins(0,0,0,Utils.getNavigationBarHeight(this@CallkitIncomingActivity))
        llAction.layoutParams = params

        ivAcceptCall = findViewById(R.id.ivAcceptCall)
        ivDeclineCall = findViewById(R.id.ivDeclineCall)
        //animateAcceptCall()

        ivAcceptCall.setOnClickListener {
            onAcceptClick()
        }
        ivDeclineCall.setOnClickListener {
            onDeclineClick()
        }

        tvNameCaller.setTypeface(null, Typeface.BOLD)
        tvDeclineCallText.setTypeface(null, Typeface.BOLD)
        tvAcceptCallText.setTypeface(null, Typeface.BOLD)
        tvCardSubText.setTypeface(null, Typeface.BOLD)
    }

    private fun animateAcceptCall() {
        val shakeAnimation =
            AnimationUtils.loadAnimation(this@CallkitIncomingActivity, R.anim.shake_anim)
        ivAcceptCall.animation = shakeAnimation
    }


    private fun onAcceptClick() {
        val data = intent.extras?.getBundle(EXTRA_CALLKIT_INCOMING_DATA)
        val intent =
            CallkitIncomingBroadcastReceiver.getIntentAccept(this@CallkitIncomingActivity, data)
        sendBroadcast(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }

    private fun onDeclineClick() {
        val data = intent.extras?.getBundle(EXTRA_CALLKIT_INCOMING_DATA)
        val intent =
            CallkitIncomingBroadcastReceiver.getIntentDecline(this@CallkitIncomingActivity, data)
        sendBroadcast(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        } else {
            finish()
        }
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

    override fun onDestroy() {
        unregisterReceiver(endedCallkitIncomingBroadcastReceiver)
        turnScreenOffAndKeyguardOn()
        super.onDestroy()
    }

    override fun onBackPressed() {}

    private fun Activity.turnScreenOnAndKeyguardOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
        }

        with(getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requestDismissKeyguard(this@turnScreenOnAndKeyguardOff, null)
            }
        }
    }

    fun Activity.turnScreenOffAndKeyguardOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(false)
            setTurnScreenOn(false)
        } else {
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
        }
    }


}