package diobede.bbsaaa;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.events.RegisterFormEditorsEvent;
import mchorse.bbs_mod.api.client.events.RegisterFormRenderersEvent;
import mchorse.bbs_mod.api.client.events.RegisterFormSectionsEvent;
import mchorse.bbs_mod.api.client.events.RegisterL10nEvent;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.resources.Link;

import java.util.List;

import diobede.bbsaaa.forms.AAAParticleForm;
import diobede.bbsaaa.forms.AAAParticleSection;
import diobede.bbsaaa.forms.renderers.AAAParticleFormRenderer;
import diobede.bbsaaa.ui.forms.editors.forms.UIAAAParticleForm;

/** Shared registration and effect lifecycle; host entrypoints supply only host events. */
public abstract class ParticleClientRegistration implements BBSAddonMod
{
    protected static final String[] TRACKS = {"paused", "loop", "speed", "restart", "particleScale"};

    private final ParticleClientLifecycle lifecycle = new ParticleClientLifecycle();

    protected final void initializeClient()
    {
        this.lifecycle.initialize();
    }

    protected final void shutdownClient()
    {
        this.lifecycle.shutdown();
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
