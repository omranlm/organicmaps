package app.organicmaps.syria

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import app.organicmaps.R
import app.organicmaps.routing.NavigationService
import app.organicmaps.sdk.Framework
import app.organicmaps.sdk.routing.RouteMarkType
import app.organicmaps.sdk.routing.RoutingInfo
import app.organicmaps.sdk.util.Graphics
import app.organicmaps.util.Utils
import kotlin.math.roundToInt

/**
 * The navigation notification, which the user sees once they leave the app while driving, with the whole trip
 * in it like Google Maps: the next turn and street, the share of the trip done on a progress bar with a car
 * moving toward the destination flag, the time and distance left, the arrival time and the destination.
 * On Android 16 it is a Live Update: the time left also shows in a chip in the status bar.
 * Called by [NavigationService] on every location update while navigating.
 */
object TripNotification : NavigationService.TripNotificationDecorator {
    private const val PROGRESS_MAX = 100

    override fun decorate(context: Context, builder: NotificationCompat.Builder, info: RoutingInfo) {
        val percent = info.completionPercent.roundToInt().coerceIn(0, PROGRESS_MAX)
        val timeLeft = formatTimeLeft(context, info.totalTimeInSeconds)
        val turn = info.distToTurn.toString(context)
        builder
            .setContentTitle(if (info.nextStreet.isNullOrEmpty()) turn else "$turn · ${info.nextStreet}")
            .setContentText(
                context.getString(
                    R.string.syria_trip_progress,
                    percent,
                    timeLeft,
                    info.distToTarget.toString(context),
                    Utils.formatArrivalTime(context, info.totalTimeInSeconds),
                ),
            )
            .setSubText(destination()?.let { context.getString(R.string.syria_trip_destination, it) })
            .setStyle(progress(context, percent))
            .setShortCriticalText(timeLeft)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            // A Live Update must not be colorized; Android shows it in the status bar and on the lock screen.
            builder.setColorized(false).setRequestPromotedOngoing(true)
            // The turn icon NavigationService draws is white for a colorized notification: tint it instead.
            turnIcon(context, info)?.let { builder.setLargeIcon(it) }
        }
    }

    private var turnIconRes = 0
    private var turnIcon: Bitmap? = null

    private fun turnIcon(context: Context, info: RoutingInfo): Bitmap? {
        val res = info.carDirection.getTurnRes(info.exitNum)
        if (res != turnIconRes || turnIcon == null) {
            val drawable = AppCompatResources.getDrawable(context, res) ?: return null
            val accent = ContextCompat.getColor(context, app.organicmaps.branding.R.color.base_accent)
            turnIcon = Graphics.drawableToBitmapWithTint(drawable, accent)
            turnIconRes = res
        }
        return turnIcon
    }

    /** A progress bar from Android 16 on, a plain one before (NotificationCompat falls back by itself). */
    private fun progress(context: Context, percent: Int) = NotificationCompat.ProgressStyle()
        .setStyledByProgress(true)
        .setProgressSegments(
            listOf(
                NotificationCompat.ProgressStyle.Segment(PROGRESS_MAX)
                    .setColor(ContextCompat.getColor(context, R.color.syria_traffic_green)),
            ),
        )
        .setProgress(percent)
        .setProgressTrackerIcon(IconCompat.createWithResource(context, R.drawable.ic_syria_trip_car))
        .setProgressEndIcon(IconCompat.createWithResource(context, R.drawable.ic_syria_trip_flag))

    /** "4 min" or "1 h 5 min", as on the navigation panel of the map. */
    private fun formatTimeLeft(context: Context, seconds: Int): String {
        val totalMinutes = Utils.getRoutingMinutes(seconds)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val min = "$minutes ${context.getString(R.string.minute)}"
        return when {
            hours == 0L -> min
            minutes == 0L -> "$hours ${context.getString(R.string.hour)}"
            else -> "$hours ${context.getString(R.string.hour)} $min"
        }
    }

    private fun destination(): String? = Framework.nativeGetRoutePoints()
        .firstOrNull { it.mPointType == RouteMarkType.Finish }
        ?.mTitle
        ?.takeIf { it.isNotBlank() }
}
