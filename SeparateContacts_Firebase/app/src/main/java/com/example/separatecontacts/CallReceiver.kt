package com.example.separatecontacts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class CallReceiver : BroadcastReceiver() {

    private var overlayView: android.view.View? = null
    private var windowManager: WindowManager? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    phoneNumber?.let { number ->
                        showCallerInfo(context, number)
                    }
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    hideCallerInfo()
                }
            }
        }
    }

    private fun showCallerInfo(context: Context, phoneNumber: String) {
        try {
            val firebaseSyncManager = FirebaseSyncManager()
            
            // Use a simple coroutine approach for the broadcast receiver
            Thread {
                try {
                    // This is a simplified approach - in a real app you'd want better async handling
                    val contact = runBlocking {
                        firebaseSyncManager.getContactByPhoneNumber(phoneNumber)
                    }
                    
                    val displayName = contact?.name ?: "Unknown Contact"
                    
                    // Show overlay on main thread
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        showOverlay(context, displayName, phoneNumber)
                    }
                } catch (e: Exception) {
                    // Fallback to showing just the number
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        showOverlay(context, "Unknown Contact", phoneNumber)
                    }
                }
            }.start()
            
        } catch (e: Exception) {
            // Fallback to toast
            Toast.makeText(context, "Incoming call: $phoneNumber", Toast.LENGTH_LONG).show()
        }
    }

    private fun showOverlay(context: Context, name: String, phoneNumber: String) {
        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            val inflater = LayoutInflater.from(context)
            overlayView = inflater.inflate(R.layout.caller_id_overlay, null)
            
            val textViewName = overlayView?.findViewById<TextView>(R.id.textViewCallerName)
            val textViewNumber = overlayView?.findViewById<TextView>(R.id.textViewCallerNumber)
            
            textViewName?.text = name
            textViewNumber?.text = phoneNumber
            
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                android.graphics.PixelFormat.TRANSLUCENT
            )
            
            params.gravity = Gravity.TOP
            params.y = 100
            
            windowManager?.addView(overlayView, params)
            
        } catch (e: Exception) {
            // Fallback to toast if overlay fails
            Toast.makeText(context, "Incoming call: $name ($phoneNumber)", Toast.LENGTH_LONG).show()
        }
    }

    private fun hideCallerInfo() {
        try {
            overlayView?.let { view ->
                windowManager?.removeView(view)
                overlayView = null
                windowManager = null
            }
        } catch (e: Exception) {
            // Ignore errors when hiding
        }
    }
}

// Helper function for blocking coroutine call
private fun <T> runBlocking(block: suspend () -> T): T {
    return kotlinx.coroutines.runBlocking { block() }
}

