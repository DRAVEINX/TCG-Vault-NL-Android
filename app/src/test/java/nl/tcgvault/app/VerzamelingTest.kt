package nl.tcgvault.app

import nl.tcgvault.app.data.Kaartspel
import nl.tcgvault.app.data.Verzamelkaart
import nl.tcgvault.app.data.eigenWaardeCent
import nl.tcgvault.app.data.variantSleutel
import nl.tcgvault.app.data.zonderPrijs
import nl.tcgvault.app.scan.KaartTekstParser
import org.junit.Assert.assertEquals
import org.junit.Test

class VerzamelingTest {
    @Test fun dubbelenTellenMeeBijDeEigenWaarde() {
        val kaarten = listOf(
            Verzamelkaart(spel = Kaartspel.POKEMON, naam = "Pikachu", aantal = 3, eigenPrijsCent = 125),
            Verzamelkaart(spel = Kaartspel.MAGIC, naam = "Forest", aantal = 2)
        )
        assertEquals(375L, eigenWaardeCent(kaarten))
        assertEquals(2, zonderPrijs(kaarten))
    }

    @Test fun taalEnConditieMakenUniekeVarianten() {
        val engels = Verzamelkaart(naam = "Pikachu", taal = "Engels", conditie = "Near Mint")
        val japans = engels.copy(taal = "Japans")
        val played = engels.copy(conditie = "Played")
        assert(engels.variantSleutel() != japans.variantSleutel())
        assert(engels.variantSleutel() != played.variantSleutel())
        assertEquals(engels.variantSleutel(), engels.copy(naam = "  PIKACHU ").variantSleutel())
    }

    @Test fun scanKanKaartnummerVinden() {
        assertEquals("025/198", KaartTekstParser.kaartnummer("Pikachu\n025/198\nHP 70"))
    }
}
