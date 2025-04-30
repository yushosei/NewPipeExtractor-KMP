# NewPipeExtractor-KMP

This repository is a Compose Multiplatform-compatible adaptation of the [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor), forked from version `v0.24.5`. It is intended for extracting YouTube videos and comments within Kotlin Multiplatform projects.

---

## 📌 Overview

- Migrated to **Kotlin (Multiplatform)**.
- Based on **NewPipeExtractor v0.24.5**.
- Internal modules have been refactored and improved for better readability, structure, and platform compatibility.
- **Public APIs and function call patterns remain unchanged** to ensure compatibility with the original usage.
- This project aims to serve as a foundation for integrating content extraction logic into Compose Multiplatform applications.

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

### nanojson Removal
- [ ] Remove `nanojson` and migrate to `kotlinx.serialization.json.*`

---

## 🧪 Testing & Integration
- [ ] Add shared `commonTest` cases
- [ ] Use `runBlocking` and mock data for multiplatform testability
- [ ] (Optional) Include Compose-based demo or preview usage

---

## 📦 Module Info
- **Forked by**: [@yushosei](https://github.com/yushosei)
- **Base version**: `NewPipeExtractor v0.24.5`

---

## 📄 License & Copyright

This project is licensed under the **GNU General Public License v3** or (at your option) any later version.

- New files and contributions in this repository are © 2025 [@yushosei](https://github.com/yushosei).
- All original content from the forked NewPipeExtractor remains © the [NewPipe Team](https://github.com/TeamNewPipe).
