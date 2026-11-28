package app.organicmaps.syria

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import app.organicmaps.BuildConfig
import app.organicmaps.R
import app.organicmaps.util.Utils

/** The Syria Organic Maps part of the About page (layout/syria_about.xml). */
object SyriaAbout {
    private const val COMMUNITY_URL = "https://openstreetmap.sy/"
    private const val SOURCE_URL = "https://github.com/osm-syria/organicmaps"
    private const val NAME_AR = "خرائط سوريا"
    private const val NAME_EN = "Syria Organic Maps"

    @JvmStatic
    fun bind(root: View) {
        // The road strip at the bottom of the hero follows its rounded corners.
        root.findViewById<View>(R.id.syria_about_hero).clipToOutline = true
        // The name in the UI language first; "سوريا" and "Organic Maps" in the brand teal, as on the website.
        val arabic = ConfigurationCompat.getLocales(root.resources.configuration)[0]?.language == "ar"
        val accent = ContextCompat.getColor(root.context, R.color.syria_brand_teal_light)
        root.findViewById<TextView>(R.id.syria_about_name).text = brandName(if (arabic) NAME_AR else NAME_EN, accent)
        root.findViewById<TextView>(R.id.syria_about_name_other).text = brandName(if (arabic) NAME_EN else NAME_AR, accent)
        link(root, R.id.syria_about_website, BuildConfig.SYRIA_WEBSITE_URL)
        link(root, R.id.syria_about_community, COMMUNITY_URL)
        link(root, R.id.syria_about_source, SOURCE_URL)
        link(root, R.id.syria_about_privacy, BuildConfig.SYRIA_PRIVACY_URL)
        root.findViewById<View>(R.id.syria_about_versions).setOnClickListener { SyriaVersionHistory.show(it.context) }
    }

    /** Colours the part after the first word: "سوريا" of «خرائط سوريا», "Organic Maps" of Syria Organic Maps. */
    private fun brandName(name: String, @ColorInt accent: Int): SpannableString {
        val text = SpannableString(name)
        text.setSpan(ForegroundColorSpan(accent), name.indexOf(' ') + 1, name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return text
    }

    private fun link(root: View, @IdRes id: Int, url: String) {
        root.findViewById<View>(id).setOnClickListener { Utils.openUrl(it.context, url) }
    }
}
