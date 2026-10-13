# Jarvis Assistant (Android)

Voice assistant: say "Hey Jarvis", then a command. Offline speech recognition (Vosk)
-> Groq LLM function-calling decides the action -> Android executes it -> TextToSpeech replies.

## Pipeline
Vosk (continuous, offline) -> wake word "jarvis" -> command text -> GroqClient (tool calling) -> ActionExecutor -> JarvisSpeaker

## Setup
1. Open the project in Android Studio, let Gradle sync.
2. Get a free API key from https://console.groq.com and put it in
   `JarvisService.kt` -> `BuildConfigPlaceholder.GROQ_API_KEY` (never commit the real key).
3. Download the Vosk model `vosk-model-small-en-us-0.15` from https://alphacephei.com/vosk/models,
   unzip, rename the folder to `model-en-us`, put it in `app/src/main/assets/`.
4. Run on a physical device, tap "Start Jarvis", allow all permissions.
5. Keep Jarvis exempt from battery optimization (Settings -> Battery) so it keeps listening with screen off.

## Tools Jarvis can use
make_call, open_app, write_note, send_message, set_reminder, set_alarm,
toggle_flashlight, check_battery, search_web.

## Adding a new capability
1. Declare it in `brain/ToolRegistry.kt`
2. Implement it in `actions/ActionExecutor.kt`

## History / notes
- Wake-word attempts that did not work out: Picovoice Porcupine (signup needs company email),
  openWakeWord TFLite (melspectrogram model failed to allocate: CONV_2D prepare error),
  Android SpeechRecognizer from a Service (constant ERROR_NO_MATCH = 7). Vosk is the current approach.
