package com.safari.zyra.device.control

import android.Manifest
import android.app.AlarmClock
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.CalendarContract
import android.provider.Settings
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class AndroidToolFactory @Inject constructor(@ApplicationContext private val context: Context) {
    private val stringSchema = { key: String, description: String ->
        JSONObject().put("type", "OBJECT")
            .put("properties", JSONObject().put(key, JSONObject().put("type", "STRING").put("description", description)))
            .put("required", JSONArray().put(key))
    }
    private fun tool(
        name: String, description: String, schema: JSONObject, risk: ToolRisk,
        permissions: Set<String> = emptySet(), block: suspend (JSONObject) -> ToolResult
    ) = FunctionalZyraTool(name, description, schema, risk, permissions, block)

    fun create(): Set<ZyraTool> = setOf(
        tool("open_url", "Open a web URL in the user's browser.", stringSchema("url", "An http or https URL."), ToolRisk.LOW) { args ->
            val raw = ToolArgumentValidator.requiredString(args, "url")
            val uri = Uri.parse(raw)
            require(uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()) { "Only valid http or https URLs are allowed." }
            launch(Intent(Intent.ACTION_VIEW, uri)); ToolResult.Success("Opened the URL in the browser.")
        },
        tool("web_search", "Open a web search page for a query.", stringSchema("query", "Search terms."), ToolRisk.LOW) { args ->
            val query = ToolArgumentValidator.requiredString(args, "query", 300)
            launch(Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", query))
            ToolResult.Success("Opened a web search for: $query")
        },
        tool("open_app", "Open an installed app by exact package name or visible launcher label.", JSONObject()
            .put("type", "OBJECT").put("properties", JSONObject()
                .put("package", JSONObject().put("type", "STRING"))
                .put("label", JSONObject().put("type", "STRING"))), ToolRisk.LOW) { args ->
            val packageName = args.optString("package").takeIf { it.isNotBlank() }
            val label = args.optString("label").trim()
            val target = packageName ?: context.packageManager.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PackageManager.MATCH_ALL
            ).firstOrNull { it.loadLabel(context.packageManager).toString().equals(label, true) }?.activityInfo?.packageName
                ?: return@tool ToolResult.Failed("I couldn't find an installed app with that exact label.")
            val intent = context.packageManager.getLaunchIntentForPackage(target)
                ?: return@tool ToolResult.Failed("That app has no launchable activity.")
            launch(intent); ToolResult.Success("Opened the app.")
        },
        tool("call", "Open the phone dialer with a number. Direct calling is not implemented; the user places the call.", stringSchema("number", "Phone number to dial."), ToolRisk.HIGH) { args ->
            val number = ToolArgumentValidator.requiredString(args, "number", 40)
            require(number.matches(Regex("[+0-9*#(). -]{3,40}"))) { "Phone number contains unsupported characters." }
            launch(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)))
            ToolResult.Success("Opened the dialer with this number; tap Call to continue.")
        },
        tool("message", "Prefill an SMS message; the user must press Send.", JSONObject()
            .put("type", "OBJECT").put("properties", JSONObject()
                .put("number", JSONObject().put("type", "STRING"))
                .put("message", JSONObject().put("type", "STRING")))
            .put("required", JSONArray().put("number").put("message")), ToolRisk.HIGH) { args ->
            val number = ToolArgumentValidator.requiredString(args, "number", 40)
            val body = ToolArgumentValidator.requiredString(args, "message", 2000)
            launch(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}")).putExtra("sms_body", body))
            ToolResult.Success("Opened the SMS composer with the recipient and message; review it and tap Send.")
        },
        tool("set_alarm", "Open the system alarm app to create an alarm.", JSONObject()
            .put("type", "OBJECT").put("properties", JSONObject()
                .put("hour", JSONObject().put("type", "INTEGER"))
                .put("minute", JSONObject().put("type", "INTEGER"))
                .put("message", JSONObject().put("type", "STRING")))
            .put("required", JSONArray().put("hour").put("minute")), ToolRisk.MEDIUM) { args ->
            val hour = args.getInt("hour"); val minute = args.getInt("minute")
            require(hour in 0..23 && minute in 0..59) { "Alarm time is invalid." }
            launch(Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR, hour)
                .putExtra(AlarmClock.EXTRA_MINUTES, minute).putExtra(AlarmClock.EXTRA_MESSAGE, args.optString("message")))
            ToolResult.Success("Opened the system alarm form. Review and save the alarm there.")
        },
        tool("set_timer", "Open the system clock to create a timer.", JSONObject()
            .put("type", "OBJECT").put("properties", JSONObject()
                .put("seconds", JSONObject().put("type", "INTEGER"))
                .put("message", JSONObject().put("type", "STRING")))
            .put("required", JSONArray().put("seconds")), ToolRisk.MEDIUM) { args ->
            val seconds = args.getInt("seconds"); require(seconds in 1..86400) { "Timer must be between 1 second and 24 hours." }
            launch(Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                .putExtra(AlarmClock.EXTRA_MESSAGE, args.optString("message")).putExtra(AlarmClock.EXTRA_SKIP_UI, false))
            ToolResult.Success("Opened the system timer form. Review and start the timer there.")
        },
        tool("calendar_event", "Open the calendar event editor prefilled with an event.", JSONObject()
            .put("type", "OBJECT").put("properties", JSONObject()
                .put("title", JSONObject().put("type", "STRING"))
                .put("beginMillis", JSONObject().put("type", "INTEGER"))
                .put("endMillis", JSONObject().put("type", "INTEGER"))
                .put("description", JSONObject().put("type", "STRING")))
            .put("required", JSONArray().put("title").put("beginMillis").put("endMillis")), ToolRisk.MEDIUM) { args ->
            val title = ToolArgumentValidator.requiredString(args, "title", 160)
            val begin = args.getLong("beginMillis"); val end = args.getLong("endMillis")
            require(begin > System.currentTimeMillis() && end > begin) { "Event times must be in the future and end after start." }
            launch(Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
                .putExtra(CalendarContract.Events.DESCRIPTION, args.optString("description")))
            ToolResult.Success("Opened the calendar event editor. Review and save the event there.")
        },
        tool("flashlight", "Turn the camera torch on or off.", JSONObject()
            .put("type", "OBJECT").put("properties", JSONObject().put("enabled", JSONObject().put("type", "BOOLEAN")))
            .put("required", JSONArray().put("enabled")), ToolRisk.MEDIUM, setOf(Manifest.permission.CAMERA)) { args ->
            val camera = context.getSystemService(CameraManager::class.java)
            val id = camera.cameraIdList.firstOrNull { camera.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
                ?: return@tool ToolResult.Failed("No camera flash is available.")
            camera.setTorchMode(id, args.getBoolean("enabled"))
            ToolResult.Success(if (args.getBoolean("enabled")) "Turned the flashlight on." else "Turned the flashlight off.")
        },
        tool("volume", "Set media volume to a percentage from 0 to 100.", JSONObject()
            .put("type", "OBJECT").put("properties", JSONObject().put("percent", JSONObject().put("type", "INTEGER")))
            .put("required", JSONArray().put("percent")), ToolRisk.MEDIUM) { args ->
            val percent = args.getInt("percent"); require(percent in 0..100) { "Volume percentage must be from 0 to 100." }
            val audio = context.getSystemService(AudioManager::class.java)
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, (max * percent / 100.0).toInt(), 0)
            ToolResult.Success("Set media volume to approximately $percent percent.")
        },
        tool("navigation", "Open a map route to a destination.", stringSchema("destination", "Place name or address."), ToolRisk.LOW) { args ->
            val destination = ToolArgumentValidator.requiredString(args, "destination", 300)
            launch(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${Uri.encode(destination)}")))
            ToolResult.Success("Opened navigation for $destination.")
        },
        tool("device_info", "Read battery and connectivity status.", JSONObject().put("type", "OBJECT").put("properties", JSONObject()), ToolRisk.LOW) {
            val battery = context.getSystemService(BatteryManager::class.java)
            val percent = battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val cm = context.getSystemService(android.net.ConnectivityManager::class.java)
            val connected = cm.activeNetworkInfo?.isConnected == true
            ToolResult.Success("Battery: ${if (percent >= 0) "$percent%" else "unavailable"}. Internet connectivity: ${if (connected) "connected" else "not connected"}.")
        },
        tool("wifi_settings", "Open Android Wi-Fi settings. Android controls the Wi-Fi switch.", JSONObject().put("type", "OBJECT").put("properties", JSONObject()), ToolRisk.LOW) {
            launch(Intent(Settings.Panel.ACTION_WIFI)); ToolResult.Success("Opened Android's Wi-Fi panel. Turn Wi-Fi on or off there.")
        },
        tool("bluetooth_settings", "Open Android Bluetooth settings. Android controls the Bluetooth switch.", JSONObject().put("type", "OBJECT").put("properties", JSONObject()), ToolRisk.LOW) {
            launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); ToolResult.Success("Opened Android Bluetooth settings. Change Bluetooth there.")
        },
        tool("brightness", "Set screen brightness from 0 to 100 percent.", JSONObject()
            .put("type", "OBJECT").put("properties", JSONObject().put("percent", JSONObject().put("type", "INTEGER")))
            .put("required", JSONArray().put("percent")), ToolRisk.MEDIUM, setOf(Manifest.permission.WRITE_SETTINGS)) { args ->
            if (!Settings.System.canWrite(context)) {
                launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")))
                return@tool ToolResult.NeedsUserAction("Android opened the Modify system settings permission page. Grant permission, then retry brightness.")
            }
            val percent = args.getInt("percent"); require(percent in 1..100) { "Brightness must be from 1 to 100 percent." }
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, percent * 255 / 100)
            ToolResult.Success("Set screen brightness to approximately $percent percent.")
        }
    )

    private fun launch(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        check(intent.resolveActivity(context.packageManager) != null) { "No compatible app is installed for this action." }
        context.startActivity(intent)
    }
}
