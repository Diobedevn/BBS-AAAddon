package ports.smoke;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.forms.forms.BillboardForm;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeElement;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeTrackLayout;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.HashMap;
import java.util.List;

import diobede.bbsaaa.forms.AAAParticleForm;

/** Exercise the published host layout and installed distribution's mixin in a real client. */
public final class CmlTrackGroupsChecks
{
    public static void check(AAAParticleForm form)
    {
        UIKeyframeSheet later = sheet("speed", form.speed);
        UIKeyframeSheet paused = sheet("paused", form.paused);
        KeyframeChannel channel = paused.channel;
        List<UIKeyframeElement> elements = UIKeyframeTrackLayout.arrange(List.of(later, paused), new HashMap<>(), false);
        UIKeyframeTrackLayout.TrackGroup group = (UIKeyframeTrackLayout.TrackGroup) elements.get(0);
        require(elements.size() == 1 && group.id.equals(":bbsaaa:particle"), "form-scoped category");
        require(group.children.get(0) == paused, "explicit track ordering");
        require(paused.channel == channel && paused.id.equals("paused"), "existing channel identity");
        group.collapsed = true;
        HashMap<String, Boolean> collapse = new HashMap<>();
        UIKeyframeTrackLayout.remember(elements, collapse);
        require(((UIKeyframeTrackLayout.TrackGroup) UIKeyframeTrackLayout.arrange(List.of(paused), collapse, false).get(0)).collapsed,
            "folding survives layout rebuild");
        for (String language : List.of("es_es", "en_us"))
        {
            BBSModClient.reloadLanguage(language);
            require(group.title.get().equals(L10n.lang("bbsaaa.ui.forms.categories.aaa_particles").get()), "localized category switches with language");
        }
        BillboardForm owner = new BillboardForm();
        BodyPart part = new BodyPart("0");
        part.setForm(form);
        owner.parts.addBodyPart(part);
        UIKeyframeSheet nested = sheet("0/paused", form.paused);
        UIKeyframeTrackLayout.TrackGroup body = (UIKeyframeTrackLayout.TrackGroup)
            UIKeyframeTrackLayout.arrange(List.of(nested), new HashMap<>(), false).get(0);
        require(body.id.equals("body:0"), "bodypart hierarchy retained");
        require(((UIKeyframeTrackLayout.TrackGroup) body.children.get(0)).id.equals("0:bbsaaa:particle"), "category inside owning bodypart");
        UIKeyframeSheet unrelated = sheet("paused", owner.visible);
        UIKeyframeTrackLayout.TrackGroup generic = (UIKeyframeTrackLayout.TrackGroup)
            UIKeyframeTrackLayout.arrange(List.of(unrelated), new HashMap<>(), false).get(0);
        require(!generic.id.contains("bbsaaa:particle"), "generic paused track is not captured by addon");

        System.out.println("[Addon ports] CML TIMELINE CATEGORIES PASSED");
    }

    private static UIKeyframeSheet sheet(String id, BaseValueBasic value)
    {
        return new UIKeyframeSheet(0, false, new KeyframeChannel<>(id, KeyframeFactories.FLOAT), value);
    }

    private static void require(boolean condition, String message)
    {
        if (!condition)
        {
            throw new AssertionError(message);
        }
    }
}
