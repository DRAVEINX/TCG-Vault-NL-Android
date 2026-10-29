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
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Pagina(val label: String) { START("Start"), SCAN("Scannen"), COLLECTIE("Collectie"), PORTFOLIO("Portfolio") }

private fun euro(cent: Long): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("nl-NL"))
    .format(cent / 100.0)

@Composable
fun VaultApp(vm: KaartViewModel = viewModel()) {
    val context = LocalContext.current
    val kaarten by vm.kaarten.collectAsState()
    val recenteScans by vm.scans.collectAsState()
    val scanhistorie by vm.historie.collectAsState()
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
    var laatsteFoto by remember { mutableStateOf<Uri?>(null) }
    var zoekNaam by rememberSaveable { mutableStateOf("") }
    var backupOmTeHerstellen by remember { mutableStateOf<Uri?>(null) }

    fun verwerkAfbeelding(uri: Uri) {
        laatsteFoto = uri
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

    fun zoekOpNaam() {
        if (zoekNaam.isBlank() || bezig) return
        bezig = true
        scanFout = null
        scope.launch {
            try {
                val resultaat = KaartZoeker.zoek(Kaartspel.vanSleutel(spel), zoekNaam.trim())
                matchOpties = resultaat.kandidaten
                if (matchOpties.isNotEmpty()) {
                    ocrTekst = null
                    toonMatches = true
                } else {
                    scanFout = resultaat.bericht ?: "Geen kaarten gevonden. Probeer de volledige naam."
                }
            } finally { bezig = false }
        }
    }

    val backupOpslaan = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) vm.exporteerBackup(uri) }

    val backupOpenen = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) backupOmTeHerstellen = uri }

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
                Modifier.fillMaxWidth().background(Achtergrond).padding(horizontal = 20.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(45.dp)
                        .background(Brush.linearGradient(listOf(Accent.copy(alpha = .25f), Color(0xFF16323B))), RoundedCornerShape(14.dp))
                        .border(1.dp, Accent.copy(alpha = .35f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Layers, null, tint = Accent, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("TCG VAULT", color = Licht, fontSize = 19.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                    Text("VERZAMELAARSVERSIE", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                }
                Surface(color = PaneelLicht, shape = RoundedCornerShape(50), border = BorderStroke(1.dp, Lijn)) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(Accent, CircleShape))
                        Spacer(Modifier.width(7.dp))
                        Text("BÈTA 0.5", color = Licht, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },

        bottomBar = {
            NavigationBar(containerColor = Color(0xFF101B29), tonalElevation = 0.dp) {
                Pagina.entries.forEach { item ->
                    val actief = pagina == item
                    NavigationBarItem(
                        selected = actief, onClick = { pagina = item },
                        icon = {
                            Icon(when (item) {
                                Pagina.START -> Icons.Default.Home
                                Pagina.SCAN -> Icons.Default.QrCodeScanner
                                Pagina.COLLECTIE -> Icons.Default.CollectionsBookmark
                                Pagina.PORTFOLIO -> Icons.Default.Shield
                            }, contentDescription = item.label, modifier = Modifier.size(23.dp))
                        },
                        label = { Text(item.label, fontSize = 10.sp, fontWeight = if (actief) FontWeight.Bold else FontWeight.Medium) },
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
                    fotoVoorbeeld = laatsteFoto, zoekNaam = zoekNaam, veranderZoeknaam = { zoekNaam = it },
                    zoekInCatalogus = ::zoekOpNaam,
                    kiesFoto = { galerij.launch("image/*") }, handmatig = { ocrTekst = null; gekozenMatch = null; dialoog = true }
                )
                Pagina.COLLECTIE -> CollectiePagina(kaarten, vm::wijzigAantal, vm::werkKaartBij)
                Pagina.PORTFOLIO -> PortfolioPagina(
                    kaarten = kaarten,
                    recenteScans = recenteScans,
                    scanhistorie = scanhistorie,
                    delen = {
                        val overzicht = maakPortfolioTekst(kaarten)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, overzicht)
                        }
                        context.startActivity(Intent.createChooser(intent, "Deel je collectie"))
                    },
                    backupMaken = { backupOpslaan.launch("TCG-Vault-NL-backup.json") },
                    backupHerstellen = { backupOpenen.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
                )
            }
        }
    }

    backupOmTeHerstellen?.let { bestand ->
        AlertDialog(
            onDismissRequest = { backupOmTeHerstellen = null },
            containerColor = Paneel,
            title = { Text("Back-up herstellen?", color = Licht) },
            text = {
                Text(
                    "Dit vervangt ALLE kaarten en opgeslagen scanmomenten op dit toestel door " +
                        "de inhoud van het gekozen back-upbestand. Maak eerst een back-up van je " +
                        "huidige verzameling. Een ongeldig bestand wordt geweigerd.",
                    color = Muted
                )
            },
            confirmButton = {
                Button(onClick = {
                    backupOmTeHerstellen = null
                    vm.importeerBackup(bestand)
                }) { Text("Ja, vervang mijn collectie") }
            },
            dismissButton = {
                TextButton(onClick = { backupOmTeHerstellen = null }) { Text("Annuleren") }
            }
        )
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
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        item {
            Column {
                Text("JOUW VERZAMELOVERZICHT", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(6.dp))
                Text("Welkom in je vault.", color = Licht, fontSize = 29.sp, fontWeight = FontWeight.Black, letterSpacing = (-.7).sp)
                Text("Jouw wereld van trading cards, overzichtelijk bewaard.", color = Muted, fontSize = 13.sp)
            }
        }
        item {
            Box(
                Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF153C46), Color(0xFF14253A), Color(0xFF242442)))
                    )
                    .border(1.dp, Accent.copy(alpha = .18f), RoundedCornerShape(26.dp))
            ) {
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 67.dp, y = (-64).dp).size(235.dp)
                        .border(1.dp, Accent.copy(alpha = .13f), CircleShape)
                )
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 25.dp, y = (-22).dp).size(155.dp)
                        .border(1.dp, Accent.copy(alpha = .2f), CircleShape)
                )
                Column(Modifier.align(Alignment.CenterStart).padding(22.dp)) {
                    Surface(color = Accent.copy(alpha = .15f), shape = RoundedCornerShape(50)) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Accent, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("JOUW DIGITALE KLUIS", color = Accent, fontWeight = FontWeight.Bold, fontSize = 9.sp, letterSpacing = 1.sp)
                        }
                    }
                    Spacer(Modifier.height(13.dp))
                    Text("Scan. Verzamel.\nOntdek.", color = Licht, fontSize = 30.sp, lineHeight = 35.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text("Herken kaarten en bouw je collectie.", color = Color(0xFFC8D7E4), fontSize = 12.sp)
                    Spacer(Modifier.height(15.dp))
                    Button(
                        onClick = naarScanner,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Kaart scannen", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        item {
            Surface(color = Paneel, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Lijn)) {
                Column(Modifier.fillMaxWidth().padding(19.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Euro, null, tint = Goud, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("EIGEN COLLECTIEWAARDE", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(euro(eigenWaardeCent(kaarten)), color = Licht, fontWeight = FontWeight.Black, fontSize = 34.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Gebaseerd op jouw eigen prijzen • geen live Cardmarket-koers", color = Muted, fontSize = 11.sp)
                    if (zonderPrijs(kaarten) > 0) {
                        Spacer(Modifier.height(10.dp))
                        Text("Nog " + zonderPrijs(kaarten) + " kaarten zonder prijs", color = Goud, fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Kengetal("Exemplaren", exemplaren.toString(), Accent, Modifier.weight(1f))
                Kengetal("Uniek", kaarten.size.toString(), Licht, Modifier.weight(1f))
                Kengetal("Dubbelen", dubbel.toString(), Goud, Modifier.weight(1f))
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mijn kaartspellen", color = Licht, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Verdeling van je verzameling", color = Muted, fontSize = 12.sp)
                }
                TextButton(onClick = naarCollectie) { Text("Bekijk alles") }
            }
        }
        items(Kaartspel.entries.toList()) { game ->
            val aantal = kaarten.filter { it.spel == game }.sumOf { it.aantal }
            val gameKleur = spelKleur(game)
            Surface(
                color = Paneel, shape = RoundedCornerShape(17.dp),
                border = BorderStroke(1.dp, Lijn),
                modifier = Modifier.fillMaxWidth().clickable(onClick = naarCollectie)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp)
                            .background(gameKleur.copy(alpha = .13f), RoundedCornerShape(13.dp))
                            .border(1.dp, gameKleur.copy(alpha = .22f), RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Style, null, tint = gameKleur, modifier = Modifier.size(23.dp))
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(game.titel, color = Licht, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("In jouw collectie", color = Muted, fontSize = 11.sp)
                    }
                    Text(aantal.toString(), color = gameKleur, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(7.dp))
                    Icon(Icons.Default.ArrowForward, null, tint = Muted, modifier = Modifier.size(17.dp))
                }
            }
        }
        item { InfoPrijs() }
    }
}

