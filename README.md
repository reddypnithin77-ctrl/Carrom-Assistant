# Carrom Assistant

An Android carrom practice app with a standalone offline automatic-play simulator and a separate manual floating aiming guide.

## Offline automatic simulator

- Draws its own simulated carrom board and coins.
- Selects a target and pocket, calculates a contact point, and animates the shot on the simulator board.
- Includes reset and repeatable single-shot testing.
- The simulated bot does not connect to, control, or interact with any third-party game.

## Manual floating guide

- Floating, draggable aiming guide.
- Manually adjustable aiming line with a live angle readout in degrees.
- Practice mode labels: own-colour, own-colour plus queen, and free-style.

The floating guide is manual. The automatic shot behavior exists only inside the app's offline simulator; there is no game injection, external screen capture, game-memory reading, or anti-cheat bypass.

## Build

Open the project in Android Studio and build the `app` debug variant. Android 10 (API 29) or newer is required for the overlay implementation.

A GitHub Actions workflow builds the debug APK on pushes to `main`.
