# NewPipeExtractor-KMP

This repository is a Compose Multiplatform-compatible adaptation of the [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor), forked from version `v0.24.5`. It is intended for extracting YouTube videos and comments within Kotlin Multiplatform projects.

---

## 📌 Overview

- Migrated to **Kotlin (Multiplatform)**.
- Based on **NewPipeExtractor v0.24.5**.
- Internal modules have been refactored and improved for better readability, structure, and platform compatibility.
- Replaced `nanojson` with a **pure Kotlin implementation**.
- **Public APIs and function call patterns remain unchanged** to ensure compatibility with the original usage.
- This project aims to serve as a foundation for integrating content extraction logic into Compose Multiplatform applications.

---



## 🎥 Demo Video

## Android & iOS

https://github.com/user-attachments/assets/d4221163-619f-49fa-83a5-48b580da29af

## Web

https://github.com/user-attachments/assets/5f8ab309-4e46-40fb-9911-ff82c18c5a3e

## Desktop

https://github.com/user-attachments/assets/35e628ea-50ad-4cba-a970-7e5cb4705823




> ✅ This demo showcases **successful audio stream extraction** and **search suggestions retrieval** on both **Android** and **iOS** using Kotlin Multiplatform code.  
> Although the test focused on audio streams for simplicity, the extractor is also capable of handling **video streams**.
> 
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
- [x] Audio stream extraction migrated to Kotlin
- [ ] Code cleanup and module publishing pending

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
