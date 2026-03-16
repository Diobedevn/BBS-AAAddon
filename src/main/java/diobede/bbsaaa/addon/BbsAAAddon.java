package diobede.bbsaaa.addon;

import diobede.bbsaaa.forms.AAAParticleForm;
import mchorse.bbs_mod.addons.BBSAddon;
import mchorse.bbs_mod.events.register.RegisterFormsEvent;
import mchorse.bbs_mod.resources.Link;

public class BbsAAAddon extends BBSAddon
{
    @Override
    protected void registerForms(RegisterFormsEvent event)
    {
        event.getForms().register(Link.bbs("aaa_particle"), AAAParticleForm.class);
    }
}
