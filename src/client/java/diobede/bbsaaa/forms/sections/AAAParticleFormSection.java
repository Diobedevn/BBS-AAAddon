package diobede.bbsaaa.forms.sections;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.forms.FormCategories;
import diobede.bbsaaa.forms.AAAParticleForm;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.sections.SubFormSection;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.watchdog.WatchDogEvent;
import mod.chloeprime.aaaparticles.client.loader.EffekAssetLoader;
import net.minecraft.util.Identifier;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

/**
 * AAA Particle Form Section
 *
 * Lists all available Effekseer particle effects from the AAA Particles mod.
 * Effects are loaded from:
 * - Resource packs: assets/[namespace]/effeks/
 * - BBS external assets: config/bbs/assets/effeks/ (no namespace required)
 */
public class AAAParticleFormSection extends SubFormSection
{
    public AAAParticleFormSection(FormCategories parent)
    {
        super(parent);
    }

    @Override
    public void initiate()
    {
        System.out.println("AAAParticleFormSection: Initiating...");
        /* Load from AAA Particles resource pack loader */
        EffekAssetLoader loader = EffekAssetLoader.get();

        if (loader != null)
        {
            loader.forEach((id, definition) ->
            {
                System.out.println("AAAParticleFormSection: Found particle: " + id);
                this.add(id.toString());
            });
        }
        else
        {
            System.out.println("AAAParticleFormSection: EffekAssetLoader is null!");
        }

        /* Also scan BBS external assets folder directly (no namespace required) */
        this.scanBBSExternalAssets();

        if (this.categories.isEmpty())
        {
            System.out.println("AAAParticleFormSection: No particles found, forcing root category.");
            this.getCategory("aaa_particles_root");
        }
    }

    /**
     * Scans config/bbs/assets/effeks/ folder for .efkefc files without namespace requirement
     */
    private void scanBBSExternalAssets()
    {
        File assetsFolder = BBSMod.getAssetsFolder();
        File effeksFolder = new File(assetsFolder, "effeks");

        if (!effeksFolder.exists() || !effeksFolder.isDirectory())
        {
            System.out.println("AAAParticleFormSection: Effeks folder not found at " + effeksFolder.getAbsolutePath());
            return;
        }

        this.scanEffeksFolder(effeksFolder, "bbs", "");
    }

    /**
     * Recursively scans for .efkefc files
     */
    private void scanEffeksFolder(File folder, String namespace, String prefix)
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
                this.scanEffeksFolder(file, namespace, path);
            }
            else if (file.getName().endsWith(".efkefc"))
            {
                /* Remove .efkefc extension for the key */
                String key = namespace + ":" + path.substring(0, path.length() - 7);
                System.out.println("AAAParticleFormSection: Adding external particle: " + key);

                this.add(key);
            }
        }
    }

    @Override
    protected IKey getTitle()
    {
        return L10n.lang("bbsaaa.ui.forms.categories.aaa_particles");
    }

    @Override
    protected Form create(String key)
    {
        AAAParticleForm form = new AAAParticleForm();
        Identifier id = new Identifier(key);

        /* Create link with effeks/ prefix for the effect path */
        form.effect.set(new Link(id.getNamespace(), "effeks/" + id.getPath() + ".efkefc"));

        return form;
    }

    @Override
    protected boolean isEqual(Form form, String key)
    {
        if (!(form instanceof AAAParticleForm particleForm))
        {
            return false;
        }

        Link effect = particleForm.effect.get();

        if (effect == null)
        {
            return false;
        }

        /* Reconstruct the identifier from the effect link */
        String path = effect.path;

        if (path == null)
        {
            return false;
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

        String formId = effect.source + ":" + path;

        return Objects.equals(formId, key);
    }

    @Override
    public void accept(Path path, WatchDogEvent event)
    {
        Link link = BBSMod.getProvider().getLink(path.toFile());

        if (link.path.startsWith("effeks/") && link.path.endsWith(".efkefc"))
        {
            String key = link.source + ":" + link.path.substring("effeks/".length());

            /* Remove .efkefc extension */
            key = key.substring(0, key.length() - 7);

            if (event == WatchDogEvent.DELETED)
            {
                this.remove(key);
            }
            else
            {
                this.add(key);
            }

            this.parent.markDirty();
        }
    }
}
