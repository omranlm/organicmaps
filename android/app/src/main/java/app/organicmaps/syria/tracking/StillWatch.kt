package app.organicmaps.syria.tracking

/**
 * Tells when the user has stopped moving: every fix for [stillMs] stayed within [radiusM] of where the stop
 * began. GPS drift while parked stays inside the radius (widened by the fix's accuracy). Traffic does not end
 * a trip: creeping 50 m in a jam, or one fix at driving speed, starts the count again, and a car standing
 * still at a light or a checkpoint has [stillMs] before the trip ends.
 */
class StillWatch(
    private val stillMs: Long = STILL_MS,
    private val radiusM: Double = 50.0,
    /** A reported speed at or above this counts as moving, wherever the fix is. */
    private val movingSpeedMps: Double = 10 / 3.6,
    private val maxAccuracyM: Float = 100f,
) {
    private var anchorLat = 0.0
    private var anchorLon = 0.0
    private var anchorTimeMs: Long? = null

    /** Returns true once the user has not moved for [stillMs]. */
    fun offer(timeMs: Long, lat: Double, lon: Double, speedMps: Float?, accuracyM: Float?): Boolean {
        if (accuracyM != null && accuracyM > maxAccuracyM) return false
        val since = anchorTimeMs
        val moved = since == null || timeMs < since ||
            (speedMps != null && speedMps >= movingSpeedMps) ||
            TrackingEngine.haversineM(anchorLat, anchorLon, lat, lon) > radiusM + (accuracyM ?: 0f)
        if (moved) {
            anchorLat = lat
            anchorLon = lon
            anchorTimeMs = timeMs
            return false
        }
        return timeMs - since!! >= stillMs
    }

    fun reset() {
        anchorTimeMs = null
    }

    companion object {
        /** Ten minutes: also in the notification text (syria_auto_stop_text). */
        const val STILL_MS = 10 * 60_000L
    }
}
