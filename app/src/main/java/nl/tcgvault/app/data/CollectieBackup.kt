package nl.tcgvault.app.data

import org.json.JSONArray
import org.json.JSONObject

/** Een exporteerbare scan. Kaart-ID is een verwijzing binnen het back-upbestand. */
data class BackupScan(val kaartId: Long, val gescandOp: Long, val herkendeTekst: String)

data class BackupInhoud(
    val kaarten: List<Verzamelkaart>,
    val scans: List<BackupScan>
)

/**
 * Zelfstandig, leesbaar JSON-back-upformaat. Bevat géén afbeeldingbestanden,
 * wachtwoorden of accountgegevens. Behoudt wel kaarten, aantallen en OCR-scans.
 *
 * Import wordt eerst VOLLEDIG gecontroleerd; bij een fout mag de database nooit
 * worden gewist. De database-restore zelf is één SQLite-transactie.
 */
object CollectieBackup {
    private const val TYPE = "TCG_VAULT_NL_BACKUP"
    private const val VERSIE = 1
    const val MAX_BESTAND_TEKENS = 16_000_000
    private const val MAX_KAARTEN = 30_000
    private const val MAX_SCANS = 100_000

    fun schrijf(kaarten: List<Verzamelkaart>, scans: List<BackupScan>): String {
        require(kaarten.size <= MAX_KAARTEN && scans.size <= MAX_SCANS) {
            "De verzameling is te groot voor deze back-upversie."
        }
        val resultaat = JSONObject()
            .put("formaat", TYPE)
            .put("versie", VERSIE)
            .put("gemaaktOp", System.currentTimeMillis())

        val items = JSONArray()
        kaarten.forEach { kaart ->
            items.put(JSONObject()
                .put("id", kaart.id)
                .put("spel", kaart.spel.name)
                .put("naam", kaart.naam)
                .put("setNaam", kaart.setNaam)
                .put("kaartnummer", kaart.kaartnummer)
                .put("taal", kaart.taal)
                .put("uitvoering", kaart.uitvoering)
                .put("conditie", kaart.conditie)
                .put("aantal", kaart.aantal)
                .put("eigenPrijsCent", kaart.eigenPrijsCent ?: JSONObject.NULL)
                .put("notitie", kaart.notitie)
                .put("toegevoegdOp", kaart.laatstToegevoegd)
                .put("afbeeldingUrl", kaart.afbeeldingUrl)
                .put("catalogusBron", kaart.catalogusBron)
                .put("catalogusId", kaart.catalogusId)
            )
        }
        val gebeurtenissen = JSONArray()
        scans.forEach { scan ->
            gebeurtenissen.put(JSONObject()
                .put("kaartId", scan.kaartId)
                .put("gescandOp", scan.gescandOp)
                .put("herkendeTekst", scan.herkendeTekst)
            )
        }
        val json = resultaat.put("kaarten", items).put("scans", gebeurtenissen).toString(2)
        require(json.length <= MAX_BESTAND_TEKENS) {
            "De back-up is groter dan 16 MB. Neem contact op voor een uitgebreid exportformaat."
        }
        return json
    }

    fun lees(json: String): BackupInhoud {
        require(json.length <= MAX_BESTAND_TEKENS) { "Het bestand is groter dan 16 MB." }
        val root = JSONObject(json)
        require(root.optString("formaat") == TYPE) {
            "Dit is geen TCG Vault NL-back-up."
        }
        require(root.optInt("versie", -1) == VERSIE) {
            "Deze back-upversie wordt niet ondersteund."
        }
        val kaartenArray = root.getJSONArray("kaarten")
        val scansArray = root.getJSONArray("scans")
        require(kaartenArray.length() <= MAX_KAARTEN && scansArray.length() <= MAX_SCANS) {
            "De back-up bevat te veel items."
        }

        val kaarten = (0 until kaartenArray.length()).map { index ->
            val obj = kaartenArray.getJSONObject(index)
            val spel = Kaartspel.entries.firstOrNull { it.name == obj.getString("spel") }
                ?: throw IllegalArgumentException("Ongeldig kaartspel in de back-up.")
            val aantal = obj.getInt("aantal")
            require(aantal in 1..100_000) { "Ongeldig aantal kaarten." }
            val id = obj.getLong("id")
            require(id > 0) { "Ongeldig kaart-ID." }
            val prijs = if (obj.isNull("eigenPrijsCent")) null else obj.getLong("eigenPrijsCent").also {
                require(it >= 0) { "Ongeldige eigen prijs." }
            }
            Verzamelkaart(
                id = id,
                spel = spel,
                naam = veld(obj, "naam", 200).also { require(it.isNotBlank()) { "Kaartnaam ontbreekt." } },
                setNaam = veld(obj, "setNaam", 200),
                kaartnummer = veld(obj, "kaartnummer", 100),
                taal = veld(obj, "taal", 100).also { require(it.isNotBlank()) { "Kaarttaal ontbreekt." } },
                uitvoering = veld(obj, "uitvoering", 100),
                conditie = veld(obj, "conditie", 100),
                aantal = aantal,
                eigenPrijsCent = prijs,
                notitie = veld(obj, "notitie", 4000),
                laatstToegevoegd = obj.getLong("toegevoegdOp").also { require(it >= 0) },
                afbeeldingUrl = veld(obj, "afbeeldingUrl", 1500),
                catalogusBron = veld(obj, "catalogusBron", 120),
                catalogusId = veld(obj, "catalogusId", 300)
            )
        }
        val ids = kaarten.map { it.id }.toSet()
        require(ids.size == kaarten.size) { "Dubbele kaart-ID's in de back-up." }

        val scans = (0 until scansArray.length()).map { index ->
            val obj = scansArray.getJSONObject(index)
            BackupScan(
                kaartId = obj.getLong("kaartId").also {
                    require(it in ids) { "Een scan verwijst naar een ontbrekende kaart." }
                },
                gescandOp = obj.getLong("gescandOp").also { require(it >= 0) },
                herkendeTekst = veld(obj, "herkendeTekst", 10_000)
            )
        }
        return BackupInhoud(kaarten, scans)
    }

    private fun veld(obj: JSONObject, naam: String, maximaal: Int): String {
        val waarde = obj.getString(naam)
        require(waarde.length <= maximaal) { "Veld $naam is te lang." }
        return waarde
    }
}
