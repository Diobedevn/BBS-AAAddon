package diobede.bbsaaa;

import diobede.bbsaaa.forms.AAAParticleForm;
import diobede.bbsaaa.forms.renderers.AAAParticleFormRenderer;
import diobede.bbsaaa.forms.renderers.BBSEffectLoader;
import diobede.bbsaaa.ui.forms.editors.forms.UIAAAParticleForm;
import diobede.bbsaaa.forms.AAAParticleSection;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.events.RegisterFormEditorsEvent;
import mchorse.bbs_mod.api.client.events.RegisterFormRenderersEvent;
import mchorse.bbs_mod.api.client.events.RegisterFormSectionsEvent;
import mchorse.bbs_mod.api.client.events.RegisterL10nEvent;
import mchorse.bbs_mod.api.client.events.RegisterTrackPresentationEvent;
import mchorse.bbs_mod.api.replays.TrackPresentation;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.resources.Link;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.ArrayList;
import java.util.List;

public class BbsAAClientAddon implements BBSAddonMod
{
    private static final String GROUP = BBSAAaddon.MOD_ID + ":particle";
    private static final String GROUP_KEY = "bbsaaa.ui.forms.categories.aaa_particles";

    /* Animatable properties that are specific to the AAA particle form */
    private static final String[] TRACKS = {"restart", "speed", "particleScale", "sortingLayer", "worldSpace"};

    @Override
    public int requiredApiVersion()
    {
        return 2;
    }

    @Override
    public void initialize(mchorse.bbs_mod.api.AddonContext context)
    {
        /* Global cleanup watchdog: stops effects whose form renderer is no longer active */
        ClientTickEvents.END_CLIENT_TICK.register((client) -> BbsAAClientAddon.tickCleanup());
    }

    private static void tickCleanup()
    {
        if (BBSEffectLoader.beginReload())
        {
            try
            {
                for (AAAParticleFormRenderer renderer : new ArrayList<>(AAAParticleFormRenderer.activeRenderers))
                {
                    renderer.cleanup();
                }

                BBSEffectLoader.clearCache();
            }
            finally
            {
                BBSEffectLoader.endReload();
            }
        }

        if (!AAAParticleFormRenderer.activeRenderers.isEmpty())
        {
            for (AAAParticleFormRenderer renderer : new ArrayList<>(AAAParticleFormRenderer.activeRenderers))
            {
                renderer.checkCleanup();
            }
        }
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

    @Subscribe
    public void tracks(RegisterTrackPresentationEvent event)
    {
        /* Smooth gradient from #d00093 to #00ef77 */
        int start = 0xD00093;
        int end = 0x00EF77;

        for (int i = 0; i < TRACKS.length; i++)
        {
            float ratio = (float) i / (TRACKS.length - 1);
            int r = (int) (((start >> 16) & 0xFF) + ratio * (((end >> 16) & 0xFF) - ((start >> 16) & 0xFF)));
            int g = (int) (((start >> 8) & 0xFF) + ratio * (((end >> 8) & 0xFF) - ((start >> 8) & 0xFF)));
            int b = (int) ((start & 0xFF) + ratio * ((end & 0xFF) - (start & 0xFF)));

            event.register(new TrackPresentation(TRACKS[i], "bbsaaa.track." + TRACKS[i], (r << 16) | (g << 8) | b,
                GROUP, GROUP_KEY, "", i));
        }
    }
}
