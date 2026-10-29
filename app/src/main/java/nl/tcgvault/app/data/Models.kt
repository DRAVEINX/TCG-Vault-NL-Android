package nl.tcgvault.app.data

import java.util.Locale

enum class Kaartspel(val titel: String) {
    POKEMON("Pokémon"), MAGIC("Magic: The Gathering"), ONE_PIECE("One Piece"), YUGIOH("Yu-Gi-Oh!"), RIFTBOUND("Riftbound");
    companion object { fun vanSleutel(sleutel: String) = entries.find { it.name == sleutel } ?: POKEMON }
}

val KAARTTALEN = listOf(
    "Nederlands", "Engels", "Duits", "Frans", "Spaans", "Italiaans", "Portugees",
    "Japans", "Koreaans", "Chinees (vereenvoudigd)", "Chinees (traditioneel)",
    "Indonesisch", "Thais", "Russisch", "Pools", "Tsjechisch", "Zweeds", "Deens",
    "Noors", "Fins", "Turks", "Overig (zelf invullen)"
)
val KAARTCONDITIES = listOf("Mint", "Near Mint", "Excellent", "Good", "Light Played", "Played", "Poor")

/** Één kaartvariant in de persoonlijke verzameling. Dubbelen komen in aantal. */
data class Verzamelkaart(
    val id: Long = 0L,
    val spel: Kaartspel = Kaartspel.POKEMON,
    val naam: String,
    val setNaam: String = "",
    val kaartnummer: String = "",
    val taal: String = "Engels",
    val uitvoering: String = "Normaal",
    val conditie: String = "Near Mint",
    val aantal: Int = 1,
    /** Optionele eigen inschatting; nooit presenteren als actuele Cardmarket-prijs. */
    val eigenPrijsCent: Long? = null,
    val notitie: String = "",
    val laatstToegevoegd: Long = System.currentTimeMillis(),
    val scanAantal: Int = 0,
    /** Alleen een HTTPS-afbeelding uit een toegestane catalogus, optioneel. */
    val afbeeldingUrl: String = "",
    val catalogusBron: String = "",
    val catalogusId: String = "",
)

/** Combineert twee identieke varianten zonder aantallen of notities te verliezen. */
fun combineerVarianten(bestaand: Verzamelkaart, bewerkt: Verzamelkaart): Verzamelkaart {
    require(bestaand.variantSleutel() == bewerkt.variantSleutel()) { "Deze kaarten zijn niet dezelfde variant." }
    val som = bestaand.aantal.toLong() + bewerkt.aantal.toLong()
    require(som in 1..100_000) { "Te veel kaarten om samen te voegen." }
    val notities = listOf(bestaand.notitie.trim(), bewerkt.notitie.trim())
        .filter { it.isNotBlank() }.distinct().joinToString("\n")
    return bewerkt.copy(
        id = bestaand.id,
        aantal = som.toInt(),
        notitie = notities,
        laatstToegevoegd = maxOf(bestaand.laatstToegevoegd, bewerkt.laatstToegevoegd),
        afbeeldingUrl = bewerkt.afbeeldingUrl.ifBlank { bestaand.afbeeldingUrl },
        catalogusBron = bewerkt.catalogusBron.ifBlank { bestaand.catalogusBron },
        catalogusId = bewerkt.catalogusId.ifBlank { bestaand.catalogusId }
    )
}

/** Zichtbaar overzicht van eerder bevestigde scanmomenten. */
data class ScanMoment(
    val kaartId: Long,
    val kaartNaam: String,
    val spel: Kaartspel,
    val gescandOp: Long,
    val herkendeTekst: String = ""
)

fun Verzamelkaart.variantSleutel() = listOf(
    spel.name, naam, setNaam, kaartnummer, taal, uitvoering, conditie
).joinToString("|") { it.trim().lowercase(Locale.ROOT) }

/** Handmatig ingevoerde prijzen hebben expliciet een andere herkomst dan marktprijzen. */
interface MarktprijsBron {
    suspend fun zoekPrijs(kaart: Verzamelkaart): Marktprijs?
}

data class Marktprijs(val cent: Long, val bron: String, val opgehaaldOp: Long)

/** Een live Cardmarket-feed is niet geconfigureerd; er worden geen prijzen verzonnen. */
class GeenMarktprijsBron : MarktprijsBron {
    override suspend fun zoekPrijs(kaart: Verzamelkaart): Marktprijs? = null
}

fun eigenWaardeCent(kaarten: List<Verzamelkaart>): Long =
    kaarten.sumOf { kaart -> (kaart.eigenPrijsCent ?: 0L) * kaart.aantal }

fun zonderPrijs(kaarten: List<Verzamelkaart>): Int =
    kaarten.filter { it.eigenPrijsCent == null }.sumOf { it.aantal }
