package nl.tcgvault.app.scan

import java.text.Normalizer
import java.util.Locale

/**
 * Uitlegbare tekstmatch, géén beeldherkenningszekerheid of garantie op druk/foil.
 * Deze logica is op de JVM te testen zonder netwerk of Android API's.
 */
data class MatchBeoordeling(
    val punten: Int,
    val label: String,
    val uitleg: String
)

object KaartMatchLogica {
    private val nummerFractie = Regex("""(?<!\d)(\d{1,4})\s*/\s*(\d{1,4})(?!\d)""")
    private val onePiece = Regex("""(?<![A-Z0-9])(?:OP|ST|EB|PRB|P)\s*[-–]?\s*\d{1,3}\s*[-–]\s*\d{3,4}(?![A-Z0-9])""")
    private val yuGiOh = Regex("""(?<![A-Z0-9])[A-Z]{2,6}\d{0,3}-[A-Z0-9]{3,6}(?![A-Z0-9])""")

    private fun standaard(tekst: String): String =
        Normalizer.normalize(tekst, Normalizer.Form.NFKC)
            .uppercase(Locale.ROOT)
            .replace('–', '-')
            .replace('—', '-')
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun code(tekst: String): String = standaard(tekst).replace(Regex("""\s*-\s*"""), "-")

    fun gevondenKaartcode(tekst: String, spel: nl.tcgvault.app.data.Kaartspel): String? {
        val t = code(tekst)
        val patroon = when (spel) {
            nl.tcgvault.app.data.Kaartspel.ONE_PIECE -> onePiece
            nl.tcgvault.app.data.Kaartspel.YUGIOH -> yuGiOh
            else -> return null
        }
        return patroon.find(t)?.value?.replace(" ", "")
    }

    fun gevondenNummer(tekst: String): String? =
        nummerFractie.find(standaard(tekst))?.groupValues?.get(1)?.trimStart('0')
            ?.ifBlank { "0" }

    fun beoordeel(kandidaat: KaartKandidaat, herkend: String): MatchBeoordeling {
        val t = standaard(herkend)
        val naam = standaard(kandidaat.naam)
        val naamMatch = naam.length >= 2 &&
            (t.contains(naam) || herkend.lineSequence().any { regel ->
                val r = standaard(regel)
                r.length >= naam.length && r.contains(naam)
            })
        val volCode = gevondenKaartcode(herkend, kandidaat.spel)
        val kaartCode = code(kandidaat.nummer).replace(" ", "")
        val codeMatch = volCode != null && kaartCode.isNotBlank() && volCode == kaartCode
        val codeConflict = volCode != null && kaartCode.isNotBlank() && !codeMatch &&
            (kandidaat.spel == nl.tcgvault.app.data.Kaartspel.ONE_PIECE ||
                kandidaat.spel == nl.tcgvault.app.data.Kaartspel.YUGIOH)
        val lokaalNummer = gevondenNummer(herkend)
        val kandidaatNummer = Regex("""^\d{1,4}""").find(kaartCode)?.value
            ?.trimStart('0')?.ifBlank { "0" }
        val nummerMatch = lokaalNummer != null && kandidaatNummer == lokaalNummer
        val nummerConflict = lokaalNummer != null && kandidaatNummer != null && !nummerMatch

        val score = (if (naamMatch) 60 else 0) +
            (if (codeMatch) 160 else 0) +
            (if (nummerMatch) 90 else 0) -
            (if (codeConflict) 140 else 0) -
            (if (nummerConflict) 45 else 0) -
            (if (naam.isBlank()) 100 else 0)

        val (label, uitleg) = when {
            codeMatch && naamMatch -> "Sterke tekstmatch" to "Naam en volledige kaartcode komen overeen."
            codeMatch -> "Kaartcode gevonden" to "Volledige kaartcode komt overeen; controleer de naam."
            nummerMatch && naamMatch -> "Goede tekstmatch" to "Naam en kaartnummer komen overeen; controleer de set."
            nummerMatch -> "Nummer komt overeen" to "Controleer vooral naam, set en uitvoering."
            codeConflict || nummerConflict -> "Nummer wijkt af" to "De gevonden kaartcode of het nummer is anders."
            naamMatch -> "Naam komt overeen" to "De editie kan afwijken; controleer set en nummer."
            else -> "Zelf controleren" to "Geen duidelijke tekstovereenkomst; vergelijk afbeelding en gegevens."
        }
        return MatchBeoordeling(score, label, uitleg)
    }
}
