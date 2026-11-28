package app.organicmaps.syria.account

import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import app.organicmaps.R
import app.organicmaps.sdk.util.log.Logger
import app.organicmaps.syria.ApiClient
import app.organicmaps.syria.ApiException
import app.organicmaps.syria.SyriaPrefs
import app.organicmaps.syria.tracking.ActivityTrackingService
import app.organicmaps.syria.tracking.LiveLocationService
import app.organicmaps.syria.tracking.TrackingDb
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.IOException

/**
 * Settings → Account → Delete account (Google Play account deletion requirement): deletes the account and all
 * its data on the server, then the recordings and queued events on this phone, and signs out.
 */
object AccountDeletion {
    private const val TAG = "SyriaAccount"

    @JvmStatic
    fun confirm(activity: FragmentActivity, onDeleted: Runnable) {
        // A running recording would keep writing to what is being deleted.
        if (ActivityTrackingService.isActive()) {
            Toast.makeText(activity, R.string.syria_delete_account_stop_recording, Toast.LENGTH_LONG).show()
            return
        }
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.syria_delete_account_title)
            .setMessage(R.string.syria_delete_account_confirm)
            .setNegativeButton(R.string.syria_cancel, null)
            .setPositiveButton(R.string.syria_delete_account_button) { _, _ -> delete(activity, onDeleted) }
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(activity, R.color.syria_sync_red))
    }

    private fun delete(activity: FragmentActivity, onDeleted: Runnable) {
        val progress = MaterialAlertDialogBuilder(activity)
            .setMessage(R.string.syria_delete_account_progress)
            .setCancelable(false)
            .show()
        val context = activity.applicationContext
        Thread {
            val error = try {
                ApiClient.deleteAccount(context)
                LiveLocationService.disable(context)
                TrackingDb.get(context).deleteAll()
                SyriaPrefs.clearSession(context)
                Logger.i(TAG, "Account deleted")
                null
            } catch (e: ApiException) {
                Logger.w(TAG, "Account deletion refused: HTTP ${e.status} ${e.message}")
                e.message ?: e.toString()
            } catch (e: IOException) {
                Logger.w(TAG, "Account deletion failed: $e")
                context.getString(R.string.syria_delete_account_offline)
            }
            activity.runOnUiThread {
                if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                progress.dismiss()
                if (error == null) onDeleted.run()
                MaterialAlertDialogBuilder(activity)
                    .setMessage(error ?: activity.getString(R.string.syria_delete_account_done))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }.start()
    }
}
