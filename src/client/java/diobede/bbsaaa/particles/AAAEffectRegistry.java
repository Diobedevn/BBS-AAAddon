package diobede.bbsaaa.particles;

import diobede.bbsaaa.BBSAAaddon;
import mod.chloeprime.aaaparticles.api.client.EffectDefinition;
import mod.chloeprime.aaaparticles.api.client.EffectHolder;
import mod.chloeprime.aaaparticles.api.client.EffectMetadata;
import mod.chloeprime.aaaparticles.client.loader.EffekAssetLoader;
import net.minecraft.resources.Identifier;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/** The only dependency on AAA Particles' private registry layout. */
public final class AAAEffectRegistry
{
    private static final Map<Identifier, EffectHolder> OWNED = new HashMap<>();
    private static Field effectsField;
    private static boolean unavailable;

    private AAAEffectRegistry()
    {}

    @SuppressWarnings("unchecked")
    private static Map<Identifier, EffectHolder> effects(EffekAssetLoader loader) throws ReflectiveOperationException
    {
        if (effectsField == null)
        {
            effectsField = EffekAssetLoader.class.getDeclaredField("loadedEffects");
            effectsField.setAccessible(true);
        }

        return (Map<Identifier, EffectHolder>) effectsField.get(loader);
    }

    /** Takes ownership only on success. The caller must close rejected definitions. */
    public static boolean register(Identifier id, EffectDefinition definition)
    {
        EffekAssetLoader loader = EffekAssetLoader.get();
        if (loader == null || unavailable)
        {
            return false;
        }

        EffectHolder previous;
        try
        {
            Map<Identifier, EffectHolder> effects = effects(loader);
            if (effects.containsKey(id))
            {
                return false;
            }

            EffectHolder holder = new EffectHolder(EffectMetadata.DEFAULT, () -> definition);
            holder.load().join();
            effects.put(id, holder);
            previous = OWNED.put(id, holder);
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            unavailable = true;
            BBSAAaddon.LOGGER.error("AAA Particles registry integration failed; external effects are disabled", e);
            return false;
        }

        // Registration succeeded. A failure disposing an old holder must not
        // make the caller close the newly registered definition.
        if (previous != null)
        {
            try
            {
                previous.close();
            }
            catch (RuntimeException e)
            {
                BBSAAaddon.LOGGER.error("Cannot release replaced particle effect {}", id, e);
            }
        }
        return true;
    }

    public static void clear()
    {
        Map<Identifier, EffectHolder> effects = null;
        EffekAssetLoader loader = EffekAssetLoader.get();
        if (loader != null && effectsField != null)
        {
            try
            {
                effects = effects(loader);
            }
            catch (ReflectiveOperationException | RuntimeException e)
            {
                BBSAAaddon.LOGGER.error("Cannot detach BBS effects from AAA Particles", e);
            }
        }

        for (Map.Entry<Identifier, EffectHolder> entry : OWNED.entrySet())
        {
            try
            {
                // A resource reload may have installed a different holder with this ID.
                if (effects != null)
                {
                    effects.remove(entry.getKey(), entry.getValue());
                }
                entry.getValue().close();
            }
            catch (RuntimeException e)
            {
                BBSAAaddon.LOGGER.error("Cannot release external particle effect {}", entry.getKey(), e);
            }
        }
        OWNED.clear();
    }

    public static boolean isAvailable()
    {
        return !unavailable;
    }
}
