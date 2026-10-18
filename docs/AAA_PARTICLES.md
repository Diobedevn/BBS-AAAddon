# AAA Particles integration for CML 3.0

The addon registers the existing `bbs:aaa_particle` form using CML addon API 2.
Saved property names and effect links remain unchanged.

## Build

- Minecraft 1.20.1, Fabric, AAA Particles 2.2.3 and CML 3.0 beta 2.
- Run the current Gradle wrapper with JDK 25. The addon bytecode targets Java 17.
- Place `bbs-cml-3.0-beta-2-1.20.1.jar` in `libs/`, then run `./gradlew build`
  (`.\gradlew.bat build` on Windows).
- To use a different intermediary release jar, pass `-Pbbs_dev_jar=<path>`.
  Loom remaps the release jar for the development environment; a named development
  jar is not interchangeable with an intermediary release jar.
- The local CML jar is ignored by Git. A fresh checkout or CI runner must provision
  it before building. The build reports its missing path explicitly.
- `./gradlew test` runs the playback state regression tests without Minecraft or
  the native Effekseer runtime.

## Effects

Place effects under `effeks/` inside the assets directory configured by BBS/CML.
For example, `<BBS assets>/effeks/magic/circle.efkefc` appears as
`bbs:magic/circle`. Keep each effect's textures, models, curves and materials at
the relative paths referenced by its Effekseer project.

The picker lists this external directory recursively. Playback also checks AAA
Particles' registry first, allowing already-registered resource pack effects to
be used by saved forms. The older `effekseer/` path inside the BBS assets directory
is retained as a loading fallback; the picker lists `effeks/` only.

Use **Reload Effects** after adding or repairing files. Reload stops active
emitters, releases addon-owned effect holders, and clears both successful and
failed load bookkeeping. Failed files are not retried every render frame.

## Editor controls

- **Effect** selects the effect file and displays its current identifier.
- **Reload Effects** rescans resources on the next client tick, stopping all addon
  emitters and releasing their effect holders. It is for changed files, not for
  replaying a single effect.
- **Thumbnail** selects the image shown in the form palette. It does not change
  the 3D effect or its rendering.
- **Paused** freezes the effect independently of the film pause state.
- **Loop** restarts an effect after its native simulation finishes. An effect
  authored to emit forever never finishes, so this toggle cannot impose a duration.
- **Speed** changes simulation time; it is independent of spatial Transform.
- **Restart preview** is a repeatable, transient editor action. It works even after
  a non-looping effect has ended and does not write a value into the saved form.
  For film keyframes, the existing `restart` property remains available as
  **Restart trigger**: false to true fires once; held true does not keep restarting.

Use CML's normal **Transform** panel for position, rotation, pivot and scale.
The preview now uses the already-transformed matrix from CML without applying
the emitter transform twice.

The old `particleScale` uniform multiplier still applies and remains available to
existing animation tracks. Its **Additional scale (legacy)** control is shown
when opening a form with a value other than 1. This prevents an old multiplier
from becoming invisible and uneditable. New forms use Transform; setting the
legacy multiplier back to 1 removes its contribution. It is not automatically
folded into Transform, which could change pivots, child forms or saved keyframes.

The unused `bone`, `duration`, `sortingLayer` and `worldSpace` properties are hidden
from the panel and new-property picker. Their values are still serialized. They
have no effect in the current renderer. Paused, loop, speed and restart have track
presentations; legacy extra scale retains its track presentation too.

## Responsibilities

- `AAAParticleForm`: serialized properties; no native or client dependencies.
- `AAAParticleFormRenderer`: form transforms, icon drawing and renderer lifetime.
- `ParticlePlayback`: emitter creation, pause, restart, looping and playback clock.
- `PlaybackState`: pure Java clock and restart bookkeeping, covered by unit tests.
- `FilmPlaybackTracker`: reads actual controller pause state through CML lifecycle
  events rather than treating a slow tick as a paused film.
- `AAAPreviewRenderer`: typed Fabric preview calls and a camera subclass. Minecraft
  method names are remapped by Loom; no reflective camera lookup is required.
- `BBSEffectLoader`: external effect and dependency loading.
- `AAAEffectRegistry`: isolated access to AAA's private effect registry. Cleanup
  removes only holders owned by this addon, preserving replacements from reloads.
- `ParticleEffectIds`: shared conversion between saved links and AAA identifiers.

Film shutdown, disconnect and addon shutdown clean up emitters. A fallback stops
renderers after 20 client ticks without a normal render. Shadow and picking passes
do not update particle playback.

## Current limits and remaining work

### First-person hand depth

When addon world emitters are active, `AAARenderContextMixin` moves AAA's
Sodium/no-shader world draw to its existing before-hand hook. Its late draw
restores the captured world depth, which lacks the first-person hand; drawing
before the hand allows Minecraft to composite the hand over world particles.
The decision checks owned emitter type rather than native liveness, so it remains
stable between AAA's before-hand and end-of-frame checks even if an effect ends.

AAA batches world effects together, so this ordering also applies to other AAA
world effects while an addon world emitter is active. Shader-enabled rendering,
the form editor preview, and scenes without addon world emitters retain AAA's
existing ordering. Effects with depth testing disabled can still draw over world
geometry; this workaround addresses hand composition only.

The fix needs visual verification with the same first-person scene, both an empty
hand and a held item, third person, and Iris shaders on/off.

### Playback and properties

Seeking backward or skipping ticks restarts the effect. It does **not** restore a
historical particle simulation. The former tick cache did not contain simulation
state and has been removed. Playback still uses elapsed time, so deterministic
film export and exact seeking require a separate timeline-driven implementation.

`bone`, `duration`, `sortingLayer` and `worldSpace` remain serialized for
compatibility but are hidden from the editor because they are not implemented.
Dynamic Effekseer inputs are not currently exposed by this form.

AAA's private registry integration remains version-sensitive, although it is now
isolated and reports failures. The preview uses AAA's experimental main-hand
rendering path, shared by effects of that type. Multiple simultaneous previews
still require in-game validation.

## In-game verification

After building, check one short effect and one continuous effect in CML:

1. Select and preview an effect; change its scale and rotations beyond 90 degrees.
2. Test pause, speeds 0.5/1/2, repeated **Restart preview** clicks, and natural looping.
   Also hold a restart keyframe true across several ticks; it should fire only once.
   Open an old form with extra scale other than 1 and verify its legacy control.
3. Pause/resume a film and seek forward/backward; seeking should restart the effect.
4. Test a model block and an actor, then remove them and close the film.
5. Reload effects, repair a missing effect and reload again, then leave/rejoin a world.
6. Repeat preview and world rendering with Iris enabled, checking shadow passes.

Compilation and pure Java tests do not validate native rendering, GL state or
shader compatibility; those checks require a running Minecraft client.
