package nl.tcgvault.app.scan

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.tcgvault.app.data.Kaartspel
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Zoek in openbare kaartcatalogi na OCR. Een match is NOOIT een gegarandeerde
 * exacte druk/taal/uitvoering. Laat de gebruiker de kandidaat bevestigen.
 * Geen marktprijzen of Cardmarket-claims worden vanuit deze catalogi overgenomen.
 */
data class KaartKandidaat(
    val spel: Kaartspel,
    val naam: String,
    val setNaam: String = "",
    val nummer: String = "",
    val taal: String = "Engels",
    val bron: String,
    val bronId: String = "",
    val afbeeldingUrl: String = ""
)

data class ZoekUitkomst(val kandidaten: List<KaartKandidaat>, val bericht: String? = null)

object KaartZoeker {
    private val uitgesloten = setOf(
        "pokemon", "pokémon", "basic", "stage", "energy", "trainer", "ability",
        "illustrator", "illustration", "common", "rare", "uncommon", "attack",
        "damage", "copyright", "creature", "instant", "sorcery", "artifact",
        "battle", "leader", "character", "counter", "trigger", "spell", "unit",
        "riot games", "bandai", "konami", "weakness", "resistance", "retreat"
    )
    private val onePieceCode = Regex("""\b(?:OP|ST|EB|PRB|P)\d{1,3}-\d{3}\b""", RegexOption.IGNORE_CASE)
    private val ygoCode = Regex("""\b[A-Z]{2,6}-[A-Z0-9]{3,6}\b""")
    private val nummer = Regex("""\b\d{1,4}\s*/\s*\d{1,4}\b""")

    /** De herkenbare schrijfstijl bepaalt de eerste Pokémon-catalogustaal. */
    fun catalogusTaal(tekst: String): Pair<String, String> = when {
        Regex("""[\u3040-\u30ff]""").containsMatchIn(tekst) -> "ja" to "Japans"
        Regex("""[\uac00-\ud7a3]""").containsMatchIn(tekst) -> "ko" to "Koreaans"
        Regex("""[\u3400-\u9fff]""").containsMatchIn(tekst) -> "zh-tw" to "Chinees (traditioneel)"
        else -> "en" to "Engels"
    }

    fun naamSuggesties(ocr: String): List<String> {
        return ocr.lineSequence()
            .map { it.trim().replace(Regex("""\s+"""), " ") }
            .filter { it.length in 3..48 && it.any(Char::isLetter) && it.count(Char::isDigit) < it.length / 2 }
            .filter { regel ->
                val laag = regel.lowercase(Locale.ROOT)
                uitgesloten.none { laag == it || laag.startsWith(it + " ") } &&
                    !regel.contains("©") && !regel.contains("™") &&
                    !regel.contains("http") && !regel.contains("/")
            }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .take(5).toList()
    }

    /** Score op kaartnummer en naam, om verwisselingen van herdrukken te verminderen. */
    fun score(kandidaat: KaartKandidaat, ocr: String): Int {
        val tekst = ocr.lowercase(Locale.ROOT)
        val naam = kandidaat.naam.lowercase(Locale.ROOT)
        val gevondenNummer = nummer.find(ocr)?.value?.substringBefore("/")?.trimStart('0')
        val kaartNummer = kandidaat.nummer.substringBefore("/").trimStart('0')
        val codeMatch = kandidaat.nummer.length >= 5 && tekst.contains(kandidaat.nummer.lowercase(Locale.ROOT))
        return (if (codeMatch) 120 else 0) +
            (if (gevondenNummer != null && kaartNummer == gevondenNummer) 65 else 0) +
            (if (naam.isNotBlank() && tekst.contains(naam)) 30 else 0) -
            (if (kandidaat.naam.isBlank()) 100 else 0)
    }

    suspend fun zoek(spel: Kaartspel, ocr: String): ZoekUitkomst = withContext(Dispatchers.IO) {
        val namen = naamSuggesties(ocr)
        if (namen.isEmpty() && spel != Kaartspel.YUGIOH) {
            return@withContext ZoekUitkomst(emptyList(), "Geen bruikbare kaartnaam gevonden. Controleer de foto of voer de kaart handmatig in.")
        }
        try {
            val resultaten = when (spel) {
                Kaartspel.POKEMON -> pokemon(namen, ocr)
                Kaartspel.MAGIC -> magic(namen)
                Kaartspel.YUGIOH -> yugioh(namen, ocr)
                Kaartspel.ONE_PIECE -> onePiece(namen, ocr)
                Kaartspel.RIFTBOUND -> riftbound(namen)
            }
            val uniek = resultaten.distinctBy {
                listOf(it.spel.name, it.naam, it.setNaam, it.nummer, it.taal)
                    .joinToString("|").lowercase(Locale.ROOT)
            }.sortedByDescending { score(it, ocr) }.take(12)
            ZoekUitkomst(
                uniek,
                if (uniek.isEmpty()) "Geen zekere catalogusmatch gevonden. Gebruik handmatige invoer of probeer een duidelijkere foto." else null
            )
        } catch (e: Exception) {
            ZoekUitkomst(emptyList(), "De kaartcatalogus is niet bereikbaar. Controleer je internetverbinding of voeg handmatig toe.")
        }
    }

