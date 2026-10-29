# TCG Vault NL — Projectstatus

**Native Android bèta 0.3.0 — 29 oktober 2026**

## Nieuw in 0.3
- Catalogusafbeeldingen bij ondersteunde Pokémon-, Magic- en One Piece-kaarten, met automatische fallback.
- Verbeterde sortering van matches op naam en kaartnummer; Magic toont meerdere fysieke drukken.
- Foto-voorvertoning na scannen of kiezen van een afbeelding.
- Handmatige cataloguszoekfunctie op kaartnaam wanneer de OCR niet goed werkt.
- Klikbaar kaartdetailvenster in de collectie met afbeelding en kaartinformatie.
- SQLite-databasemigratie 1 → 2, die bestaande kaartgegevens behoudt.
- Tests voor kaartnummerherkenning, verschillende TCG-codes en Aziatische tekens.

## Bestaande functies
- Volledig Nederlandstalige Kotlin/Jetpack Compose-interface en donker verzamelthema.
- Vijf TCG's: Pokémon, Magic, One Piece, Yu-Gi-Oh! en Riftbound.
- ML Kit OCR voor Latijns, Chinees, Japans en Koreaans schrift.
- Online kaartzoekroutes via TCGdex, Scryfall, YGOPRODeck, OPTCG API en OpenRift, met manuele bevestiging.
- Lokale SQLite-collectie, dubbelen, aantallen, scanrecords en eigen prijsindicaties.
- Portfolio delen als tekst via Android.

## Wat is nog niet af
- Geen gegarandeerde identieke kaartmatch voor iedere druk, taal, set en foil.
- Geen geautoriseerde live Cardmarket-koppeling of automatisch bewezen actuele prijzen.
- Geen openbaar online portfolio, accounts, cloudsync of backup/export.
- Yu-Gi-Oh! en Riftbound gebruiken nog gestileerde placeholderafbeeldingen wegens voorwaarden en afbeeldingsrechten.
- Camera-, API- en layoutgedrag moeten nog op echte Android-toestellen door gebruikers worden getest.

De GitHub Actions-workflow voert unit-tests uit en bouwt een debug-APK. Een geslaagde workflow is geen garantie voor perfecte scans.
