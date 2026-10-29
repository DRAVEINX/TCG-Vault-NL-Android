package nl.tcgvault.app

import nl.tcgvault.app.scan.KaartZoeker
import nl.tcgvault.app.scan.KaartKandidaat
import nl.tcgvault.app.data.Kaartspel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class KaartZoekerTest {
    @Test fun tekstregelsFilterenMerknamen() {
        val regels = KaartZoeker.naamSuggesties("Pokémon\nBASIC\nPikachu\n012/102")
        assertEquals("Pikachu", regels.first())
    }

    @Test fun herkennenVanAziatischeSchriften() {
        assertEquals("ja", KaartZoeker.catalogusTaal("ピカチュウ").first)
        assertEquals("ko", KaartZoeker.catalogusTaal("피카츄").first)
        assertEquals("zh-tw", KaartZoeker.catalogusTaal("寶可夢").first)
        assertEquals("en", KaartZoeker.catalogusTaal("Pikachu").first)
    }

    @Test fun naamSuggestiesNietLeegVoorKaartnaam() {
        assertFalse(KaartZoeker.naamSuggesties("Monkey D. Luffy\nOP01-001").isEmpty())
    }
    @Test fun kaartnummerKrijgtVoorrangBijPokemon() {
        val exact = KaartKandidaat(Kaartspel.POKEMON, "Pikachu", "base1", "025", bron = "TCGdex")
        val andereDruk = exact.copy(nummer = "031")
        val tekst = "Pikachu\n025/102"
        org.junit.Assert.assertTrue(
            KaartZoeker.score(exact, tekst) > KaartZoeker.score(andereDruk, tekst)
        )
    }

    @Test fun onePieceSetcodeTeltZwaarderMee() {
        val juist = KaartKandidaat(Kaartspel.ONE_PIECE, "Monkey D. Luffy", "Romance Dawn", "OP01-001", bron = "OPTCG API")
        val fout = juist.copy(nummer = "OP07-001")
        org.junit.Assert.assertTrue(
            KaartZoeker.score(juist, "Monkey D. Luffy OP01-001") > KaartZoeker.score(fout, "Monkey D. Luffy OP01-001")
        )
    }

    @Test fun chineseEnJapanseTaalDuidelijk() {
        assertEquals("Japans", KaartZoeker.catalogusTaal("ピカチュウ").second)
        assertEquals("Koreaans", KaartZoeker.catalogusTaal("피카츄").second)
    }

}
