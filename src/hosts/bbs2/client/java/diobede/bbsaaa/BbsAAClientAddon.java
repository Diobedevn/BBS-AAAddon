package diobede.bbsaaa;

import mchorse.bbs_mod.events.Subscribe;
import mchorse.bbs_mod.events.register.RegisterL10nEvent;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

import diobede.bbsaaa.forms.AAAParticleForm;
import diobede.bbsaaa.forms.renderers.AAAParticleFormRenderer;
import diobede.bbsaaa.ui.forms.editors.forms.UIAAAParticleForm;

/** Methods are declared directly because upstream's event bus ignores inherited handlers. */
public final class BbsAAClientAddon
{
    public void initialize()
    {
        FormUtilsClient.register(AAAParticleForm.class, AAAParticleFormRenderer::new);
        UIFormEditor.register(AAAParticleForm.class, UIAAAParticleForm::new);
        ParticleClientLifecycle lifecycle = new ParticleClientLifecycle();
        lifecycle.initialize();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> lifecycle.shutdown());
    }

    @Subscribe
    public void languages(RegisterL10nEvent event)
    {
        event.l10n.registerOne(language -> Link.create("bbs-aaaddon:strings/" + language + ".json"));
        event.l10n.reload();
    }

}
