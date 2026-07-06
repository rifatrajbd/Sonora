# Sonora 🎵

A multi-source, Material 3 **Expressive** music player for Android — inspired by
[InnerTune](https://github.com/z-huang/InnerTune), but not tied to a single backend.

Sonora streams from **five private/trick-based sources** behind one pluggable interface, with
automatic failover so playback never dies when an upstream API changes.

## Sources

| Source | How it's accessed | Quality |
|--------|-------------------|---------|
| **YouTube Music** | NewPipeExtractor (InnerTube client-spoof + PoToken, ReVanced-style) | ~256 kbps |
| **Qobuz** | self-hosted **squid.wtf** backend | 24-bit/192 kHz FLAC |
| **Tidal** | self-hosted **squid.wtf** backend | 24-bit/192 kHz FLAC |
| **Amazon Music** | self-hosted **squid.wtf** backend | FLAC |
| **Apple Music** | anonymous web-token method | 256 kbps AAC |
| **JioSaavn** *(fallback)* | unofficial JioSaavn API | 320 kbps, DRM-free |

## Tech stack

- **Kotlin + Jetpack Compose + Material 3 Expressive** (dynamic album-art color, AMOLED dark)
- **Media3 / ExoPlayer** — gapless background playback via `PlaybackService`
- **Room** — library (liked songs, playlists, downloads)
- **Hilt** — DI; the 5 sources are multibound into `SourceResolver`
- **Retrofit / OkHttp / kotlinx.serialization** — networking
- **Coil** — artwork

## Architecture

```
UI (Compose)  ──►  MusicRepository  ──►  SourceResolver  ──►  [ 5x MusicSource ]
                          │                    │ failover
                          └► Room (library)    └► resolve stream URL on demand
PlayerConnection ──► PlaybackService (Media3/ExoPlayer, OkHttp data source)
RemoteConfigRepository ──► GitHub-hosted config.json (hot-swap sources, no APK update)
```

Every source implements `MusicSource` (`search`, `getStream`, `getLyrics`, `isHealthy`). Adding a
source = one class + one line in `SourceModule`.

## 🔧 Updating without shipping a new APK — the important part

Two layers keep Sonora alive as private APIs change:

1. **Self-hosted proxy backends.** The fragile "trick" logic (Qobuz/Tidal/Amazon auth, JioSaavn,
   Apple tokens) lives on servers **you** control (squid.wtf, a JioSaavn instance). When a trick
   breaks, you patch the *server* — every install is fixed instantly, no reinstall.

2. **Remote config (`config.json` on GitHub).** The app fetches this at launch. Change it to:
   - flip `enabled: false` on a broken source,
   - point a source at a new `baseUrl`,
   - rotate `params` (client_id, app_id, tokens, region),
   - re-order source `priority`,
   - announce an update.

   Host it at e.g. `https://raw.githubusercontent.com/<you>/sonora-config/main/config.json` and set
   `RemoteConfigRepository.CONFIG_URL`. See [`config.sample.json`](config.sample.json).

3. **When code *must* change** (e.g. a NewPipeExtractor bump, or a brand-new source): bump
   `update.latest_version_code` in `config.json` and set `update.apk_url` to your new
   **GitHub Release** APK. Sonora is side-loaded (not on Play Store), so an in-app updater reads
   that and prompts users to download the new build. Set `mandatory: true` for hard-breaking changes.

## Build

1. Open in **Android Studio** (Ladybug or newer). It will download the Gradle wrapper jar + SDK.
2. Set your backends in `local.properties` (or the hosted `config.json`):
   ```
   SONORA_SQUID_BASE_URL=https://your-squidwtf.example.com/
   SONORA_JIOSAAVN_BASE_URL=https://your-saavn.example.com/
   ```
3. Update `RemoteConfigRepository.CONFIG_URL` to your GitHub raw config URL.
4. Run. Java 17+ required (Java 21 tested).

## Legal

Sonora accesses several services through unofficial means for **personal use**. The lossless
sources impersonate paid accounts and are against those services' ToS — this is why Sonora is not,
and cannot be, distributed on Google Play. Host your own backends and respect the services you use.

## Status

Early scaffold. MVP targets: multi-source search, playback, queue, library, offline downloads,
synced lyrics. See the About screen in-app for full credits.
