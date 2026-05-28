# Math Voice — Android App

A voice-first math quiz app covering every major topic from basic arithmetic to calculus.
Speak your answers naturally — the engine understands equivalent forms like "x squared", "x^2", "one half", "1/2" etc.

---

## ✨ Features

| Feature | Details |
|---|---|
| **8 Math Topics** | Arithmetic · Algebra · Geometry · Trigonometry · Functions · Limits · Differentiation · Integration |
| **3 Difficulty Levels** | Easy → Medium → Hard (progressive unlocking for calculus topics) |
| **200+ Questions** | Shuffled on every session; no repeat runs |
| **Flexible Voice Input** | Accepts spoken equivalents, fractions, symbols, and natural language |
| **Live Partial Results** | See your spoken words appear in real-time as you speak |
| **Immediate Feedback** | Animated ✓/✗ card with correct answer + explanation |
| **Hint System** | Tap 💡 for a nudge without spoiling the answer |
| **Score Summary** | Percentage + motivational message on completion |
| **Dark Theme** | Easy on the eyes, colourful topic cards |

---

## 🗂 Project Structure

```
MathVoiceApp/
├── app/src/main/
│   ├── java/com/mathvoiceapp/
│   │   ├── MainActivity.kt                  # Single-activity host
│   │   ├── data/
│   │   │   ├── MathQuestion.kt              # Data classes + @Parcelize enums
│   │   │   └── QuestionBank.kt             # 200+ questions, all topics
│   │   ├── voice/
│   │   │   ├── VoiceRecognitionManager.kt   # SpeechRecognizer wrapper
│   │   │   └── AnswerEvaluator.kt          # Flexible math answer matching
│   │   └── ui/
│   │       ├── home/HomeFragment.kt         # Topic + difficulty picker
│   │       └── quiz/
│   │           ├── QuizViewModel.kt         # Quiz state (LiveData)
│   │           ├── QuizFragment.kt          # Quiz UI + mic animation
│   │           └── ResultFragment.kt        # Score screen
│   └── res/
│       ├── layout/                          # 3 fragment layouts + activity
│       ├── navigation/nav_graph.xml         # Navigation + Safe Args definitions
│       ├── drawable/                        # Vector icons, mic backgrounds
│       ├── anim/                            # Slide transition animations
│       ├── mipmap-*/                        # Launcher icons (all densities)
│       └── values/                          # colors, strings, themes
```

---

## 🛠 Build Instructions

### Requirements
- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 17** (bundled with Android Studio)
- **minSdk 24** (Android 7.0+), **targetSdk 34**

### Steps

1. **Open the project**
   ```
   File → Open → select the MathVoiceApp folder
   ```

2. **Let Gradle sync** (it will download ~50 MB of dependencies on first run)

3. **Run on device or emulator**
   ```
   Run → Run 'app'   (Shift+F10)
   ```
   > Voice recognition works best on a **real device** with an internet connection
   > (Android's SpeechRecognizer uses Google's servers by default).
   > On an emulator, enable the microphone in AVD settings.

4. **Build a release APK**
   ```bash
   ./gradlew assembleRelease
   # Output: app/build/outputs/apk/release/app-release-unsigned.apk
   ```

---

## 🎙 Voice Recognition Tips

The app uses Android's built-in `SpeechRecognizer` (Google Speech Services).

**What it understands:**

| You say | Matched answer |
|---|---|
| "x squared" | x² |
| "one half" / "half" | 1/2 |
| "square root of three" | √3 |
| "negative one" | -1 |
| "pi over two" | π/2 |
| "e to the x" | eˣ |
| "does not exist" | DNE |
| "two x sin x plus x squared cos x" | 2x sinx + x² cosx |

**Tips:**
- Speak clearly and at a normal pace
- You can tap the mic again to **stop listening** early
- If recognition fails, just tap again — it retries cleanly
- Partial results appear live so you can see what it's hearing

---

## 🧠 Architecture

```
HomeFragment
    │  (navArgs: Topic, Difficulty)
    ▼
QuizFragment ◄──── QuizViewModel (LiveData<QuizState>)
    │                    │
    │              QuestionBank.kt  (200+ questions, filtered + shuffled)
    │
    ├── VoiceRecognitionManager   (SpeechRecognizer wrapper)
    │       └── onResult → AnswerEvaluator.evaluate()
    │
    └── (on finish) ResultFragment
```

**AnswerEvaluator pipeline:**
1. Normalise raw speech (spoken words → symbols)
2. Fuzzy string match vs canonical + all accepted forms
3. Numeric evaluation (`"0.5"` == `"1/2"` == `"half"`)
4. Substring match (catches "x = 7" inside "x equals seven")

---

## ➕ Adding More Questions

Open `QuestionBank.kt` and add a new `MathQuestion` to the `buildList`:

```kotlin
MathQuestion(
    id               = "al_h_11",           // unique ID: topic_difficulty_number
    topic            = Topic.ALGEBRA,
    difficulty       = Difficulty.HARD,
    question         = "Solve: x² - 6x + 9 = 0",
    answer           = "x = 3",
    acceptedAnswers  = listOf("3", "x=3", "x equals 3"),
    hint             = "It's a perfect square",
    explanation      = "(x - 3)² = 0"
)
```

---

## 📦 Dependencies

| Library | Purpose |
|---|---|
| AndroidX Navigation 2.7.6 | Fragment navigation + Safe Args |
| Material 3 | UI components |
| ConstraintLayout 2.1.4 | Layouts |
| Lifecycle ViewModel/LiveData 2.7.0 | MVVM state |
| CardView | Question + feedback cards |
| Android SpeechRecognizer | Voice input (built-in, no extra dep) |

---

## 📄 License
MIT — free to use, modify, and distribute.
