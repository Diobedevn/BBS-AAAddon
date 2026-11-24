package diobede.bbsaaa.compat.cml;

import mchorse.bbs_mod.api.replays.TrackPresentation;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeElement;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeGroup;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;

import java.util.Comparator;
import java.util.List;

import diobede.bbsaaa.forms.AAAParticleForm;

/** Presentation metadata only: stored paths, channels and host layout remain unchanged. */
public final class CmlTrackGroups
{
    private record Entry(String id, String label, String group, String groupLabel, int order) {}

    private static final List<Entry> ENTRIES = List.of(
        new Entry("paused", "bbsaaa.track.paused", "particle", "bbsaaa.ui.forms.categories.aaa_particles", 0),
        new Entry("loop", "bbsaaa.track.loop", "particle", "bbsaaa.ui.forms.categories.aaa_particles", 1),
        new Entry("speed", "bbsaaa.track.speed", "particle", "bbsaaa.ui.forms.categories.aaa_particles", 2),
        new Entry("restart", "bbsaaa.track.restart", "particle", "bbsaaa.ui.forms.categories.aaa_particles", 3),
        new Entry("particleScale", "bbsaaa.track.particleScale", "particle", "bbsaaa.ui.forms.categories.aaa_particles", 4));

    public static TrackPresentation presentation(UIKeyframeSheet sheet)
    {
        if (sheet.property == null || !(FormUtils.getForm(sheet.property) instanceof AAAParticleForm))
        {
            return null;
        }
        String[] parts = sheet.id.replaceFirst("^(?:[0-9]+/)+", "").split("/");
        Entry entry = ENTRIES.stream().filter(candidate -> candidate.id().equals(parts[0])).findFirst().orElse(null);
        if (entry == null)
        {
            return null;
        }
        String label = entry.label();

        float ratio = entry.order() / 4F;
        int red = (int) (0xD0 * (1F - ratio));
        int green = (int) (0xEF * ratio);
        int blue = (int) (0x93 + ratio * (0x77 - 0x93));
        return new TrackPresentation(sheet.id, label, (red << 16) | (green << 8) | blue,
            "bbsaaa:" + entry.group(), entry.groupLabel(), "", entry.order());
    }

    public static void finish(List<UIKeyframeElement> elements)
    {
        for (UIKeyframeElement element : elements)
        {
            if (element instanceof UIKeyframeGroup group)
            {
                finish(group.children);
                if (!group.children.isEmpty() && group.children.stream().allMatch(child ->
                    child instanceof UIKeyframeSheet sheet && presentation(sheet) != null))
                {
                    /* Stable ordering retains the host's order within repeated list entries. */
                    group.children.sort(Comparator.comparingInt(child -> presentation((UIKeyframeSheet) child).order()));
                }
            }
        }
    }
}
