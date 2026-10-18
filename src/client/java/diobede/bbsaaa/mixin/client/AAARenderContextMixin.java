package diobede.bbsaaa.mixin.client;

import diobede.bbsaaa.forms.renderers.AAAParticleFormRenderer;
import mod.chloeprime.aaaparticles.client.internal.RenderContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep CML world effects behind the first-person hand in AAA's Sodium path. */
@Mixin(value = RenderContext.class, remap = false)
public abstract class AAARenderContextMixin
{
    @Inject(method = "renderLevelAfterHand", at = @At("RETURN"), cancellable = true)
    private static void bbsaaa$renderWorldBeforeHand(CallbackInfoReturnable<Boolean> callback)
    {
        // AAA's late Sodium draw restores world-only depth after the hand has
        // already been drawn. Use its existing before-hand draw instead: vanilla
        // then composites the hand over the world effects. An active shader pack
        // already makes AAA return false, so its deferred shader path is untouched.
        if (callback.getReturnValueZ() && AAAParticleFormRenderer.hasWorldEmitters())
        {
            callback.setReturnValue(false);
        }
    }
}
