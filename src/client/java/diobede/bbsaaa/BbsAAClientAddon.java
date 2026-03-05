package diobede.bbsaaa;

import diobede.bbsaaa.forms.AAAParticleForm;
import diobede.bbsaaa.forms.renderers.AAAParticleFormRenderer;
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
            if (!AAAParticleFormRenderer.activeRenderers.isEmpty())
            {
                List<AAAParticleFormRenderer> renderers = new ArrayList<>(AAAParticleFormRenderer.activeRenderers);

                for (AAAParticleFormRenderer renderer : renderers)
                {
                    renderer.checkCleanup();
                }
            }
        });
        
        /* Manual registration fallback in case event bus fails or timing is off */
        ClientLifecycleEvents.CLIENT_STARTED.register(client ->
        {
            try
            {
                 /* Manually register renderer */
                 FormUtilsClient.register(AAAParticleForm.class, AAAParticleFormRenderer::new);
                 /* Manually register panel */
                 UIFormEditor.register(AAAParticleForm.class, UIAAAParticleForm::new);
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
