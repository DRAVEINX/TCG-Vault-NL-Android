package nl.tcgvault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import nl.tcgvault.app.ui.VaultApp
import nl.tcgvault.app.ui.VaultThema

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VaultThema { VaultApp() } }
    }
}
