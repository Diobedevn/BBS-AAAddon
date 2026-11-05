package diobede.bbsaaa;

import mchorse.bbs_mod.api.AddonContext;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.events.RegisterTrackPresentationEvent;
import mchorse.bbs_mod.api.replays.TrackPresentation;

public class BbsAAClientAddon extends ParticleClientRegistration
{
    private static final String GROUP = BBSAAaddon.MOD_ID + ":particle";
    private static final String GROUP_KEY = "bbsaaa.ui.forms.categories.aaa_particles";

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

    @Subscribe
    public void tracks(RegisterTrackPresentationEvent event)
    {
        /* Smooth gradient from #d00093 to #00ef77 */
        int start = 0xD00093;
        int end = 0x00EF77;

        for (int i = 0; i < TRACKS.length; i++)
        {
            float ratio = (float) i / (TRACKS.length - 1);
            int r = (int) (((start >> 16) & 0xFF) + ratio * (((end >> 16) & 0xFF) - ((start >> 16) & 0xFF)));
            int g = (int) (((start >> 8) & 0xFF) + ratio * (((end >> 8) & 0xFF) - ((start >> 8) & 0xFF)));
            int b = (int) ((start & 0xFF) + ratio * ((end & 0xFF) - (start & 0xFF)));

            event.register(new TrackPresentation(TRACKS[i], "bbsaaa.track." + TRACKS[i], (r << 16) | (g << 8) | b,
                GROUP, GROUP_KEY, "", i));
        }
    }
}
