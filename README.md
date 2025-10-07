# Android SpeechRecognizer – Developer Guide

## Overview
`SpeechRecognizer` is part of the Android Speech API (`android.speech.SpeechRecognizer`) that provides speech-to-text functionality. It allows apps to capture voice input from the user, convert it into text, and handle it in real-time (partial + final results).

---

## Key Classes
- **SpeechRecognizer**: Core engine for speech recognition.  
- **RecognizerIntent**: Configures recognition (language, silence timeout, etc).  
- **RecognitionListener**: Callback interface for recognition events.  

---

## Lifecycle
1. **Initialization**: Create `SpeechRecognizer` and set `RecognitionListener`.  
2. **Listening**: Call `startListening()` with `RecognizerIntent`.  
3. **Errors & Recovery**: Handle errors in `onError()`.  
4. **Cleanup**: Call `stopListening()` and `destroy()` in `onDestroy()`.  

---

## RecognizerIntent Settings
```kotlin
intentRecognizer = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 500)
    putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 500)
    putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 1000)
}
