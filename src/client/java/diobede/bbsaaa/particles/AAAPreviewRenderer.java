package diobede.bbsaaa.particles;

import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mod.chloeprime.aaaparticles.client.render.EffekRenderer;
import net.minecraft.client.Camera;
import net.minecraft.world.InteractionHand;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** AAA 2.2.3 preview bridge for Minecraft 26.1.2's extracted camera render state. */
public final class AAAPreviewRenderer
{
    private final PreviewCamera camera = new PreviewCamera();
    private final CameraRenderState cameraState = new CameraRenderState();

    public void render(FormRenderingContext context)
    {
        this.camera.configure(context.camera);
        this.cameraState.pos = this.camera.position();
        this.cameraState.orientation.set(this.camera.rotation());
        this.cameraState.initialized = true;
        this.cameraState.projectionMatrix.set(context.camera.projection);
        this.cameraState.viewRotationMatrix.set(context.camera.view);
        EffekRenderer.onRenderHand(context.transition, InteractionHand.MAIN_HAND, context.stack,
            context.camera.projection, this.camera, this.cameraState);
    }

    private static final class PreviewCamera extends Camera
    {
        private final Vector3f forward = new Vector3f();

        private void configure(mchorse.bbs_mod.camera.Camera source)
        {
            this.setPosition(source.position.x, source.position.y, source.position.z);
            this.rotation().setFromNormalized(source.view).conjugate();
            this.rotation().transform(this.forward.set(0, 0, -1));
        }

        @Override
        public Vector3fc forwardVector()
        {
            return this.forward;
        }
    }
}
