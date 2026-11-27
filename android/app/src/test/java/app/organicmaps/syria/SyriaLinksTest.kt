package app.organicmaps.syria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyriaLinksTest {
    private val hour = 60 * 60_000L

    @Test
    fun myPositionText_usesOurDomain() {
        val text = "موقعي الحالي\n33.51305, 36.27652\nhttps://omaps.app/o4B4pYZsRs"
        assertEquals(
            "موقعي الحالي\n33.51305, 36.27652\nhttps://organicmaps.openstreetmap.sy/l/o4B4pYZsRs",
            SyriaLinks.toShared(text),
        )
    }

    @Test
    fun htmlBody_rewritesPlaceAndGetAppLinks() {
        val html = "<a href=\"https://omaps.app/o4B4pYZsRs/Bab_Touma\">Open</a><br>\n" +
            "<a href=\"https://omaps.app/get\">Get the app</a><br>\n"
        assertEquals(
            "<a href=\"https://organicmaps.openstreetmap.sy/l/o4B4pYZsRs/Bab_Touma\">Open</a><br>\n" +
                "<a href=\"https://organicmaps.openstreetmap.sy/#download\">Get the app</a><br>\n",
            SyriaLinks.toShared(html),
        )
    }

    @Test
    fun escapedName_isKept() {
        val name = "%D8%A8%D8%A7%D8%A8_%D8%AA%D9%88%D9%85%D8%A7"
        val link = "https://omaps.app/o4B4pYZsRs/$name"
        val shared = SyriaLinks.toShared(link)
        assertEquals("https://organicmaps.openstreetmap.sy/l/o4B4pYZsRs/$name", shared)
        assertEquals(link, SyriaLinks.toCore(shared))
    }

    @Test
    fun toCore_roundTripsHttpsAndHttp() {
        assertEquals(
            "https://omaps.app/o4B4pYZsRs/Name",
            SyriaLinks.toCore("https://organicmaps.openstreetmap.sy/l/o4B4pYZsRs/Name"),
        )
        assertEquals(
            "https://omaps.app/o4B4pYZsRs",
            SyriaLinks.toCore("http://organicmaps.openstreetmap.sy/l/o4B4pYZsRs"),
        )
    }

    @Test
    fun toCore_rejectsOtherLinks() {
        assertNull(SyriaLinks.toCore("https://organicmaps.openstreetmap.sy/apps/SyriaOrganicMaps-google.apk"))
        assertNull(SyriaLinks.toCore("https://organicmaps.openstreetmap.sy/l/"))
        assertNull(SyriaLinks.toCore("https://omaps.app/o4B4pYZsRs"))
        assertNull(SyriaLinks.toCore("https://evil.example/organicmaps.openstreetmap.sy/l/o4B4pYZsRs"))
    }

    @Test
    fun attribution_onlyOnceAndWithin24Hours() {
        val installed = 1_000_000L
        assertTrue(SyriaLinks.shouldAttribute(installed, installed + hour, alreadyLogged = false))
        assertFalse(SyriaLinks.shouldAttribute(installed, installed + hour, alreadyLogged = true))
        assertFalse(SyriaLinks.shouldAttribute(installed, installed + 25 * hour, alreadyLogged = false))
        assertFalse(SyriaLinks.shouldAttribute(installed, installed - hour, alreadyLogged = false))
    }
}
