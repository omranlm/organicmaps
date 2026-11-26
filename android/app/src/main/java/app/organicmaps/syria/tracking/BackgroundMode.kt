package app.organicmaps.syria.tracking

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import org.json.JSONObject

/**
 * The app's battery setting, which decides whether a recording keeps going with the screen off:
 * Unrestricted (no battery optimization), Optimized (the default: Android may delay or stop it) or
 * Restricted (stopped in the background). Phone makers' own savers (Xiaomi Autostart, Huawei App launch,
 * Samsung sleeping apps) cannot be read: the maker and model are sent with the mode to explain them.
 */
enum class BackgroundMode(val key: String) {
    UNRESTRICTED("unrestricted"),
    OPTIMIZED("optimized"),
    RESTRICTED("restricted"),
    ;

    companion object {
        @JvmStatic
        fun current(context: Context): BackgroundMode {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && activityManager?.isBackgroundRestricted == true) {
                return RESTRICTED
            }
            // No battery optimization before Android 6.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return UNRESTRICTED
            val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return OPTIMIZED
            return if (power.isIgnoringBatteryOptimizations(context.packageName)) UNRESTRICTED else OPTIMIZED
        }

        /** Event payload: the mode and the other battery settings, with the phone they apply to. */
        fun payload(context: Context, mode: BackgroundMode = current(context)): JSONObject {
            val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val json = JSONObject()
                .put("batteryMode", mode.key)
                .put("powerSaver", power?.isPowerSaveMode == true)
                .put("manufacturer", Build.MANUFACTURER)
                .put("model", Build.MODEL)
                .put("androidSdk", Build.VERSION.SDK_INT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // What the power saver does to location (GPS off with the screen off, ...), and how often
                // Android lets the app run, from 5 (exempted: unrestricted battery) to 45 (restricted).
                power?.let { json.put("locationPowerSaveMode", locationModeKey(it.locationPowerSaveMode)) }
                val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                usage?.let { json.put("standbyBucket", it.appStandbyBucket) }
            }
            return json
        }

        @SuppressLint("InlinedApi") // Constants only; the value comes from Android 9+.
        private fun locationModeKey(mode: Int): String = when (mode) {
            PowerManager.LOCATION_MODE_NO_CHANGE -> "no_change"
            PowerManager.LOCATION_MODE_GPS_DISABLED_WHEN_SCREEN_OFF -> "gps_off_screen_off"
            PowerManager.LOCATION_MODE_ALL_DISABLED_WHEN_SCREEN_OFF -> "all_off_screen_off"
            PowerManager.LOCATION_MODE_FOREGROUND_ONLY -> "foreground_only"
            PowerManager.LOCATION_MODE_THROTTLE_REQUESTS_WHEN_SCREEN_OFF -> "throttled_screen_off"
            else -> mode.toString()
        }

        /** App info, where Battery > Unrestricted is one tap away; the optimization list as a fallback. */
        fun openSettings(activity: Activity) {
            val intents = listOf(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null)),
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                Intent(Settings.ACTION_SETTINGS),
            )
            for (intent in intents) {
                try {
                    activity.startActivity(intent)
                    return
                } catch (e: ActivityNotFoundException) {
                    continue
                }
            }
        }
    }
}
