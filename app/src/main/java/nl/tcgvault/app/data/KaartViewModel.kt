package nl.tcgvault.app.data

import android.app.Application
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
    private val _melding = MutableStateFlow<String?>(null)
    val melding = _melding.asStateFlow()

    init { vernieuw() }

    fun vernieuw() = viewModelScope.launch {
        _kaarten.value = withContext(Dispatchers.IO) { database.alleKaarten() }
    }

    fun voegToe(kaart: Verzamelkaart, tekst: String? = null) = viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) { database.voegToe(kaart, tekst) }
            vernieuw()
            _melding.value = "Kaart toegevoegd aan je collectie"
        } catch (e: Exception) { _melding.value = "Opslaan mislukt: ${e.localizedMessage ?: "onbekende fout"}" }
    }

    fun wijzigAantal(id: Long, aantal: Int) = viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) { database.pasAantalAan(id, aantal) }
            vernieuw()
        } catch (e: Exception) { _melding.value = "Wijzigen mislukt" }
    }

    fun wisMelding() { _melding.value = null }
    override fun onCleared() { database.close(); super.onCleared() }
}
