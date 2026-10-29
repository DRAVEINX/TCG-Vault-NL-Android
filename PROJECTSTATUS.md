# TCG Vault NL — Projectstatus

**Android bèta 0.2.0 (29 oktober 2026)**

## Geïmplementeerd
- Native Kotlin/Jetpack Compose Android-app met vernieuwde premium donkere Nederlandstalige interface.
- Startdashboard, scanstudio, collectie, aantallen en dubbelen, lokaal portfolio met delen als tekst.
- ML Kit OCR op gekozen afbeelding/camerafoto: Latijns, Chinees, Japans en Koreaans.
- Nieuwe `KaartZoeker.kt`: OCR-gestuurde online voorstellen voor Pokémon (TCGdex), Magic (Scryfall), Yu-Gi-Oh! (YGOPRODeck), One Piece (OPTCG API) en Riftbound (OpenRift).
- Matchvenster waarmee de gebruiker een kandidaat bevestigt. Set, taal en druk blijven door de gebruiker te controleren.
- Lokale SQLite-collectie bestaande data compatibel. Er zijn geen databasemigraties nodig.
- Nieuwe JVM-unit-tests voor kaartnaamregels en tekstdetectie van Aziatische schriften.
- GitHub Actions bouwt een debug-APK en voert tests uit.

## Grenzen van deze bèta
- Het is **geen garantie op exacte automatische identificatie**: catalogi, OCR en uitgaven verschillen per taal/spel. Er is geen visuele kaartbeeld-fingerprint.
- De kaartfoto wordt niet naar kaartcatalogi verstuurd; herkende zoektekst wél. Collectiongegevens blijven lokaal.
- Geen actuele Cardmarket-prijzen, geen geautoriseerde Cardmarket API, geen cloudaccounts of publiek portfolio.
- Geen volledige ondersteuning voor **alle** kaarttalen. Handmatige invoer blijft nodig voor niet-herkende schriften, drukvarianten en promo's.
- De debug-APK moet op echte toestellen getest worden; een succesvolle CI is geen vervanging voor camera-, netwerk- en toesteltests.

Zie [README.md](README.md) voor installatie, scanstappen, privacy en bronvermelding.