    private fun pokemon(namen: List<String>, ocr: String): List<KaartKandidaat> {
        val bekendNummer = nummer.find(ocr)?.value?.substringBefore("/")?.trimStart('0')
        val (taalCode, taalNaam) = catalogusTaal(ocr)
        val totaal = mutableListOf<KaartKandidaat>()
        for (naam in namen.take(3)) {
            val url = "https://api.tcgdex.net/v2/" + taalCode + "/cards?name=" + Uri.encode(naam) + "&pagination:itemsPerPage=30"
            val lijst = haalArray(url)
            for (i in 0 until minOf(30, lijst.length())) {
                val kaart = lijst.optJSONObject(i) ?: continue
                val id = kaart.optString("id")
                val lokaal = kaart.optString("localId")
                val set = id.substringBeforeLast("-", "")
                val basis = kaart.optString("image")
                val afbeelding = if (basis.startsWith("https://assets.tcgdex.net/")) "$basis/low.webp" else ""
                totaal.add(KaartKandidaat(Kaartspel.POKEMON, kaart.optString("name"), set, lokaal, taal = taalNaam, bron = "TCGdex", bronId = id, afbeeldingUrl = afbeelding))
            }
            if (totaal.isNotEmpty()) break
        }
        return totaal.sortedWith(compareByDescending<KaartKandidaat> {
            bekendNummer != null && it.nummer.trimStart('0') == bekendNummer
        }.thenBy { it.naam.length })
    }

    private fun magic(namen: List<String>): List<KaartKandidaat> {
        // Eerst de kaartnaam benaderen, daarna verschillende fysieke drukken aanbieden.
        // Zonder deze stap kan een willekeurige Scryfall-druk onterecht exact lijken.
        for (voorstel in namen.take(3)) {
            val kaart = haalObject("https://api.scryfall.com/cards/named?fuzzy=" + Uri.encode(voorstel))
            if (kaart.optString("object") != "card") continue
            val naam = kaart.optString("name")
            val zoekQuery = "!\"" + naam.replace("\"", "") + "\" game:paper"
            val drukken = haalObject(
                "https://api.scryfall.com/cards/search?q=" + Uri.encode(zoekQuery) + "&unique=prints&order=released"
            ).optJSONArray("data")
            val uitgaven = drukken?.let { lijst ->
                (0 until minOf(lijst.length(), 24)).mapNotNull { lijst.optJSONObject(it) }
            } ?: listOf(kaart)
            return uitgaven.map { item ->
                val afbeeldingen = item.optJSONObject("image_uris")
                    ?: item.optJSONArray("card_faces")?.optJSONObject(0)?.optJSONObject("image_uris")
                val afbeelding = afbeeldingen?.optString("small").orEmpty()
                KaartKandidaat(
                    Kaartspel.MAGIC, item.optString("name"), item.optString("set_name"),
                    item.optString("collector_number"),
                    taal = when (item.optString("lang")) {
                        "ja" -> "Japans"; "ko" -> "Koreaans"; "zhs" -> "Chinees (vereenvoudigd)"
                        "zht" -> "Chinees (traditioneel)"; "de" -> "Duits"; "fr" -> "Frans"
                        else -> "Engels"
                    },
                    bron = "Scryfall", bronId = item.optString("id"),
                    afbeeldingUrl = if (afbeelding.startsWith("https://cards.scryfall.io/")) afbeelding else ""
                )
            }
        }
        return emptyList()
    }

