package diobede.bbsaaa.ui.forms.editors.panels;

import diobede.bbsaaa.forms.AAAParticleForm;
import mchorse.bbs_mod.forms.forms.Form;
import diobede.bbsaaa.forms.renderers.AAAParticleFormRenderer;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.utils.UI;
import mod.chloeprime.aaaparticles.client.loader.EffekAssetLoader;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * AAA Particle Form Panel
 *
 * UI panel for editing AAA Particle (Effekseer) form properties.
 * Provides controls for effect selection, playback, bone attachment, and transform.
 */
public class UIAAAParticleFormPanel extends UIFormPanel<AAAParticleForm>
{
    /* Effect selection */
    public UIStringList effectList;

    /* Preview image */
    public UIButton pickPreview;

    /* Bone attachment */
    public UITextbox bone;

    /* Playback controls */
    public UIToggle paused;
    public UIToggle restart;
    public UIToggle loop;
    public UITrackpad duration;
    public UITrackpad speed;

    /* Transform */
    public UITrackpad particleScale;
    public UITrackpad sortingLayer;
    public UIToggle worldSpace;

    public UIAAAParticleFormPanel(UIForm editor)
    {
        super(editor);

        /* Effect list */
        this.effectList = new UIStringList((item) -> this.setEffect(item.get(0)));
        this.effectList.background().h(120);

        /* Preview picker */
        this.pickPreview = new UIButton(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.preview"), (b) ->
        {
            UITexturePicker.open(this.getContext(), this.form.preview.get(), (l) -> this.form.preview.set(l));
        });

        /* Bone attachment */
        this.bone = new UITextbox(120, (t) -> this.form.bone.set(t));
        this.bone.tooltip(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.bone.tooltip"));

        /* Playback controls */
        this.paused = new UIToggle(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.paused"), (b) -> this.form.paused.set(b.getValue()));
        this.restart = new UIToggle(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.restart"), (b) -> this.form.restart.set(b.getValue()));
        this.loop = new UIToggle(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.loop"), (b) -> this.form.loop.set(b.getValue()));
        this.loop.tooltip(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.loop.tooltip"));

        this.duration = new UITrackpad((v) -> this.form.duration.set(v.intValue()));
        this.duration.limit(0).integer();
        this.duration.tooltip(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.duration.tooltip"));

        this.speed = new UITrackpad((v) -> this.form.speed.set(v.floatValue()));
        this.speed.limit(0.01D, 10D);

        /* Transform */
        this.particleScale = new UITrackpad((v) -> this.form.particleScale.set(v.floatValue()));
        this.particleScale.limit(0.01D, 100D);

        this.sortingLayer = new UITrackpad((v) -> this.form.sortingLayer.set(v.intValue()));
        this.sortingLayer.integer();

        this.worldSpace = new UIToggle(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.world_space"), (b) -> this.form.worldSpace.set(b.getValue()));
        this.worldSpace.tooltip(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.world_space.tooltip"));

        /* Add all to options panel */
        this.options.add(UI.label(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.effect")), this.effectList);
        this.options.add(this.pickPreview);
        this.options.add(UI.label(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.bone")), this.bone);
        this.options.add(this.paused, this.restart, this.loop);
        this.options.add(UI.label(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.duration")), this.duration);
        this.options.add(UI.label(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.speed")), this.speed);
        this.options.add(UI.label(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.scale")), this.particleScale);
        this.options.add(UI.label(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.sorting_layer")), this.sortingLayer);
        this.options.add(this.worldSpace);

        this.populateEffectList();
    }

    /**
     * Populate the effect list with available Effekseer effects
     */
    private void populateEffectList()
    {
        List<String> effects = new ArrayList<>();
        EffekAssetLoader loader = EffekAssetLoader.get();

        if (loader != null)
        {
            loader.forEach((id, definition) ->
            {
                effects.add(id.toString());
            });
        }

        effects.sort(String::compareTo);
        this.effectList.clear();
        this.effectList.add(effects);
    }

    /**
     * Set the selected effect
     */
    private void setEffect(String effectId)
    {
        if (effectId == null || effectId.isEmpty())
        {
            this.form.effect.set(null);
            return;
        }

        Identifier id = new Identifier(effectId);

        /* Create link with effeks/ prefix */
        this.form.effect.set(new Link(id.getNamespace(), "effeks/" + id.getPath() + ".efkefc"));
    }

    /**
     * Get the current effect ID from the form
     */
    private String getCurrentEffectId()
    {
        Link effect = this.form.effect.get();

        if (effect == null)
        {
            return null;
        }

        String path = effect.path;

        if (path == null)
        {
            return null;
        }

        /* Remove effeks/ prefix and .efkefc suffix */
        if (path.startsWith("effeks/"))
        {
            path = path.substring(7);
        }

        if (path.endsWith(".efkefc"))
        {
            path = path.substring(0, path.length() - 7);
        }

        return effect.source + ":" + path;
    }

    @Override
    public void startEdit(AAAParticleForm form)
    {
        super.startEdit(form);

        /* Refresh effect list in case new effects were loaded */
        this.populateEffectList();

        /* Select current effect in list */
        String currentEffect = this.getCurrentEffectId();

        if (currentEffect != null)
        {
            this.effectList.setCurrentScroll(currentEffect);
        }
        else
        {
            this.effectList.deselect();
        }

        /* Update UI values */
        this.bone.setText(form.bone.get());
        this.paused.setValue(form.paused.get());
        this.restart.setValue(form.restart.get());
        this.loop.setValue(form.loop.get());
        this.duration.setValue(form.duration.get());
        this.speed.setValue(form.speed.get());
        this.particleScale.setValue(form.particleScale.get());
        this.sortingLayer.setValue(form.sortingLayer.get());
        this.worldSpace.setValue(form.worldSpace.get());
    }

    @Override
    public void pickBone(String bone)
    {
        this.bone.setText(bone);
        this.form.bone.set(bone);
    }
}
