package diobede.bbsaaa.compat.host;

import java.lang.reflect.Method;

/** Shared native mixins must never link the inactive host renderer. */
public final class WorldEmitterProbe
{
    private static final Method PROBE = resolve();

    private static Method resolve()
    {
        try
        {
            String name = HostLanguageAdapter.implementation("diobede.bbsaaa.forms.renderers.AAAParticleFormRenderer");
            return Class.forName(name).getMethod("hasWorldEmitters");
        }
        catch (ReflectiveOperationException exception)
        {
            throw new IllegalStateException("Cannot resolve selected host renderer", exception);
        }
    }

    public static boolean hasWorldEmitters()
    {
        try
        {
            return (Boolean) PROBE.invoke(null);
        }
        catch (ReflectiveOperationException exception)
        {
            throw new IllegalStateException("Cannot query selected host renderer", exception);
        }
    }
}
