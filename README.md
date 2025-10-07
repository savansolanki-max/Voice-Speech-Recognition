Android SpeechRecognizer – Developer Guide

Overview
SpeechRecognizer is part of the Android Speech API (android.speech.SpeechRecognizer) that provides
speech-to-text functionality. It allows apps to capture voice input from the user, convert it into text, and handle it in real-time (partial + final results).

Key Classes
SpeechRecognizer: Core engine for speech recognition.
RecognizerIntent: Configures recognition (language, silence timeout, etc).
RecognitionListener: Callback interface for recognition events.

Lifecycle
Initialization: Create SpeechRecognizer and set RecognitionListener.
Listening: Call startListening() with RecognizerIntent.
Errors & Recovery: Handle errors in onError().
Cleanup: Call stopListening() and destroy() in onDestroy().

RecognizerIntent Settings
intentRecognizer = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM) putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()) putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 500)
putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 500)
putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 1000)
}


RecognitionListener Callbacks
onReadyForSpeech(): Engine ready.
onBeginningOfSpeech(): User started speaking.
onRmsChanged(): Mic level (animate UI).
onPartialResults(): Streaming recognition.
onResults(): Final recognition.
onError(): Error handling.
onEndOfSpeech(): User stopped speaking.

Error Codes
1: Network timeout
2: Network error
3: Audio recording error 4: Server error
5: Client error
6: No speech input
7: Nothing recognized
8: Recognizer busy
9: Permission missing

Continuous Listening

By default, recognition stops after silence. To make it continuous, restart recognition in onResults() and onError(ERROR_NO_MATCH / ERROR_SPEECH_TIMEOUT).

Integration with Observer
Use SpeechObserver (singleton with LiveData) to decouple recognition engine from UI. Post partial/final results and mic RMS level.

Pros & Cons
Pros:
Free (uses built-in engine)
Multi-language support
Partial results (real-time)
Offline support

Cons:
Needs restart for continuous use
Accuracy depends on engine
Possible network delay
Sensitive to noise
Battery usage if kept running long.

Example Implementation
fun init() {
intentRecognizer = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM) putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()) putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 500)
putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 500)
}

recognizer = SpeechRecognizer.createSpeechRecognizer(context) recognizer?.setRecognitionListener(object : SimpleRecognitionListener() {
override fun onPartialResults(partialResults: Bundle?) {
val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.first text?.let { SpeechObserver.postResult(it, false) }
}

override fun onResults(results: Bundle?) {
val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull( text?.let { SpeechObserver.postResult(it, true) }
restartListening()
}

override fun onError(error: Int) { when (error) {
SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> restartListening() else -> Log.e("Speech", "Error code: $error")
}
}

override fun onRmsChanged(rmsdB: Float) { SpeechObserver.postRmsLevel(rmsdB)
}


🔹 Current config
putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 200)
putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 400)
putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 2000)

COMPLETE_SILENCE_LENGTH → how long (ms) of silence before recognizer finalizes speech.


POSSIBLY_COMPLETE_SILENCE_LENGTH → softer threshold (if it thinks user stopped).


MINIMUM_LENGTH → minimum audio length required before producing a result.



🔥 For ultra-fast response (but still accurate):
putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 100)   // finalize faster
putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 200) 
putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 500)           // allow short commands


⚡ Super-aggressive "instant mode" (useful for single word commands):
putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 50)
putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 100)
putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 200)

✅ Detects very quickly.
 ❌ May cut users off if they pause briefly while speaking.

🏆 Recommended Balanced Config (fast but stable for sentences):
putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 250)
putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 300)
putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 800)

Works great for commands & short sentences.


Keeps latency low without cutting off longer speech.


Gives better real-time partial results.



