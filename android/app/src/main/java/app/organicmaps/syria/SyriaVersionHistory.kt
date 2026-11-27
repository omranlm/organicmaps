package app.organicmaps.syria

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import app.organicmaps.BuildConfig
import app.organicmaps.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.GregorianCalendar
import org.json.JSONObject

/** About > Version history: the Syria Organic Maps versions of res/raw/syria_version_history.json. */
object SyriaVersionHistory {
    data class Version(val name: String, val year: Int, val month: Int, val day: Int, val notes: List<String>) {
        val dateMillis: Long get() = GregorianCalendar(year, month - 1, day, 12, 0).timeInMillis
    }

    private val VERSION = Regex("""^(\d{4})\.(\d{2})\.(\d{2})-\d+""")

    /** The version without the flavour and build type, e.g. "2026.11.27-2" of "2026.11.27-2-Google-debug". */
    @JvmStatic
    fun coreVersion(versionName: String): String = VERSION.find(versionName)?.value ?: versionName

    /**
     * Versions, newest first, with the notes in [language] ("ar" or "en"). The entry without a version is the
     * build that contains it, so it gets [installed].
     */
    fun parse(json: String, installed: String, language: String): List<Version> {
        val versions = JSONObject(json).getJSONArray("versions")
        return (0 until versions.length()).map { i ->
            val entry = versions.getJSONObject(i)
            val name = entry.optString("version").ifEmpty { installed }
            val date = requireNotNull(VERSION.find(name)) { "Version without a date: $name" }.groupValues
            val notes = entry.getJSONArray(language)
            Version(
                name,
                date[1].toInt(),
                date[2].toInt(),
                date[3].toInt(),
                (0 until notes.length()).map { notes.getString(it) },
            )
        }
    }

    @JvmStatic
    fun show(context: Context) {
        val json = context.resources.openRawResource(R.raw.syria_version_history).bufferedReader().use { it.readText() }
        val installed = coreVersion(BuildConfig.VERSION_NAME)
        val versions = parse(json, installed, context.getString(R.string.syria_version_history_language))
        // A local build of a released commit repeats that version: badge only the newest entry.
        val installedIndex = versions.indexOfFirst { it.name == installed }
        val inflater = LayoutInflater.from(context)
        val list = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = resources.getDimensionPixelSize(R.dimen.margin_base_plus)
            setPadding(padding, padding, padding, 0)
        }
        versions.forEachIndexed { i, version ->
            val item = inflater.inflate(R.layout.item_syria_version, list, false)
            item.findViewById<TextView>(R.id.syria_version_name).text = version.name
            item.findViewById<View>(R.id.syria_version_this).visibility =
                if (i == installedIndex) View.VISIBLE else View.GONE
            item.findViewById<TextView>(R.id.syria_version_date).text = DateUtils.formatDateTime(
                context,
                version.dateMillis,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR,
            )
            item.findViewById<TextView>(R.id.syria_version_notes).text = version.notes.joinToString("\n") { "• $it" }
            // The road runs from each version to the next one.
            if (i == versions.lastIndex) item.findViewById<View>(R.id.syria_version_road).visibility = View.INVISIBLE
            list.addView(item)
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.syria_version_history)
            .setView(NestedScrollView(context).apply { addView(list) })
            .setPositiveButton(R.string.close, null)
            .show()
    }
}
