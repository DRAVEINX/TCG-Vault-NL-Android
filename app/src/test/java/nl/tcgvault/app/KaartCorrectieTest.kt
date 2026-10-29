package nl.tcgvault.app

import nl.tcgvault.app.data.Kaartspel
import nl.tcgvault.app.data.Verzamelkaart
import nl.tcgvault.app.data.combineerVarianten
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class KaartCorrectieTest {
    private val bestaand = Verzamelkaart(
        id = 7L, spel = Kaartspel.POKEMON,
        naam = "Pikachu", setNaam = "Base", kaartnummer = "025",
        taal = "Engels", uitvoering = "Normaal", conditie = "Near Mint",
        aantal = 3, eigenPrijsCent = 245L, notitie = "In map A",
        laatstToegevoegd = 100L, afbeeldingUrl = "https://assets.tcgdex.net/base/025/low.webp"
    )

    @Test fun correctieTotZelfdeVariantCombineertExemplaren() {
        val correctie = bestaand.copy(id = 9L, aantal = 2, notitie = "Nieuwe scan", laatstToegevoegd = 200L)
        val resultaat = combineerVarianten(bestaand, correctie)
        assertEquals(7L, resultaat.id)
        assertEquals(5, resultaat.aantal)
        assertEquals(200L, resultaat.laatstToegevoegd)
        assertEquals("In map A\nNieuwe scan", resultaat.notitie)
        assertTrue(resultaat.afbeeldingUrl.isNotBlank())
    }

    @Test fun identiekeNotitiesWordenNietGedupliceerd() {
        val resultaat = combineerVarianten(bestaand, bestaand.copy(id = 8L, aantal = 1))
        assertEquals("In map A", resultaat.notitie)
        assertEquals(4, resultaat.aantal)
    }

    @Test fun afwijkendeEditieMagNietAutomatischMergen() {
        assertThrows(IllegalArgumentException::class.java) {
            combineerVarianten(bestaand, bestaand.copy(id = 9L, setNaam = "Andere set"))
        }
    }

    @Test fun aantalOverMaximumWordtGeweigerd() {
        assertThrows(IllegalArgumentException::class.java) {
            combineerVarianten(bestaand.copy(aantal = 90_000), bestaand.copy(aantal = 20_000))
        }
    }

    @Test fun overschrevenWaardeKomtUitBewerkformulier() {
        val resultaat = combineerVarianten(
            bestaand, bestaand.copy(id = 10L, aantal = 1, eigenPrijsCent = 500L)
        )
        assertEquals(500L, resultaat.eigenPrijsCent)
    }
}
