# TCG Vault NL — Android-app (bèta 0.2)

Een Nederlandstalige **native Android-app** voor Pokémon, Magic: The Gathering, One Piece, Yu-Gi-Oh! en Riftbound. Gebouwd met Kotlin, Jetpack Compose, Google ML Kit en SQLite.

## Nieuw in 0.2

- **Professionele donkere interface:** collector-dashboard, vernieuwde scanstudio, spelkleur per TCG, collectiekaarten, vernieuwd portfolio en onderste navigatie.
- **Automatisch kaartvoorstellen zoeken:** maak een foto of kies een afbeelding. ML Kit herkent lokaal tekst en de app doorzoekt online kaartcatalogi. Er verschijnt een lijst mogelijke matches; tik de juiste kaart aan en bevestig naam, set, nummer, taal, conditie en uitvoering voordat je opslaat.
- **Vijf catalogusroutes:** TCGdex (Pokémon), Scryfall (Magic), YGOPRODeck (Yu-Gi-Oh!), OPTCG API (One Piece) en OpenRift (Riftbound).
- **Meerdere schriften:** OCR in Latijns, Chinees, Japans en Koreaans schrift. Pokémon-zoekroute kiest bij Aziatische OCR automatisch een overeenkomstige catalogustaal, afhankelijk van beschikbare gegevens.
- **Handmatige terugval** bij onduidelijke foto, ontbrekende cataloguskaart of netwerkstoring.
- Bestaande lokale collectiegegevens blijven behouden; de SQLite-tabel is niet gewijzigd.

### Zo scan je een kaart

1. Open **Scannen**, kies het juiste kaartspel en maak een duidelijke foto (liefst recht van voren).
2. De app herkent tekst op het toestel en vraagt daarna online mogelijke kaarten op.
3. Kies de juiste match. Als geen match verschijnt, vul de kaart handmatig in.
4. Controleer vooral **set, kaartnummer, taal, uitvoering/foil en conditie**.
5. Sla op. Dezelfde variant opnieuw opslaan verhoogt het aantal en telt als dubbel.

**Belangrijk:** dit is OCR-gestuurde catalogusmatching, geen gegarandeerde visuele identificatie op basis van artwork. Niet elke kaart, taal, herdruk of alternatieve uitvoering kan automatisch worden gematcht. Vooral Aziatische kaarten van niet-Pokémon-spellen en oudere/promokaarten kunnen handmatige invoer vereisen.

## Wat al werkt

- Lokale collectie met filteren, zoeken, talen, varianten en dubbelen.
- Scanhistorie per opgeslagen kaartvariant (aantal scans), aantallen verhogen/verlagen.
- Eigen optionele prijzen per kaart en totaal van **zelf ingevoerde** waarden.
- Portfolio als tekst delen via andere apps. Nog geen publiek online portfolio of accounts.
- Volledig Nederlandstalige interface en handmatige kaartinvoer.

## Wat nog niet werkt

- **Live Cardmarket-waardes:** er is geen officiële Cardmarket-koppeling. De app toont bewust geen verzonnen prijzen. Een eigen prijs wordt nooit als Cardmarket-prijs gelabeld.
- Gegarandeerde herkenning van elke taal/druk en precieze illustratievergelijking.
- Online accounts, openbare collectiepagina's, cloudback-ups en fotoarchief.
- Realtime videoherkenning zonder foto. Dit is een **foto-scanner**.

## Installeren / bouwen

Deze repository bouwt automatisch een debug-APK via GitHub Actions:

1. Ga naar [GitHub Actions — Android bouwen](https://github.com/DRAVEINX/TCG-Vault-NL-Android/actions).
2. Open de nieuwste **geslaagde** build.
3. Scroll naar **Artifacts**, kies `tcg-vault-nl-debug-apk` en pak de ZIP uit.
4. Installeer `app-debug.apk` op je Android-telefoon (sta installatie uit deze bron toe, indien nodig).

Android Studio: open de repository, gebruik JDK 17 en Android SDK 35. De GitHub-workflow gebruikt Gradle 8.11.1 en bouwt met `gradle :app:testDebugUnitTest` en `gradle :app:assembleDebug`.

De `gradle-wrapper.jar` ontbreekt nog; gebruik lokaal Gradle 8.11.1 of genereer de wrapper. De CI-workflow werkt zonder wrapper. De APK is voor **testen**, niet voor Play Store-publicatie.

## Privacy en externe diensten

- OCR vindt plaats **op het toestel** met ML Kit. De app uploadt de scanfoto niet naar onze eigen server.
- **Zoekwoorden uit de herkende kaarttekst** worden via HTTPS naar de catalogusdienst van het gekozen spel gestuurd. Die externe dienst kan de zoekopdracht en verbindingsgegevens zien. Gebruik handmatige invoer als je dat niet wilt.
- De collectie blijft lokaal in SQLite. Er is geen account, advertentietracking of synchronisatie.
- Je eigen prijzen zijn niet automatisch opgehaald en dus geen geverifieerde marktprijzen.
- Bij verwijdering van de app kun je de collectie verliezen; export/import bestaat nog niet.

## Catalogusbronnen

- [TCGdex](https://tcgdex.dev/rest/cards) — Pokémon, meertalige dekking verschilt per taal.
- [Scryfall](https://scryfall.com/docs/api) — Magic.
- [YGOPRODeck](https://ygoprodeck.com/api-guide/) — Yu-Gi-Oh!. Hergebruik geen artwork door direct te hotlinken; deze app toont in de collectie gestileerde placeholders.
- [OPTCG API](https://optcg-api.ryanmichaelhirst.us/docs/cards/list) — One Piece.
- [OpenRift](https://openrift.app/developers) — Riftbound; jonge API, kan nog wijzigen.

API-afhankelijkheden kunnen tijdelijk niet werken of wijzigen. Diensten hebben hun eigen voorwaarden en rate limits. Een geautomatiseerde match blijft een **voorstel**, geen officiële prijs-, kaart- of taalgarantie.

## Code

- `app/src/main/java/nl/tcgvault/app/ui/VaultApp.kt` — Nederlandstalige schermen, matchkeuze en formulier.
- `app/src/main/java/nl/tcgvault/app/ui/VaultThema.kt` — premium donker thema.
- `app/src/main/java/nl/tcgvault/app/scan/TekstHerkenner.kt` — ML Kit OCR.
- `app/src/main/java/nl/tcgvault/app/scan/KaartZoeker.kt` — externe catalogusmatching.
- `app/src/main/java/nl/tcgvault/app/data/` — lokale database, collectie en waardefuncties.
