package nl.tcgvault.app

import nl.tcgvault.app.data.Kaartspel
import nl.tcgvault.app.scan.KaartKandidaat
import nl.tcgvault.app.scan.KaartMatchLogica
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KaartMatchLogicaTest {
    @Test fun pokemonNaamEnFractieBeterDanNaamAlleen() {
        val juist = KaartKandidaat(Kaartspel.POKEMON, "Pikachu", "sv01", "025", bron = "TCGdex")
        val fout = juist.copy(nummer = "061")
        val scan = "Pikachu\n025 / 198"
        val a = KaartMatchLogica.beoordeel(juist, scan)
        val b = KaartMatchLogica.beoordeel(fout, scan)
        assertEquals("Goede tekstmatch", a.label)
        assertTrue(a.punten > b.punten)
        assertEquals("Nummer wijkt af", b.label)
    }

    @Test fun volledigeOnePieceCodeLeidtTotSterkeTekstmatch() {
        val juist = KaartKandidaat(Kaartspel.ONE_PIECE, "Monkey D. Luffy", "Romance Dawn", "OP01-001", bron = "OPTCG API")
        val fout = juist.copy(nummer = "OP09-001")
        val ocr = "Monkey D. Luffy\nOP01-001"
        assertEquals("OP01-001", KaartMatchLogica.gevondenKaartcode(ocr, Kaartspel.ONE_PIECE))
        assertEquals("Sterke tekstmatch", KaartMatchLogica.beoordeel(juist, ocr).label)
        assertTrue(KaartMatchLogica.beoordeel(juist, ocr).punten >
            KaartMatchLogica.beoordeel(fout, ocr).punten)
    }

    @Test fun yugiohExactSetcodeVoorAndereDruk() {
        val juist = KaartKandidaat(Kaartspel.YUGIOH, "Dark Magician", "Starter Deck Yugi", "SDY-006", bron = "YGOPRODeck")
        val fout = juist.copy(nummer = "LOB-005")
        val ocr = "Dark Magician\nSDY-006"
        assertEquals("SDY-006", KaartMatchLogica.gevondenKaartcode(ocr, Kaartspel.YUGIOH))
        assertEquals("Sterke tekstmatch", KaartMatchLogica.beoordeel(juist, ocr).label)
        assertTrue(KaartMatchLogica.beoordeel(juist, ocr).punten > KaartMatchLogica.beoordeel(fout, ocr).punten)
    }

    @Test fun ocrLeestGevouwenUnicodeEnVerschillendeStreepjes() {
        val kandidaat = KaartKandidaat(Kaartspel.ONE_PIECE, "Luffy", "OP01", "OP01-001", bron = "test")
        assertEquals("OP01-001", KaartMatchLogica.gevondenKaartcode("ＯＰ０１–００１", Kaartspel.ONE_PIECE))
        assertEquals("Kaartcode gevonden", KaartMatchLogica.beoordeel(kandidaat, "ＯＰ０１–００１").label)
    }

    @Test fun alleenNaamBewijstGeenSet() {
        val kandidaat = KaartKandidaat(Kaartspel.MAGIC, "Lightning Bolt", "Alpha", "161", bron = "Scryfall")
        assertEquals("Naam komt overeen", KaartMatchLogica.beoordeel(kandidaat, "Lightning Bolt").label)
    }

    @Test fun japanseKaartnaamVergelijken() {
        val kandidaat = KaartKandidaat(Kaartspel.POKEMON, "ピカチュウ", "sv1", "025", taal = "Japans", bron = "TCGdex")
        assertTrue(KaartMatchLogica.beoordeel(kandidaat, "ピカチュウ").punten > 0)
    }
}
