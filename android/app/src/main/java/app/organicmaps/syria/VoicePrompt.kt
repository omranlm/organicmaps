package app.organicmaps.syria

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import app.organicmaps.R
import app.organicmaps.sdk.sound.TtsPlayer
import app.organicmaps.sdk.util.Config
import app.organicmaps.sdk.util.log.Logger
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Voice directions need a text-to-speech voice in the app's language, which many phones do not have for
 * Arabic: directions then stay silent, or come in English. When navigation starts without one, a prompt
 * offers to download it in one tap (Android's voice data screen, or Google's speech engine when the phone
 * has none), and directions switch to it once the user is back.
 */
object VoicePrompt {
    private const val TAG = "SyriaVoice"
    private const val SNOOZE_MS = 24 * 60 * 60 * 1000L
    private const val GOOGLE_TTS = "com.google.android.tts"

    @JvmStatic
    fun onNavigationStarted(activity: AppCompatActivity) {
        val state = TtsPlayer.getState()
        // Too early to tell; navigation starts again on the next trip.
        if (state == TtsPlayer.State.INITIALIZING) return
        val language = SyriaLocale.currentLanguage(activity)
        if (isInstalled(state, language)) {
            // Back from downloading it: directions in that voice from now on.
            if (SyriaPrefs.voiceDownloadAt(activity) != 0L) {
                SyriaPrefs.setVoiceDownloadAt(activity, 0L)
                use(language)
            }
            return
        }
        val now = System.currentTimeMillis()
        if (now - SyriaPrefs.voicePromptAt(activity) < SNOOZE_MS) return
        SyriaPrefs.setVoicePromptAt(activity, now)
        val name = activity.getString(if (language == "ar") R.string.syria_language_ar else R.string.syria_language_en)
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.syria_voice_prompt_title)
            .setMessage(activity.getString(R.string.syria_voice_prompt_message, name))
            .setPositiveButton(R.string.syria_voice_prompt_download) { _, _ -> download(activity, language) }
            .setNegativeButton(R.string.syria_voice_prompt_later, null)
            .show()
    }

    private fun isInstalled(state: TtsPlayer.State, language: String): Boolean =
        state != TtsPlayer.State.UNAVAILABLE &&
            TtsPlayer.INSTANCE.refreshLanguages().any { it.locale.language == language && it.downloaded }

    private fun download(activity: AppCompatActivity, language: String) {
        val intents = listOf(
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
            // No speech engine on the phone: Google's, which has Arabic.
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GOOGLE_TTS")),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$GOOGLE_TTS")),
        )
        for (intent in intents) {
            try {
                activity.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Logger.w(TAG, "No screen for ${intent.action} ${intent.data}: ${e.message}")
                continue
            }
            SyriaPrefs.setVoiceDownloadAt(activity, System.currentTimeMillis())
            // The engine only sees the new voice once restarted: on the way back from the download screen.
            // (A new observer gets the current onResume right away, before the user has left.)
            activity.lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    private var left = false

                    override fun onStop(owner: LifecycleOwner) {
                        left = true
                    }

                    override fun onResume(owner: LifecycleOwner) {
                        if (!left) return
                        owner.lifecycle.removeObserver(this)
                        use(language)
                    }
                },
            )
            return
        }
    }

    /** Directions in [language] (if its voice is there by now: the engine falls back to another otherwise). */
    private fun use(language: String) {
        Config.TTS.setLanguage(language)
        Config.TTS.setEnabled(true)
        TtsPlayer.INSTANCE.reload()
    }
}
