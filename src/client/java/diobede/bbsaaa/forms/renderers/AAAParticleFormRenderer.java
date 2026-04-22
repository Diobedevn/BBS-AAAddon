package diobede.bbsaaa.forms.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import diobede.bbsaaa.forms.AAAParticleForm;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.graphics.texture.Texture;
import mod.chloeprime.aaaparticles.api.client.effekseer.ParticleEmitter;
import mod.chloeprime.aaaparticles.api.client.EffectDefinition;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Vector3d;
import org.joml.Vector3f;
import mod.chloeprime.aaaparticles.client.render.EffekRenderer;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AAA Particle Form Renderer
 *
 * Renders Effekseer particles using the AAA Particles mod.
 * Supports bone attachment, looping, duration control, and context-aware rendering.
 * Includes tick memory system for film playback synchronization.
 */
public class AAAParticleFormRenderer extends FormRenderer<AAAParticleForm> implements ITickable
{
    /* Global registry of active renderers to ensure cleanup */
    public static final List<AAAParticleFormRenderer> activeRenderers = Collections.synchronizedList(new ArrayList<>());

    public static final Link ICON = new Link("bbs-aaaddon", "textures/aaa_texture.png");

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        Texture texture = context.render.getTextures().getTexture(ICON);

        if (texture == null)
        {
            return;
        }

        float min = Math.min(texture.width, texture.height);

        if (min <= 0)
        {
            min = 1;
        }

        int ow = (x2 - x1) - 4;
        int oh = (y2 - y1) - 4;

        int w = (int) ((texture.width / min) * ow);
        int h = (int) ((texture.height / min) * ow);

        int x = x1 + (ow - w) / 2 + 2;
        int y = y1 + (oh - h) / 2 + 2;

