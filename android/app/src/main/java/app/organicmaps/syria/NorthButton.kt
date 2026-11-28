package app.organicmaps.syria

import android.widget.ImageView
import app.organicmaps.sdk.Framework
import app.organicmaps.sdk.location.LocationState
import java.lang.ref.WeakReference
import kotlin.math.PI

/**
 * The north button on the map (layout/map_buttons_syria_north.xml), in place of the renderer's compass. Always
 * shown; its needle turns with the map. A tap turns the map north up; a second tap while following your
 * position turns it to the direction of travel, like the position button does.
 */
object NorthButton {
    private const val NORTH_TOLERANCE_RAD = 0.02

    private var button = WeakReference<ImageView>(null)
    private var azimuth = 0.0
    private var listening = false

    @JvmStatic
    fun bind(view: ImageView) {
        // The needle keeps its own red and grey, unlike the other map button icons.
        view.imageTintList = null
        view.setOnClickListener { onClick() }
        button = WeakReference(view)
        render(view)
        // One listener for the whole app: the map buttons are inflated again for navigation.
        if (!listening) {
            listening = true
            Framework.nativeSetMapRotationListener { rotation ->
                azimuth = rotation
                button.get()?.let { render(it) }
            }
        }
    }

    /** North is turned clockwise on the screen by [azimuth], and so is the needle. */
    private fun render(view: ImageView) {
        view.rotation = Math.toDegrees(azimuth).toFloat()
    }

    private fun isNorthUp(): Boolean = azimuth < NORTH_TOLERANCE_RAD || azimuth > 2 * PI - NORTH_TOLERANCE_RAD

    private fun onClick() {
        val mode = LocationState.getMode()
        when {
            // Follow mode with north up (the same as a tap on the renderer's compass).
            mode == LocationState.FOLLOW_AND_ROTATE -> LocationState.nativeSwitchToNextMode()

            // Direction of travel up, when the phone has a compass or a route is followed.
            mode == LocationState.FOLLOW && isNorthUp() -> LocationState.nativeSwitchToNextMode()

            else -> Framework.nativeRotateToNorth()
        }
    }
}
