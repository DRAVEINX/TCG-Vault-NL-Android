# TCG Vault NL — native Android-app 🇳🇱

Een eerste **Android Studio/Kotlin/Jetpack Compose**-versie van een Nederlandstalige app om ruilkaarten te scannen en in je eigen verzameling te bewaren. De app is een **bèta (0.1.0)**, geen afgerond commercieel product.

## Ondersteunde kaartspellen

- Pokémon TCG
- Magic: The Gathering
- One Piece Card Game
- Yu-Gi-Oh!
- Riftbound

## Wat werkt in deze eerste versie?

- 📷 Foto nemen met je Android-camera of foto kiezen uit je galerij.
- 🔤 Tekstherkenning met Google ML Kit in **Latijns, Chinees, Japans en Koreaans schrift**. De herkende tekst wordt als voorstel in het kaartformulier gezet. Dit is **geen gegarandeerde kaartidentificatie**; controle van set, nummer, druk, taal en conditie is noodzakelijk.
- 🌍 Taalkeuze met veelgebruikte kaarttalen en een vrij invoerveld voor alle andere talen. OCR herkent niet automatisch ieder wereldwijd schrift.
- 📚 Verzameling bewaren in een lokale SQLite-database. Geen account nodig, geen uploads van collectiegegevens.
- 🔁 Dezelfde kaartvariant opnieuw toevoegen verhoogt het aantal. Verschil in kaartspel, naam, set, nummer, taal, uitvoering of conditie telt als aparte variant.
- 🗃️ Elk bevestigd scanevent wordt opgeslagen met datum en uitgelezen OCR-tekst; de collectie toont het aantal scans van die kaartvariant. De scanfoto zelf wordt niet permanent opgeslagen.
- 🔎 Zoeken, op kaartspel filteren, dubbelen tonen, hoeveelheden verhogen en verlagen.
- 🧾 Je eigen prijs **per kaart** invoeren en het totaal van die **eigen inschattingen** zien, met zichtbare aanduiding van kaarten zonder prijs.
- 📤 Portfolio als tekst delen via Android, bijvoorbeeld in WhatsApp of e-mail (geen openbaar profiel of URL).
- 🇳🇱 De volledige app-interface is Nederlandstalig.

## Niet inbegrepen / eerstvolgende ontwikkeling

1. **Live Cardmarket-prijzen:** deze bèta heeft geen Cardmarket-API-toegang. Het configuratiepunt `MarktprijsBron` geeft bewust geen prijs terug. Cardmarket accepteert momenteel geen nieuwe API-aanvragen. Er worden geen prijzen gescrapet of verzonnen. Voor betrouwbare realtime prijzen is een toegestane databron en correcte matching per editie, taal, conditie en variant nodig.
2. **Automatische exacte kaartidentificatie:** OCR geeft tekstvoorstellen, geen productdatabase-match. Voor elk spel is een juiste catalogus- of licentiedatabron nodig.
3. **Online accounts en publieke portfolio-URL's:** de gegevens blijven vooralsnog alleen op het Android-toestel. Voor publieke portfolio's is een backend nodig, inclusief accountbeveiliging, toegangsrechten en privacyregels.
4. Cloudback-up, data-export/-import, offline kaartafbeeldingen, volledige scanhistoriepagina, geverifieerde marktprijsverloopgrafieken en automatische kaartafbeeldingsmatching.

## Android Studio starten

Vereisten: Android Studio met **JDK 17**, Android SDK Platform 35 en internet voor de eerste dependency-download. De app ondersteunt Android 8.0 (API 26) en hoger.

1. Pak de ZIP uit of clone de GitHub-repository.
2. Open de map `TCG-Vault-NL-Android` in **Android Studio → Open**.
3. Laat Gradle synchroniseren. Het project gebruikt **Android Gradle Plugin 8.9.2**, **Kotlin 2.0.21**, **Gradle 8.11.1**, Jetpack Compose en ML Kit.
4. Kies een emulator of aangesloten Android-toestel.
5. Klik op **Run ▶** om te testen.

