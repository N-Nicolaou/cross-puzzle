# CrossPuzzle 🔢

**Module:** Mobile Applications — University of Westminster  
**Platform:** Android | **Language:** Kotlin | **UI:** Jetpack Compose

A crossword-style puzzle game for Android — but instead of words, the grid is filled with **arithmetic equations**. Solve the missing values to complete the puzzle!

---

## How It Works

The app generates a random grid and places arithmetic equations across and down it — just like a crossword, but with maths. One value in each equation is left blank for the player to fill in.

```
  ?  +  3  =  7       → player fills in 4
  ×
  2  -  1  =  1
  =
  6
```

---

## Features

- ➕ **Four operations** — addition, subtraction, multiplication, division
- 🎲 **Randomly generated** — every puzzle is unique, with a random grid size (11×11 to 20×20) and random equations
- 📐 **Dynamic layout** — equations placed horizontally and vertically without overlapping
- ✅ **Answer validation** — checks player input against the correct value
- 🎯 **Difficulty setting** — control how many equations appear in the puzzle
- 🔄 **Coroutine-based generation** — puzzle generation runs off the main thread to keep the UI smooth

---

## Technical Highlights

- **Puzzle generation algorithm** — places equations on a 2D grid array, tracking used cells to avoid conflicts between horizontal and vertical equations
- **Equation generation** — produces valid integer arithmetic (division always results in whole numbers)
- **Jetpack Compose UI** — fully declarative UI with `remember` and `rememberSaveable` for state persistence across recomposition
- **Coroutines** — puzzle generation dispatched to `Dispatchers.Default` to avoid blocking the UI thread

---

## Tech Stack

| Technology | Usage |
|------------|-------|
| Kotlin | Primary language |
| Jetpack Compose | Declarative UI |
| Coroutines | Background puzzle generation |
| Android Studio | IDE |

---

## Getting Started

1. Clone the repo
2. Open in **Android Studio**
3. Let Gradle sync
4. Run on an emulator or physical Android device

---

## Built With
- Kotlin
- Jetpack Compose
- Android Studio

