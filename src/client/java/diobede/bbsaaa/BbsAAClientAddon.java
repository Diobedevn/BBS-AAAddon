package diobede.bbsaaa;

import mchorse.bbs_mod.api.AddonContext;

public class BbsAAClientAddon extends ParticleClientRegistration
{

    @Override
    public int requiredApiVersion()
    {
        return 2;
    }

    @Override
    public void initialize(AddonContext context)
    {
        this.initializeClient();
    }

    @Override
    public void shutdown(AddonContext context)
    {
        this.shutdownClient();
    }

}