    private fun yugioh(namen: List<String>, ocr: String): List<KaartKandidaat> {
        val setCode = ygoCode.find(ocr.uppercase(Locale.ROOT))?.value
        if (setCode != null) {
            val kaart = haalObject("https://db.ygoprodeck.com/api/v7/cardsetsinfo.php?setcode=" + Uri.encode(setCode))
            val naam = kaart.optString("name")
            if (naam.isNotBlank()) {
                return listOf(KaartKandidaat(
                    Kaartspel.YUGIOH, naam, kaart.optString("set_name"),
                    kaart.optString("set_code", setCode), bron = "YGOPRODeck",
                    bronId = kaart.optString("id")
                ))
            }
        }
        for (naam in namen.take(3)) {
            val kaarten = haalObject("https://db.ygoprodeck.com/api/v7/cardinfo.php?fname=" + Uri.encode(naam))
                .optJSONArray("data") ?: continue
            if (kaarten.length() == 0) continue
            val kandidaten = mutableListOf<KaartKandidaat>()
            for (i in 0 until minOf(8, kaarten.length())) {
                val card = kaarten.optJSONObject(i) ?: continue
                val sets = card.optJSONArray("card_sets")
                val set = sets?.optJSONObject(0)
                kandidaten.add(KaartKandidaat(
                    Kaartspel.YUGIOH, card.optString("name"),
                    set?.optString("set_name").orEmpty(),
                    set?.optString("set_code").orEmpty(),
                    bron = "YGOPRODeck", bronId = card.optString("id")
                ))
            }
            if (kandidaten.isNotEmpty()) return kandidaten
        }
        return emptyList()
    }

    private fun onePiece(namen: List<String>, ocr: String): List<KaartKandidaat> {
        val code = onePieceCode.find(ocr.uppercase(Locale.ROOT))?.value
        for (naam in namen.take(3)) {
            val data = haalObject("https://optcg-api.ryanmichaelhirst.us/api/v1/cards?search=" +
                Uri.encode(naam) + "&per_page=30").optJSONArray("data") ?: continue
            val kandidaten = mutableListOf<KaartKandidaat>()
            for (i in 0 until minOf(data.length(), 30)) {
                val kaart = data.optJSONObject(i) ?: continue
                val naamKaart = kaart.optString("name")
                if (naamKaart.isNotBlank()) {
                    kandidaten.add(KaartKandidaat(
                        Kaartspel.ONE_PIECE, naamKaart, kaart.optString("set"),
                        kaart.optString("code"), bron = "OPTCG API", bronId = kaart.optString("id"),
                        afbeeldingUrl = kaart.optString("image").takeIf { it.startsWith("https://") }.orEmpty()
                    ))
                }
            }
            if (kandidaten.isNotEmpty()) return kandidaten.sortedByDescending {
                code != null && it.nummer.equals(code, ignoreCase = true)
            }.take(12)
        }
        return emptyList()
    }

    private fun riftbound(namen: List<String>): List<KaartKandidaat> {
        for (naam in namen.take(3)) {
            val obj = haalObject("https://openrift.app/api/v1/search?q=" + Uri.encode(naam) + "&limit=15")
            val data = obj.opt("data")
            val lijst = when (data) {
                is JSONArray -> data
                is JSONObject -> data.optJSONArray("cards") ?: data.optJSONArray("results") ?: data.optJSONArray("items") ?: JSONArray()
                else -> obj.optJSONArray("cards") ?: obj.optJSONArray("results") ?: JSONArray()
            }
            val kandidaten = mutableListOf<KaartKandidaat>()
            for (i in 0 until minOf(15, lijst.length())) {
                val kaart = lijst.optJSONObject(i) ?: continue
                val naamKaart = kaart.optString("name", kaart.optString("title"))
                if (naamKaart.isNotBlank()) {
                    kandidaten.add(KaartKandidaat(
                        Kaartspel.RIFTBOUND, naamKaart, kaart.optString("setName", kaart.optString("set")),
                        kaart.optString("number", kaart.optString("collectorNumber")),
                        bron = "OpenRift", bronId = kaart.optString("slug", kaart.optString("id"))
                    ))
                }
            }
            if (kandidaten.isNotEmpty()) return kandidaten
        }
        return emptyList()
    }

    private fun haalObject(url: String): JSONObject = try { JSONObject(haalJson(url)) }
        catch (_: org.json.JSONException) { JSONObject() }

    private fun haalArray(url: String): JSONArray = try { JSONArray(haalJson(url)) }
        catch (_: org.json.JSONException) { JSONArray() }

    private fun haalJson(url: String): String {
        require(url.startsWith("https://")) { "Alleen HTTPS" }
        val verbinding = URL(url).openConnection() as HttpURLConnection
        verbinding.connectTimeout = 7000
        verbinding.readTimeout = 7000
        verbinding.setRequestProperty("Accept", "application/json")
        verbinding.setRequestProperty("User-Agent", "TCGVaultNL/0.3 (catalog search; github.com/DRAVEINX/TCG-Vault-NL-Android)")
        try {
            if (verbinding.responseCode !in 200..299) return "{}"
            return verbinding.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally { verbinding.disconnect() }
    }
}
