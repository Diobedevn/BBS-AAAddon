package diobede.bbsaaa;

import diobede.bbsaaa.forms.AAAParticleForm;
import diobede.bbsaaa.forms.renderers.AAAParticleFormRenderer;
import diobede.bbsaaa.forms.renderers.BBSEffectLoader;
import diobede.bbsaaa.ui.forms.editors.forms.UIAAAParticleForm;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.addons.BBSClientAddon;
import mchorse.bbs_mod.events.register.RegisterFormsRenderersEvent;
import mchorse.bbs_mod.events.register.RegisterL10nEvent;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.resources.packs.InternalAssetsSourcePack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BbsAAClientAddon extends BBSClientAddon implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        BBSMod.events.register(this);
        
        /* Register global cleanup watchdog for AAA Particles
         * This ensures effects are stopped when the form renderer is no longer active */
        ClientTickEvents.END_CLIENT_TICK.register(client ->
        {
            if (BBSEffectLoader.beginReload())
            {
                try
                {
                    List<AAAParticleFormRenderer> renderers = new ArrayList<>(AAAParticleFormRenderer.activeRenderers);

                    for (AAAParticleFormRenderer renderer : renderers)
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
                List<AAAParticleFormRenderer> renderers = new ArrayList<>(AAAParticleFormRenderer.activeRenderers);

                for (AAAParticleFormRenderer renderer : renderers)
                {
                    renderer.checkCleanup();
                }
            }
        });
        
        ClientLifecycleEvents.CLIENT_STARTED.register(client ->
        {
            try
            {
                 FormUtilsClient.register(AAAParticleForm.class, AAAParticleFormRenderer::new);
                 UIFormEditor.register(AAAParticleForm.class, UIAAAParticleForm::new);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            try
            {
                /* Register track colors using a smooth gradient from #d00093 to #00ef77 */
                int startColor = 0xd00093;
                int endColor = 0x00ef77;

                int r1 = (startColor >> 16) & 0xFF;
                int g1 = (startColor >> 8) & 0xFF;
                int b1 = startColor & 0xFF;

                int r2 = (endColor >> 16) & 0xFF;
                int g2 = (endColor >> 8) & 0xFF;
                int b2 = endColor & 0xFF;

                String[] properties = {
                    "bone", "paused", "restart", "loop", 
                    "duration", "speed", "particleScale", "sortingLayer", "worldSpace"
                };

                for (int i = 0; i < properties.length; i++)
                {
                    float ratio = (float) i / (properties.length - 1);
                    int r = (int) (r1 + ratio * (r2 - r1));
                    int g = (int) (g1 + ratio * (g2 - g1));
                    int b = (int) (b1 + ratio * (b2 - b1));
                    int color = (r << 16) | (g << 8) | b;
                    
                    UIReplaysEditor.registerColor(properties[i], color);
                }

                /* Register track icons */
                UIReplaysEditor.registerIcon("bone", Icons.LIST);
                UIReplaysEditor.registerIcon("restart", Icons.REDO);
                UIReplaysEditor.registerIcon("loop", Icons.REFRESH);
                UIReplaysEditor.registerIcon("duration", Icons.TIME);
                UIReplaysEditor.registerIcon("speed", Icons.FORWARD);
                UIReplaysEditor.registerIcon("particleScale", Icons.MAXIMIZE);
                UIReplaysEditor.registerIcon("sortingLayer", Icons.PARTICLE);
                UIReplaysEditor.registerIcon("worldSpace", Icons.GLOBE);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            try
            {
                /* Register source pack for bbs-aaaddon namespace */
                /* This ensures that BBSResources.init() has already run and we don't get overwritten */
                BBSMod.getProvider().register(new InternalAssetsSourcePack("bbs-aaaddon", "assets/bbs-aaaddon", BbsAAClientAddon.class));
                BBSMod.getProvider().register(new InternalAssetsSourcePack("bbs-aaaddon_icons", "assets", BbsAAClientAddon.class));

                /* Register L10n links directly and reload after packs are in place */
                BBSModClient.getL10n().register((lang) -> java.util.List.of(
                    new Link("bbs-aaaddon", "strings/" + L10n.DEFAULT_LANGUAGE + ".json"),
                    new Link("bbs-aaaddon", "strings/" + lang + ".json")
                ));
                BBSModClient.getL10n().reload();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            try
            {
                if (BBSMod.getForms() != null)
                {
                    BBSMod.getForms().register(Link.bbs("aaa_particle"), AAAParticleForm.class);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            try
            {
                if (BBSModClient.getFormCategories() != null &&
                    BBSModClient.getFormCategories().getExtraForms() != null &&
                    BBSModClient.getFormCategories().getExtraForms().getExtraCategory() != null)
                {
                    BBSModClient.getFormCategories().getExtraForms().getExtraCategory().addForm(new AAAParticleForm());
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void registerL10n(RegisterL10nEvent event)
    {
        event.l10n.register((lang) -> java.util.List.of(
            new Link("bbs-aaaddon", "strings/en_us.json"),
            new Link("bbs-aaaddon", "strings/" + lang + ".json")
        ));
        
        try
        {
            event.l10n.reload();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void registerFormsRenderers(RegisterFormsRenderersEvent event)
    {
        event.registerRenderer(AAAParticleForm.class, AAAParticleFormRenderer::new);
        event.registerPanel(AAAParticleForm.class, UIAAAParticleForm::new);
    }
}
