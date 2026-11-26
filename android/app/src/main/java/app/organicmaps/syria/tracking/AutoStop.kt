package app.organicmaps.syria.tracking

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.organicmaps.R
import app.organicmaps.sdk.util.log.Logger

/**
 * Ends the recording and live location when the user has not moved for a while ([StillWatch]): they were
 * probably left on after the trip, using the battery and sending a parked position. A notification says
 * why and asks the user to record again when they next drive or ride in a car.
 */
object AutoStop {
    private const val TAG = "SyriaAutoStop"
    private const val CHANNEL_ID = "SYRIA_AUTO_STOP"
    private const val NOTIFICATION_ID = 54325

    @JvmStatic
    fun stopAll(context: Context) {
        val recording = ActivityTrackingService.isActive()
        val live = LiveLocationService.isRunning()
        if (!recording && !live) return
        Logger.i(TAG, "Not moving: stopping recording=$recording live=$live")
        if (recording) ActivityTrackingService.stopNotMoving(context)
        if (live) LiveLocationService.disable(context)
        notify(context.applicationContext)
    }

    @SuppressLint("MissingPermission") // areNotificationsEnabled() is checked.
    private fun notify(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.syria_auto_stop_channel))
            .build()
        manager.createNotificationChannel(channel)
        val open = Intent(context, TrackingActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val immutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val text = context.getString(R.string.syria_auto_stop_text)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_track_recording_off)
            .setColor(ContextCompat.getColor(context, R.color.notification))
            .setContentTitle(context.getString(R.string.syria_auto_stop_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(context, 1, open, PendingIntent.FLAG_UPDATE_CURRENT or immutable),
            )
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    /** A new recording makes the notice out of date. */
    @JvmStatic
    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
