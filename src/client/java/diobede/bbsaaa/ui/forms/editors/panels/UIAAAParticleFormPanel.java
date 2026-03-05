package diobede.bbsaaa.ui.forms.editors.panels;

import diobede.bbsaaa.forms.AAAParticleForm;
import mchorse.bbs_mod.BBSMod;
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
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UI;
import net.minecraft.util.Identifier;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AAA Particle Form Panel
 *
 * UI panel for editing AAA Particle (Effekseer) form properties.
 * Provides controls for effect selection, playback, bone attachment, and transform.
 */
public class UIAAAParticleFormPanel extends UIFormPanel<AAAParticleForm>
{
    public UIButton pickEffect;
    public UIButton pickPreview;
    public UITextbox bone;
    public UIToggle paused;
    public UIToggle restart;
    public UIToggle loop;
    public UITrackpad duration;
    public UITrackpad speed;
    public UITrackpad particleScale;
    public UITrackpad sortingLayer;
    public UIToggle worldSpace;

    private List<String> cachedEffects;

    public UIAAAParticleFormPanel(UIForm editor)
    {
        super(editor);

        this.pickEffect = new UIButton(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.pick_effect"), (b) -> this.openPicker());

        this.pickPreview = new UIButton(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.preview"), (b) ->
        {
            UITexturePicker.open(this.getContext(), this.form.preview.get(), (l) -> this.form.preview.set(l));
        });

        this.bone = new UITextbox(120, (t) -> this.form.bone.set(t));
        this.bone.tooltip(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.bone.tooltip"));

        this.paused = new UIToggle(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.paused"), (b) -> this.form.paused.set(b.getValue()));
        this.restart = new UIToggle(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.restart"), (b) -> this.form.restart.set(b.getValue()));
        this.loop = new UIToggle(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.loop"), (b) -> this.form.loop.set(b.getValue()));
        this.loop.tooltip(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.loop.tooltip"));

        this.duration = new UITrackpad((v) -> this.form.duration.set(v.intValue()));
        this.duration.limit(0).integer();
        this.duration.tooltip(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.duration.tooltip"));

        this.speed = new UITrackpad((v) -> this.form.speed.set(v.floatValue()));
        this.speed.limit(0.01D, 10D);

        this.particleScale = new UITrackpad((v) -> this.form.particleScale.set(v.floatValue()));
        this.particleScale.limit(0.01D, 100D);

        this.sortingLayer = new UITrackpad((v) -> this.form.sortingLayer.set(v.intValue()));
        this.sortingLayer.integer();

        this.worldSpace = new UIToggle(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.world_space"), (b) -> this.form.worldSpace.set(b.getValue()));
        this.worldSpace.tooltip(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.world_space.tooltip"));

        this.options.add(UI.label(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.effect")), this.pickEffect);
        this.options.add(this.pickPreview);
        this.options.add(UI.label(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.bone")), this.bone);
        this.options.add(this.paused, this.restart, this.loop);
        this.options.add(UI.label(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.duration")), this.duration);
        this.options.add(UI.label(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.speed")), this.speed);
        this.options.add(UI.label(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.scale")), this.particleScale);
        this.options.add(UI.label(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.sorting_layer")), this.sortingLayer);
        this.options.add(this.worldSpace);
    }

    private void openPicker()
    {
        if (this.cachedEffects == null)
        {
            this.cachedEffects = new ArrayList<>();
            this.populateEffects(this.cachedEffects);
        }

        UIListOverlayPanel panel = new UIListOverlayPanel(
            L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.select_effect"),
            (str) ->
            {
                if (this.form != null)
                {
                    this.setEffect(str);
                }
            }
        );

        panel.addValues(this.cachedEffects);
        UIOverlay.addOverlay(this.getContext(), panel, 0.5F, 0.7F);
    }

    private void populateEffects(List<String> list)
    {
        try
        {
            File assetsFolder = BBSMod.getAssetsFolder();
            File effeksFolder = new File(assetsFolder, "effeks");

            if (effeksFolder.exists() && effeksFolder.isDirectory())
            {
                this.scanEffeksFolder(list, effeksFolder, "bbs", "");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        Collections.sort(list);
    }

    private void scanEffeksFolder(List<String> list, File folder, String namespace, String prefix)
    {
        File[] files = folder.listFiles();

        if (files == null)
        {
            return;
        }

        for (File file : files)
        {
            String path = prefix.isEmpty() ? file.getName() : prefix + "/" + file.getName();

            if (file.isDirectory())
            {
                this.scanEffeksFolder(list, file, namespace, path);
            }
            else if (file.getName().endsWith(".efkefc"))
            {
                String key = namespace + ":" + path.substring(0, path.length() - 7);
                list.add(key);
            }
        }
    }

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

        String currentEffect = this.getCurrentEffectId();

        if (currentEffect != null)
        {
            this.pickEffect.label = L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.pick_effect").format(currentEffect);
        }
        else
        {
            this.pickEffect.label = L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.pick_effect");
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
