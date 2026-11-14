package com.safari.zyra.assistant.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.os.Build

/** Quick Settings entry point; tapping it is an explicit user action. */
class ZyraQuickSettingsTile : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = if (ZyraAssistantService.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "Zyra"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val action = if (ZyraAssistantService.isRunning) ZyraAssistantService.ACTION_STOP
            else ZyraAssistantService.ACTION_START
        val serviceIntent = Intent(this, ZyraAssistantService::class.java).setAction(action)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && action == ZyraAssistantService.ACTION_START) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        qsTile?.apply {
            state = if (action == ZyraAssistantService.ACTION_START) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }
}