        context.batcher.fullTexturedBox(texture, x, y, w, h);
    }
    
    /* Tick memory system - stores particle state per tick for film seeking */
    private static class ParticleState
    {
        int tick;
        boolean wasPlaying;
        Identifier effectId;

        ParticleState(int tick, boolean wasPlaying, Identifier effectId)
        {
            this.tick = tick;
            this.wasPlaying = wasPlaying;
            this.effectId = effectId;
        }
    }

    /* Track emitter state per form instance */
    private ParticleEmitter emitter;
    private Identifier lastEffectId;
    private boolean lastPaused = false;
    private boolean lastRestart = false;

    /* Film synchronization */
    private int currentTick = -1;
    private int lastTick = -1;
    private int lastRenderTick = -1;
    private boolean filmWasPlaying = false;
    private boolean hadForm = false;
    private Map<Integer, ParticleState> tickMemory = new HashMap<>();
    private long lastTickTime = 0;
    private long lastRenderTime = 0;
    private FormRenderType lastRenderType = null;
    private boolean loggedDebug = false;
    private float editorProgress = 0f;
    private long lastEditorTime = 0;
    private long lastFrameTime = 0;
    
    /* Unique identifier for named emitters */
    private final Identifier emitterName;

    public AAAParticleFormRenderer(AAAParticleForm form)
    {
        super(form);

        this.emitterName = Identifier.of(BBSMod.MOD_ID, "form_" + UUID.randomUUID().toString().replace("-", ""));
        
        activeRenderers.add(this);
    }
    
    /**
     * Called by global client tick to clean up abandoned effects
     */
    public boolean checkCleanup()
    {
        if (System.currentTimeMillis() - this.lastRenderTime > 100)
        {
            this.stopEmitter();
            return true;
        }
        
        return false;
    }

    public ParticleEmitter getEmitter()
    {
        return this.emitter;
    }

    /**
     * Get the effect identifier from the form's effect link
     */
    private Identifier getEffectId()
    {
        Link effect = this.form.effect.get();

        if (effect == null)
        {
            return null;
        }

        String path = effect.path;

        if (path == null || path.isEmpty())
        {
            return null;
        }

        /* Remove .efkefc extension if present */
        if (path.endsWith(".efkefc"))
        {
            path = path.substring(0, path.length() - 7);
        }

        /* Remove effeks/ prefix if present */
        if (path.startsWith("effeks/"))
        {
            path = path.substring(7);
        }

        return Identifier.of(effect.source, path);
    }

    /**
     * Ensure the emitter is created and up to date
     */
    private void ensureEmitter()
    {
        if (BBSRendering.isIrisShadowPass())
        {
            return;
        }

        if (!BBSEffectLoader.canLoadExternalEffects())
        {
            return;
        }

        Identifier effectId = this.getEffectId();

        if (effectId == null)
        {
            this.stopEmitter();
            return;
        }

        /* Check if effect changed */
        if (this.lastEffectId == null || !this.lastEffectId.equals(effectId))
        {
            this.stopEmitter();
            this.lastEffectId = effectId;
            
            /* Clear tick memory when effect changes - old cache is invalid */
            this.tickMemory.clear();
        }

        /* Get effect definition via BBSEffectLoader, which checks EffekAssetLoader first
         * (handles both resource-pack effects and BBS external assets).
         * Direct EffectRegistry usage was removed for compatibility with AAA Particles 2.x. */
        EffectDefinition definition = BBSEffectLoader.getOrLoad(effectId);

        if (definition == null)
        {
            return;
        }

        /* Determine target type based on render context */
        /* Use FIRST_PERSON_MAINHAND for PREVIEW (Form Editor) to use local coordinate system */
        /* Use WORLD for standard World rendering */
        boolean isPreview = this.lastRenderType == FormRenderType.PREVIEW;
        ParticleEmitter.Type targetType = isPreview ? ParticleEmitter.Type.FIRST_PERSON_MAINHAND : ParticleEmitter.Type.WORLD;

        /* Check if we need to recreate emitter due to type mismatch */
        if (this.emitter != null && this.emitter.type != targetType)
        {
            this.stopEmitter();
        }

        /* Create emitter if needed */
        if (this.emitter == null)
        {
            this.emitter = definition.play(targetType, this.emitterName);
            
            if (this.emitter != null && !activeRenderers.contains(this))
            {
                activeRenderers.add(this);
            }
        }

        /* Skip the rest if emitter doesn't exist */
        if (this.emitter == null)
        {
            return;
        }

        /* Detect if film is playing: tick() is only called when film is playing */
        /* Only use tick timing detection for ENTITY render type (films) */
        /* For model blocks, items, and other contexts, always consider as playing */
        boolean filmPlaying;
        
        if (this.lastRenderType == FormRenderType.ENTITY)
        {
            /* Film context - use tick timing to detect if film is paused */
            long currentTime = System.currentTimeMillis();
            filmPlaying = (currentTime - this.lastTickTime) < 100;
        }
        else
        {
            /* Model block, item, or unknown context - always playing */
            filmPlaying = true;
        }

        /* Update pause state - pause particle when film is paused OR when paused property is true */
        boolean manualSpeed = Math.abs(this.form.speed.get() - 1.0f) > 0.001f;
        boolean shouldPause = this.form.paused.get() || !filmPlaying;

        if (shouldPause != this.lastPaused)
        {
            if (shouldPause)
            {
                this.emitter.pause();
            }
            else
            {
                this.emitter.resume();
            }

            this.lastPaused = shouldPause;
        }

        /* Update visibility */
        this.emitter.setVisibility(this.form.visible.get());
    }

    /**
     * Stop and clean up the current emitter
     */
    private void stopEmitter()
    {
        if (this.emitter != null)
        {
            this.emitter.setVisibility(false);
            this.emitter.stop();
            this.emitter = null;
        }

        this.lastPaused = false;
        this.lastRestart = false;
        this.tickMemory.clear();
        this.currentTick = -1;
        this.lastTick = -1;
        this.editorProgress = 0f;
        
        activeRenderers.remove(this);
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        this.lastRenderTime = System.currentTimeMillis();
        // System.out.println("AAAParticleFormRenderer.render3D: " + this.getEffectId());
        /* Store render type for film playing detection */
        this.lastRenderType = context.type;

        /* Check if entity or form was removed - stop particle */
        /* For ModelBlocks and Preview, the form might not be set on the StubEntity yet?
         * But we have this.form which IS the form we are rendering.
         */
        boolean isPreview = context.type == FormRenderType.PREVIEW;
        
        if (context.entity == null || (context.entity.getForm() == null && context.type != FormRenderType.MODEL_BLOCK && !isPreview))
        {
            if (this.hadForm)
            {
                this.stopEmitter();
                this.hadForm = false;
            }
            return;
        }
        
        /* Ensure entity has form set for consistency */
        if ((context.type == FormRenderType.MODEL_BLOCK || isPreview) && context.entity != null && context.entity.getForm() == null)
        {
            context.entity.setForm(this.form);
        }

        /* Check if effect was cleared - stop particle when replay is removed */
        if (this.form.effect.get() == null)
        {
            if (this.hadForm)
            {
                this.stopEmitter();
                this.hadForm = false;
            }
            return;
        }

        this.hadForm = true;

        /* Check for seeks even when paused - render is called even when film is paused */
        int renderTick = context.entity.getAge();
        boolean seekedWhilePaused = false;

        if (this.lastRenderTick >= 0 && renderTick != this.lastRenderTick)
        {
            /* Tick changed between renders - check if we seeked (jumped backwards or skipped) */
            if (renderTick < this.lastRenderTick || Math.abs(renderTick - this.lastRenderTick) > 1)
            {
                seekedWhilePaused = true;

                /* Try to restore particle state from memory */
                ParticleState state = this.tickMemory.get(renderTick);

                if (state != null && state.effectId != null && state.effectId.equals(this.getEffectId()))
                {
                    /* Restore particle from cached state */
                    this.stopEmitter();
                }
                else
                {
                    /* No cached state, restart particle */
                    this.stopEmitter();
                }
            }
        }

        this.lastRenderTick = renderTick;

        this.ensureEmitter();

        if (this.emitter == null || !this.emitter.exists())
        {
            return;
        }

        if (BBSRendering.isIrisShadowPass())
        {
            return;
        }

            /* Get View Space Matrix from Stack */
        Matrix4f pose = context.stack.peek().getPositionMatrix();
        
        /* Check if we are in Preview (Form Editor) mode */
        // boolean isPreview = false; // Already defined above
            try 
            { 
                try 
                { 
                    java.lang.reflect.Field typeField = context.getClass().getField("type"); 
                    Object typeObj = typeField.get(context); 

                    if (typeObj != null) 
                    { 
                        String typeName = typeObj.toString(); 
                        if (typeName.contains("PREVIEW")) 
                        { 
                            isPreview = true; 
                        } 
                    } 
                } 
                catch (Exception e) 
                { 
                } 
            } 
            catch (Exception e) 
            { 
            }
            
            /* Transform values — used directly in preview mode only.
             * In world mode, FormRenderer has already applied the full transform
             * (including rotate2 and pivot) onto the MatrixStack before render3D(). */
            Transform t = this.form.transform.get();
            Vector3f tPos = t.translate;
            Vector3f tRot = t.rotate;
            Vector3f tScale = t.scale;

            if (isPreview)
            {
                 /* Preview Mode (Form Editor) - Use Local Coordinates */
                 /* We use Type.FIRST_PERSON_MAINHAND which uses the passed matrix as base */
                 
                 /* Set Emitter Position (Local) */
                 this.emitter.setPosition(tPos.x, tPos.y, tPos.z);
                 
                 /* Set Emitter Rotation (Local) — tRot is already in radians */
                 this.emitter.setRotation(tRot.x, tRot.y, tRot.z);
                 
                 /* Set Emitter Scale (Local) */
                 float formScale = this.form.particleScale.get();
                 this.emitter.setScale(tScale.x * formScale, tScale.y * formScale, tScale.z * formScale);
                 
                 /* Render manually using onRenderHand */
                 try
                 {
                     Object stackObj = context.stack;
                     Matrix4f projection = RenderSystem.getProjectionMatrix();
                     
                     /* Create dummy camera for Preview */
                     net.minecraft.client.render.Camera camera = new net.minecraft.client.render.Camera();
                     mchorse.bbs_mod.camera.Camera bbsCam = context.camera;
                     
                     try {
                          /* Set Position */
                          java.lang.reflect.Method setPos = net.minecraft.client.render.Camera.class.getDeclaredMethod("setPos", double.class, double.class, double.class);
                          setPos.setAccessible(true);
                          setPos.invoke(camera, bbsCam.position.x, bbsCam.position.y, bbsCam.position.z);
                          
                          /* Set Rotation */
                          float pitch = (float) Math.toDegrees(bbsCam.rotation.x);
                          float yaw = (float) Math.toDegrees(bbsCam.rotation.y) - 180.0f;
                          
                          java.lang.reflect.Method setRot = net.minecraft.client.render.Camera.class.getDeclaredMethod("setRotation", float.class, float.class);
                          setRot.setAccessible(true);
                          setRot.invoke(camera, yaw, pitch);
                     } catch (Exception e) {
                          camera = MinecraftClient.getInstance().gameRenderer.getCamera();
                     }
                     
                     /* Use reflection to find and invoke onRenderHand */
                     /* Method signature: onRenderHand(float, InteractionHand, PoseStack, Matrix4f, Camera) */
                     java.lang.reflect.Method renderMethod = null;
                     
                     for (java.lang.reflect.Method m : EffekRenderer.class.getMethods()) {
                         if (m.getName().equals("onRenderHand") && m.getParameterCount() == 5) {
                             renderMethod = m;
                             break;
                         }
                     }
                     
                     if (renderMethod != null) {
                         /* Find InteractionHand.MAIN_HAND */
                         /* Since we don't know the exact class name (Fabric vs Forge), we find by name */
                         Object mainHand = null;
                         Class<?> handClass = renderMethod.getParameterTypes()[1]; // 2nd argument is InteractionHand
                         
                         if (handClass.isEnum()) {
                             for (Object constant : handClass.getEnumConstants()) {
                                 if (constant.toString().equals("MAIN_HAND")) {
                                     mainHand = constant;
                                     break;
                                 }
                             }
                         }
                         
                         if (mainHand != null) {
                             renderMethod.invoke(null, context.transition, mainHand, stackObj, projection, camera);
                         }
                     }
                 }
                 catch (Exception e)
                 {
                 }
            }
            else
            {
                /* World Mode - Use World Coordinates.
                 * FormRenderer.render() already applied the full transform (translate, rotate,
                 * rotate2, scale, pivot) onto the MatrixStack before calling render3D(), so
                 * 'pose' already encodes everything. We must NOT re-apply the transform values
                 * manually — that would double them.
                 *
                 * We pass the transform directly as a 3×4 matrix to bypass Euler angle
                 * decomposition entirely. getEulerAnglesXYZ() clamps the Y axis to [-π/2, π/2],
                 * which causes wrong results for Y rotations beyond ±90°. */
                matrix = new Matrix4f(pose);
                matrix.mul(pose);

                /* Position: extract translation then add camera world position */
                Vector3f trans = new Vector3f();
                matrix.getTranslation(trans);
                org.joml.Vector3d camPos = context.camera.position;
                float finalX = (float)(trans.x + camPos.x);
                float finalY = (float)(trans.y + camPos.y);
                float finalZ = (float)(trans.z + camPos.z);

                /* Build a 3×4 row-major matrix for Effekseer.
                 * The rotation/scale columns come directly from the JOML matrix (no Euler
                 * decomposition), scaled by particleScale. Translation is world position. */
                float ps = this.form.particleScale.get();
                float[] mat = {
                    matrix.m00() * ps, matrix.m10() * ps, matrix.m20() * ps, finalX,
                    matrix.m01() * ps, matrix.m11() * ps, matrix.m21() * ps, finalY,
                    matrix.m02() * ps, matrix.m12() * ps, matrix.m22() * ps, finalZ
                };
                this.emitter.setTransformMatrix(mat);
            }
        
        /* Calculate speed and manual playback condition */
        float speed = this.form.speed.get();
        boolean manualSpeed = Math.abs(speed - 1.0f) > 0.001f;
        boolean isGamePaused = MinecraftClient.getInstance().isPaused();
        
        /* If we are NOT paused (game or form), we should update progress */
        boolean shouldUpdate = !this.form.paused.get() && (!isGamePaused || isPreview);

        if (shouldUpdate)
        {
            long now = System.currentTimeMillis();

            if (this.lastFrameTime == 0)
            {
                this.lastFrameTime = now;
            }

            long timeDiff = now - this.lastFrameTime;
            this.lastFrameTime = now;

            /* If speed is 1.0, we rely on native update, BUT we still track progress to avoid jumps when switching */
            /* If manual speed, we use this progress to drive the animation */
            float framesToAdd = (timeDiff / 1000f * 60f) * speed;

            /* Cap large jumps */
            if (Math.abs(framesToAdd) > 10f)
            {
                framesToAdd = Math.signum(framesToAdd) * 1f;
            }
            
            if (framesToAdd < 0f && speed >= 0)
            {
                framesToAdd = 0f;
            }

            this.editorProgress += framesToAdd;
        }
        else
        {
            this.lastFrameTime = 0;
        }
        
        /* If manual speed is active OR game is paused (preview), we apply manual progress */
        /* IMPORTANT: setProgress must be called every frame to override native update if speed != 1.0 */
        if (manualSpeed || (isGamePaused && isPreview))
        {
            this.emitter.setProgress(this.editorProgress);
        }
    }

    @Override
    public void tick(IEntity entity)
    {
        /* Check if entity was removed - stop particle */
        if (entity == null || entity.getForm() == null)
        {
            /* If we are in PREVIEW mode (Form Editor), we might have a dummy entity with no form set */
            /* In this case, we should NOT stop the emitter, because render3D handles it */
            if (this.lastRenderType == FormRenderType.PREVIEW)
            {
                 /* Just ensure emitter and update time */
                 this.ensureEmitter();
                 this.lastTickTime = System.currentTimeMillis();
                 this.hadForm = true;
                 return;
            }

            if (this.hadForm)
            {
                this.stopEmitter();
                this.hadForm = false;
            }
            return;
        }

        /* Check if effect was cleared - stop particle when replay is removed */
        if (this.form.effect.get() == null)
        {
            if (this.hadForm)
            {
                this.stopEmitter();
                this.hadForm = false;
            }
            return;
        }

        this.hadForm = true;

        /* Record tick time - this tells ensureEmitter that film is playing */
        this.lastTickTime = System.currentTimeMillis();

        this.currentTick = entity.getAge();

        /* Detect seeking (tick jumped backwards or skipped forward significantly) */
        boolean seeked = this.lastTick >= 0 && (this.currentTick < this.lastTick || Math.abs(this.currentTick - this.lastTick) > 1);

        /* If seeked, try to restore particle state from memory */
        if (seeked)
        {
            ParticleState state = this.tickMemory.get(this.currentTick);

            if (state != null && state.effectId != null && state.effectId.equals(this.getEffectId()))
            {
                /* Restore particle from cached state */
                this.stopEmitter();
                this.ensureEmitter();
                this.filmWasPlaying = state.wasPlaying;
            }
            else
            {
                /* No cached state, restart particle */
                this.stopEmitter();
                this.ensureEmitter();
            }
        }
        else
        {
            this.ensureEmitter();
        }

        /* Store current state in tick memory (with size limit to prevent unbounded growth) */
        if (this.currentTick >= 0)
        {
            Identifier effectId = this.getEffectId();

            if (effectId != null)
            {
                /* Limit tick memory to 1000 entries to prevent memory leaks on very long films */
                if (this.tickMemory.size() >= 1000)
                {
                    /* Remove oldest entry (lowest tick number) */
                    int minTick = this.tickMemory.keySet().stream().min(Integer::compare).orElse(-1);
                    
                    if (minTick >= 0)
                    {
                        this.tickMemory.remove(minTick);
                    }
                }
                
                this.tickMemory.put(this.currentTick, new ParticleState(this.currentTick, true, effectId));
            }
        }

        if (this.emitter == null)
        {
            this.lastTick = this.currentTick;
            return;
        }

        /* Handle restart keyframe - trigger restart when property changes to true */
        boolean restart = this.form.restart.get();

        if (restart && !this.lastRestart)
        {
            this.emitter.stop();
            this.emitter = null;
            this.ensureEmitter();
        }

        this.lastRestart = restart;

        /* Check if effect finished and handle loop */
        if (!this.emitter.exists())
        {
            if (this.form.loop.get())
            {
                /* Restart by clearing emitter - ensureEmitter will recreate it */
                this.emitter = null;
                this.ensureEmitter();
            }
        }

        this.lastTick = this.currentTick;
        this.filmWasPlaying = true;
    }

    /**
     * Clean up resources when renderer is destroyed
     */
    public void cleanup()
    {
        this.stopEmitter();
    }
}
