package ports.smoke;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.StubEntity;

import java.util.Map;

import diobede.bbsaaa.forms.AAAParticleForm;
import diobede.bbsaaa.particles.FilmPlaybackTracker;

/** Exercise host linkage and clocks with independent controllers in a running client. */
public final class Bbs2ClientChecks
{
    public static void check(AAAParticleForm form)
    {
        Film film = new Film();
        Replay replay = film.replays.addReplay();
        replay.looping.set(10);
        StubEntity entity = new StubEntity();
        entity.setForm(form);
        ClockController controller = new ClockController(film);
        controller.getEntities().put(0, entity);
        BBSModClient.getFilms().getControllers().add(controller);
        try
        {
            require(!FilmPlaybackTracker.isPaused(entity), "running owning film");
            controller.paused = true;
            require(FilmPlaybackTracker.isPaused(entity), "paused owning film");
            require(!FilmPlaybackTracker.isPaused(new StubEntity()), "unrelated entity stays live");
            controller.shutdown();
            try
            {
                Class.forName("mchorse.bbs_mod.film.WorldFilmController");
            }
            catch (ClassNotFoundException exception)
            {
                throw new AssertionError(exception);
            }
        }
        finally
        {
            BBSModClient.getFilms().getControllers().remove(controller);
        }
    }

    private static void require(boolean condition, String check)
    {
        if (!condition)
        {
            throw new AssertionError(check);
        }
    }

    private static final class ClockController extends BaseFilmController
    {
        private int tick = 23;

        private ClockController(Film film)
        {
            super(film);
        }

        @Override
        public Map<String, Integer> getActors()
        {
            return Map.of();
        }

        @Override
        public int getTick()
        {
            return this.tick;
        }
    }
}
