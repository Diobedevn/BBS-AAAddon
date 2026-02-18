package diobede.bbsaaa;

import diobede.bbsaaa.addon.BbsAAAddon;
import diobede.bbsaaa.forms.AAAParticleForm;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.resources.Link;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BBSAAaddon implements ModInitializer {
	public static final String MOD_ID = "bbs-aaaddon";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("BBS AAaddon initialized!");
		
		// Register the addon instance for other events
		BBSMod.events.register(new BbsAAAddon());
        
        // Manual registration fallback to ensure the form is registered in the factory
        try {
            mchorse.bbs_mod.BBSMod.getForms().register(new Link("bbs-aaaddon", "aaa_particle"), AAAParticleForm.class);
            LOGGER.info("Manually registered AAAParticleForm to FormUtils.");
        } catch (Exception e) {
            LOGGER.error("Failed to manually register AAAParticleForm", e);
        }
	}
}
