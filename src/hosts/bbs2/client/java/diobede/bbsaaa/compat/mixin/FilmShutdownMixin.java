package diobede.bbsaaa.compat.mixin;

import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.WorldFilmController;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import diobede.bbsaaa.forms.renderers.AAAParticleFormRenderer;

/** Stop owned emitters while the controller still retains its entity mapping. */
@Mixin(value = {BaseFilmController.class, WorldFilmController.class}, remap = false)
public abstract class FilmShutdownMixin
{
    @Inject(method = "shutdown", at = @At("HEAD"))
    private void bbs2$cleanup(CallbackInfo info)
    {
        AAAParticleFormRenderer.cleanupFilm((BaseFilmController) (Object) this);
    }
}
