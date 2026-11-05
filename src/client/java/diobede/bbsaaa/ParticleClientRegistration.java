package diobede.bbsaaa;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.events.RegisterFormEditorsEvent;
import mchorse.bbs_mod.api.client.events.RegisterFormRenderersEvent;
import mchorse.bbs_mod.api.client.events.RegisterFormSectionsEvent;
import mchorse.bbs_mod.api.client.events.RegisterL10nEvent;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.resources.Link;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import java.util.List;

import diobede.bbsaaa.forms.AAAParticleForm;
import diobede.bbsaaa.forms.AAAParticleSection;
import diobede.bbsaaa.forms.renderers.AAAParticleFormRenderer;
import diobede.bbsaaa.forms.renderers.BBSEffectLoader;
import diobede.bbsaaa.particles.FilmPlaybackTracker;
import diobede.bbsaaa.ui.forms.editors.forms.UIAAAParticleForm;

/** Shared registration and effect lifecycle; host entrypoints supply only host events. */
public abstract class ParticleClientRegistration implements BBSAddonMod
{
    protected static final String[] TRACKS = {"paused", "loop", "speed", "restart", "particleScale"};

    protected final void initializeClient()
    {
        FilmPlaybackTracker.initialize();
        ClientTickEvents.END_CLIENT_TICK.register(client -> tickCleanup());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> cleanup());
    }

    protected final void shutdownClient()
    {
        cleanup();
    }

    private static void cleanup()
    {
        AAAParticleFormRenderer.cleanupAll();
        BBSEffectLoader.clearCache();
        FilmPlaybackTracker.clear();
    }

    private static void tickCleanup()
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

    @Subscribe
    public void languages(RegisterL10nEvent event)
    {
        event.l10n.register((lang) -> List.of(
            Link.create(BBSAAaddon.MOD_ID + ":strings/" + L10n.DEFAULT_LANGUAGE + ".json"),
            Link.create(BBSAAaddon.MOD_ID + ":strings/" + lang + ".json")
        ));
    }

    @Subscribe
    public void sections(RegisterFormSectionsEvent event)
    {
        event.register(AAAParticleSection::new);
    }

    @Subscribe
    public void renderers(RegisterFormRenderersEvent event)
    {
        event.register(AAAParticleForm.class, AAAParticleFormRenderer::new);
    }

    @Subscribe
    public void editors(RegisterFormEditorsEvent event)
    {
        event.register(AAAParticleForm.class, UIAAAParticleForm::new);
    }

}
