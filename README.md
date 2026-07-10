# NewPipeExtractor-KMP

This repository is a Compose Multiplatform-compatible adaptation of the [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor). The migration started from `v0.24.5` and includes selected service-compatibility fixes from newer upstream versions. It is intended for extracting YouTube and SoundCloud content within Kotlin Multiplatform projects.

---

## 📌 Overview

- Migrated to **Kotlin (Multiplatform)**.
- Migration base: **NewPipeExtractor v0.24.5**, with targeted current-service compatibility fixes.
- Internal modules have been refactored and improved for better readability, structure, and platform compatibility.
- Replaced `nanojson` with a **pure Kotlin implementation**.
- **Public APIs and function call patterns remain unchanged** to ensure compatibility with the original usage.
- ✅ **Audio stream extraction supports both YouTube and SoundCloud**.
- ✅ **YouTube progressive and video-only stream extraction is available**.
- This project aims to serve as a foundation for integrating content extraction logic into Compose Multiplatform applications.

---

## 🚀 Usage

### 1. Add Dependency

```kotlin
implementation("io.github.yushosei:newpipe-extractor-kmp:1.3.0")
```

### 2. Initialization

```kotlin
NewPipe.init(DefaultDownloaderImpl.initDefault())
```

### 3. Search

```kotlin
ExtractorHelper.searchFor(
    SERVICE_ID, searchText, listOf("videos"),
    "",
)
```

### 4. Get More Search Items

```kotlin
ExtractorHelper.getMoreSearchItems(
    SERVICE_ID, searchText, listOf("videos"), "", page
)
```

### 5. Search Suggestions

```kotlin
ExtractorHelper.suggestionsFor(SERVICE_ID, searchText)
```

### 6. Stream Extraction

```kotlin
val info = ExtractorHelper.getStreamInfo(SERVICE_ID, item.url)

val audioStreams = info.audioStreams
val progressiveVideoStreams = info.videoStreams // video + audio
val adaptiveVideoStreams = info.videoOnlyStreams // pair with an audio stream
```

For more details, refer to the sampleApp.



## 🎥 Demo Video

## Android & iOS

https://github.com/user-attachments/assets/d4221163-619f-49fa-83a5-48b580da29af

## Web

https://github.com/user-attachments/assets/5f8ab309-4e46-40fb-9911-ff82c18c5a3e

## Desktop

https://github.com/user-attachments/assets/35e628ea-50ad-4cba-a970-7e5cb4705823




> ✅ The sample supports **audio and video playback modes**. YouTube uses progressive video streams with embedded audio, while SoundCloud remains audio-only.
> Platform video surfaces are implemented for Android (Media3), iOS (AVPlayer), desktop (VLCJ), and Wasm (HTML video).
> All core logic is written in **pure Kotlin**, making it platform-independent and theoretically usable across **desktop and web environments** too.  
>  
> The current implementation has been verified on Android and iOS targets using Compose Multiplatform UI.  
>  
> 📦 We plan to release a cleaned-up and library-ready version of the module in the near future to facilitate reuse and integration !


## ⚠️ Note on `nanojson` Replacement

This project includes a reimplementation of `nanojson` in **pure Kotlin**, rewritten from scratch to work within the Kotlin Multiplatform (NewPipeExtractor).

- ✅ The implementation has been **tested in conjunction with NewPipe** and verified to extract data correctly.
- ⚠ **However, this Kotlin version of nanojson is an interim solution.** It was built for functional compatibility and **does not guarantee performance parity** with the original Java version.
- It exists **only to enable current operation** and **may be replaced in the future** with a more robust or performant JSON parser tailored for KMP.

---

## ✅ Migration Themes & Checklist

### Java Dependency Removal
- [ ] Remove core Java-only APIs (e.g. `Serializable`, `Optional`, `Pattern`, `URL`, `Objects`)
- [ ] Replace Java collections, streams, and IO with Kotlin equivalents

### Rhino JavaScript Handling
- [ ] Decide on Rhino integration approach (e.g. maintain, isolate, or replace)

### Custom Kotlin Object Replacement
- [ ] Convert Java utility classes (`Pair`, `ManifestCache`, etc.) to idiomatic Kotlin

### Serialization Strategy Change
- [ ] Replace `Serializable` with `@Serializable` and `kotlinx.serialization`

---

### Lightweight JSON Optimization
- [x] Remove `nanojson` dependency
- [x] Reimplement `nanojson` using pure Kotlin logic
- [ ] Evaluate long-term direction for JSON handling:
  - [ ] Convert to a **pure Kotlin @Serializable-based structure**
  - [ ] More faithfully replicate **original nanojson behavior** for performance and features

---

### Feature Migration Status (NewPipeExtractor Modules)
> Core functionality has been migrated but not yet fully cleaned or published.

- [x] Search functionality migrated to Kotlin
- [x] Search suggestion handling migrated to Kotlin
- [x] Stream extraction (YouTube video info) migrated to Kotlin
- [x] Audio stream extraction (YouTube, SoundCloud) migrated to Kotlin
- [x] Progressive and video-only stream extraction (YouTube) migrated to Kotlin
- [x] Compose Multiplatform sample video playback surfaces
- [ ] Code cleanup and module publishing pending

---

## 🧪 Testing & Integration
- [x] Add shared `commonTest` cases
- [x] Use `runBlocking` and mock data for multiplatform testability
- [x] Include a Compose Multiplatform audio/video demo

Run deterministic tests with:

```shell
./gradlew :newpipe-KMP:desktopTest
```

Run opt-in live service smoke tests with:

```shell
./gradlew :newpipe-KMP:desktopTest -Dnewpipe.liveTests=true
```

---

## 📦 Module Info
- **Forked by**: [@yushosei](https://github.com/yushosei)
- **Migration base**: `NewPipeExtractor v0.24.5`
- **Library version**: `1.3.0`

---

## 📄 License & Copyright

This project is licensed under the **GNU General Public License v3** or (at your option) any later version.

- New files and contributions in this repository are © 2025 [@yushosei](https://github.com/yushosei).
- All original content from the forked NewPipeExtractor remains © the [NewPipe Team](https://github.com/TeamNewPipe).
