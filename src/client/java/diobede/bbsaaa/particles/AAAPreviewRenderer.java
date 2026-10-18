package diobede.bbsaaa.particles;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mod.chloeprime.aaaparticles.client.render.EffekRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.util.Hand;

/** Fabric 1.20.1 preview bridge; method references are remapped by Loom. */
public final class AAAPreviewRenderer
{
    private final PreviewCamera camera = new PreviewCamera();

    public void render(FormRenderingContext context)
    {
        this.camera.configure(context.camera);
        EffekRenderer.onRenderHand(context.transition, Hand.MAIN_HAND, context.stack,
            RenderSystem.getProjectionMatrix(), this.camera);
    }

    private static final class PreviewCamera extends Camera
    {
        private void configure(mchorse.bbs_mod.camera.Camera source)
        {
            this.setPos(source.position.x, source.position.y, source.position.z);
            this.setRotation((float) Math.toDegrees(source.rotation.y) - 180F,
                (float) Math.toDegrees(source.rotation.x));
        }
    }
}
