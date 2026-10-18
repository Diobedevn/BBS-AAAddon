package diobede.bbsaaa.ui.forms.editors.panels;

import diobede.bbsaaa.forms.AAAParticleForm;
import diobede.bbsaaa.forms.renderers.BBSEffectLoader;
import diobede.bbsaaa.forms.renderers.AAAParticleFormRenderer;
import diobede.bbsaaa.particles.ParticleEffectIds;
import diobede.bbsaaa.BBSAAaddon;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
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
 * Effect selection and playback controls. Spatial transforms belong to CML.
 */
public class UIAAAParticleFormPanel extends UIFormPanel<AAAParticleForm>
{
    public UIButton pickEffect;
    public UIButton reloadEffects;
    public UIButton pickPreview;
    public UIToggle paused;
    public UIButton restartPreview;
    public UIToggle loop;
    public UITrackpad speed;
    public UITrackpad particleScale;
    private final UIElement legacyScaleControls;

    public UIAAAParticleFormPanel(UIForm editor)
    {
        super(editor);

        this.pickEffect = new UIButton(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.pick_effect"), (b) -> this.openPicker());

        this.reloadEffects = new UIButton(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.reload_effects"), (b) ->
        {
            BBSEffectLoader.requestReload();
        });

        this.pickPreview = new UIButton(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.preview"), (b) ->
        {
            UITexturePicker.open(this.getContext(), this.form.preview.get(), (l) -> this.form.preview.set(l));
        });
        this.pickPreview.tooltip(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.preview.tooltip"));

        this.paused = new UIToggle(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.paused"), (b) -> this.form.paused.set(b.getValue()));
        this.restartPreview = new UIButton(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.restart_preview"), (b) ->
        {
            if (this.form != null && FormUtilsClient.getRenderer(this.form) instanceof AAAParticleFormRenderer renderer)
            {
                renderer.restartPreview();
            }
        });
        this.restartPreview.tooltip(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.restart_preview.tooltip"));
        this.loop = new UIToggle(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.loop"), (b) -> this.form.loop.set(b.getValue()));
        this.loop.tooltip(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.loop.tooltip"));

        this.speed = new UITrackpad((v) -> this.form.speed.set(v.floatValue()));
        this.speed.limit(0.01D, 10D);
        this.speed.tooltip(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.speed.tooltip"));

        this.particleScale = new UITrackpad((v) -> this.form.particleScale.set(v.floatValue()));
        this.particleScale.limit(0.01D, 100D);
        this.particleScale.tooltip(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.legacy_scale.tooltip"));
        this.legacyScaleControls = UI.column(5,
            UI.label(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.legacy_scale")), this.particleScale);

        this.options.add(UI.label(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.effect")), this.pickEffect);
        this.options.add(this.reloadEffects);
        this.options.add(this.pickPreview);
        this.options.add(this.paused, this.loop, this.restartPreview);
        this.options.add(UI.label(L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.speed")), this.speed);
    }

    private void openPicker()
    {
        List<String> effects = new ArrayList<>();
        this.populateEffects(effects);

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

        panel.addValues(effects);
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
            BBSAAaddon.LOGGER.error("Failed to list external particle effects", e);
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
                if (Identifier.tryParse(key) != null)
                {
                    list.add(key);
                }
            }
        }
    }

    private void setEffect(String effectId)
    {
        if (effectId == null || effectId.isEmpty())
        {
            this.form.effect.set(null);
            this.updateEffectLabel();
            return;
        }

        Identifier id = Identifier.tryParse(effectId);
        if (id != null)
        {
            this.form.effect.set(ParticleEffectIds.toLink(id));
            this.updateEffectLabel();
        }
    }

    private String getCurrentEffectId()
    {
        Identifier id = ParticleEffectIds.fromLink(this.form.effect.get());
        return id == null ? null : id.toString();
    }

    private void updateEffectLabel()
    {
        String currentEffect = this.getCurrentEffectId();
        this.pickEffect.label = currentEffect == null
            ? L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.pick_effect") : IKey.raw(currentEffect);
    }

    @Override
    public void startEdit(AAAParticleForm form)
    {
        super.startEdit(form);
        this.updateEffectLabel();

        /* Update UI values */
        this.paused.setValue(form.paused.get());
        this.loop.setValue(form.loop.get());
        this.speed.setValue(form.speed.get());
        this.particleScale.setValue(form.particleScale.get());
        this.legacyScaleControls.removeFromParent();
        if (Float.compare(form.particleScale.get(), 1F) != 0)
        {
            this.options.add(this.legacyScaleControls);
        }
    }
}
