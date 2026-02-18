# AAA Particles Integration for BBS Mod

This addon integrates AAAParticles (Effekseer) into the BBS Mod form system.

## Features

- **Custom Form**: `aaa_particle` form type.
- **Automatic Discovery**: Loads `.efkefc` files from the `effekseer/` folder in your game directory.
- **Full UI Integration**: Dedicated panel in the BBS Dashboard to select effects and configure parameters.
- **Dynamic Control**: Support for pausing, scaling, and 4 dynamic inputs.

## Usage

### 1. Installation

1.  Ensure `aaa_particles` mod is installed.
2.  Install this addon.

### 2. Adding Effects

1.  Navigate to your game directory (e.g., `.minecraft` or instance folder).
2.  Create a folder named `effekseer`.
3.  Inside `effekseer`, create a folder named `effeks` (required by AAAParticles loader).
4.  Place your `.efkefc` files inside `effekseer/effeks/`.
    *   Example: `effekseer/effeks/magic_circle.efkefc`

### 3. Using in BBS

1.  Open the BBS Dashboard (`B` by default).
2.  Go to **Actors** (or any form-based editor).
3.  Add a new Form.
4.  Select **aaa_particle** from the list.
5.  In the **AAA Particles** tab:
    *   **Effect**: Select your effect from the list (e.g., `effekseer:magic_circle`).
    *   **Options**: Toggle pause or adjust scale.
    *   **Inputs**: Adjust dynamic inputs (0-3) which map to Effekseer dynamic parameters.

## Technical Details

- **Form ID**: `bbs-aaaddon:aaa_particle`
- **Renderer**: `AAAParticleFormRenderer` uses `EffectRegistry` to spawn and manage `ParticleEmitter`.
- **Resource Loading**: `AAAParticlesSourcePack` registers the `effekseer/` folder as a resource pack with namespace `effekseer`.
- **Entity Binding**: Emitters are bound to the form's entity, updating position automatically every tick.

## Troubleshooting

- **Effect not showing in list**:
    - Ensure the file is in `effekseer/effeks/`.
    - Restart the game (resources are scanned on startup).
    - Check logs for "effekseer" resource pack errors.
- **Particle not rendering**:
    - Ensure the effect file is valid.
    - Check if `scale` is too small.
    - Check if `paused` is enabled.
