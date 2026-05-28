package com.mathvoiceapp.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Wraps Android's SpeechRecognizer for seamless math voice input.
 *
 * Usage:
 *   val vrm = VoiceRecognitionManager(context)
 *   vrm.startListening(
 *     onPartial  = { text -> showLiveText(text) },
 *     onResult   = { text -> processAnswer(text) },
 *     onError    = { code -> showError(code) },
 *     onReady    = { animateMicPulse() },
 *     onEnd      = { stopMicAnimation() }
 *   )
 *   // Later:
 *   vrm.stopListening()
 *   vrm.destroy()  // in onDestroyView
 */
class VoiceRecognitionManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    var onReadyForSpeech: (() -> Unit)? = null
    var onBeginningOfSpeech: (() -> Unit)? = null
    var onPartialResult: ((String) -> Unit)? = null
    var onResult: ((String) -> Unit)? = null
    var onError: ((Int, String) -> Unit)? = null
    var onEndOfSpeech: (() -> Unit)? = null

    private var isListening = false

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening() {
        if (!isAvailable()) {
            onError?.invoke(-1, "Speech recognition not available on this device.")
            return
        }

        destroyRecognizer()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(buildListener())
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)           // top-5 hypotheses
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
        }

        isListening = true
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
    }

    fun destroy() {
        destroyRecognizer()
    }

    fun isCurrentlyListening() = isListening

    // ─── Private ───────────────────────────────────────────────────────────

    private fun destroyRecognizer() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }

    private fun buildListener() = object : RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) {
            isListening = true
            onReadyForSpeech?.invoke()
        }

        override fun onBeginningOfSpeech() {
            onBeginningOfSpeech?.invoke()
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Could expose for waveform visualisation; unused here
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: return
            onPartialResult?.invoke(partial)
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (matches.isNullOrEmpty()) {
                onError?.invoke(SpeechRecognizer.ERROR_NO_MATCH, "No speech detected.")
                return
            }

            // Pick the best hypothesis after math-aware processing.
            // We try each hypothesis; if one looks like a short, clean answer, prefer it.
            val best = selectBestHypothesis(matches)
            onResult?.invoke(best)
        }

        override fun onEndOfSpeech() {
            isListening = false
            onEndOfSpeech?.invoke()
        }

        override fun onError(error: Int) {
            isListening = false
            onError?.invoke(error, errorMessage(error))
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    /**
     * From multiple speech-to-text hypotheses, pick the one that looks
     * most like a math answer (shortest non-empty result wins as a tie-break,
     * but prefer one that contains recognisable math tokens).
     */
    private fun selectBestHypothesis(candidates: List<String>): String {
        val mathKeywords = listOf(
            "x", "pi", "π", "sqrt", "sin", "cos", "tan", "log", "ln",
            "infinity", "∞", "plus", "minus", "times", "divided", "squared",
            "cubed", "root", "over", "equals", "negative", "positive", "e"
        )

        // Score each candidate: prefer shorter and math-rich
        data class Scored(val text: String, val score: Int)
        val scored = candidates.map { c ->
            val lower = c.lowercase()
            val mathScore = mathKeywords.count { k -> lower.contains(k) } * 2
            val lengthScore = if (c.length < 20) 3 else 0
            Scored(c, mathScore + lengthScore)
        }
        return scored.maxByOrNull { it.score }?.text ?: candidates.first()
    }

    private fun errorMessage(code: Int) = when (code) {
        SpeechRecognizer.ERROR_AUDIO            -> "Audio recording error."
        SpeechRecognizer.ERROR_CLIENT           -> "Client-side error."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Missing RECORD_AUDIO permission."
        SpeechRecognizer.ERROR_NETWORK          -> "Network error. Try offline mode."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT  -> "Network timed out."
        SpeechRecognizer.ERROR_NO_MATCH         -> "Didn't catch that – tap to try again."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY  -> "Recogniser busy – please wait."
        SpeechRecognizer.ERROR_SERVER           -> "Server error."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT   -> "No speech detected – tap to try again."
        else                                    -> "Unknown error ($code)."
    }
}
