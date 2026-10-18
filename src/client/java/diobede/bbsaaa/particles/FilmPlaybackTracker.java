package diobede.bbsaaa.particles;

import mchorse.bbs_mod.api.client.events.FilmEvents;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.forms.entities.IEntity;
import diobede.bbsaaa.forms.renderers.AAAParticleFormRenderer;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Uses CML lifecycle events instead of inferring film pause from render timing. */
public final class FilmPlaybackTracker
{
    private static final Set<BaseFilmController> FILMS = Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean initialized;

    private FilmPlaybackTracker()
    {}

    public static void initialize()
    {
        if (initialized)
        {
            return;
        }

        initialized = true;
        FilmEvents.CREATED.register(FILMS::add);
        FilmEvents.TICK_BEFORE.register((film, tick) -> FILMS.add(film));
        FilmEvents.SHUTDOWN.register((film) ->
        {
            AAAParticleFormRenderer.cleanupFilm(film);
            FILMS.remove(film);
        });
    }

    public static boolean isPaused(IEntity entity)
    {
        for (BaseFilmController film : FILMS)
        {
            for (IEntity actor : film.getEntities().values())
            {
                if (actor == entity)
                {
                    return film.paused;
                }
            }
        }

        return false;
    }

    public static void clear()
    {
        FILMS.clear();
    }
}
