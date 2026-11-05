package ports.smoke;

import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.resources.Link;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import diobede.bbsaaa.forms.AAAParticleForm;
import diobede.bbsaaa.forms.renderers.AAAParticleFormRenderer;
import diobede.bbsaaa.particles.NativeParticlePlatform;
import diobede.bbsaaa.particles.ParticleEffectIds;
import diobede.bbsaaa.ui.forms.editors.forms.UIAAAParticleForm;

/** Opt-in startup contract check, excluded from distribution artifacts. */
public final class AddonClientSmoke implements ClientModInitializer
{
    private int ticks;

    @Override
    public void onInitializeClient()
    {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.getOverlay() != null || client.currentScreen == null)
            {
                return;
            }
            this.ticks++;
            if (this.ticks == 60)
            {
                try
                {
                    this.check();
                    System.out.println("[Addon ports] REGISTRATION, EDITOR AND SERIALIZATION PASSED");
                }
                catch (Throwable failure)
                {
                    throw new IllegalStateException("Addon port smoke failed", failure);
                }
            }
            if (this.ticks == 100)
            {
                client.scheduleStop();
            }
        });
    }

    private void check()
    {
        AAAParticleForm form = new AAAParticleForm();
        System.out.println("[Addon host] " + form.getClass().getProtectionDomain().getCodeSource().getLocation());
        form.effect.set(Link.create("bbs:effeks/validation/test.efkefc"));
        form.speed.set(1.5F);
        form.paused.set(true);
        AAAParticleForm copy = (AAAParticleForm) FormUtils.fromData(FormUtils.toData(form));
        require("bbs:aaa_particle".equals(form.getFormId()), "canonical form id");
        require(copy.paused.get() && copy.speed.get() == 1.5F, "playback serialization");
        require(form.effect.get().equals(copy.effect.get()), "effect serialization");
        require(FormUtilsClient.getRenderer(form) instanceof AAAParticleFormRenderer, "renderer registration");
        require(new UIAAAParticleForm() != null, "editor construction");
        require(ParticleEffectIds.toLink(ParticleEffectIds.fromLink(form.effect.get())).equals(form.effect.get()), "identifier/link conversion");
        require("bbs:test".equals(NativeParticlePlatform.identifier("bbs", "test").toString()), "native identifier creation");
        NativeParticlePlatform.paused();
    }

    private static void require(boolean condition, String check)
    {
        if (!condition)
        {
            throw new AssertionError(check);
        }
    }
}
