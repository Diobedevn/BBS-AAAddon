package diobede.bbsaaa;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import diobede.bbsaaa.forms.renderers.AAAParticleFormRenderer;
import diobede.bbsaaa.forms.renderers.BBSEffectLoader;
import diobede.bbsaaa.particles.FilmPlaybackTracker;

/** Shared native effect ownership; host adapters supply registration and shutdown hooks. */
public final class ParticleClientLifecycle
{
    public void initialize()
    {
        FilmPlaybackTracker.initialize();
        ClientTickEvents.END_CLIENT_TICK.register(client -> this.tickCleanup());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> this.cleanup());
    }

    public void shutdown()
    {
        this.cleanup();
    }

    private void cleanup()
    {
        AAAParticleFormRenderer.cleanupAll();
        BBSEffectLoader.clearCache();
        FilmPlaybackTracker.clear();
    }

    private void tickCleanup()
    {
        if (BBSEffectLoader.beginReload())
        {
            try
            {
                AAAParticleFormRenderer.cleanupAll();
                BBSEffectLoader.clearCache();
            }
            finally
            {
                BBSEffectLoader.endReload();
            }
        }

        AAAParticleFormRenderer.checkActiveRenderers();
    }

}
