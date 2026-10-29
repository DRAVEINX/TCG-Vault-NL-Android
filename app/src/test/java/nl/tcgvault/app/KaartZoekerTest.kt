package nl.tcgvault.app

import nl.tcgvault.app.scan.KaartZoeker
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
}
