package diobede.bbsaaa.particles;

import diobede.bbsaaa.forms.AAAParticleForm;
import diobede.bbsaaa.forms.renderers.BBSEffectLoader;
import mod.chloeprime.aaaparticles.api.client.EffectDefinition;
import mod.chloeprime.aaaparticles.api.client.effekseer.ParticleEmitter;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.UUID;

/** Owns one native emitter; loading and creation only happen on the render path. */
public final class ParticlePlayback
{
    private final Identifier name = Identifier.of("bbs-aaaddon", "form_" + UUID.randomUUID().toString().replace("-", ""));
    private final PlaybackState state = new PlaybackState();
    private Identifier effectId;
    private ParticleEmitter.Type type;
    private ParticleEmitter emitter;

    public ParticleEmitter getEmitter()
    {
        return this.emitter;
    }

    public void tick(int tick, boolean restart)
    {
        this.state.observeTick(tick, restart);
    }

    public void requestRestart()
    {
        this.state.requestRestart();
    }

    public ParticleEmitter update(AAAParticleForm form, Identifier id, boolean preview,
        boolean filmPaused, boolean gamePaused)
    {
        ParticleEmitter.Type target = preview ? ParticleEmitter.Type.FIRST_PERSON_MAINHAND : ParticleEmitter.Type.WORLD;

        if (!Objects.equals(id, this.effectId) || this.type != target)
        {
            this.stopEmitter();
            this.effectId = id;
            this.type = target;
        }

        if (id == null || !BBSEffectLoader.canLoadExternalEffects())
        {
            return null;
        }

        if (this.state.consumeRestart())
        {
            this.stopEmitter();
        }

        if (this.emitter != null && !this.emitter.exists() && form.loop.get())
        {
            this.stopEmitter();
        }

        if (this.emitter == null)
        {
            EffectDefinition definition = BBSEffectLoader.getOrLoad(id);

            if (definition == null)
            {
                return null;
            }

            this.emitter = definition.play(target, this.name);
        }

        if (this.emitter == null || !this.emitter.exists())
        {
            return null;
        }

        boolean paused = form.paused.get() || filmPaused || (gamePaused && !preview);
        if (paused != this.emitter.isPaused())
        {
            if (paused)
            {
                this.emitter.pause();
            }
            else
            {
                this.emitter.resume();
            }
        }

        this.emitter.setVisibility(form.visible.get());
        float speed = form.speed.get();
        float progress = this.state.advance(System.nanoTime(), speed, paused);

        // AAA's native speed override also advances paused emitters. Keep its speed at
        // one and apply our clock after AAA's manager update via a pre-draw callback.
        this.manualProgress = Math.abs(speed - 1F) > 0.001F || (gamePaused && preview);
        this.progress = progress;
        return this.emitter;
    }

    private boolean manualProgress;
    private float progress;
    private ParticleEmitter callbackEmitter;

    public void prepareDraw()
    {
        if (this.emitter != null && this.callbackEmitter != this.emitter)
        {
            this.callbackEmitter = this.emitter;
            this.emitter.addPreDrawCallback((emitter, partial) ->
            {
                if (this.manualProgress)
                {
                    emitter.setProgress(this.progress);
                }
            });
        }
    }

    private void stopEmitter()
    {
        if (this.emitter != null)
        {
            this.emitter.setVisibility(false);
            this.emitter.stop();
            this.emitter = null;
        }

        this.callbackEmitter = null;
        this.state.resetProgress();
        this.manualProgress = false;
    }

    public void stop()
    {
        this.stopEmitter();
        this.state.reset();
        this.effectId = null;
        this.type = null;
    }
}