**Gradle-wrapper:** vanwege de manier waarop deze eerste broncodebundel is gemaakt, is er wel `gradle/wrapper/gradle-wrapper.properties` maar **geen binaire `gradle-wrapper.jar`** toegevoegd. Als Android Studio hierom vraagt, installeer lokaal Gradle 8.11.1 en voer in de projectmap `gradle wrapper --gradle-version 8.11.1` uit. Hierna kun je `./gradlew :app:assembleDebug` gebruiken. Op GitHub Actions is dit al ondervangen met de `setup-gradle`-actie; de workflow gebruikt de geïnstalleerde Gradle direct.

### Een APK via GitHub Actions

Zodra de bronbestanden op de `main`-branch van je repository staan, bouwt `.github/workflows/android.yml` automatisch een **debug-APK**. Ga op GitHub naar **Actions → Android bouwen → laatste run → Artifacts → tcg-vault-nl-debug-apk**. Een debug-APK is bedoeld voor testen, niet voor publicatie in de Play Store. De workflow is nog niet op GitHub uitgevoerd.

## Op GitHub plaatsen (nieuwe repository)

1. Ga naar <https://github.com/new> en maak een **lege** repository aan, bijvoorbeeld `TCG-Vault-NL-Android`. Selecteer bij het aanmaken **geen** README, `.gitignore` of licentie.
2. Open een terminal in de uitgepakte projectmap.
3. Voer uit (vervang `JOUW_GEBRUIKERSNAAM` en de repositorynaam waar nodig):

```bash
git init
git add .
git commit -m "Eerste Android-versie van TCG Vault NL"
git branch -M main
git remote add origin https://github.com/JOUW_GEBRUIKERSNAAM/TCG-Vault-NL-Android.git
git push -u origin main
```

Daarna staan de broncode en GitHub Actions-workflow in GitHub. Deel gerust de repository-URL als de lege repository is aangemaakt; de gekoppelde GitHub-app kan code toevoegen aan bestaande repositories.

## Structuur

```text
app/src/main/java/nl/tcgvault/app/
├── MainActivity.kt
├── data/
│   ├── Models.kt           # kaartspellen, talen, prijzen, varianten
│   ├── KaartDatabase.kt    # SQLite, scanhistorie en dubbelen
│   └── KaartViewModel.kt   # lokale data en acties
├── scan/
│   └── TekstHerkenner.kt   # ML Kit OCR + kaartnummer-voorstel
└── ui/
    ├── VaultApp.kt        # complete Nederlandstalige interface
    └── VaultThema.kt      # donker kleurenthema
```

## Privacy en prijzen

- Er is **geen** account, analytics, tracking of eigen server in deze versie.
- Foto's worden alleen gekozen/gemaakt voor verwerking op het toestel; tijdelijke camerabestanden kunnen in de appcache blijven tot Android die opschoont.
- De collectie en scan-tekst worden lokaal bewaard. De app heeft bewust `allowBackup="false"`; zonder export kun je deze gegevens bij het verwijderen van de app kwijtraken.
- **Waarde ≠ Cardmarket-koers**: alleen bedragen die je zelf invoert, worden opgeteld. Ongemeten kaarten ontbreken in dat totaal.
- Geen Cardmarket-inloggegevens in Android-app of publiek GitHub-repository stoppen. Een toekomstige toegestane API-koppeling moet op een beveiligde server verlopen.

## Bronnen

- [Google ML Kit Text Recognition voor Android](https://developers.google.com/ml-kit/vision/text-recognition/v2/android)
- [Cardmarket API – huidige toegangsvoorwaarden](https://help.cardmarket.com/en/cardmarket-api)
- [Android Developers](https://developer.android.com/)

**Opmerking:** dit project is met broncode- en structuurcontroles voorbereid. Het is niet in deze omgeving gecompileerd of op een toestel getest; laat de eerste CI-run en Android Studio-compile de daadwerkelijke build verifiëren.
