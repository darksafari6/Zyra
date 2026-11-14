package com.safari.zyra.device.control

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ZyraNotificationItem(
    val packageName: String,
    val title: String,
    val text: String,
    val postedAt: Long
)

class ZyraNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        mutableItems.value = (listOf(ZyraNotificationItem(
            sbn.packageName, title.take(120), text.take(400), sbn.postTime
        )) + mutableItems.value.filterNot { it.packageName == sbn.packageName && it.title == title }).take(30)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        requestRebind(android.content.ComponentName(this, ZyraNotificationListener::class.java))
    }

    companion object {
        private val mutableItems = MutableStateFlow<List<ZyraNotificationItem>>(emptyList())
        val items = mutableItems.asStateFlow()
    }
}
