package nl.tcgvault.app.scan

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** OCR helpt bij invoer. Exacte kaart, druk, taal en conditie blijven te bevestigen door gebruiker. */
object TekstHerkenner {
    suspend fun lees(context: Context, uri: Uri): String {
        val afbeelding = InputImage.fromFilePath(context, uri)
        val lezers: List<TextRecognizer> = listOf(
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
            TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()),
            TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()),
            TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()),
        )
        val regels = linkedSetOf<String>()
        try {
            for (lezer in lezers) {
                val tekst = suspendCancellableCoroutine<String> { vervolg ->
                    lezer.process(afbeelding)
                        .addOnSuccessListener { resultaat -> if (vervolg.isActive) vervolg.resume(resultaat.text) }
                        .addOnFailureListener { if (vervolg.isActive) vervolg.resume("") }
                        .addOnCanceledListener { if (vervolg.isActive) vervolg.resume("") }
                }
                tekst.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.forEach { regels.add(it) }
            }
        } finally { lezers.forEach { it.close() } }
        return regels.joinToString("\n")
    }
}

object KaartTekstParser {
    private val kaartnummerPatroon = Regex("(?<![\\p{L}\\d])(?:[A-Z]{0,4}[- ]?)?\\d{1,4}[/\\-]\\d{1,4}(?!\\d)", RegexOption.IGNORE_CASE)
    fun kaartnummer(tekst: String): String = kaartnummerPatroon.find(tekst)?.value?.trim() ?: ""
    fun naamVoorstel(tekst: String): String = tekst.lineSequence()
        .map { it.trim() }
        .firstOrNull { regel -> regel.length in 3..50 && regel.any { it.isLetter() } && !regel.contains("/") }
        .orEmpty()
}
