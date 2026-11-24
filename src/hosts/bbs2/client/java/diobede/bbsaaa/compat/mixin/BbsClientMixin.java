package diobede.bbsaaa.compat.mixin;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import diobede.bbsaaa.BbsAAClientAddon;

/** Hook only the upstream lifecycle; other hosts use their existing addon events. */
@Mixin(value = BBSModClient.class, remap = false)
public abstract class BbsClientMixin
{
    @Unique
    private BbsAAClientAddon bbs2$registration;

    @Inject(method = "onInitializeClient", at = @At("HEAD"))
    private void bbs2$subscribe(CallbackInfo info)
    {
        this.bbs2$registration = new BbsAAClientAddon();
        BBSMod.events.register(this.bbs2$registration);
    }

    @Inject(method = "onInitializeClient", at = @At("TAIL"))
    private void bbs2$initialize(CallbackInfo info)
    {
        this.bbs2$registration.initialize();
    }
}
