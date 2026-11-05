# Shared AAA addon sources

Current build dependency: **CML 3.0 beta 7**, pinned by `cml_version` and the addon metadata. Provision the matching beta-7 host for each configured Minecraft target. Earlier validation notes below describe the original migration.

## Scope and design

The existing version branches now share a source tree. Three Fabric targets are configured: 1.20.1, 1.21.1 and 26.1.2.

Shared form definitions, serialized Value identifiers, effect loading, playback and editor controls remain in their existing packages. Native version boundaries live in src/platform. NativeParticlePlatform isolates identifier construction, pause lookup and the version-specific host matrix convention. AAAPreviewRenderer isolates the native camera and Effekseer hand-preview signatures. The 26.x generated source bridge changes only the Identifier import package, whose simple name and used members match; no method-name substitutions are used.

This migration preserves the current rendering behavior of each branch. It does not claim support for unconfigured Minecraft releases, switch loaders, replace the particle engine, or rename saved form properties. Existing branches are retained as historical references.

## Build

Run the Gradle wrapper on JDK 25:

```powershell
./gradlew.bat buildAll
./gradlew.bat build '-Pmc=1.21.1'
```

Installable JARs are collected in build/libs. Target-specific intermediates and test reports live under build/targets/<mc>. Every artifact declares its exact Minecraft target and corresponding Java requirement. Builds stop on unknown targets and incompatible CML dependencies.

Place the existing matching CML distribution JARs in libs. A folder containing multiple versions is supported: selection checks the mod id, manifest Minecraft version and namespace. For 26.1.2 the explicitly pinned host artifact is CML 26.1 (official names), matching the existing branch. Override a folder with -PlibDir=<directory>, or select one host with -PcmlJar=<path>. The legacy -Pbbs_dev_jar override is retained. A single override must match each selected target; use a mixed libs directory for buildAll.

The default build remains 1.20.1. The selected target chooses the matching Loom namespace, Java bytecode, Fabric/AAA/Architectury/config dependencies. The old global LWJGL override has been removed so each Minecraft target uses its matching native libraries.

## Verification

```powershell
python tools/verify-targets.py
python tools/test-clients.py
```

Artifact verification checks exact metadata, bytecode, duplicate archive entries, collected-byte equality and exclusion of CML, particle engines, Minecraft and nested dependencies. The eight existing PlaybackStateTest cases run for every target during buildAll.

The opt-in -PportSmoke fixture is excluded from release artifacts. It checks native client initialization, canonical form registration, effect/runtime serialization, renderer registration and editor construction after resource loading. Tests use build/validation/<mc>/client, never player profiles. Configure the matching Java runtime in the ignored gradle/local-runtime.properties (runJavaHome.<mc>=<Java home>) or pass -PrunJavaHome. The production build does not require this file.

Visual acceptance remains separate: actual authored effects, pause/scrub and seeking, model-block/bodypart placement, preview/world alignment, cleanup and shaders enabled/disabled need in-game inspection. Native startup checks do not establish rendering parity. All three native client fixtures passed with the selected CML and AAA dependencies: 1.20.1, 1.21.1 and 26.1.2. Each loaded COMMON/CLIENT addon API 2, initialized the native Effekseer manager, registered the form/renderer, constructed its editor and round-tripped saved playback/effect data. These checks do not render an authored particle effect or validate shaderpack behavior.
