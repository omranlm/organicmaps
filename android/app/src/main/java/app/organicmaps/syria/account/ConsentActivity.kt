package app.organicmaps.syria.account

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import app.organicmaps.BuildConfig
import app.organicmaps.R
import app.organicmaps.base.BaseMwmFragmentActivity
import app.organicmaps.syria.SyriaPrefs
import app.organicmaps.syria.tracking.LiveLocationService
import app.organicmaps.syria.tracking.SyncWorker
import app.organicmaps.syria.tracking.TrackEvent
import app.organicmaps.syria.tracking.TrackingDb
import app.organicmaps.util.Utils
import app.organicmaps.util.WindowInsetUtils
import java.util.UUID
import org.json.JSONObject

/**
 * Asks for consent before anything is sent to our server (Google Play user data policy): before signing in,
 * before recording, and once per app start for a signed-in user who has not agreed yet (installs from before
 * this screen). Until the user agrees, nothing is uploaded and live location stays off.
 */
class ConsentActivity : BaseMwmFragmentActivity() {

    override fun getContentLayoutResId(): Int = R.layout.activity_syria_consent

    override fun requiresMapsReady(): Boolean = false

    // No dark toolbar behind the status bar here: dark icons in the light theme.
    override fun getStatusBarStyle(): SystemBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)

    override fun onSafeCreate(savedInstanceState: Bundle?) {
        super.onSafeCreate(savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.consent_root),
            WindowInsetUtils.PaddingInsetsListener.allSides(),
        )
        val items = findViewById<LinearLayout>(R.id.consent_items)
        ITEMS.forEach { (icon, text) -> addItem(items, icon, text) }
        findViewById<View>(R.id.consent_privacy).setOnClickListener {
            Utils.openUrl(this, BuildConfig.SYRIA_PRIVACY_URL)
        }
        findViewById<View>(R.id.consent_agree).setOnClickListener { agree() }
        findViewById<View>(R.id.consent_not_now).setOnClickListener { notNow() }
        onBackPressedDispatcher.addCallback(this) { notNow() }
    }

    private fun addItem(parent: LinearLayout, @DrawableRes icon: Int, @StringRes text: Int) {
        val row = LayoutInflater.from(this).inflate(R.layout.item_syria_consent, parent, false)
        row.findViewById<ImageView>(R.id.consent_item_icon).setImageResource(icon)
        row.findViewById<TextView>(R.id.consent_item_text).setText(text)
        parent.addView(row)
    }

    private fun agree() {
        val context = applicationContext
        SyriaPrefs.setConsentVersion(context, VERSION)
        // Kept on the server with the account, as a record of what the user agreed to.
        val payload = JSONObject().put("version", VERSION)
        val event =
            TrackEvent(UUID.randomUUID().toString(), TYPE_CONSENT, System.currentTimeMillis(), null, payload.toString())
        Thread {
            TrackingDb.get(context).insertEvent(event)
            if (SyriaPrefs.isLoggedIn(context)) SyncWorker.enqueueNow(context)
        }.start()
        LiveLocationService.startIfEnabled(this)
        IntentCompat.getParcelableExtra(intent, EXTRA_NEXT, Intent::class.java)?.let { startActivity(it) }
        finish()
    }

    private fun notNow() {
        Toast.makeText(this, R.string.syria_consent_declined, Toast.LENGTH_LONG).show()
        finish()
    }

    companion object {
        /** Raise when the list of collected data changes, so that everyone is asked again. */
        const val VERSION = 1
        private const val TYPE_CONSENT = "consent"
        private const val EXTRA_NEXT = "next"

        private val ITEMS = listOf(
            R.drawable.ic_syria_trip_car to R.string.syria_consent_recordings,
            R.drawable.ic_syria_live to R.string.syria_consent_live,
            R.drawable.ic_search to R.string.syria_consent_searches,
            R.drawable.ic_download to R.string.syria_consent_maps,
            R.drawable.ic_phone_android to R.string.syria_consent_device,
        )

        private var askedThisRun = false

        @JvmStatic
        fun isGiven(context: Context): Boolean = SyriaPrefs.consentVersion(context) >= VERSION

        /**
         * Opens the consent screen when the user has not agreed yet, and [next] after they agree. Returns true
         * when it did: the caller then closes itself.
         */
        @JvmStatic
        fun redirectIfNeeded(activity: Activity, next: Intent?): Boolean {
            if (isGiven(activity)) return false
            activity.startActivity(Intent(activity, ConsentActivity::class.java).putExtra(EXTRA_NEXT, next))
            return true
        }

        /** Map screen: asks a signed-in user who has not agreed yet, once per app start. */
        @JvmStatic
        fun askSignedInUser(activity: Activity) {
            if (askedThisRun || !SyriaPrefs.isLoggedIn(activity)) return
            askedThisRun = true
            redirectIfNeeded(activity, null)
        }
    }
}
