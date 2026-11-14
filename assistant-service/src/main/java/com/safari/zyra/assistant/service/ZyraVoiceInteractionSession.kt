package com.safari.zyra.assistant.service

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.service.voice.VoiceInteractionSession
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Small system-owned assistant surface. The full conversation remains in MainActivity. */
class ZyraVoiceInteractionSession(context: android.content.Context) : VoiceInteractionSession(context) {
    override fun onCreateContentView(): View {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 24, 28, 24)
            setBackgroundColor(Color.rgb(12, 17, 32))
        }
        root.addView(TextView(context).apply {
            text = "ZYRA"
            textSize = 24f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, -2))
        root.addView(TextView(context).apply {
            text = "Your assistant by Safari"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 20)
        }, LinearLayout.LayoutParams(-1, -2))
        root.addView(Button(context).apply {
            text = "Open Zyra"
            setOnClickListener {
                val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                if (launch != null) context.startActivity(launch)
                hide()
            }
        }, LinearLayout.LayoutParams(-1, -2))
        return root
    }
}