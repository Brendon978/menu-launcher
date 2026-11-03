package com.brendon978.menulauncher.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/** Ouve enquanto o dedo segura o símbolo; ao soltar entrega o texto (ou null se não entendeu). Só na thread principal. */
class VoiceCapture(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null

    fun start(onResult: (String?) -> Unit) {
        cancel()
        val r = if (SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
        recognizer = r
        var delivered = false
        fun deliver(text: String?) {
            if (delivered) return
            delivered = true
            onResult(text?.trim()?.takeIf { it.isNotEmpty() })
            cancel()
        }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) =
                deliver(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull())
            override fun onError(error: Int) = deliver(null)
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        r.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true),
        )
    }

    /** Soltou o dedo: para de ouvir e espera o resultado. */
    fun stop() = recognizer?.stopListening()

    fun cancel() {
        recognizer?.destroy()
        recognizer = null
    }
}
