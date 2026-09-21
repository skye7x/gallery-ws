# Gallery

A modern, native Android gallery app built with Kotlin and Jetpack Compose.

![Min SDK](https://img.shields.io/badge/minSdk-24-green)
![Target SDK](https://img.shields.io/badge/targetSdk-36-blue)
![Kotlin](https://img.shields.io/badge/kotlin-2.2.10-purple)
![Compose BOM](https://img.shields.io/badge/composeBom-2024.09-orange)

## Features

- **Photo & Video Browsing** — Grid views with All, Days, Months, and Years grouping
- **Editorial View** — Hero images with grouped thumbnails for a curated feel
- **Albums** — Auto-grouped by folder, with SD card detection
- **Search** — Full-text search across titles, albums, and dates with suggestions
- **For You** — Memory recaps, featured photos, and "On This Day" throwbacks with Ken Burns animation
- **Photo Details** — EXIF metadata viewer (camera, ISO, shutter speed, GPS)
- **Photo Editing** — Exposure, contrast, saturation, warmth adjustments with live preview
- **Selection Mode** — Multi-select with select all, batch delete with system confirmation
- **Favorites** — Mark and filter favorite photos
- **SD Card Support** — Detects and badges media stored on external SD cards
- **Adaptive Icon** — Flower icon with dynamic color theming

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Image Loading | Coil (with video frame thumbnails) |
| Database | Room |
| Architecture | MVVM with StateFlow |
| Networking | Retrofit + Moshi |
| Animations | Backdrop (liquid glass tabs) |

## Build

```bash
./gradlew assembleDebug
```

Requires Android Studio with AGP 9.1.1+ and JDK 21.

## Project Structure

```
app/src/main/java/com/bzygordev/gallery/
├── data/            # Room database, entities, DAO, repository, MediaStore scanner
├── ui/
│   ├── screens/     # Photos, Albums, ForYou, Search, PhotoDetailViewer
│   ├── components/  # MediaItemCard, PhotoInfoSheet, PhotoEditSheet, TopBar
│   └── GalleryMainScreen.kt, GalleryViewModel.kt
├── GalleryApplication.kt
```

## License

MIT
