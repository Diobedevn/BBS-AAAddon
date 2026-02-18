package diobede.bbsaaa.addon;

import diobede.bbsaaa.forms.AAAParticleForm;
import mchorse.bbs_mod.addons.BBSAddon;
import mchorse.bbs_mod.events.register.RegisterFormsEvent;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.events.Subscribe;

public class BbsAAAddon extends BBSAddon
{
    @Subscribe
    public void onRegisterForms(RegisterFormsEvent event)
    {
        event.getForms().register(new Link("bbs-aaaddon", "aaa_particle"), AAAParticleForm.class);
    }
}
