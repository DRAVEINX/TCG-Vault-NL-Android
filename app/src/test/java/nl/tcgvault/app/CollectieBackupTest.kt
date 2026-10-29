package nl.tcgvault.app

import nl.tcgvault.app.data.BackupScan
import nl.tcgvault.app.data.CollectieBackup
import nl.tcgvault.app.data.Kaartspel
import nl.tcgvault.app.data.Verzamelkaart
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectieBackupTest {
    private val kaart = Verzamelkaart(
        id = 12L,
        spel = Kaartspel.POKEMON,
        naam = "Pikachu",
        setNaam = "Base Set",
        kaartnummer = "58/102",
        taal = "Japans",
        uitvoering = "Holo",
        conditie = "Near Mint",
        aantal = 3,
        eigenPrijsCent = 1234L,
        notitie = "Zelf gescand",
        laatstToegevoegd = 1_765_000_000_000L,
        scanAantal = 2,
        catalogusBron = "TCGdex",
        catalogusId = "base-58",
        afbeeldingUrl = "https://assets.tcgdex.net/base/58/low.webp"
    )

    @Test fun roundTripBewaarTalenDubbelenPrijsEnScans() {
        val scan = BackupScan(kaartId = 12L, gescandOp = 1_765_000_000_100L, herkendeTekst = "ピカチュウ")
        val backup = CollectieBackup.schrijf(listOf(kaart), listOf(scan))
        val opnieuw = CollectieBackup.lees(backup)
        assertEquals(1, opnieuw.kaarten.size)
        assertEquals(3, opnieuw.kaarten.single().aantal)
        assertEquals("Japans", opnieuw.kaarten.single().taal)
        assertEquals(1234L, opnieuw.kaarten.single().eigenPrijsCent)
        assertEquals("TCGdex", opnieuw.kaarten.single().catalogusBron)
        assertEquals(1, opnieuw.scans.size)
        assertEquals("ピカチュウ", opnieuw.scans.single().herkendeTekst)
        assertEquals(12L, opnieuw.scans.single().kaartId)
    }

    @Test fun nulPrijsBlijftEchtOnbekend() {
        val backup = CollectieBackup.schrijf(listOf(kaart.copy(eigenPrijsCent = null)), emptyList())
        val opnieuw = CollectieBackup.lees(backup)
        assertEquals(null, opnieuw.kaarten.single().eigenPrijsCent)
    }

    @Test fun verkeerdBestandWordtGeweigerd() {
        val fout = """{"formaat":"IETS_ANDERS","versie":1,"kaarten":[],"scans":[]}"""
        assertThrows(IllegalArgumentException::class.java) { CollectieBackup.lees(fout) }
    }

    @Test fun toekomstigeVersieWordtGeweigerd() {
        val json = JSONObject(CollectieBackup.schrijf(listOf(kaart), emptyList())).put("versie", 10)
        assertThrows(IllegalArgumentException::class.java) { CollectieBackup.lees(json.toString()) }
    }

    @Test fun ongeldigeScanVerwijzingWordtGeweigerd() {
        val json = JSONObject(CollectieBackup.schrijf(listOf(kaart), listOf(BackupScan(12L, 1L, "kaart"))))
        json.getJSONArray("scans").getJSONObject(0).put("kaartId", 999L)
        assertThrows(IllegalArgumentException::class.java) { CollectieBackup.lees(json.toString()) }
    }

    @Test fun dubbelKaartIdWordtGeweigerd() {
        val json = JSONObject(CollectieBackup.schrijf(listOf(kaart, kaart.copy(id = 13L)), emptyList()))
        json.getJSONArray("kaarten").getJSONObject(1).put("id", 12L)
        assertThrows(IllegalArgumentException::class.java) { CollectieBackup.lees(json.toString()) }
    }

    @Test fun geenKaartenIsEenGeldigeBackup() {
        val inhoud = CollectieBackup.lees(CollectieBackup.schrijf(emptyList(), emptyList()))
        assertTrue(inhoud.kaarten.isEmpty())
        assertFalse(inhoud.scans.isNotEmpty())
    }
}
