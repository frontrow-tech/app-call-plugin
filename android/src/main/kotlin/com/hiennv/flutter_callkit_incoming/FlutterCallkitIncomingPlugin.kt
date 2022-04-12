package com.hiennv.flutter_callkit_incoming

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat.startActivity
import io.flutter.embedding.engine.FlutterEngineCache

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import org.json.JSONObject

/** FlutterCallkitIncomingPlugin */
class FlutterCallkitIncomingPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    companion object {
        var coreChannel: MethodChannel? = null
        @SuppressLint("StaticFieldLeak")
        private var instance: FlutterCallkitIncomingPlugin? = null
        lateinit var eventHandler: EventCallbackHandler
        public fun getInstance(): FlutterCallkitIncomingPlugin  {
            if(instance == null){
                instance = FlutterCallkitIncomingPlugin()
            }
            return instance!!
        }


        fun sendEvent(event: String, body: Map<String, Any>) {
            eventHandler.send(event, body)
        }

        fun initChannel(channel: MethodChannel) {
            coreChannel = channel
        }

    }

    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private var activity: Activity? = null
    private var context: Context? = null
    private var callkitNotificationManager: CallkitNotificationManager? = null
    private var channel: MethodChannel? = null
    private var events: EventChannel? = null


    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        eventHandler = EventCallbackHandler(con = context!!)
        callkitNotificationManager = CallkitNotificationManager(flutterPluginBinding.applicationContext)
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_callkit_incoming")
        channel?.setMethodCallHandler(this)
        events =
            EventChannel(flutterPluginBinding.binaryMessenger, "flutter_callkit_incoming_events")
        events?.setStreamHandler(eventHandler)
 //       sharePluginWithRegister(flutterPluginBinding, this)
    }


    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        try {
            when (call.method) {
                "showCallkitIncoming" -> {
                    val data = Data(call.arguments())
                    data.from = "notification"
                    callkitNotificationManager?.showIncomingNotification(data.toBundle())
                    //send BroadcastReceiver
                    context?.sendBroadcast(
                        CallkitIncomingBroadcastReceiver.getIntentIncoming(
                            requireNotNull(context),
                            data.toBundle()
                        )
                    )
                    result.success("OK")
                }
                "showMissCallNotification" -> {
                    val data = Data(call.arguments())
                    data.from = "notification"
                    result.success("OK")
                }
                "startCall" -> {
                    val data = Data(call.arguments())
                    context?.sendBroadcast(
                        CallkitIncomingBroadcastReceiver.getIntentStart(
                            requireNotNull(context),
                            data.toBundle()
                        )
                    )
                    result.success("OK")
                }
                "endCall" -> {
                    val data = Data(call.arguments())
                    context?.sendBroadcast(
                        CallkitIncomingBroadcastReceiver.getIntentEnded(
                            requireNotNull(context),
                            data.toBundle()
                        )
                    )
                    result.success("OK")
                }
                "endAllCalls" -> {
                    val calls = getDataActiveCalls(context)
                    calls.forEach {
                        if(it.isAccepted) {
                            context?.sendBroadcast(
                                CallkitIncomingBroadcastReceiver.getIntentEnded(
                                    requireNotNull(context),
                                    it.toBundle()
                                )
                            )
                        }else {
                            context?.sendBroadcast(
                                CallkitIncomingBroadcastReceiver.getIntentDecline(
                                    requireNotNull(context),
                                    it.toBundle()
                                )
                            )
                        }
                    }
                    removeAllCalls(context)
                    result.success("OK")
                }
                "activeCalls" -> {
                    result.success(getActiveCalls(context))
                }
                "getDevicePushTokenVoIP" -> {
                    result.success("")
                }
            }
        } catch (error: Exception) {
            result.error("error", error.message, "")
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel?.setMethodCallHandler(null)
    }


    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        this.activity = binding.activity
        this.context = binding.activity.applicationContext
    }

    override fun onDetachedFromActivityForConfigChanges() {
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        this.activity = binding.activity
        this.context = binding.activity.applicationContext
    }

    override fun onDetachedFromActivity() {}


    class EventCallbackHandler(con: Context) : EventChannel.StreamHandler {

        private var eventSink: EventChannel.EventSink? = null

        private var context: Context = con

        override fun onListen(arguments: Any?, sink: EventChannel.EventSink) {
            eventSink = sink
        }

        fun send(event: String, body: Map<String, Any>) {
            val data = mapOf(
                "event" to event,
                "body" to body
            )
            val pm: PackageManager? = context.packageManager
            val launchIntent: Intent? = pm?.getLaunchIntentForPackage("io.connectcourses.app")
            context.startActivity(launchIntent)
            val jsonData = JSONObject()
            if(event == "com.hiennv.flutter_callkit_incoming.ACTION_CALL_DECLINE"){
                jsonData.put("runtimeType", "rebooking_screen")
                val extraData = body["extra"] as Map<*, *>
                jsonData.put("courseId", extraData["courseId"])
            } else if(event == "com.hiennv.flutter_callkit_incoming.ACTION_CALL_ACCEPT"){
                jsonData.put("runtimeType", "live_session")
                jsonData.put("meetingId", "62504dcca26d2f8ec0702a7a")
            }
            if(event == "com.hiennv.flutter_callkit_incoming.ACTION_CALL_ACCEPT" || event == "com.hiennv.flutter_callkit_incoming.ACTION_CALL_DECLINE")
                Handler(Looper.getMainLooper()).postDelayed( {
                coreChannel?.invokeMethod("IN_APP_CALL_SCREEN", jsonData.toString())
                //eventSink?.success(data)
            }, 2000)
        }

        override fun onCancel(arguments: Any?) {
            eventSink = null
        }
    }
}
