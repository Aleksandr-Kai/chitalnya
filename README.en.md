<p align="center"><img src="docs/icon.png" width="120" alt="Chitalnya"></p>

<h1 align="center">Chitalnya</h1>

<p align="center">
  A cosy reader for school textbooks in PDF on Android tablets.<br>
  Your folders stay intact, bookmarks have labels, the screen stays on.
</p>

<p align="center">
  <a href="README.md">🇷🇺 Русский</a> · 🇬🇧 English · <a href="https://aleksandr-kai.github.io/chitalnya/?lang=en">Presentation</a>
</p>

<p align="center">
<img src="docs/shots/library-home.jpg" width="49%" alt="Home">
<img src="docs/shots/library-books.jpg" width="49%" alt="Covers, “Library”">
<img src="docs/shots/scifi-books.jpg" width="49%" alt="“Sci-fi”">
<img src="docs/shots/scifi-reader.jpg" width="49%" alt="Reading">
</p>

---

## What is it

**Chitalnya** (Russian *читальня*, "reading room") is a free tablet app that turns a folder of PDF textbooks into a tidy library. Pick the folder once and the app arranges the books on shelves, mirroring your folder structure.

## Features

- 📚 **Library from a folder**: choose your textbooks folder; its structure is kept in the menu and on the home screen.
- 🖼 **Covers**: books appear as cards with the first page as cover, folders as separate tiles.
- 🔖 **Labelled bookmarks**: any number per book, each with its own label, plus a library-wide bookmark list.
- 🔢 **Jump to page**: tap the page counter.
- 💾 **Memory**: a book opens where you stopped; zoom is remembered per book and separately for portrait and landscape.
- 🔍 **Zoom**: pinch or buttons; portrait defaults to fit-to-width.
- 🎞 **Thumbnail strip**: shown beside the page when there is spare room, current page framed, tap to switch (can be turned off).
- 💡 **Screen stays on** while you read.
- 🖥 **Immersive mode**: system bars are hidden (optional).
- 🎨 **Themes**: "Library" (default), a sci-fi HUD dashboard theme, light, dark and system.

## Install

The app is not on Google Play; it installs from an APK file.

1. [Download Chitalnya.apk](https://github.com/Aleksandr-Kai/chitalnya/releases/latest/download/Chitalnya.apk) to your tablet.
2. Open the file and, if Android asks, allow installs from this source.
3. If Play Protect warns you, choose "Install anyway".

Requires Android 8.0 (API 26) or newer.

<details>
<summary>Build from source (for developers)</summary>

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

</details>

## Usage

1. Open **Settings** and choose your textbooks folder.
2. Books appear on the home screen; folders open as "galaxies" or "cabinets" depending on the theme.
3. Open a book, flip pages, zoom, add bookmarks.

The app only **reads** your files and sends nothing anywhere: it does not request internet access.

## Tech

Kotlin · Jetpack Compose (Material 3) · Storage Access Framework · `PdfRenderer`.

## License

[PolyForm Noncommercial 1.0.0](LICENSE): free to use, modify and share for **non-commercial** purposes. Commercial use and selling are not permitted.

For a commercial license, contact the author.
