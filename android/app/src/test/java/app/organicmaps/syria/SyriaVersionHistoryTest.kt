package app.organicmaps.syria

import android.app.Application
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Robolectric provides the real org.json that the parser uses.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SyriaVersionHistoryTest {
    private val json = File("src/main/res/raw/syria_version_history.json").readText()

    @Test
    fun coreVersion_dropsFlavourAndBuildType() {
        assertEquals("2026.11.27-2", SyriaVersionHistory.coreVersion("2026.11.27-2-Google-debug"))
        assertEquals("2026.11.27-12", SyriaVersionHistory.coreVersion("2026.11.27-12-Huawei"))
        assertEquals("dev", SyriaVersionHistory.coreVersion("dev"))
    }

    @Test
    fun unversionedEntry_isTheInstalledBuild() {
        val versions = SyriaVersionHistory.parse(json, "2030.01.02-3", "en")
        assertEquals("2030.01.02-3", versions.first().name)
        assertEquals(listOf(2030, 1, 2), versions.first().let { listOf(it.year, it.month, it.day) })
    }

    @Test
    fun historyFile_isConsistent() {
        val en = SyriaVersionHistory.parse(json, "2999.12.31-1", "en")
        val ar = SyriaVersionHistory.parse(json, "2999.12.31-1", "ar")
        assertEquals("Only the newest entry may have no version", 1, en.count { it.name == "2999.12.31-1" })
        assertEquals("2999.12.31-1", en.first().name)
        assertEquals("Versions are listed once", en.size, en.map { it.name }.toSet().size)
        en.zipWithNext().forEach { (newer, older) ->
            assertTrue("$newer is listed before the older $older", newer.dateMillis >= older.dateMillis)
        }
        en.zip(ar).forEach { (e, a) ->
            assertTrue("${e.name} has English notes", e.notes.isNotEmpty() && e.notes.all { it.isNotBlank() })
            assertEquals("${e.name} has as many Arabic notes as English ones", e.notes.size, a.notes.size)
        }
    }
}
