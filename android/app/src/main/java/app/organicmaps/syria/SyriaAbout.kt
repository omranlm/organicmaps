package app.organicmaps.syria

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import app.organicmaps.BuildConfig
import app.organicmaps.R
import app.organicmaps.util.Utils

/** The Syria Organic Maps part of the About page (layout/syria_about.xml). */
object SyriaAbout {
    private const val COMMUNITY_URL = "https://openstreetmap.sy/"
    private const val SOURCE_URL = "https://github.com/osm-syria/organicmaps"
    private const val BRAND_ACCENT = "Organic Maps"

    @JvmStatic
    fun bind(root: View) {
        // The road strip at the bottom of the hero follows its rounded corners.
        root.findViewById<View>(R.id.syria_about_hero).clipToOutline = true
        root.findViewById<TextView>(R.id.syria_about_name).let { name ->
            val text = SpannableString(name.text)
            val start = text.indexOf(BRAND_ACCENT)
            if (start >= 0) {
                val color = ContextCompat.getColor(name.context, R.color.syria_brand_teal_light)
                text.setSpan(ForegroundColorSpan(color), start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                name.text = text
            }
        }
        link(root, R.id.syria_about_website, BuildConfig.SYRIA_WEBSITE_URL)
        link(root, R.id.syria_about_community, COMMUNITY_URL)
        link(root, R.id.syria_about_source, SOURCE_URL)
        link(root, R.id.syria_about_privacy, BuildConfig.SYRIA_PRIVACY_URL)
        root.findViewById<View>(R.id.syria_about_versions).setOnClickListener { SyriaVersionHistory.show(it.context) }
    }

    private fun link(root: View, @IdRes id: Int, url: String) {
        root.findViewById<View>(id).setOnClickListener { Utils.openUrl(it.context, url) }
    }
}
