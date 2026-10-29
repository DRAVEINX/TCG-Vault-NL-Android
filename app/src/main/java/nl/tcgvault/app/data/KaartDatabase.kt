package nl.tcgvault.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Lokale opslag per apparaat. Geen account, geen onbedoelde online synchronisatie. */
class KaartDatabase(context: Context) : SQLiteOpenHelper(context, "tcg_vault.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE kaarten (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                spel TEXT NOT NULL, naam TEXT NOT NULL, set_naam TEXT NOT NULL,
                nummer TEXT NOT NULL, taal TEXT NOT NULL, uitvoering TEXT NOT NULL,
                conditie TEXT NOT NULL, aantal INTEGER NOT NULL CHECK(aantal > 0),
                eigen_prijs_cent INTEGER, notitie TEXT NOT NULL,
                afbeelding_url TEXT NOT NULL DEFAULT '',
                catalogus_bron TEXT NOT NULL DEFAULT '',
                catalogus_id TEXT NOT NULL DEFAULT '',
                toegevoegd_op INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE scans (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                kaart_id INTEGER NOT NULL,
                gescand_op INTEGER NOT NULL,
                herkende_tekst TEXT NOT NULL,
                FOREIGN KEY(kaart_id) REFERENCES kaarten(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_scans_kaart_id ON scans(kaart_id)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oud: Int, nieuw: Int) {
        if (oud < 2) {
            db.execSQL("ALTER TABLE kaarten ADD COLUMN afbeelding_url TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE kaarten ADD COLUMN catalogus_bron TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE kaarten ADD COLUMN catalogus_id TEXT NOT NULL DEFAULT ''")
        }
    }
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    fun alleKaarten(): List<Verzamelkaart> {
        val lijst = mutableListOf<Verzamelkaart>()
        readableDatabase.rawQuery("""
            SELECT k.*, (SELECT COUNT(*) FROM scans s WHERE s.kaart_id = k.id) AS scan_totaal
            FROM kaarten k ORDER BY k.toegevoegd_op DESC, k.id DESC
        """.trimIndent(), null).use { cursor ->
            while (cursor.moveToNext()) lijst.add(cursor.alsKaart())
        }
        return lijst
    }

    /** Opslaan vanuit de scanner: dezelfde variant telt als een extra exemplaar. */
    fun voegToe(kaart: Verzamelkaart, herkendeTekst: String? = null): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val bestaand = alleKaarten().firstOrNull { it.variantSleutel() == kaart.variantSleutel() }
            val id = if (bestaand != null) {
                val values = ContentValues().apply {
                    put("aantal", bestaand.aantal + kaart.aantal.coerceAtLeast(1))
                    put("toegevoegd_op", System.currentTimeMillis())
                    if (kaart.eigenPrijsCent != null) put("eigen_prijs_cent", kaart.eigenPrijsCent)
                    if (kaart.afbeeldingUrl.isNotBlank()) put("afbeelding_url", kaart.afbeeldingUrl)
                    if (kaart.catalogusId.isNotBlank()) {
                        put("catalogus_bron", kaart.catalogusBron)
                        put("catalogus_id", kaart.catalogusId)
                    }
                }
                db.update("kaarten", values, "id = ?", arrayOf(bestaand.id.toString()))
                bestaand.id
            } else {
                db.insertOrThrow("kaarten", null, kaart.naarValues())
            }
            if (herkendeTekst != null) {
                val values = ContentValues().apply {
                    put("kaart_id", id)
                    put("gescand_op", System.currentTimeMillis())
                    put("herkende_tekst", herkendeTekst)
                }
                db.insertOrThrow("scans", null, values)
            }
            db.setTransactionSuccessful()
            return id
        } finally {
            db.endTransaction()
        }
    }

    /** Laat de nieuwste scanmomenten zien, maximaal 500 om de UI responsief te houden. */
    fun recenteScans(limiet: Int = 10): List<ScanMoment> {
        val lijst = mutableListOf<ScanMoment>()
        readableDatabase.rawQuery(
            """
                SELECT s.kaart_id, k.naam, k.spel, s.gescand_op, s.herkende_tekst
                FROM scans s JOIN kaarten k ON k.id = s.kaart_id
                ORDER BY s.gescand_op DESC, s.id DESC
                LIMIT ?
            """.trimIndent(),
            arrayOf(limiet.coerceIn(1, 500).toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                lijst.add(
                    ScanMoment(
                        kaartId = cursor.getLong(0),
                        kaartNaam = cursor.getString(1),
                        spel = Kaartspel.vanSleutel(cursor.getString(2)),
                        gescandOp = cursor.getLong(3),
                        herkendeTekst = cursor.getString(4)
                    )
                )
            }
        }
        return lijst
    }

    /** Neem alle scanmomenten mee bij het maken van een back-up. */
    fun alleScans(): List<BackupScan> {
        val lijst = mutableListOf<BackupScan>()
        readableDatabase.rawQuery(
            "SELECT kaart_id, gescand_op, herkende_tekst FROM scans ORDER BY id ASC", null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                lijst.add(
                    BackupScan(
                        kaartId = cursor.getLong(0),
                        gescandOp = cursor.getLong(1),
                        herkendeTekst = cursor.getString(2)
                    )
                )
            }
        }
        return lijst
    }

    fun maakBackup(): String = CollectieBackup.schrijf(alleKaarten(), alleScans())

    /**
     * Alleen aanroepen na CollectieBackup.lees(). Alles of niets: bij een fout
     * wordt de oude database door SQLite automatisch teruggezet.
     */
    fun herstelBackup(backup: BackupInhoud) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("scans", null, null)
            db.delete("kaarten", null, null)
            val ids = mutableMapOf<Long, Long>()
            for (kaart in backup.kaarten) {
                val values = kaart.naarValues()
                values.put("toegevoegd_op", kaart.laatstToegevoegd)
                val nieuwId = db.insertOrThrow("kaarten", null, values)
                check(nieuwId > 0) { "Herstellen van een kaart mislukt." }
                ids[kaart.id] = nieuwId
            }
            for (scan in backup.scans) {
                val values = ContentValues().apply {
                    put("kaart_id", ids[scan.kaartId] ?: error("Ongeldige scanverwijzing."))
                    put("gescand_op", scan.gescandOp)
                    put("herkende_tekst", scan.herkendeTekst)
                }
                db.insertOrThrow("scans", null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Corrigeer een opgeslagen kaart. Als de gecorrigeerde variant al bestaat,
     * worden aantallen EN scanmomenten in één transactie samengevoegd.
     */
    fun bewerkKaart(bewerkt: Verzamelkaart): Boolean {
        require(bewerkt.id > 0) { "Geen geldig kaart-ID." }
        require(bewerkt.naam.isNotBlank() && bewerkt.taal.isNotBlank()) { "Naam en taal zijn verplicht." }
        require(bewerkt.aantal in 1..100_000) { "Het aantal moet tussen 1 en 100.000 liggen." }
        val db = writableDatabase
        db.beginTransaction()
        try {
            val lijst = alleKaarten()
            val oorspronkelijk = lijst.firstOrNull { it.id == bewerkt.id }
                ?: error("De kaart staat niet meer in de collectie.")
            val correctie = bewerkt.copy(laatstToegevoegd = oorspronkelijk.laatstToegevoegd)
            val dezelfde = lijst.firstOrNull {
                it.id != bewerkt.id && it.variantSleutel() == correctie.variantSleutel()
            }
            if (dezelfde == null) {
                val waarden = correctie.naarValues()
                waarden.put("toegevoegd_op", oorspronkelijk.laatstToegevoegd)
                check(db.update("kaarten", waarden, "id = ?", arrayOf(bewerkt.id.toString())) == 1)
                db.setTransactionSuccessful()
                return false
            }
            val samengevoegd = combineerVarianten(dezelfde, correctie)
            val waarden = samengevoegd.naarValues()
            waarden.put("toegevoegd_op", samengevoegd.laatstToegevoegd)
            check(db.update("kaarten", waarden, "id = ?", arrayOf(dezelfde.id.toString())) == 1)
            // Scanmomenten blijven bestaan en verwijzen naar de overblijvende kaart.
            val scanWaarden = ContentValues().apply { put("kaart_id", dezelfde.id) }
            db.update("scans", scanWaarden, "kaart_id = ?", arrayOf(bewerkt.id.toString()))
            check(db.delete("kaarten", "id = ?", arrayOf(bewerkt.id.toString())) == 1)
            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    fun pasAantalAan(id: Long, nieuwAantal: Int) {
        if (nieuwAantal <= 0) {
            verwijder(id)
        } else {
            val values = ContentValues().apply { put("aantal", nieuwAantal) }
            writableDatabase.update("kaarten", values, "id = ?", arrayOf(id.toString()))
        }
    }

    fun verwijder(id: Long) {
        writableDatabase.delete("kaarten", "id = ?", arrayOf(id.toString()))
    }

    private fun Verzamelkaart.naarValues() = ContentValues().apply {
        put("spel", spel.name); put("naam", naam.trim()); put("set_naam", setNaam.trim())
        put("nummer", kaartnummer.trim()); put("taal", taal.trim()); put("uitvoering", uitvoering.trim())
        put("conditie", conditie.trim()); put("aantal", aantal.coerceAtLeast(1))
        if (eigenPrijsCent != null) put("eigen_prijs_cent", eigenPrijsCent)
        put("notitie", notitie.trim()); put("toegevoegd_op", System.currentTimeMillis())
        put("afbeelding_url", afbeeldingUrl)
        put("catalogus_bron", catalogusBron)
        put("catalogus_id", catalogusId)
    }

    private fun Cursor.alsKaart() = Verzamelkaart(
        id = getLong(getColumnIndexOrThrow("id")),
        spel = Kaartspel.vanSleutel(getString(getColumnIndexOrThrow("spel"))),
        naam = getString(getColumnIndexOrThrow("naam")),
        setNaam = getString(getColumnIndexOrThrow("set_naam")),
        kaartnummer = getString(getColumnIndexOrThrow("nummer")),
        taal = getString(getColumnIndexOrThrow("taal")),
        uitvoering = getString(getColumnIndexOrThrow("uitvoering")),
        conditie = getString(getColumnIndexOrThrow("conditie")),
        aantal = getInt(getColumnIndexOrThrow("aantal")),
        eigenPrijsCent = getColumnIndexOrThrow("eigen_prijs_cent").let { if (isNull(it)) null else getLong(it) },
        notitie = getString(getColumnIndexOrThrow("notitie")),
        laatstToegevoegd = getLong(getColumnIndexOrThrow("toegevoegd_op")),
        scanAantal = getInt(getColumnIndexOrThrow("scan_totaal")),
        afbeeldingUrl = getString(getColumnIndexOrThrow("afbeelding_url")),
        catalogusBron = getString(getColumnIndexOrThrow("catalogus_bron")),
        catalogusId = getString(getColumnIndexOrThrow("catalogus_id"))
    )
}
