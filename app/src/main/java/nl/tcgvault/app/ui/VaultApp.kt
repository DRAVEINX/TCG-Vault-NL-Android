package nl.tcgvault.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import nl.tcgvault.app.BuildConfig
import nl.tcgvault.app.data.*
import nl.tcgvault.app.scan.KaartTekstParser
import nl.tcgvault.app.scan.KaartZoeker
import nl.tcgvault.app.scan.KaartKandidaat
import nl.tcgvault.app.scan.TekstHerkenner
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private enum class Pagina(val label: String) { START("Start"), SCAN("Scannen"), COLLECTIE("Collectie"), PORTFOLIO("Portfolio") }

private fun euro(cent: Long): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("nl-NL"))
    .format(cent / 100.0)

@Composable
fun VaultApp(vm: KaartViewModel = viewModel()) {
    val context = LocalContext.current
    val kaarten by vm.kaarten.collectAsState()
    val melding by vm.melding.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pagina by rememberSaveable { mutableStateOf(Pagina.START) }
    var spel by rememberSaveable { mutableStateOf(Kaartspel.POKEMON.name) }
    var dialoog by remember { mutableStateOf(false) }
    var ocrTekst by remember { mutableStateOf<String?>(null) }
    var matchOpties by remember { mutableStateOf<List<KaartKandidaat>>(emptyList()) }
    var toonMatches by remember { mutableStateOf(false) }
    var gekozenMatch by remember { mutableStateOf<KaartKandidaat?>(null) }
    var bezig by remember { mutableStateOf(false) }
    var scanFout by remember { mutableStateOf<String?>(null) }
    var fotoUri by remember { mutableStateOf<Uri?>(null) }

    fun verwerkAfbeelding(uri: Uri) {
        bezig = true
        scanFout = null
        scope.launch {
            try {
                val herkend = TekstHerkenner.lees(context, uri)
                ocrTekst = herkend
                val gevonden = KaartZoeker.zoek(Kaartspel.vanSleutel(spel), herkend)
                matchOpties = gevonden.kandidaten
                if (matchOpties.isNotEmpty()) {
                    toonMatches = true
                } else {
                    scanFout = gevonden.bericht
                    gekozenMatch = null
                    dialoog = true
                }
            } catch (e: Exception) {
                scanFout = "De afbeelding kon niet worden gelezen. Probeer een scherpere foto of vul de kaart handmatig in."
            } finally { bezig = false }
        }
    }

    val galerij = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) verwerkAfbeelding(uri)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { gelukt ->
        if (gelukt) fotoUri?.let(::verwerkAfbeelding)
        else scanFout = "Er is geen foto gemaakt."
    }
    fun maakFoto() {
        try {
            val map = File(context.cacheDir, "scans").apply { mkdirs() }
            val file = File.createTempFile("kaart_", ".jpg", map)
            val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
            fotoUri = uri
            camera.launch(uri)
        } catch (e: Exception) { scanFout = "De camera kon niet worden geopend. Je kunt ook een foto kiezen." }
    }
    LaunchedEffect(melding) {
        melding?.let { snackbarHost.showSnackbar(it); vm.wisMelding() }
    }

    Scaffold(
        containerColor = Achtergrond,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            Row(
                Modifier.fillMaxWidth().background(Achtergrond).padding(horizontal = 20.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Accent.copy(alpha = .16f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Style, contentDescription = null, tint = Accent, modifier = Modifier.size(26.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("TCG VAULT", color = Licht, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text("JOUW VERZAMELING • NL", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                }
                Surface(color = Paneel, shape = RoundedCornerShape(50)) {
                    Text("BÈTA 0.1", Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = Muted, fontSize = 10.sp)
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Paneel) {
                Pagina.entries.forEach { item ->
                    NavigationBarItem(
                        selected = pagina == item, onClick = { pagina = item },
                        icon = {
                            Icon(when (item) {
                                Pagina.START -> Icons.Default.Home
                                Pagina.SCAN -> Icons.Default.CameraAlt
                                Pagina.COLLECTIE -> Icons.Default.CollectionsBookmark
                                Pagina.PORTFOLIO -> Icons.Default.Share
                            }, contentDescription = item.label)
                        },
                        label = { Text(item.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Achtergrond, selectedTextColor = Accent,
                            indicatorColor = Accent, unselectedIconColor = Muted, unselectedTextColor = Muted
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (pagina == Pagina.COLLECTIE) {
                FloatingActionButton(onClick = { ocrTekst = null; gekozenMatch = null; dialoog = true }, containerColor = Accent) {
                    Icon(Icons.Default.Add, "Kaart toevoegen", tint = Achtergrond)
                }
            }
        }
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
            when (pagina) {
                Pagina.START -> StartPagina(kaarten, naarScanner = { pagina = Pagina.SCAN }, naarCollectie = { pagina = Pagina.COLLECTIE })
                Pagina.SCAN -> ScannerPagina(
                    spel = Kaartspel.vanSleutel(spel), veranderSpel = { spel = it.name },
                    bezig = bezig, fout = scanFout, maakFoto = ::maakFoto,
                    kiesFoto = { galerij.launch("image/*") }, handmatig = { ocrTekst = null; gekozenMatch = null; dialoog = true }
                )
                Pagina.COLLECTIE -> CollectiePagina(kaarten, vm::wijzigAantal)
                Pagina.PORTFOLIO -> PortfolioPagina(kaarten) {
                    val overzicht = maakPortfolioTekst(kaarten)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, overzicht)
                    }
                    context.startActivity(Intent.createChooser(intent, "Deel je collectie"))
                }
            }
        }
    }

    if (toonMatches) {
        MatchDialoog(
            opties = matchOpties,
            sluiten = { toonMatches = false },
            kies = { match ->
                gekozenMatch = match
                toonMatches = false
                dialoog = true
            },
            handmatig = {
                gekozenMatch = null
                toonMatches = false
                dialoog = true
            }
        )
    }

    if (dialoog) {
        KaartDialoog(
            spel = Kaartspel.vanSleutel(spel), herkendeTekst = ocrTekst,
            suggestie = gekozenMatch,
            sluiten = { dialoog = false; ocrTekst = null; gekozenMatch = null },
            opslaan = { kaart ->
                vm.voegToe(kaart, ocrTekst)
                dialoog = false
                ocrTekst = null
                gekozenMatch = null
                pagina = Pagina.COLLECTIE
            }
        )
    }
}

@Composable
private fun StartPagina(kaarten: List<Verzamelkaart>, naarScanner: () -> Unit, naarCollectie: () -> Unit) {
    val exemplaren = kaarten.sumOf { it.aantal }
    val dubbel = kaarten.sumOf { (it.aantal - 1).coerceAtLeast(0) }
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column {
                Text("Welkom in je vault", color = Licht, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(5.dp))
                Text("Al je TCG-kaarten, talen en dubbelen op één plek.", color = Muted, fontSize = 14.sp)
            }
        }
        item {
            Surface(color = Paneel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Euro, contentDescription = null, tint = Goud)
                        Spacer(Modifier.width(8.dp))
                        Text("JOUW OPGEGEVEN WAARDE", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(euro(eigenWaardeCent(kaarten)), color = Licht, fontSize = 37.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(7.dp))
                    Text("Gebaseerd op prijzen die je zelf invult. Geen live Cardmarket-koersen.", color = Muted, fontSize = 12.sp)
                    if (zonderPrijs(kaarten) > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text("${zonderPrijs(kaarten)} kaarten nog zonder prijs", color = Goud, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Kengetal("Kaarten", exemplaren.toString(), Accent, Modifier.weight(1f))
                Kengetal("Verschillend", kaarten.size.toString(), Licht, Modifier.weight(1f))
                Kengetal("Dubbelen", dubbel.toString(), Goud, Modifier.weight(1f))
            }
        }
        item {
            Button(onClick = naarScanner, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(Modifier.width(10.dp))
                Text("SCAN EEN KAART", fontWeight = FontWeight.Black)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Jouw spellen", color = Licht, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = naarCollectie) { Text("Bekijk alles") }
            }
        }
        items(Kaartspel.entries.toList()) { game ->
            val totaal = kaarten.filter { it.spel == game }.sumOf { it.aantal }
            Surface(color = Paneel, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = naarCollectie)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(43.dp).background(Accent.copy(alpha = .13f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Style, null, tint = Accent)
                    }
                    Spacer(Modifier.width(13.dp))
                    Text(game.titel, modifier = Modifier.weight(1f), color = Licht, fontWeight = FontWeight.SemiBold)
                    Text("$totaal kaarten", color = Muted, fontSize = 12.sp)
                }
            }
        }
        item { InfoPrijs() }
    }
}

@Composable
private fun Kengetal(titel: String, waarde: String, kleur: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Paneel, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(waarde, color = kleur, fontWeight = FontWeight.Black, fontSize = 25.sp)
            Spacer(Modifier.height(3.dp))
            Text(titel, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ScannerPagina(
    spel: Kaartspel, veranderSpel: (Kaartspel) -> Unit, bezig: Boolean, fout: String?,
    maakFoto: () -> Unit, kiesFoto: () -> Unit, handmatig: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(20.dp))
        Text("Kaart scannen", color = Licht, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(5.dp))
        Text("Maak een foto of kies een afbeelding", color = Muted)
        Spacer(Modifier.height(25.dp))
        Surface(shape = RoundedCornerShape(26.dp), color = Paneel, modifier = Modifier.fillMaxWidth().height(220.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, null, tint = Accent, modifier = Modifier.size(66.dp))
                    Spacer(Modifier.height(14.dp))
                    Text("Houd de kaart recht en scherp", color = Licht, fontWeight = FontWeight.SemiBold)
                    Text("Voorkom reflecties op folie", color = Muted, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(21.dp))
        Text("Kies eerst het kaartspel", color = Muted, modifier = Modifier.fillMaxWidth(), fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Keuzelijst("Kaartspel", spel.titel, Kaartspel.entries.map { it.titel }) { gekozen ->
            Kaartspel.entries.firstOrNull { it.titel == gekozen }?.let(veranderSpel)
        }
        Spacer(Modifier.height(19.dp))
        if (bezig) {
            CircularProgressIndicator(color = Accent)
            Spacer(Modifier.height(10.dp))
            Text("Tekst herkennen…", color = Muted)
        } else {
            Button(onClick = maakFoto, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(15.dp)) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(Modifier.width(8.dp))
                Text("Maak foto", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = kiesFoto, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(15.dp)) {
                Icon(Icons.Default.Image, null)
                Spacer(Modifier.width(8.dp))
                Text("Kies uit galerij")
            }
            TextButton(onClick = handmatig) { Text("Of voeg de kaart handmatig toe") }
        }
        if (fout != null) {
            Spacer(Modifier.height(7.dp))
            Text(fout, color = Goud, fontSize = 13.sp)
        }
        Spacer(Modifier.height(18.dp))
        Surface(color = Paneel, shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.padding(16.dp)) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Goud)
                Spacer(Modifier.width(12.dp))
                Text("De scanner herkent tekst in Latijns, Chinees, Japans en Koreaans schrift. Controleer de naam, set, taal en kaartnummer voor het opslaan. Andere talen kun je zelf invoeren.", color = Muted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun CollectiePagina(kaarten: List<Verzamelkaart>, wijzigAantal: (Long, Int) -> Unit) {
    var zoekterm by rememberSaveable { mutableStateOf("") }
    var filterSpel by rememberSaveable { mutableStateOf("Alle spellen") }
    var alleenDubbel by rememberSaveable { mutableStateOf(false) }
    val gefilterd = kaarten.filter { kaart ->
        (filterSpel == "Alle spellen" || kaart.spel.titel == filterSpel) &&
        (!alleenDubbel || kaart.aantal > 1) &&
        (zoekterm.isBlank() || listOf(kaart.naam, kaart.setNaam, kaart.kaartnummer, kaart.taal)
            .any { it.contains(zoekterm.trim(), ignoreCase = true) })
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text("Mijn collectie", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Licht)
        Text("${kaarten.sumOf { it.aantal }} kaarten · ${kaarten.size} varianten", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = zoekterm, onValueChange = { zoekterm = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Zoek kaart, set, nummer, taal…") },
            leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true,
            shape = RoundedCornerShape(15.dp)
        )
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                Keuzelijst("Filter", filterSpel, listOf("Alle spellen") + Kaartspel.entries.map { it.titel }) { filterSpel = it }
            }
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = alleenDubbel, onClick = { alleenDubbel = !alleenDubbel }, label = { Text("Dubbelen") })
        }
        Spacer(Modifier.height(12.dp))
        if (gefilterd.isEmpty()) {
            LeegScherm(if (kaarten.isEmpty()) "Nog geen kaarten toegevoegd" else "Geen kaarten gevonden", "Gebruik Scannen of de + knop om kaarten toe te voegen.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                items(gefilterd, key = { it.id }) { kaart ->
                    KaartRij(kaart, wijzigAantal)
                }
            }
        }
    }
}

@Composable
private fun KaartRij(kaart: Verzamelkaart, wijzigAantal: (Long, Int) -> Unit) {
    Surface(color = Paneel, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 54.dp, height = 76.dp).clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF27455A)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Style, contentDescription = null, tint = Accent, modifier = Modifier.size(31.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(kaart.naam, color = Licht, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(kaart.spel.titel, color = Accent, fontSize = 11.sp)
                Text(listOf(kaart.setNaam, kaart.kaartnummer, kaart.taal).filter { it.isNotBlank() }.joinToString(" · "), color = Muted, fontSize = 11.sp, maxLines = 2)
                Text("${kaart.uitvoering} · ${kaart.conditie}", color = Muted, fontSize = 11.sp)
                Text(kaart.eigenPrijsCent?.let { "Eigen prijs: ${euro(it)} / stuk" } ?: "Geen prijs ingesteld", color = Goud, fontSize = 11.sp)
                if (kaart.scanAantal > 0) Text("${kaart.scanAantal}× gescand", color = Accent, fontSize = 10.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { wijzigAantal(kaart.id, kaart.aantal - 1) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Eén kaart minder", tint = Muted, modifier = Modifier.size(17.dp))
                    }
                    Text("×${kaart.aantal}", color = Licht, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { wijzigAantal(kaart.id, kaart.aantal + 1) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Eén kaart meer", tint = Accent, modifier = Modifier.size(18.dp))
                    }
                    if (kaart.aantal > 1) {
                        Spacer(Modifier.width(4.dp))
                        Text("DUBBEL", color = Goud, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PortfolioPagina(kaarten: List<Verzamelkaart>, delen: () -> Unit) {
    val totaal = kaarten.sumOf { it.aantal }
    val dubbelen = kaarten.sumOf { (it.aantal - 1).coerceAtLeast(0) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Mijn portfolio", color = Licht, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("Jouw persoonlijke collectie om te laten zien", color = Muted)
        }
        item {
            Surface(color = Paneel, shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(68.dp).background(Accent.copy(alpha = .18f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Verified, null, tint = Accent, modifier = Modifier.size(35.dp))
                    }
                    Spacer(Modifier.height(13.dp))
                    Text("MIJN TCG VAULT", color = Licht, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("$totaal kaarten • ${kaarten.size} varianten • $dubbelen dubbelen", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    Text(euro(eigenWaardeCent(kaarten)), color = Goud, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text("Opgegeven waarde (niet live)", color = Muted, fontSize = 11.sp)
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = delen, modifier = Modifier.fillMaxWidth(), enabled = kaarten.isNotEmpty()) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Portfolio delen als tekst")
                    }
                }
            }
        }
        item { Text("Per kaartspel", color = Licht, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        items(Kaartspel.entries.toList()) { spel ->
            val spellen = kaarten.filter { it.spel == spel }
            Surface(color = Paneel, shape = RoundedCornerShape(13.dp)) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(spel.titel, Modifier.weight(1f), color = Licht, fontWeight = FontWeight.SemiBold)
                    Text("${spellen.sumOf { it.aantal }} kaarten", color = Accent)
                }
            }
        }
        item { InfoPrijs() }
        item { Text("Een openbare profiellink en online accounts komen in een volgende versie. Nu staat de collectie alleen op dit toestel.", color = Muted, fontSize = 12.sp) }
    }
}

private fun maakPortfolioTekst(kaarten: List<Verzamelkaart>): String = buildString {
    appendLine("MIJN TCG VAULT — TCG Vault NL")
    appendLine("${kaarten.sumOf { it.aantal }} kaarten, ${kaarten.size} varianten")
    appendLine("Opgegeven waarde: ${euro(eigenWaardeCent(kaarten))} (eigen invoer, geen marktprijs)")
    appendLine()
    Kaartspel.entries.forEach { spel ->
        val selectie = kaarten.filter { it.spel == spel }
        if (selectie.isNotEmpty()) {
            appendLine("${spel.titel}:")
            selectie.forEach { appendLine("• ${it.aantal}× ${it.naam} | ${it.setNaam} ${it.kaartnummer} | ${it.taal} | ${it.uitvoering}") }
            appendLine()
        }
    }
}

@Composable
private fun InfoPrijs() {
    Surface(color = Paneel, shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, Goud.copy(alpha = .28f))) {
        Row(Modifier.padding(16.dp)) {
            Icon(Icons.Default.Info, null, tint = Goud)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Over kaartprijzen", color = Licht, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Er is nog geen geautoriseerde live Cardmarket-koppeling. Alleen eigen ingevoerde prijzen worden opgeteld, niet als actuele marktwaarde getoond.", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun LeegScherm(titel: String, subtitel: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 54.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.CollectionsBookmark, null, tint = Accent, modifier = Modifier.size(54.dp))
        Spacer(Modifier.height(14.dp))
        Text(titel, color = Licht, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(subtitel, color = Muted, textAlign = TextAlign.Center, fontSize = 13.sp)
    }
}

@Composable
private fun Keuzelijst(label: String, waarde: String, opties: List<String>, wijzig: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text("$label: $waarde", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            opties.forEach { optie ->
                DropdownMenuItem(text = { Text(optie) }, onClick = { wijzig(optie); open = false })
            }
        }
    }
}

@Composable
private fun KaartDialoog(
    spel: Kaartspel, herkendeTekst: String?, suggestie: KaartKandidaat?,
    sluiten: () -> Unit, opslaan: (Verzamelkaart) -> Unit
) {
    var gekozenSpel by remember(suggestie) { mutableStateOf(suggestie?.spel ?: spel) }
    var naam by remember(suggestie, herkendeTekst) { mutableStateOf(suggestie?.naam ?: herkendeTekst?.let(KaartTekstParser::naamVoorstel).orEmpty()) }
    var setNaam by remember(suggestie) { mutableStateOf(suggestie?.setNaam.orEmpty()) }
    var nummer by remember(suggestie, herkendeTekst) { mutableStateOf(suggestie?.nummer ?: herkendeTekst?.let(KaartTekstParser::kaartnummer).orEmpty()) }
    var taal by remember(suggestie) { mutableStateOf(suggestie?.taal ?: "Engels") }
    var vrijeTaal by remember { mutableStateOf("") }
    var conditie by remember { mutableStateOf("Near Mint") }
    var uitvoering by remember { mutableStateOf("Normaal") }
    var aantalTekst by remember { mutableStateOf("1") }
    var prijsTekst by remember { mutableStateOf("") }
    var notitie by remember { mutableStateOf("") }
    var fout by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = sluiten) {
        Surface(color = Paneel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column {
                Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(19.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (suggestie != null) "Kaartmatch controleren" else if (herkendeTekst != null) "Scan controleren" else "Kaart toevoegen", color = Licht, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    if (suggestie != null) Text("Voorstel via " + suggestie.bron + " • controleer set, taal en uitvoering.", color = Accent, fontSize = 12.sp)
                    Text("Controleer alle gegevens. Dezelfde variant wordt als dubbel opgeteld.", color = Muted, fontSize = 12.sp)
                    if (herkendeTekst != null) {
                        Surface(color = Achtergrond, shape = RoundedCornerShape(12.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("HERKENDE TEKST", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(herkendeTekst.take(400).ifBlank { "Geen tekst herkend — voer de kaart handmatig in." }, color = Muted, fontSize = 11.sp)
                            }
                        }
                    }
                    Keuzelijst("Spel", gekozenSpel.titel, Kaartspel.entries.map { it.titel }) { keuze ->
                        gekozenSpel = Kaartspel.entries.first { it.titel == keuze }
                    }
                    OutlinedTextField(naam, { naam = it }, label = { Text("Naam kaart *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(setNaam, { setNaam = it }, label = { Text("Set / uitbreiding") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(nummer, { nummer = it }, label = { Text("Kaartnummer") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Keuzelijst("Taal", taal, KAARTTALEN) { taal = it }
                    if (taal == "Overig (zelf invullen)") {
                        OutlinedTextField(vrijeTaal, { vrijeTaal = it }, label = { Text("Welke taal?") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    Keuzelijst("Uitvoering", uitvoering, listOf("Normaal", "Holo", "Reverse holo", "Foil", "Full Art", "Alternate Art", "Promo", "First Edition", "Overig")) { uitvoering = it }
                    Keuzelijst("Conditie", conditie, KAARTCONDITIES) { conditie = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(aantalTekst, { aantalTekst = it.filter(Char::isDigit).take(4) }, label = { Text("Aantal") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(prijsTekst, { prijsTekst = it }, label = { Text("Eigen €/stuk") }, placeholder = { Text("Optioneel") }, singleLine = true, modifier = Modifier.weight(1.5f))
                    }
                    Text("De prijs is jouw eigen invoer, geen Cardmarket-prijs.", color = Muted, fontSize = 11.sp)
                    OutlinedTextField(notitie, { notitie = it }, label = { Text("Notitie") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    if (fout != null) Text(fout!!, color = Goud, fontSize = 12.sp)
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 11.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedButton(onClick = sluiten, modifier = Modifier.weight(1f)) { Text("Annuleren") }
                    Button(onClick = {
                        val aantal = aantalTekst.toIntOrNull()
                        val prijs = prijsTekst.replace(',', '.').trim().takeIf { it.isNotEmpty() }
                        val cent = try { prijs?.toBigDecimal()?.multiply(BigDecimal(100))?.setScale(0, RoundingMode.HALF_UP)?.longValueExact() } catch (e: Exception) { null }
                        val gekozenTaal = if (taal == "Overig (zelf invullen)") vrijeTaal.trim() else taal
                        fout = when {
                            naam.isBlank() -> "Vul een kaartnaam in."
                            gekozenTaal.isBlank() -> "Vul de taal in."
                            aantal == null || aantal < 1 -> "Het aantal moet minstens 1 zijn."
                            prijs != null && (cent == null || cent < 0) -> "Vul een geldige prijs in."
                            else -> null
                        }
                        if (fout == null) opslaan(Verzamelkaart(
                            spel = gekozenSpel, naam = naam.trim(), setNaam = setNaam.trim(),
                            kaartnummer = nummer.trim(), taal = gekozenTaal,
                            uitvoering = uitvoering, conditie = conditie, aantal = aantal!!,
                            eigenPrijsCent = cent, notitie = notitie.trim()
                        ))
                    }, modifier = Modifier.weight(1.2f)) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Opslaan")
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchDialoog(
    opties: List<KaartKandidaat>,
    sluiten: () -> Unit,
    kies: (KaartKandidaat) -> Unit,
    handmatig: () -> Unit
) {
    Dialog(onDismissRequest = sluiten) {
        Surface(
            color = Paneel,
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, Lijn),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(.82f)
        ) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Accent, modifier = Modifier.size(25.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Kaart herkend?", color = Licht, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text("Kies de juiste kaart om door te gaan", color = Muted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Dit zijn catalogusvoorstellen, geen bevestigde druk of taal. " +
                        "Controleer de gegevens voordat je opslaat.",
                    color = Muted, fontSize = 12.sp, lineHeight = 18.sp
                )
                Spacer(Modifier.height(14.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    items(opties) { kandidaat ->
                        Surface(
                            color = PaneelLicht,
                            shape = RoundedCornerShape(15.dp),
                            border = BorderStroke(1.dp, Lijn),
                            modifier = Modifier.fillMaxWidth().clickable { kies(kandidaat) }
                        ) {
                            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(width = 49.dp, height = 66.dp)
                                        .background(
                                            Brush.linearGradient(listOf(spelKleur(kandidaat.spel).copy(alpha = .32f), Achtergrond)),
                                            RoundedCornerShape(8.dp)
                                        ).border(1.dp, spelKleur(kandidaat.spel).copy(alpha = .45f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Style, null, tint = spelKleur(kandidaat.spel))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(kandidaat.naam, color = Licht, fontWeight = FontWeight.Bold, maxLines = 2)
                                    Text(
                                        listOf(kandidaat.setNaam, kandidaat.nummer).filter { it.isNotBlank() }.joinToString(" · "),
                                        color = Muted, fontSize = 12.sp, maxLines = 2
                                    )
                                    Text("Bron: " + kandidaat.bron, color = Accent, fontSize = 10.sp)
                                }
                                Icon(Icons.Default.ArrowForward, null, tint = Accent)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = handmatig, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp)
                ) { Text("Mijn kaart staat er niet tussen") }
            }
        }
    }
}

private fun spelKleur(spel: Kaartspel): Color = when (spel) {
    Kaartspel.POKEMON -> Color(0xFFFFCB67)
    Kaartspel.MAGIC -> Color(0xFFAF94FF)
    Kaartspel.ONE_PIECE -> Color(0xFFFF8191)
    Kaartspel.YUGIOH -> Color(0xFF77A7FF)
    Kaartspel.RIFTBOUND -> Color(0xFF5AE4D0)
}
