package app.organicmaps.syria

import android.content.Context
import android.content.pm.PackageManager
import app.organicmaps.sdk.util.log.Logger
import app.organicmaps.syria.tracking.SyncWorker
import app.organicmaps.syria.tracking.TrackEvent
import app.organicmaps.syria.tracking.TrackingDb
import java.util.UUID
import org.json.JSONObject

/**
 * Shared place links on our own domain: they open this app when it is installed (an Android App Link, see
 * the manifest and the website's .well-known/assetlinks.json) and our landing page with the download
 * buttons otherwise. The core still builds an omaps.app ge0 link; it is rewritten when sharing and back when
 * opening, so the ge0 code and its parser stay the core's.
 */
object SyriaLinks {
    private const val TAG = "SyriaLinks"
    private const val HOST = "organicmaps.openstreetmap.sy"
    private const val CORE_PREFIX = "https://omaps.app/"
    private const val CORE_GET_APP = "https://omaps.app/get"
    const val SHARE_PREFIX = "https://$HOST/l/"
    const val DOWNLOAD_URL = "https://$HOST/#download"

    /** A first open from a shared link this soon after installing counts as an install from that link. */
    const val ATTRIBUTION_WINDOW_MS = 24 * 60 * 60_000L
    private const val HOUR_MS = 60 * 60_000L

    /** Replaces the omaps.app links of a shared text (plain or HTML) with ours. */
    @JvmStatic
    fun toShared(text: String): String = text
        .replace(Regex("""${Regex.escape(CORE_GET_APP)}(\?[A-Za-z0-9=&]*)?"""), DOWNLOAD_URL)
        .replace(CORE_PREFIX, SHARE_PREFIX)

    /** Our shared link as the core's omaps.app link, or null when [uri] is not one. */
    @JvmStatic
    fun toCore(uri: String): String? {
        for (scheme in arrayOf("https://", "http://")) {
            val prefix = "$scheme$HOST/l/"
            if (uri.length > prefix.length && uri.startsWith(prefix, ignoreCase = true)) {
                return CORE_PREFIX + uri.substring(prefix.length)
            }
        }
        return null
    }

    @JvmStatic
    fun shouldAttribute(installedAtMs: Long, nowMs: Long, alreadyLogged: Boolean): Boolean =
        !alreadyLogged && nowMs >= installedAtMs && nowMs - installedAtMs < ATTRIBUTION_WINDOW_MS

    /**
     * Called when the app opens one of our shared links: the first such open soon after installing records
     * that the install came from a shared link (shown in the admin as "installs from shared links").
     */
    @JvmStatic
    fun onLinkOpened(context: Context) {
        val appContext = context.applicationContext
        val installedAt = try {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).firstInstallTime
        } catch (e: PackageManager.NameNotFoundException) {
            Logger.w(TAG, "No install time", e)
            return
        }
        val now = System.currentTimeMillis()
        if (!shouldAttribute(installedAt, now, SyriaPrefs.isInstallSourceLogged(appContext))) return
        SyriaPrefs.markInstallSourceLogged(appContext)
        val payload = JSONObject()
            .put("source", "share_link")
            .put("hoursSinceInstall", (now - installedAt) / HOUR_MS)
        val event = TrackEvent(UUID.randomUUID().toString(), TYPE_INSTALL_SOURCE, now, null, payload.toString())
        Logger.i(TAG, "Install from a shared link")
        Thread {
            TrackingDb.get(appContext).insertEvent(event)
            if (SyriaPrefs.isLoggedIn(appContext)) SyncWorker.enqueueNow(appContext)
        }.start()
    }

    private const val TYPE_INSTALL_SOURCE = "install_source"
}
