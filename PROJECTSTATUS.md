# TCG Vault NL — projectstatus

**Native Android bèta 0.6.0 — 29 oktober 2026**

## Nieuw in 0.6
- Transparante matchscore uit OCR-naam, nummerfractie en volledige kaartcodes met kwalitatieve tekstlabels. Geen foutieve claim van officiële herkenningszekerheid.
- Pokémon: meerdere cataloguspagina's bij een leesbaar kaartnummer; Magic: meer fysieke drukken; Yu-Gi-Oh!: meer setvarianten; One Piece: zoekroute op volledige code.
- In matchvenster kan de gebruiker een verkeerd OCR-gelezen kaartnaam direct verbeteren en de zoekopdracht herhalen.
- Extra JVM-unit-tests van One Piece- en Yu-Gi-Oh!-setcodes, Pokémon-kaartnummers, internationale Unicode-schrijfwijzen en Japanse namen.
- Bestaande collecties, back-upformaat en databaseversie zijn ongewijzigd. De debug-bèta blijft een apart pakket `nl.tcgvault.app.beta`.
- Belangrijk: OCR-tekstmatch is geen visuele artwork-match. Exacte editie, set, taal en foil moeten nog bevestigd worden.

## Nieuw in 0.5
- Kaartdetailvenster heeft de actie **Bewerken**: corrigeer naam, set, kaartnummer, taal, conditie, uitvoering, aantal, eigen prijs en notitie.
- Bij een correctie naar een al bestaande variant combineert een SQLite-transactie de aantallen en verplaatst de scanmomenten zonder dubbel verlies. Notities worden bewaard.
- Scanmomenten zijn doorzoekbaar in een apart scherm met spel-, naam- en OCR-tekstfilter; de UI beperkt de lijst tot de recentste 500 voor prestaties. Back-up bevat de volledige historie.
- De laatste kaart kan niet meer door een losse tik op de minknop verdwijnen. Verwijderen vereist een aparte bevestiging in het detailvenster.
- Extra JVM-tests voor het pure samenvoegmodel: aantallen, notities, grenswaarden, prijs en afwijkende varianten.
- **Geen database- of JSON-back-upformaatwijziging** ten opzichte van 0.4. Bestaande collecties en eerdere 0.4-back-ups blijven bruikbaar.

## Nieuw in 0.4
- Leesbare, draagbare JSON-back-ups voor alle kaarten, aantallen, varianten, talen, conditie, eigen prijzen, catalogusverwijzingen, notities en volledige OCR-scanmomenten.
- Android-bestandskiezer voor lokaal/cloud opslaan en selecteren; geen eigen back-upserver of account nodig.
- Vooraf valideren van bestandstype, versie, kaart-ID's, scanverwijzingen en limieten.
- Herstellen vervangt de huidige collectie uitsluitend na expliciete bevestiging, binnen één SQLite-transactie. Bij een fout blijft de oude collectie behouden.
- Collectie sorteren op recent, naam A–Z, meeste exemplaren, meeste dubbelen of hoogste eigen ingevoerde waarde.
- Portfolio toont de twaalf recentste opgeslagen scans met kaartspel en tijdstip.
- JVM-tests voor back-up roundtrip (prijs, taal, dubbelen, metadata, scanmomenten) en ongeldige back-ups.

## Reeds beschikbaar in 0.3
- Nederlandstalige premium donkere Android-interface, scanstudio en kaartdetailvenster.
- ML Kit foto-OCR (Latijn, Japans, Chinees, Koreaans); online catalogusmatching via TCGdex, Scryfall, YGOPRODeck, OPTCG API en OpenRift.
- Kaartvoorstellen in een bevestigingsscherm; afbeelding waar toegestaan en beschikbaar.
- Lokale SQLite-collectie, dubbelen, aantallen, optionele eigen prijs en tekstportfolio.

## Niet gereed
- Geen officiële live Cardmarket-koppeling of geverifieerde realtime Cardmarket-prijzen.
- Geen gegarandeerde herkenning van elke kaart, editie, taal of speciale uitvoering.
- Geen publieke accountportfolio's, cloudsync of automatische cloudback-up.
- Back-ups bevatten geen originele camerafoto's. JSON-back-ups kunnen persoonlijke notities/OCR-tekst bevatten: bewaar ze veilig.
- De GitHub debug-APK is bedoeld voor testen; camera en scanaccuracy blijven te controleren op een echt toestel.

Zie [README.md](README.md) voor installatiestappen en gebruik van back-up/herstel.
