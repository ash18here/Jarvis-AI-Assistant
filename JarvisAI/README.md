# Jarvis AI — Android Voice Assistant (Starter)

A Kotlin Android starter project for a personal assistant. This first milestone is deliberately small so it can be built and tested before adding speech recognition and cloud AI.

## Implemented in this starter
- Android app screen with command input
- Android Text-to-Speech response
- Battery percentage query
- Open Google and YouTube in the browser
- Basic command routing

## Not implemented yet
- Vosk offline voice recognition
- Groq AI chat integration
- Flashlight control
- Notes and reminders
- Background listening / foreground service

These are planned milestones, not features that currently work in this starter.

## Requirements
- Android Studio with JDK 17 support
- Android SDK Platform 35 (or install the SDK version Android Studio prompts for)
- Android device or emulator running Android 8.0 (API 26) or newer

## Run
1. Extract the project ZIP.
2. In Android Studio, choose **Open** and select the `JarvisAI` folder.
3. Allow Gradle sync to finish. If prompted, install the Android SDK components requested by Android Studio.
4. Run the `app` configuration on an emulator or Android phone.
5. Try `battery`, `open Google`, `open YouTube`, or `help` in the command box.

## Next milestones
1. Add microphone permission flow and Vosk offline model assets.
2. Add Groq chat with the API key kept out of Git.
3. Add deterministic tool routing for Android actions.
4. Add notes/reminders and test repeated commands, lifecycle cleanup, and audio focus.

## Security
Never commit API keys, passwords, signing keys, or `local.properties`. Do not hard-code a production API key in the app; a mobile APK can be reverse-engineered. For a portfolio demo, use a backend proxy for any secret key.

## License
No license has been selected yet. Add a license before accepting reuse or contributions.
