<div align="center">

# 🎵 Sonora

### A multi-source, Material 3 **Expressive** music player for Android

Search once, play from the best available source — with automatic fallback so the music never stops.

Inspired by [InnerTune](https://github.com/z-huang/InnerTune), built from scratch with a pluggable, provider-agnostic core.

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Min SDK](https://img.shields.io/badge/min%20SDK-26-blue)
![Language](https://img.shields.io/badge/Kotlin-100%25-7F52FF?logo=kotlin&logoColor=white)
![UI](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
[![Release](https://img.shields.io/github/v/release/rifatrajbd/Sonora?include_prereleases&color=FF7A59)](https://github.com/rifatrajbd/Sonora/releases/latest)
[![Download](https://img.shields.io/github/downloads/rifatrajbd/Sonora/total?color=FF7A59)](https://github.com/rifatrajbd/Sonora/releases)

</div>

---

## ✨ Features

- 🔀 **Multiple providers, one app** — a pluggable source layer you configure yourself
- 🎧 **Unified search** across every configured provider, merged and de-duplicated
- 🛟 **Automatic fallback** — if one provider can't stream a track, Sonora silently uses another
- 🏷️ **Quality chips** — see **HiFi · Hi-Res · HD** at a glance, Spotify-style
- ▶️ **Gapless background playback** (Media3/ExoPlayer) with a real queue, next/prev & auto-advance
- 🔒 **Lock-screen & notification controls**, Bluetooth / Android Auto ready
- ❤️ **Library** — liked songs, playlists, downloads
- 🎨 **Material 3 Expressive UI** — dynamic album-art color, AMOLED-black theme, glass Now Playing
- 🔧 **Remote config** — providers are hot-swappable from a hosted JSON, **no app update needed**
- 🚫 **No ads**

> **Coming next:** synced lyrics, offline downloads, home feed, discovery.

---

## 📥 Download

Grab the latest APK from the [**Releases**](https://github.com/rifatrajbd/Sonora/releases/latest) page and side-load it.

> Providers are **not** bundled or configured out of the box — Sonora ships as a player shell.
> You supply your own provider endpoints via `config.json` / `local.properties`.

---

## 🧱 Tech stack

| Layer | Choice |
|-------|--------|
| Language / UI | Kotlin · Jetpack Compose · Material 3 Expressive |
| Playback | Media3 / ExoPlayer |
| Storage | Room |
| DI | Hilt |
| Networking | Retrofit · OkHttp · kotlinx.serialization |
| Images | Coil |

### Architecture

```
UI (Compose) ─► MusicRepository ─► SourceResolver ─► [ pluggable MusicSource providers ]
                     │                   │ failover
                     └► Room (library)   └► resolve stream URL on demand
PlayerConnection ─► PlaybackService (Media3/ExoPlayer)
RemoteConfigRepository ─► hosted config.json  (hot-swap providers, no APK update)
```

Every provider implements one `MusicSource` interface (`search`, `getStream`, `getLyrics`, `isHealthy`).
Adding a provider = one class + one line of DI. Provider implementations are configuration, not part
of the published shell.

---

## 🔧 Build it yourself

```bash
git clone https://github.com/rifatrajbd/Sonora.git
cd Sonora
./gradlew :app:assembleDebug        # or open in Android Studio and Run
```

Requires JDK 17+ (JDK 21 tested) and Android SDK 35. Configure your provider endpoints in
`local.properties` or the hosted `config.json` — see [`config.sample.json`](config.sample.json).

---

## ⚠️ Disclaimer

Sonora is a **player shell** for **personal, educational use**. It bundles no content and no
provider credentials; how you configure and use it is your responsibility. Respect the terms of
any service you connect it to.

Sonora is not affiliated with any streaming service or with InnerTune.

---

<div align="center">

Built with ☕ and Kotlin · Inspired by [InnerTune](https://github.com/z-huang/InnerTune)

</div>
