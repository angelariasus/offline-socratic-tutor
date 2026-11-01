# YachayText

Tutor socrático y multimodal que funciona sin internet, para la Educación Intercultural Bilingüe (castellano–quechua). Proyecto final de Desarrollo de Aplicaciones Móviles, UNMSM, Grupo 6.

## Stack

Kotlin · Jetpack Compose (Material 3) · MVVM · Room/SQLite · AGP 9.3 · Gradle 9.5. Más adelante: CameraX + ML Kit (OCR), MediaPipe Text Embedder, LiteRT-LM (SLM INT4), Vosk (STT) y TTS nativo.

Objetivo de hardware: Android 9 (API 28) o superior, ARM64, 2 GB de RAM.

## Comandos

```bash
./gradlew assembleDebug            # APK de depuración
./gradlew testDebugUnitTest        # tests (Room, embeddings)
./gradlew recordRoborazziDebug     # genera PNG de las pantallas en app/screenshots/
```

## Estructura

```
app/src/main/java/pe/edu/unmsm/yachaytext/
├── data/local/   Room: entidades del modelo entidad-relación, DAOs, codec de embeddings
└── ui/           Compose: tema, navegación y pantallas
docs/alcance.md   Alcance de pantallas (entregables vs. Figma)
```
