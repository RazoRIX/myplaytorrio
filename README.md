# PlayTorrio Enhanced build kit v3

This replaces the first renderer-factory based kit.

## What changed in v3

- No `EnhancedRenderersFactory`
- No custom Media3 `BaseAudioProcessor`
- No subclassing PlayTorrio's customized Media3 classes
- Hooks the finished ExoPlayer instance and follows its audio session ID
- Loudness Equalization uses Android `LoudnessEnhancer`
- Voice Boost uses Android `Equalizer`
- Automatically removes/undoes files and source substitutions produced by the first kit
- Repositions the enhancement controls beside the whole CC/subtitle button
- Uses `android-actions/setup-android@v4`

The side-by-side application ID remains:

`com.playtorrio.tv.enhanced`

## Upgrade from the first kit

In your fork, replace these folders with the ones from this ZIP:

- `.github`
- `scripts`
- `enhancements`

Commit the replacement, then run:

**Actions -> Build PlayTorrio Enhanced APK -> Run workflow**

The patcher cleans up the old renderer-factory integration automatically before applying v3.

## Passthrough

Android session audio effects operate on audio handled by the Android audio framework. If PlayTorrio's encoded audio passthrough is enabled for Dolby/DTS output, turn passthrough off while using Loudness Equalization or Voice Boost so the effects receive normal decoded/mixed audio.


## V3 compiler compatibility fix

V3 no longer reads `player.audioSessionId`. PlayTorrio's bundled/custom Media3 API exposes the audio session through `Player.Listener.onAudioSessionIdChanged(...)` but not as a readable `Player.audioSessionId` property. The controller now binds effects only from that callback.
