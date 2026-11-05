package diobede.bbsaaa;

import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.editor.TrackCategory;
import mchorse.bbs_mod.api.client.events.RegisterTrackCategoriesEvent;
import mchorse.bbs_mod.api.client.events.RegisterTrackStylesEvent;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

import java.util.List;

public class BbsAAClientAddon extends ParticleClientRegistration
{
    public BbsAAClientAddon()
    {
        this.initializeClient();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> this.shutdownClient());
    }

    @Subscribe
    public void categories(RegisterTrackCategoriesEvent event)
    {
        event.register(new TrackCategory("bbs-aaaddon:particle", Icons.PARTICLE,
            L10n.lang("bbsaaa.ui.forms.categories.aaa_particles"), L10n.lang("bbsaaa.ui.forms.categories.aaa_particles")), (track, pose) -> List.of(TRACKS).contains(track.property()));
    }

    @Subscribe
    public void tracks(RegisterTrackStylesEvent event)
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

            event.register(TRACKS[i], Icons.PARTICLE, (r << 16) | (g << 8) | b, L10n.lang("bbsaaa.track." + TRACKS[i]));
        }
    }
}
