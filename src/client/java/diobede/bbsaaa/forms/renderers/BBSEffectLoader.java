package diobede.bbsaaa.forms.renderers;

import mchorse.bbs_mod.BBSMod;
import mod.chloeprime.aaaparticles.api.client.EffectDefinition;
import mod.chloeprime.aaaparticles.api.client.EffectHolder;
import mod.chloeprime.aaaparticles.api.client.EffectMetadata;
import mod.chloeprime.aaaparticles.api.client.effekseer.EffekseerEffect;
import mod.chloeprime.aaaparticles.api.client.effekseer.TextureType;
import mod.chloeprime.aaaparticles.client.loader.EffekAssetLoader;
import mod.chloeprime.aaaparticles.client.render.RenderUtil;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BBS Effect Loader
 *
 * Custom loader for Effekseer effects from BBS external assets folder.
 * Injects loaded effects into AAA Particles' EffekAssetLoader so they
 * are rendered automatically by the AAA Particles rendering pipeline.
 */
public class BBSEffectLoader
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BBSEffectLoader.class);


    /* Track which effects we've loaded so we can clean them up */
    private static final Set<Identifier> bbsLoadedEffects = new HashSet<>();

    /* Cached reflection field for AAA Particles' loaded effects map */
    private static Field loadedEffectsField = null;

    private static volatile boolean reloadRequested;
    private static volatile boolean reloading;

    public static void requestReload()
    {
        reloadRequested = true;
    }

    public static boolean beginReload()
    {
        if (!reloadRequested)
        {
            return false;
        }

        reloadRequested = false;
        reloading = true;

        return true;
    }

    public static void endReload()
    {
        reloading = false;
    }

    public static boolean isReloading()
    {
        return reloading;
    }

    public static boolean canLoadExternalEffects()
    {
        return !reloading;
    }

    /**
     * Get or load an effect definition from BBS external assets.
     * Effects are injected into AAA Particles' EffekAssetLoader for automatic rendering.
     */
    public static EffectDefinition getOrLoad(Identifier id)
    {
        if (!canLoadExternalEffects())
        {
            return null;
        }

        /* First check if AAA Particles already has this effect */
        EffekAssetLoader loader = EffekAssetLoader.get();

        if (loader != null)
        {
            EffectHolder holder = loader.get(id);

            if (holder != null)
            {
                /* Try lazy get (already loaded) first, otherwise trigger load */
                EffectDefinition existing = holder.lazyGet()
                        .orElseGet(() -> holder.load().join().orElse(null));

                if (existing != null)
                {
                    return existing;
                }
            }
        }
        else
        {
            LOGGER.warn("EffekAssetLoader.get() returned null!");
        }

        /* Check if we already tried to load this */
        if (bbsLoadedEffects.contains(id))
        {
            /* Already loaded into AAA Particles, get from there */
            if (loader == null) return null;
            EffectHolder cached = loader.get(id);
            return cached != null ? cached.lazyGet().orElse(null) : null;
        }

        LOGGER.info("Attempting to load effect from BBS assets: {}", id);

        /* Try to load from external assets */
        File assetsFolder = BBSMod.getAssetsFolder();
        String path = id.getPath();

        /* If path doesn't include effeks/, add it */
        if (!path.startsWith("effeks/"))
        {
            path = "effeks/" + path;
        }

        /* Add .efkefc extension if not present */
        if (!path.endsWith(".efkefc"))
        {
            path = path + ".efkefc";
        }

        File effectFile = new File(assetsFolder, path);

        /* Fallback logic: check effekseer/ prefix if file not found */
        if (!effectFile.exists())
        {
            String altPath = id.getPath();
            
            if (!altPath.startsWith("effekseer/"))
            {
                altPath = "effekseer/" + altPath;
            }
            
            if (!altPath.endsWith(".efkefc"))
            {
                altPath = altPath + ".efkefc";
            }
            
            File altEffectFile = new File(assetsFolder, altPath);
            
            if (altEffectFile.exists())
            {
                effectFile = altEffectFile;
                // We don't update 'path' here because it might be used for logging relative to what we expected,
                // or maybe we should? The original code uses 'path' only for logging in loadEffect if it fails.
                // But loadEffect takes effectFile, so it should be fine.
            }
        }

        if (!effectFile.exists())
        {
            LOGGER.debug("Effect file not found: {}", effectFile.getAbsolutePath());

            return null;
        }

        EffectDefinition definition = loadEffect(effectFile, id);

        if (definition != null)
        {
            /* Inject into AAA Particles */
            injectIntoAAAParticles(id, definition);
            bbsLoadedEffects.add(id);
        }

        return definition;
    }

    /**
     * Inject an effect definition into AAA Particles' EffekAssetLoader using reflection.
     * This allows our effects to be rendered by AAA Particles' rendering pipeline.
     */
    private static void injectIntoAAAParticles(Identifier id, EffectDefinition definition)
    {
        try
        {
            EffekAssetLoader loader = EffekAssetLoader.get();

            if (loader == null)
            {
                LOGGER.warn("EffekAssetLoader not initialized, cannot inject effect {}", id);

                return;
            }

            if (loadedEffectsField == null)
            {
                loadedEffectsField = EffekAssetLoader.class.getDeclaredField("loadedEffects");
                loadedEffectsField.setAccessible(true);
            }

            @SuppressWarnings("unchecked")
            Map<Object, EffectHolder> loadedEffects = (Map<Object, EffectHolder>) loadedEffectsField.get(loader);

            /* Wrap the definition in a holder so the registry pipeline can use it */
            EffectHolder holder = new EffectHolder(EffectMetadata.DEFAULT, () -> definition);
            holder.load().join(); /* Mark it as loaded immediately */
            loadedEffects.put(id, holder);

            LOGGER.info("Injected BBS effect into AAA Particles: {}", id);
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            LOGGER.error("Failed to inject effect into AAA Particles: {}", id, e);
        }
    }

    /**
     * Load an effect from file
     */
    private static EffectDefinition loadEffect(File effectFile, Identifier id)
    {
        try (FileInputStream input = new FileInputStream(effectFile))
        {
            EffekseerEffect effect = new EffekseerEffect();
            boolean success = effect.load(input, 1);

            if (!success)
            {
                LOGGER.error("Failed to load effect: {}", effectFile.getAbsolutePath());

                return null;
            }

            File parentDir = effectFile.getParentFile();

            /* Wrap all GPU uploads in GL state protection.
             * Minecraft sets GL_UNPACK_ROW_LENGTH and similar pixel store state when
             * loading its own textures. Without this wrapper, texture/model uploads
             * happening mid-frame pick up that dirty state, causing UV corruption. */
            RenderUtil.runEffekLoadCodeHealthily(() ->
            {
                /* Load textures */
                for (TextureType texType : TextureType.values())
                {
                    int count = effect.textureCount(texType);

                    for (int i = 0; i < count; i++)
                    {
                        final int index = i;
                        String texturePath = effect.getTexturePath(i, texType);

                        if (texturePath != null && !texturePath.isEmpty())
                        {
                            loadAsset(parentDir, texturePath, (data, len) ->
                                effect.loadTexture(data, len, index, texType));
                        }
                    }
                }

                /* Load models */
                int modelCount = effect.modelCount();

                for (int i = 0; i < modelCount; i++)
                {
                    final int index = i;
                    String modelPath = effect.getModelPath(i);

                    if (modelPath != null && !modelPath.isEmpty())
                    {
                        loadAsset(parentDir, modelPath, (data, len) ->
                            effect.loadModel(data, len, index));
                    }
                }

                /* Load curves */
                int curveCount = effect.curveCount();

                for (int i = 0; i < curveCount; i++)
                {
                    final int index = i;
                    String curvePath = effect.getCurvePath(i);

                    if (curvePath != null && !curvePath.isEmpty())
                    {
                        loadAsset(parentDir, curvePath, (data, len) ->
                            effect.loadCurve(data, len, index));
                    }
                }

                /* Load materials */
                int materialCount = effect.materialCount();

                for (int i = 0; i < materialCount; i++)
                {
                    final int index = i;
                    String materialPath = effect.getMaterialPath(i);

                    if (materialPath != null && !materialPath.isEmpty())
                    {
                        loadAsset(parentDir, materialPath, (data, len) ->
                            effect.loadMaterial(data, len, index));
                    }
                }
            });

            EffectDefinition definition = new EffectDefinition(EffectMetadata.DEFAULT);

            definition.setEffect(effect);

            LOGGER.info("Loaded BBS effect: {} from {}", id, effectFile.getAbsolutePath());

            return definition;
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to load effect file: {}", effectFile.getAbsolutePath(), e);

            return null;
        }
    }

    /**
     * Load an asset file relative to the effect file
     */
    private static void loadAsset(File parentDir, String assetPath, AssetLoader loader)
    {
        /* Normalize path separators */
        assetPath = assetPath.replace("\\", "/");

        File assetFile = new File(parentDir, assetPath);

        if (!assetFile.exists())
        {
            LOGGER.warn("Asset file not found: {}", assetFile.getAbsolutePath());

            return;
        }

        try (FileInputStream input = new FileInputStream(assetFile))
        {
            byte[] data = input.readAllBytes();

            loader.load(data, data.length);
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to load asset: {}", assetFile.getAbsolutePath(), e);
        }
    }

    public static int preloadExternalEffects()
    {
        if (!canLoadExternalEffects())
        {
            return 0;
        }

        File assetsFolder = BBSMod.getAssetsFolder();
        File effeksFolder = new File(assetsFolder, "effeks");

        if (!effeksFolder.exists() || !effeksFolder.isDirectory())
        {
            return 0;
        }

        List<File> files = new ArrayList<>();
        collectEfks(files, effeksFolder);

        int loaded = 0;

        for (File file : files)
        {
            String relative = effeksFolder.toPath().relativize(file.toPath()).toString().replace("\\", "/");

            if (!relative.endsWith(".efkefc"))
            {
                continue;
            }

            relative = relative.substring(0, relative.length() - 7);

            Identifier id;

            try
            {
                id = Identifier.of("bbs", relative);
            }
            catch (Exception e)
            {
                continue;
            }

            if (bbsLoadedEffects.contains(id))
            {
                continue;
            }

            EffectDefinition definition = getOrLoad(id);

            if (definition != null)
            {
                loaded += 1;
            }
        }

        return loaded;
    }

    private static void collectEfks(List<File> list, File folder)
    {
        File[] files = folder.listFiles();

        if (files == null)
        {
            return;
        }

        for (File file : files)
        {
            if (file.isDirectory())
            {
                collectEfks(list, file);
            }
            else if (file.getName().endsWith(".efkefc"))
            {
                list.add(file);
            }
        }
    }

    /**
     * Clear all BBS-loaded effects from AAA Particles
     */
    public static void clearCache()
    {
        try
        {
            EffekAssetLoader loader = EffekAssetLoader.get();

            if (loader != null && loadedEffectsField != null)
            {
                @SuppressWarnings("unchecked")
                Map<Object, EffectHolder> loadedEffects = (Map<Object, EffectHolder>) loadedEffectsField.get(loader);

                List<Identifier> ids = new ArrayList<>(bbsLoadedEffects);

                for (Identifier id : ids)
                {
                    EffectHolder holder = loadedEffects.remove(id);

                    if (holder != null)
                    {
                        holder.close();
                    }
                }
            }
        }
        catch (Throwable e)
        {
            LOGGER.error("Failed to clear BBS effects from AAA Particles", e);
        }

        bbsLoadedEffects.clear();
    }

    /**
     * Check if an effect is loaded
     */
    public static boolean isLoaded(Identifier id)
    {
        return bbsLoadedEffects.contains(id);
    }

    /**
     * Check if there are any BBS effects loaded
     */
    public static boolean hasLoadedEffects()
    {
        return !bbsLoadedEffects.isEmpty();
    }

    @FunctionalInterface
    private interface AssetLoader
    {
        boolean load(byte[] data, int length);
    }
}
