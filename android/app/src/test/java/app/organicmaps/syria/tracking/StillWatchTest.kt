package app.organicmaps.syria.tracking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StillWatchTest {
    private val t0 = 1_700_000_000_000L
    private val tenMinutesS = 600

    // 0.001 deg of latitude is ~111.2 m.
    private val metersPerDegree = 111_195.0

    /** One fix every 5 s for [seconds], moving north at [kmh] (derived only, no reported speed). */
    private fun StillWatch.feed(
        kmh: Double,
        seconds: Int,
        startS: Int = 0,
        lat0: Double = 36.2,
        jitterM: Double = 0.0,
    ) = (0..seconds step 5).map { s ->
        val jitter = if ((s / 5) % 2 == 0) jitterM else -jitterM
        val lat = lat0 + (kmh / 3.6 * s + jitter) / metersPerDegree
        offer(t0 + (startS + s) * 1000L, lat, 37.15, null, 5f)
    }

    @Test
    fun parkedTriggersAfterTenMinutes() {
        val results = StillWatch().feed(kmh = 0.0, seconds = tenMinutesS)
        assertFalse(results.dropLast(1).any { it })
        assertTrue(results.last())
    }

    @Test
    fun gpsDriftWhileParkedStillTriggers() {
        // Fixes swing 40 m back and forth around the parked car.
        assertTrue(StillWatch().feed(kmh = 0.0, seconds = tenMinutesS, jitterM = 20.0).last())
        // Indoors the fixes are worse and say so: the circle grows with the accuracy.
        val watch = StillWatch()
        val results = (0..tenMinutesS step 5).map { s ->
            val lat = 36.2 + (if ((s / 5) % 2 == 0) 60.0 else -60.0) / metersPerDegree
            watch.offer(t0 + s * 1000L, lat, 37.15, null, 80f)
        }
        assertTrue(results.last())
    }

    @Test
    fun drivingNeverTriggers() {
        assertFalse(StillWatch().feed(kmh = 40.0, seconds = 3600).any { it })
    }

    @Test
    fun crawlingInAJamNeverTriggers() {
        // 1 km/h, a slow walk's pace: 50 m takes three minutes.
        assertFalse(StillWatch().feed(kmh = 1.0, seconds = 3600).any { it })
    }

    @Test
    fun stopAndGoTrafficNeverTriggers() {
        // An hour of queueing: 9 minutes standing still, then 100 m forward, again and again.
        val watch = StillWatch()
        var lat = 36.2
        var startS = 0
        repeat(6) {
            assertFalse(watch.feed(kmh = 0.0, seconds = 540, startS = startS, lat0 = lat).any { it })
            lat += 100 / metersPerDegree
            startS += 545
        }
    }

    @Test
    fun movingAgainRestartsTheCount() {
        val watch = StillWatch()
        watch.feed(kmh = 0.0, seconds = 540)
        // Drives 200 m away, then stops there: ten more minutes are needed.
        val after = watch.feed(kmh = 0.0, seconds = 580, startS = 550, lat0 = 36.2 + 200 / metersPerDegree)
        assertFalse(after.any { it })
    }

    @Test
    fun reportedDrivingSpeedCountsAsMoving() {
        val watch = StillWatch()
        watch.feed(kmh = 0.0, seconds = 540)
        // A fast fix in the same place (e.g. driving round the block between fixes).
        assertFalse(watch.offer(t0 + 545_000, 36.2, 37.15, 40 / 3.6f, 5f))
        assertFalse(watch.offer(t0 + 900_000, 36.2, 37.15, 0f, 5f))
        assertTrue(watch.offer(t0 + 1_145_000, 36.2, 37.15, 0f, 5f))
    }

    @Test
    fun inaccurateFixesAreIgnored() {
        val watch = StillWatch()
        assertFalse(watch.offer(t0, 36.2, 37.15, null, 500f))
        // The first usable fix only starts the count.
        assertFalse(watch.offer(t0 + 700_000, 36.2, 37.15, null, 10f))
        assertTrue(watch.offer(t0 + 1_300_000, 36.2, 37.15, null, 10f))
    }

    @Test
    fun noFixesForAWhileInTheSamePlaceTriggers() {
        // GPS lost indoors: the next fix in the same place shows the user did not move.
        val watch = StillWatch()
        assertFalse(watch.offer(t0, 36.2, 37.15, null, 20f))
        assertTrue(watch.offer(t0 + 20 * 60_000L, 36.2, 37.15, null, 60f))
    }

    @Test
    fun resetStartsOver() {
        val watch = StillWatch()
        watch.feed(kmh = 0.0, seconds = 580)
        watch.reset()
        assertFalse(watch.offer(t0 + 700_000, 36.2, 37.15, null, 5f))
    }
}
