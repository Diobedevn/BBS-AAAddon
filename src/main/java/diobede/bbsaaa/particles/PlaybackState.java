package diobede.bbsaaa.particles;

/** Playback bookkeeping independent of Minecraft and native Effekseer handles. */
public final class PlaybackState
{
    private int lastTick = -1;
    private boolean lastRestart;
    private boolean restartRequested;
    private long lastFrameNanos;
    private float progress;

    public void observeTick(int tick, boolean restart)
    {
        if (this.lastTick >= 0 && (tick < this.lastTick || (long) tick - this.lastTick > 1))
        {
            this.restartRequested = true;
        }

        if (restart && !this.lastRestart)
        {
            this.restartRequested = true;
        }

        this.lastTick = tick;
        this.lastRestart = restart;
    }

    public boolean consumeRestart()
    {
        boolean requested = this.restartRequested;
        this.restartRequested = false;
        return requested;
    }

    /** A repeatable editor action, independent of the saved restart keyframe. */
    public void requestRestart()
    {
        this.restartRequested = true;
    }

    public float advance(long nowNanos, float speed, boolean paused)
    {
        if (this.lastFrameNanos != 0 && !paused)
        {
            long elapsed = Math.max(0, nowNanos - this.lastFrameNanos);
            this.progress += elapsed / 1_000_000_000F * 60F * Math.max(0, speed);
        }

        this.lastFrameNanos = paused ? 0 : nowNanos;
        return this.progress;
    }

    /** Restart simulation time without rearming a held restart keyframe. */
    public void resetProgress()
    {
        this.progress = 0;
        this.lastFrameNanos = 0;
    }

    public void reset()
    {
        this.resetProgress();
        this.lastTick = -1;
        this.lastRestart = false;
        this.restartRequested = false;
    }
}