@Composable
private fun Kengetal(titel: String, waarde: String, kleur: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Paneel, shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, Lijn)) {
        Column(Modifier.padding(vertical = 16.dp, horizontal = 12.dp)) {
            Box(Modifier.size(6.dp).background(kleur, CircleShape))
            Spacer(Modifier.height(8.dp))
            Text(waarde, color = kleur, fontWeight = FontWeight.Black, fontSize = 25.sp)
            Spacer(Modifier.height(3.dp))
            Text(titel, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ScannerPagina(
    spel: Kaartspel, veranderSpel: (Kaartspel) -> Unit, bezig: Boolean, fout: String?,
    fotoVoorbeeld: Uri?, zoekNaam: String, veranderZoeknaam: (String) -> Unit, zoekInCatalogus: () -> Unit,
    maakFoto: () -> Unit, kiesFoto: () -> Unit, handmatig: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(14.dp))
        Text("SCANSTUDIO", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.1.sp)
        Spacer(Modifier.height(7.dp))
        Text("Herken je kaarten", color = Licht, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(4.dp))
        Text("Fotografeer je kaart en kies een catalogusmatch.", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(19.dp))
        Box(
            Modifier.fillMaxWidth().height(258.dp)
                .clip(RoundedCornerShape(27.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF1D3348), Color(0xFF121D2F), Color(0xFF0F222C))))
                .border(1.dp, Accent.copy(alpha = .24f), RoundedCornerShape(27.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (fotoVoorbeeld != null) {
                SubcomposeAsyncImage(
                    model = fotoVoorbeeld,
                    contentDescription = "Jouw laatst gescande kaart",
                    modifier = Modifier.fillMaxSize().padding(12.dp).clip(RoundedCornerShape(17.dp)),
                    contentScale = ContentScale.Fit
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                        else -> Icon(Icons.Default.Image, null, tint = Accent)
                    }
                }
            } else {
            Box(
                Modifier.size(width = 133.dp, height = 183.dp)
                    .background(Color.White.copy(alpha = .025f), RoundedCornerShape(13.dp))
                    .border(1.dp, Accent.copy(alpha = .55f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.QrCodeScanner, null, tint = Accent, modifier = Modifier.size(60.dp))
                    Spacer(Modifier.height(11.dp))
                    Text(if (bezig) "HERKENNEN" else "KAART IN BEELD", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
            }
            Surface(
                color = Accent.copy(alpha = .13f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
            ) {
                Text("SLIMME MATCH", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
            Text(if (fotoVoorbeeld == null) "OCR + online catalogus" else "Laatst gekozen kaart", color = Muted, fontSize = 10.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(14.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("SELECTEER HET KAARTSPEL", color = Muted, modifier = Modifier.fillMaxWidth(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(7.dp))
        Keuzelijst("Spel", spel.titel, Kaartspel.entries.map { it.titel }) { keuze ->
            Kaartspel.entries.firstOrNull { it.titel == keuze }?.let(veranderSpel)
        }
        Spacer(Modifier.height(18.dp))
        if (bezig) {
            CircularProgressIndicator(color = Accent)
            Spacer(Modifier.height(11.dp))
            Text("Tekst lezen en kaartcatalogus doorzoeken…", color = Muted, fontSize = 13.sp)
        } else {
            Button(
                onClick = maakFoto, shape = RoundedCornerShape(15.dp),
                modifier = Modifier.fillMaxWidth().height(57.dp)
            ) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(Modifier.width(9.dp))
                Text("Fotografeer kaart", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(9.dp))
                Icon(Icons.Default.ArrowForward, null)
            }
            Spacer(Modifier.height(11.dp))
            OutlinedButton(
                onClick = kiesFoto, shape = RoundedCornerShape(15.dp),
                modifier = Modifier.fillMaxWidth().height(53.dp)
            ) {
                Icon(Icons.Default.Image, null)
                Spacer(Modifier.width(8.dp))
                Text("Kies foto uit galerij")
            }
            Spacer(Modifier.height(5.dp))
            TextButton(onClick = handmatig) { Text("Of voeg handmatig een kaart toe") }
        }
        Spacer(Modifier.height(16.dp))
        Surface(color = Paneel, shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, Lijn)) {
            Column(Modifier.fillMaxWidth().padding(15.dp)) {
                Text("Zoek op naam", color = Licht, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Geen goede foto? Zoek direct in de kaartcatalogus.", color = Muted, fontSize = 11.sp)
                Spacer(Modifier.height(9.dp))
                OutlinedTextField(
                    value = zoekNaam, onValueChange = veranderZoeknaam,
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Kaartnaam") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = zoekInCatalogus, modifier = Modifier.fillMaxWidth(),
                    enabled = zoekNaam.isNotBlank() && !bezig
                ) { Text("Zoek in catalogus") }
            }
        }
        if (fout != null) {
            Spacer(Modifier.height(8.dp))
            Text(fout, color = Goud, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        Surface(color = Paneel, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Lijn)) {
            Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, null, tint = Accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Controle vóór opslaan", color = Licht, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        "Ondersteunt OCR in Latijns, Chinees, Japans en Koreaans schrift. " +
                            "Catalogusdekking varieert per spel en taal. Controleer de exacte druk en uitvoering.",
                        color = Muted, fontSize = 11.sp, lineHeight = 17.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun CollectiePagina(
    kaarten: List<Verzamelkaart>, wijzigAantal: (Long, Int) -> Unit,
    bewerkKaart: (Verzamelkaart) -> Unit
) {
    var detailKaart by remember { mutableStateOf<Verzamelkaart?>(null) }
    var kaartInBewerking by remember { mutableStateOf<Verzamelkaart?>(null) }
    var zoekterm by rememberSaveable { mutableStateOf("") }
    var filterSpel by rememberSaveable { mutableStateOf("Alle spellen") }
    var alleenDubbel by rememberSaveable { mutableStateOf(false) }
    var sortering by rememberSaveable { mutableStateOf("Recent toegevoegd") }
    val gefilterd = kaarten.filter { kaart ->
        (filterSpel == "Alle spellen" || kaart.spel.titel == filterSpel) &&
        (!alleenDubbel || kaart.aantal > 1) &&
        (zoekterm.isBlank() || listOf(kaart.naam, kaart.setNaam, kaart.kaartnummer, kaart.taal)
            .any { it.contains(zoekterm.trim(), ignoreCase = true) })
    }
    val gesorteerd = when (sortering) {
        "Naam A-Z" -> gefilterd.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.naam })
        "Meeste exemplaren" -> gefilterd.sortedWith(
            compareByDescending<Verzamelkaart> { it.aantal }.thenBy { it.naam }
        )
        "Meeste dubbelen" -> gefilterd.sortedWith(
            compareByDescending<Verzamelkaart> { (it.aantal - 1).coerceAtLeast(0) }.thenBy { it.naam }
        )
        "Hoogste eigen waarde" -> gefilterd.sortedWith(
            compareByDescending<Verzamelkaart> { it.eigenPrijsCent?.let { prijs -> prijs * it.aantal } ?: -1L }
                .thenBy { it.naam }
        )
        else -> gefilterd.sortedByDescending { it.laatstToegevoegd }
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
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Sort, null, tint = Muted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Box(Modifier.weight(1f)) {
                Keuzelijst(
                    "Sorteren", sortering,
                    listOf("Recent toegevoegd", "Naam A-Z", "Meeste exemplaren", "Meeste dubbelen", "Hoogste eigen waarde")
                ) { sortering = it }
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            "" + gefilterd.sumOf { it.aantal } + " kaarten in " + gefilterd.size + " varianten getoond",
            color = Muted, fontSize = 11.sp
        )
        Spacer(Modifier.height(10.dp))
        if (gesorteerd.isEmpty()) {
            LeegScherm(if (kaarten.isEmpty()) "Nog geen kaarten toegevoegd" else "Geen kaarten gevonden", "Gebruik Scannen of de + knop om kaarten toe te voegen.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                items(gesorteerd, key = { it.id }) { kaart ->
                    KaartRij(kaart, wijzigAantal, bekijk = { detailKaart = it })
                }
            }
        }
        detailKaart?.let { kaart ->
            KaartDetailDialoog(
                kaart, sluiten = { detailKaart = null },
                bewerken = { detailKaart = null; kaartInBewerking = kaart }
            )
        }
        kaartInBewerking?.let { kaart ->
            KaartDialoog(
                spel = kaart.spel, herkendeTekst = null, suggestie = null,
                bestaandeKaart = kaart,
                sluiten = { kaartInBewerking = null },
                opslaan = { aangepast ->
                    bewerkKaart(aangepast)
                    kaartInBewerking = null
                }
            )
        }
    }
}

@Composable
private fun KaartRij(kaart: Verzamelkaart, wijzigAantal: (Long, Int) -> Unit, bekijk: (Verzamelkaart) -> Unit) {
    val kleur = spelKleur(kaart.spel)
    Surface(
        color = Paneel, shape = RoundedCornerShape(19.dp),
        border = BorderStroke(1.dp, Lijn)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            KaartIllustratie(kaart.afbeeldingUrl, kaart.spel,
                Modifier.size(width = 63.dp, height = 88.dp).clickable { bekijk(kaart) })
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(kaart.naam, color = Licht, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { bekijk(kaart) })
                Spacer(Modifier.height(2.dp))
                Text(kaart.spel.titel.uppercase(), color = kleur, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .65.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    listOf(kaart.setNaam, kaart.kaartnummer, kaart.taal).filter { it.isNotBlank() }.joinToString(" • "),
                    color = Muted, fontSize = 11.sp, maxLines = 2
                )
                Text(kaart.uitvoering + " • " + kaart.conditie, color = Muted, fontSize = 10.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    kaart.eigenPrijsCent?.let { "Eigen waarde: " + euro(it) + " / stuk" } ?: "Waarde niet ingevuld",
                    color = Goud, fontSize = 11.sp, fontWeight = FontWeight.Medium
                )
                if (kaart.scanAantal > 0) {
                    Text(kaart.scanAantal.toString() + " keer gescand", color = Accent, fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = PaneelLicht, shape = RoundedCornerShape(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { wijzigAantal(kaart.id, kaart.aantal - 1) }, modifier = Modifier.size(31.dp)) {
                                Icon(Icons.Default.Remove, "Eén kaart minder", tint = Muted, modifier = Modifier.size(17.dp))
                            }
                            Text(kaart.aantal.toString(), color = Licht, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            IconButton(onClick = { wijzigAantal(kaart.id, kaart.aantal + 1) }, modifier = Modifier.size(31.dp)) {
                                Icon(Icons.Default.Add, "Eén kaart meer", tint = Accent, modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                    if (kaart.aantal > 1) {
                        Spacer(Modifier.width(8.dp))
                        Surface(color = Goud.copy(alpha = .13f), shape = RoundedCornerShape(50)) {
                            Text("DUBBEL", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Goud, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PortfolioPagina(
    kaarten: List<Verzamelkaart>,
    recenteScans: List<ScanMoment>,
    scanhistorie: List<ScanMoment>,
    delen: () -> Unit,
    backupMaken: () -> Unit,
    backupHerstellen: () -> Unit
) {
    val totaal = kaarten.sumOf { it.aantal }
    val dubbelen = kaarten.sumOf { (it.aantal - 1).coerceAtLeast(0) }
    var openHistorie by remember { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("JOUW PORTFOLIO", color = Accent, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("Mijn portfolio", color = Licht, fontSize = 29.sp, fontWeight = FontWeight.Black)
            Text("Een overzicht om trots op te zijn.", color = Muted, fontSize = 13.sp)
        }
        item {
            Surface(
                shape = RoundedCornerShape(24.dp), color = Paneel,
                border = BorderStroke(1.dp, Accent.copy(alpha = .27f))
            ) {
                Column(
                    Modifier.fillMaxWidth().background(
                        Brush.verticalGradient(listOf(Color(0xFF173440), Paneel))
                    ).padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.size(73.dp).background(Accent.copy(alpha = .13f), CircleShape)
                            .border(1.dp, Accent.copy(alpha = .34f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Shield, null, tint = Accent, modifier = Modifier.size(37.dp))
                    }
                    Spacer(Modifier.height(11.dp))
                    Text("MIJN TCG VAULT", color = Licht, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text("VERZAMELAARSPROFIEL", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(17.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(21.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(totaal.toString(), color = Licht, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Text("Kaarten", color = Muted, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(kaarten.size.toString(), color = Licht, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Text("Varianten", color = Muted, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(dubbelen.toString(), color = Goud, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Text("Dubbelen", color = Muted, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(euro(eigenWaardeCent(kaarten)), color = Goud, fontSize = 29.sp, fontWeight = FontWeight.Black)
                    Text("Eigen opgegeven waarde • geen live koers", color = Muted, fontSize = 11.sp)
                    Spacer(Modifier.height(17.dp))
                    Button(
                        onClick = delen, modifier = Modifier.fillMaxWidth(), enabled = kaarten.isNotEmpty(),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Deel mijn portfolio", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Surface(
                color = Paneel, shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Lijn)
            ) {
                Column(Modifier.fillMaxWidth().padding(17.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SaveAlt, null, tint = Accent, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(9.dp))
                        Text("Back-up en herstel", color = Licht, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "Bewaar je hele collectie inclusief aantallen, varianten en scanhistorie " +
                            "als JSON-bestand op een plek die jij kiest.",
                        color = Muted, fontSize = 12.sp, lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(13.dp))
                    Button(
                        onClick = backupMaken,
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.SaveAlt, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Back-up maken")
                    }
                    Spacer(Modifier.height(7.dp))
                    OutlinedButton(
                        onClick = backupHerstellen,
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Back-up herstellen")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Let op: herstellen VERVANGT je huidige verzameling. " +
                            "Kaartafbeeldingen worden opnieuw online geladen.",
                        color = Goud, fontSize = 11.sp
                    )
                }
            }
        }
        if (recenteScans.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, null, tint = Accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Recent gescand", color = Licht, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("Je laatste " + recenteScans.size + " scanmomenten", color = Muted, fontSize = 12.sp)
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { openHistorie = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Bekijk scanhistorie")
                }
            }
            items(recenteScans) { scan ->
                Surface(
                    color = Paneel, shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, Lijn)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(39.dp).background(
                                spelKleur(scan.spel).copy(alpha = .13f),
                                RoundedCornerShape(11.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = spelKleur(scan.spel),
                                modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(scan.kaartNaam, color = Licht, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(scan.spel.titel, color = Muted, fontSize = 11.sp)
                        }
                        Text(
                            SimpleDateFormat("dd-MM-yyyy HH:mm", Locale("nl", "NL"))
                                .format(Date(scan.gescandOp)),
                            color = Muted, fontSize = 10.sp
                        )
                    }
                }
            }
        }
        item {
            Text("Jouw collectie per spel", color = Licht, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
        items(Kaartspel.entries.toList()) { spel ->
            val aantal = kaarten.filter { it.spel == spel }.sumOf { it.aantal }
            val kleur = spelKleur(spel)
            Surface(color = Paneel, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Lijn)) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(35.dp).background(kleur.copy(alpha = .14f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Style, null, tint = kleur, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Text(spel.titel, Modifier.weight(1f), color = Licht, fontWeight = FontWeight.SemiBold)
                    Text(aantal.toString(), color = kleur, fontSize = 17.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        item { InfoPrijs() }
        item {
            Text("Je portfolio wordt momenteel als tekst gedeeld. Openbare profielen komen in een latere versie.",
                color = Muted, fontSize = 12.sp)
        }
    }
    if (openHistorie) {
        ScanhistorieDialoog(scanhistorie, sluiten = { openHistorie = false })
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
    sluiten: () -> Unit, opslaan: (Verzamelkaart) -> Unit,
    bestaandeKaart: Verzamelkaart? = null
) {
    var gekozenSpel by remember(bestaandeKaart, suggestie) {
        mutableStateOf(bestaandeKaart?.spel ?: suggestie?.spel ?: spel)
    }
    var naam by remember(bestaandeKaart, suggestie, herkendeTekst) {
        mutableStateOf(bestaandeKaart?.naam ?: suggestie?.naam ?: herkendeTekst?.let(KaartTekstParser::naamVoorstel).orEmpty())
    }
    var setNaam by remember(bestaandeKaart, suggestie) {
        mutableStateOf(bestaandeKaart?.setNaam ?: suggestie?.setNaam.orEmpty())
    }
    var nummer by remember(bestaandeKaart, suggestie, herkendeTekst) {
        mutableStateOf(bestaandeKaart?.kaartnummer ?: suggestie?.nummer ?: herkendeTekst?.let(KaartTekstParser::kaartnummer).orEmpty())
    }
    var taal by remember(bestaandeKaart, suggestie) {
        val initieel = bestaandeKaart?.taal ?: suggestie?.taal ?: "Engels"
        mutableStateOf(if (initieel in KAARTTALEN) initieel else "Overig (zelf invullen)")
    }
    var vrijeTaal by remember(bestaandeKaart) {
        mutableStateOf(bestaandeKaart?.taal?.takeIf { it !in KAARTTALEN }.orEmpty())
    }
    var conditie by remember(bestaandeKaart) { mutableStateOf(bestaandeKaart?.conditie ?: "Near Mint") }
    var uitvoering by remember(bestaandeKaart) { mutableStateOf(bestaandeKaart?.uitvoering ?: "Normaal") }
    var aantalTekst by remember(bestaandeKaart) { mutableStateOf(bestaandeKaart?.aantal?.toString() ?: "1") }
    var prijsTekst by remember(bestaandeKaart) {
        mutableStateOf(bestaandeKaart?.eigenPrijsCent?.let {
            BigDecimal(it).movePointLeft(2).stripTrailingZeros().toPlainString()
        }.orEmpty())
    }
    var notitie by remember(bestaandeKaart) { mutableStateOf(bestaandeKaart?.notitie.orEmpty()) }
    var fout by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = sluiten) {
        Surface(color = Paneel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column {
                Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(19.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (bestaandeKaart != null) "Kaartgegevens bewerken"
                        else if (suggestie != null) "Kaartmatch controleren"
                        else if (herkendeTekst != null) "Scan controleren" else "Kaart toevoegen",
                        color = Licht, fontSize = 21.sp, fontWeight = FontWeight.Black
                    )
                    if (suggestie != null) {
                        Text("Voorstel via " + suggestie.bron + " • controleer set, taal en uitvoering.", color = Accent, fontSize = 12.sp)
                        if (suggestie.afbeeldingUrl.isNotBlank()) {
                            Box(Modifier.fillMaxWidth().padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                KaartIllustratie(
                                    suggestie.afbeeldingUrl, suggestie.spel,
                                    Modifier.size(width = 125.dp, height = 173.dp)
                                )
                            }
                        }
                    }
                    Text(
                        if (bestaandeKaart != null)
                            "Corrigeer de kaart. Als de variant al bestaat, worden aantallen en scanmomenten samengevoegd."
                        else "Controleer alle gegevens. Dezelfde variant wordt als dubbel opgeteld.",
                        color = Muted, fontSize = 12.sp
                    )
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
                            id = bestaandeKaart?.id ?: 0L,
                            spel = gekozenSpel, naam = naam.trim(), setNaam = setNaam.trim(),
                            kaartnummer = nummer.trim(), taal = gekozenTaal,
                            uitvoering = uitvoering, conditie = conditie, aantal = aantal!!,
                            eigenPrijsCent = cent, notitie = notitie.trim(),
                            laatstToegevoegd = bestaandeKaart?.laatstToegevoegd ?: System.currentTimeMillis(),
                            afbeeldingUrl = if (bestaandeKaart != null) {
                                if (gekozenSpel == bestaandeKaart.spel &&
                                    naam.trim() == bestaandeKaart.naam &&
                                    setNaam.trim() == bestaandeKaart.setNaam &&
                                    nummer.trim() == bestaandeKaart.kaartnummer
                                ) bestaandeKaart.afbeeldingUrl else ""
                            } else if (gekozenSpel == suggestie?.spel) suggestie.afbeeldingUrl else "",
                            catalogusBron = if (bestaandeKaart != null) {
                                if (gekozenSpel == bestaandeKaart.spel &&
                                    naam.trim() == bestaandeKaart.naam &&
                                    setNaam.trim() == bestaandeKaart.setNaam &&
                                    nummer.trim() == bestaandeKaart.kaartnummer
                                ) bestaandeKaart.catalogusBron else ""
                            } else if (gekozenSpel == suggestie?.spel) suggestie.bron else "",
                            catalogusId = if (bestaandeKaart != null) {
                                if (gekozenSpel == bestaandeKaart.spel &&
                                    naam.trim() == bestaandeKaart.naam &&
                                    setNaam.trim() == bestaandeKaart.setNaam &&
                                    nummer.trim() == bestaandeKaart.kaartnummer
                                ) bestaandeKaart.catalogusId else ""
                            } else if (gekozenSpel == suggestie?.spel) suggestie.bronId else ""
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
                                KaartIllustratie(kandidaat.afbeeldingUrl, kandidaat.spel, Modifier.size(width = 49.dp, height = 66.dp))
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

@Composable
private fun ScanhistorieDialoog(scans: List<ScanMoment>, sluiten: () -> Unit) {
    var zoekterm by remember { mutableStateOf("") }
    var spelFilter by remember { mutableStateOf("Alle spellen") }
    val getoond = scans.filter { scan ->
        (spelFilter == "Alle spellen" || scan.spel.titel == spelFilter) &&
            (zoekterm.isBlank() ||
                scan.kaartNaam.contains(zoekterm.trim(), ignoreCase = true) ||
                scan.herkendeTekst.contains(zoekterm.trim(), ignoreCase = true))
    }
    Dialog(onDismissRequest = sluiten) {
        Surface(
            color = Paneel,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Lijn),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(.90f)
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, null, tint = Accent)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Scanhistorie", color = Licht, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text("Laatste " + scans.size + " scans (maximaal 500)", color = Muted, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = zoekterm, onValueChange = { zoekterm = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Zoek kaart of herkende tekst") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(12.dp), singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Keuzelijst(
                    "Kaartspel", spelFilter,
                    listOf("Alle spellen") + Kaartspel.entries.map { it.titel }
                ) { spelFilter = it }
                Spacer(Modifier.height(10.dp))
                Text(getoond.size.toString() + " scanmomenten gevonden", color = Muted, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    items(getoond) { scan ->
                        Surface(
                            color = PaneelLicht,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Lijn)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(31.dp).background(
                                            spelKleur(scan.spel).copy(alpha = .15f),
                                            RoundedCornerShape(9.dp)
                                        ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle, null, tint = spelKleur(scan.spel),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(9.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(scan.kaartNaam, color = Licht, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(scan.spel.titel, color = Muted, fontSize = 10.sp)
                                    }
                                    Text(
                                        SimpleDateFormat("dd-MM-yyyy HH:mm", Locale("nl", "NL"))
                                            .format(Date(scan.gescandOp)),
                                        color = Muted, fontSize = 10.sp
                                    )
                                }
                                if (scan.herkendeTekst.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        scan.herkendeTekst.replace("\n", " • ").take(240),
                                        color = Muted, fontSize = 11.sp,
                                        lineHeight = 16.sp, maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Je volledige scanlog zit ook in je JSON-back-up. Hier tonen we maximaal 500 recente scans.",
                    color = Muted, fontSize = 10.sp
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = sluiten, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Sluiten") }
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

/**
 * Catalogusafbeeldingen worden op het toestel gecachet door Coil. Als een provider
 * geen afbeelding heeft of extern laden blokkeert, blijft de stijlvolle placeholder.
 */
@Composable
private fun KaartIllustratie(url: String, spel: Kaartspel, modifier: Modifier = Modifier) {
    val kleur = spelKleur(spel)
    val veiligeUrl = url.takeIf {
        it.startsWith("https://assets.tcgdex.net/") ||
            it.startsWith("https://cards.scryfall.io/") ||
            (spel == Kaartspel.ONE_PIECE && it.startsWith("https://"))
    }
    Box(
        modifier.clip(RoundedCornerShape(9.dp))
            .background(Brush.linearGradient(listOf(kleur.copy(alpha = .3f), Color(0xFF111A2A))))
            .border(1.dp, kleur.copy(alpha = .4f), RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (veiligeUrl != null) {
            SubcomposeAsyncImage(
                model = veiligeUrl,
                contentDescription = "Catalogusafbeelding " + spel.titel,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(9.dp)),
                contentScale = ContentScale.Fit
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                    else -> Icon(Icons.Default.Style, null, tint = kleur, modifier = Modifier.size(28.dp))
                }
            }
        } else {
            Icon(Icons.Default.Style, null, tint = kleur, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun KaartDetailDialoog(kaart: Verzamelkaart, sluiten: () -> Unit, bewerken: () -> Unit) {
    Dialog(onDismissRequest = sluiten) {
        Surface(
            color = Paneel, shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Lijn),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(.88f)
        ) {
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState())
                        .padding(horizontal = 19.dp, vertical = 17.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("KAARTDETAILS", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(13.dp))
                    KaartIllustratie(kaart.afbeeldingUrl, kaart.spel, Modifier.size(width = 174.dp, height = 239.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(kaart.naam, color = Licht, fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text(kaart.spel.titel, color = spelKleur(kaart.spel), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    Surface(color = PaneelLicht, shape = RoundedCornerShape(15.dp)) {
                        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DetailRij("Set", kaart.setNaam.ifBlank { "Niet opgegeven" })
                            DetailRij("Kaartnummer", kaart.kaartnummer.ifBlank { "Onbekend" })
                            DetailRij("Taal", kaart.taal)
                            DetailRij("Uitvoering", kaart.uitvoering)
                            DetailRij("Conditie", kaart.conditie)
                            DetailRij("Aantal", kaart.aantal.toString() + if (kaart.aantal > 1) " (dubbel)" else "")
                            DetailRij("Eigen prijs per stuk", kaart.eigenPrijsCent?.let(::euro) ?: "Niet opgegeven")
                            DetailRij("Herkomst", kaart.catalogusBron.ifBlank { "Handmatig toegevoegd" })
                        }
                    }
                    if (kaart.notitie.isNotBlank()) {
                        Spacer(Modifier.height(13.dp))
                        Text(kaart.notitie, color = Muted, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(11.dp))
                    Text("Afbeelding en gegevens zijn een catalogusvoorstel. Geen geverifieerde Cardmarket-prijs.",
                        color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
                }
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    OutlinedButton(
                        onClick = sluiten, shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("Sluiten") }
                    Button(
                        onClick = bewerken, shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Bewerken")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRij(titel: String, waarde: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(titel, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(.9f))
        Text(waarde, color = Licht, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
    }
}
