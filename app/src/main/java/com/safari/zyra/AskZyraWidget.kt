package com.safari.zyra

import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.dp
import androidx.glance.unit.sp

class AskZyraWidget : GlanceAppWidget() {
    @androidx.compose.runtime.Composable
    override fun Content() {
        Column(
            modifier = GlanceModifier.fillMaxSize()
                .background(ColorProvider(Color(0xFF0B1020)))
                .clickable(actionStartActivity<MainActivity>())
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            Text("ZYRA", style = TextStyle(color = ColorProvider(Color.White), fontSize = 20.sp))
            Text("Your assistant, one tap away", style = TextStyle(color = ColorProvider(Color(0xFFB9C7DD)), fontSize = 12.sp))
            Text("ASK ZYRA  →", modifier = GlanceModifier.padding(top = 10.dp), style = TextStyle(color = ColorProvider(Color(0xFF00E5FF)), fontSize = 14.sp))
        }
    }
}

class AskZyraWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AskZyraWidget()
}