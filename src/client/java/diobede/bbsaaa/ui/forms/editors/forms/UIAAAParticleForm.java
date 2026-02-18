package diobede.bbsaaa.ui.forms.editors.forms;

import diobede.bbsaaa.forms.AAAParticleForm;
import diobede.bbsaaa.ui.forms.editors.panels.UIAAAParticleFormPanel;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.utils.icons.Icons;

/**
 * AAA Particle Form Editor
 *
 * UI editor for configuring AAA Particle (Effekseer) forms.
 * Provides controls for effect selection, playback, and transform settings.
 */
public class UIAAAParticleForm extends UIForm<AAAParticleForm>
{
    public UIAAAParticleFormPanel particlePanel;

    public UIAAAParticleForm()
    {
        super();

        this.particlePanel = new UIAAAParticleFormPanel(this);

        this.registerPanel(this.particlePanel, L10n.lang("bbsaaa.ui.forms.editors.aaa_particle.title"), Icons.PARTICLE);
        this.registerDefaultPanels();

        this.defaultPanel = this.particlePanel;
    }
}
