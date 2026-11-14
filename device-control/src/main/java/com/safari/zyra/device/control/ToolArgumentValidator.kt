package com.safari.zyra.device.control

import org.json.JSONObject

object ToolArgumentValidator {
    fun requiredString(args: JSONObject, key: String, maxLength: Int = 2048): String {
        val value = args.optString(key).trim()
        require(value.isNotEmpty() && value.length <= maxLength) { "Missing or invalid '$key'." }
        return value
    }
    fun optionalString(args: JSONObject, key: String, maxLength: Int = 2048): String? =
        args.optString(key).takeIf { it.isNotBlank() && it.length <= maxLength }
}
