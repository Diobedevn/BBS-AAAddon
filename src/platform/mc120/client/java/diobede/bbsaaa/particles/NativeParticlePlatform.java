package diobede.bbsaaa.particles;

import mchorse.bbs_mod.forms.renderers.FormRenderingContext;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;

/** Native identifiers and the host's version-specific transform convention. */
public final class NativeParticlePlatform
{
    private NativeParticlePlatform()
    {
    }

    public static Identifier identifier(String namespace, String path)
    {
        return new Identifier(namespace, path);
    }

    public static boolean paused()
    {
        return MinecraftClient.getInstance().isPaused();
    }

    public static Matrix4f matrix(FormRenderingContext context)
    {
        return new Matrix4f(RenderSystem.getInverseViewRotationMatrix()).mul(context.stack.peek().getPositionMatrix());
    }
}
