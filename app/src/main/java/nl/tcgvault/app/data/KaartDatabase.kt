package nl.tcgvault.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Lokale opslag per apparaat. Geen account, geen onbedoelde online synchronisatie. */
class KaartDatabase(context: Context) : SQLiteOpenHelper(context, "tcg_vault.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE kaarten (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                spel TEXT NOT NULL, naam TEXT NOT NULL, set_naam TEXT NOT NULL,
                nummer TEXT NOT NULL, taal TEXT NOT NULL, uitvoering TEXT NOT NULL,
                conditie TEXT NOT NULL, aantal INTEGER NOT NULL CHECK(aantal > 0),
                eigen_prijs_cent INTEGER, notitie TEXT NOT NULL,
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
    override fun onUpgrade(db: SQLiteDatabase, oud: Int, nieuw: Int) = Unit
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
        scanAantal = getInt(getColumnIndexOrThrow("scan_totaal"))
    )
}
