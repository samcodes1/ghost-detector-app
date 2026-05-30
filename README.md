# Ghost Finder — Ghost Detector

A spooky augmented-reality ghost-hunting app for Android, built natively with
**Kotlin + Jetpack Compose** and powered by **ARCore + SceneView**.

> *Ambient sensors detect spectral disturbances in your environment. A child suddenly
> sprints past you. A shadow lingers in the corner — until you approach.*

## Features

- 🎬 **Splash** — animated ghost emblem with drifting fog and star-field
- 🏠 **Home** — pulsing "Hunt" button, live ambient EMF meter, encounter stats
- 📡 **AR Detector** — full-screen `ARScene` with HUD overlay:
  - 5-bar EMF meter + sweeping scanner reticle
  - Plane detection (horizontal & vertical)
  - Tracking-state feedback ("too dark", "hold steady", …)
  - Live encounter log with color-coded ghost type
  - Haptic + visual alert when a presence appears
- 👻 **Ghost behaviors**
  - **Running Kid** — spawns 6–14 s into the session, sprints across a detected
    plane, fades out
  - **Corner Shadow** — anchored at the plane's edge; **fades away as the user
    approaches** (proximity ≤ 1.2 m)
- 🎨 Custom dark "void" Material 3 palette with cyan / plasma / spectral accents

## Why the name "Ghost Finder"?

- **Phasma** = Latin for *phantom / apparition* — distinctive, brandable, low
  search competition on the Play Store
- Subtitle **"Ghost Detector"** captures the high-volume queries:
  *ghost detector, ghost camera, AR ghost, paranormal scanner*
- Result: discoverable for the category while owning a unique brand keyword

## Project structure

```
app/src/main/java/com/phasma/ghostdetector/
├── MainActivity.kt
├── PhasmaApp.kt
├── ui/
│   ├── theme/         (Color, Type, Theme — dark "void" palette)
│   ├── navigation/    (PhasmaNavHost — Splash → Home → Detector)
│   ├── splash/        (animated emblem + fog)
│   ├── home/          (pulsing CTA + ambient meter + stats)
│   ├── detector/      (DetectorScreen + DetectorViewModel)
│   └── components/    (FogBackground, GhostEmblem, EmfMeter)
└── ar/
    ├── GhostModels.kt  (GhostType / GhostEvent)
    └── GhostManager.kt (plane picking, spawning, lifecycle, proximity)
```

## Build & run

### Prereqs
- Android Studio Hedgehog (or newer) — needs **AGP 8.5+**
- JDK 17
- A physical Android device — **ARCore requires real hardware** (no emulator support)
- ARCore-supported device list: <https://developers.google.com/ar/devices>

### Steps
1. `cd "PhasmaAR/"`
2. Open in Android Studio → "Open project"
3. Sync Gradle (downloads SceneView 2.2 + ARCore deps)
4. Plug in an ARCore-compatible phone (USB debugging on)
5. Run `app` configuration

First launch will prompt for **Camera** permission and, on Play-Services
devices, may install/update the **Google Play Services for AR** APK from
the Play Store.

### Gradle wrapper
The wrapper `.properties` is included; if you don't have the gradlew JAR,
run `gradle wrapper` once from the project root using a local Gradle 8.9+
install to populate it.

## How the AR works

`GhostManager.onFrame()` is invoked every AR frame from `ARScene`'s
`onSessionUpdated`. It:

1. Picks the largest stable horizontal plane.
2. Schedules the corner shadow at ~3 s once a plane is tracked.
3. Spawns periodic running-kid events on a random 14–22 s cadence.
4. Drives per-frame motion: kid lerps along a path with bobbing + fade
   in/out; shadow fades as the camera distance crosses 1.2 m → 0.6 m.
5. Aggregates a 0–1 spectral signal back to the HUD EMF meter.

Ghost bodies render from **CC0 GLB models** that ship in
`app/src/main/assets/models/`:

- `ghost_kid.glb` — by [Quaternius](https://poly.pizza/m/Iip30bDHmu), CC0
- `ghost_shadow.glb` — by [Polygonal Mind](https://poly.pizza/m/CKLHPoYhE9), CC0

If the load fails (corrupt file, unsupported feature) `GhostManager` falls
back to procedural primitives so the app stays runnable. See
[assets/models/README.md](app/src/main/assets/models/README.md) for swapping
in your own models.

### Credits
- **Ghost (Quaternius)** — [poly.pizza/m/Iip30bDHmu](https://poly.pizza/m/Iip30bDHmu), CC0
- **Ghost Character (Polygonal Mind)** — [poly.pizza/m/CKLHPoYhE9](https://poly.pizza/m/CKLHPoYhE9), CC0

## Permissions

- `android.permission.CAMERA` — required, prompted at runtime
- `android.permission.VIBRATE` — for haptic stings on encounters

## Roadmap

- [ ] Spirit-box audio EVP playback on encounter (random whispers from `res/raw/`)
- [ ] Photo-capture button that bakes the current AR frame to gallery
- [ ] Persistent stats (Room) — replace `HomeState` defaults
- [ ] Settings: model selection, sensitivity slider, haptic toggle
- [ ] Multiple simultaneous ghost types
- [ ] Floating "ghost orb" particles using Filament instanced meshes

## Tech

- Kotlin 2.0.21
- Jetpack Compose (BOM 2024.10) + Material 3
- Navigation-Compose 2.8
- Accompanist Permissions 0.34
- **SceneView 2.2.1** (Compose AR wrapper for ARCore + Filament)
- ARCore 1.45.0
