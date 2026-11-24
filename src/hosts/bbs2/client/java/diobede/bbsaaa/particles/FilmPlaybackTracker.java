package diobede.bbsaaa.particles;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIScreen;

import net.minecraft.client.MinecraftClient;

/** Resolve the owning upstream controller without a second registry of films. */
public final class FilmPlaybackTracker
{
    public static void initialize()
    {}

    public static void clear()
    {}

    public static boolean isPaused(IEntity entity)
    {
        if (MinecraftClient.getInstance().currentScreen instanceof UIScreen screen
            && screen.getMenu() instanceof UIDashboard dashboard
            && dashboard.getPanels().panel instanceof UIFilmPanel panel)
        {
            BaseFilmController controller = panel.getController().editorController;
            if (controller != null && controller.getEntities().containsValue(entity))
            {
                return !panel.isRunning();
            }
        }
        for (BaseFilmController controller : BBSModClient.getFilms().getControllers())
        {
            if (controller.getEntities().containsValue(entity))
            {
                return controller.paused;
            }
        }
        return false;
    }
}
