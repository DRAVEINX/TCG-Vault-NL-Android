package nl.tcgvault.app.data

import android.app.Application
import android.net.Uri
import java.io.ByteArrayOutputStream
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KaartViewModel(application: Application) : AndroidViewModel(application) {
    private val database = KaartDatabase(application)
    private val _kaarten = MutableStateFlow<List<Verzamelkaart>>(emptyList())
    val kaarten = _kaarten.asStateFlow()
    private val _scans = MutableStateFlow<List<ScanMoment>>(emptyList())
    val scans = _scans.asStateFlow()
    private val _historie = MutableStateFlow<List<ScanMoment>>(emptyList())
    val historie = _historie.asStateFlow()
    private val _melding = MutableStateFlow<String?>(null)
    val melding = _melding.asStateFlow()

    init { vernieuw() }

    fun vernieuw() = viewModelScope.launch {
        val overzichten = withContext(Dispatchers.IO) {
            Triple(database.alleKaarten(), database.recenteScans(12), database.recenteScans(500))
        }
        _kaarten.value = overzichten.first
        _scans.value = overzichten.second
        _historie.value = overzichten.third
    }

    fun voegToe(kaart: Verzamelkaart, tekst: String? = null) = viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) { database.voegToe(kaart, tekst) }
            vernieuw()
            _melding.value = "Kaart toegevoegd aan je collectie"
        } catch (e: Exception) { _melding.value = "Opslaan mislukt: ${e.localizedMessage ?: "onbekende fout"}" }
    }

    fun werkKaartBij(kaart: Verzamelkaart) = viewModelScope.launch {
        try {
            val samengevoegd = withContext(Dispatchers.IO) { database.bewerkKaart(kaart) }
            vernieuw()
            _melding.value = if (samengevoegd) {
                "Kaart bijgewerkt. Identieke varianten en hun scanmomenten samengevoegd."
            } else {
                "Kaartgegevens bijgewerkt."
            }
        } catch (e: Exception) {
            _melding.value = "Bewerken mislukt: " + (e.localizedMessage ?: "Onbekende fout")
        }
    }

    fun wijzigAantal(id: Long, aantal: Int) = viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) { database.pasAantalAan(id, aantal) }
            vernieuw()
        } catch (e: Exception) { _melding.value = "Wijzigen mislukt" }
    }

    fun exporteerBackup(uri: Uri) = viewModelScope.launch {
        try {
            val aantal = withContext(Dispatchers.IO) {
                val json = database.maakBackup()
                val stream = getApplication<Application>().contentResolver.openOutputStream(uri, "w")
                    ?: error("Het gekozen bestand kan niet worden geopend.")
                stream.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
                database.alleKaarten().sumOf { it.aantal }
            }
            _melding.value = "Back-up opgeslagen: " + aantal + " kaarten."
        } catch (e: Exception) {
            _melding.value = "Back-up maken mislukt: " + (e.localizedMessage ?: "onbekende fout")
        }
    }

    /** Importeert alleen gevalideerde JSON. Alle kaarten en OCR-scans worden vervangen. */
    fun importeerBackup(uri: Uri) = viewModelScope.launch {
        try {
            val hersteld = withContext(Dispatchers.IO) {
                val input = getApplication<Application>().contentResolver.openInputStream(uri)
                    ?: error("Het gekozen bestand kan niet worden gelezen.")
                val bytes = ByteArrayOutputStream()
                input.use { stream ->
                    val buffer = ByteArray(8192)
                    var totaal = 0
                    while (true) {
                        val gelezen = stream.read(buffer)
                        if (gelezen < 0) break
                        totaal += gelezen
                        require(totaal <= CollectieBackup.MAX_BESTAND_TEKENS) {
                            "Het bestand is te groot (maximaal 16 MB)."
                        }
                        bytes.write(buffer, 0, gelezen)
                    }
                }
                val inhoud = CollectieBackup.lees(bytes.toString("UTF-8"))
                database.herstelBackup(inhoud)
                inhoud.kaarten.sumOf { it.aantal }
            }
            vernieuw()
            _melding.value = hersteld.toString() + " kaarten en hun scanhistorie teruggezet."
        } catch (e: Exception) {
            _melding.value = "Back-up herstellen mislukt. Je bestaande collectie is behouden. " +
                (e.localizedMessage ?: "Controleer het back-upbestand.")
        }
    }

    fun wisMelding() { _melding.value = null }
    override fun onCleared() { database.close(); super.onCleared() }
}
