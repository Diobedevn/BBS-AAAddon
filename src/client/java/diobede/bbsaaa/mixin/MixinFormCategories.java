package diobede.bbsaaa.mixin;

import diobede.bbsaaa.forms.sections.AAAParticleFormSection;
import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.forms.sections.FormSection;
import mchorse.bbs_mod.forms.sections.ExtraFormSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = FormCategories.class, remap = false)
public class MixinFormCategories
{
    @Shadow
    private List<FormSection> sections;

    @Inject(method = "setup", at = @At("TAIL"))
    private void bbsaaa$setup(CallbackInfo ci)
    {
        System.out.println("MixinFormCategories: Injecting AAAParticleFormSection");
        AAAParticleFormSection section = new AAAParticleFormSection((FormCategories) (Object) this);

        int index = -1;

        for (int i = 0; i < this.sections.size(); i++)
        {
            if (this.sections.get(i) instanceof ExtraFormSection)
            {
                index = i;
                break;
            }
        }

        if (index != -1)
        {
            this.sections.add(index, section);
            System.out.println("MixinFormCategories: Injected AAAParticleFormSection before ExtraFormSection at index " + index);
        }
        else
        {
            this.sections.add(section);
            System.out.println("MixinFormCategories: Injected AAAParticleFormSection at the end");
        }

        section.initiate();
        System.out.println("MixinFormCategories: Injected and initiated AAAParticleFormSection");
    }
}