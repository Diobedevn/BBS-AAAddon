package diobede.bbsaaa.particles;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlaybackStateTest
{
    @Test
    void continuousTicksAndRepeatedRenderSamplesDoNotRestart()
    {
        PlaybackState state = new PlaybackState();
        state.observeTick(10, false);
        state.observeTick(10, false);
        state.observeTick(11, false);
        assertFalse(state.consumeRestart());
    }

    @Test
    void backwardAndForwardSeeksRestartOnce()
    {
        PlaybackState state = new PlaybackState();
        state.observeTick(10, false);
        state.observeTick(5, false);
        assertTrue(state.consumeRestart());
        assertFalse(state.consumeRestart());
        state.observeTick(50, false);
        assertTrue(state.consumeRestart());
    }

    @Test
    void heldRestartDoesNotRearmWhenEmitterProgressResets()
    {
        PlaybackState state = new PlaybackState();
        state.observeTick(0, true);
        assertTrue(state.consumeRestart());
        state.resetProgress();
        state.observeTick(1, true);
        assertFalse(state.consumeRestart());
        state.observeTick(2, false);
        state.observeTick(3, true);
        assertTrue(state.consumeRestart());
    }

    @Test
    void pauseAndResumeDoNotCountTimeSpentPaused()
    {
        PlaybackState state = new PlaybackState();
        state.advance(1_000_000_000L, 1F, false);
        assertEquals(60F, state.advance(2_000_000_000L, 1F, false), 0.001F);
        assertEquals(60F, state.advance(3_000_000_000L, 1F, true), 0.001F);
        assertEquals(60F, state.advance(90_000_000_000L, 1F, true), 0.001F);
        assertEquals(60F, state.advance(91_000_000_000L, 1F, false), 0.001F);
        assertEquals(120F, state.advance(92_000_000_000L, 1F, false), 0.001F);
    }

    @Test
    void speedChangesPreserveAccumulatedProgress()
    {
        PlaybackState state = new PlaybackState();
        state.advance(1_000_000_000L, 1F, false);
        assertEquals(60F, state.advance(2_000_000_000L, 1F, false), 0.001F);
        assertEquals(180F, state.advance(3_000_000_000L, 2F, false), 0.001F);
        assertEquals(210F, state.advance(4_000_000_000L, 0.5F, false), 0.001F);
    }

    @Test
    void slowFramesCatchUpWithoutArtificiallyRestarting()
    {
        PlaybackState state = new PlaybackState();
        state.advance(1_000_000_000L, 1F, false);
        assertEquals(30F, state.advance(1_500_000_000L, 1F, false), 0.001F);
        assertFalse(state.consumeRestart());
    }

    @Test
    void fullCleanupRearmsRestartAndResetsTheClock()
    {
        PlaybackState state = new PlaybackState();
        state.observeTick(10, true);
        state.advance(1_000_000_000L, 1F, false);
        state.advance(2_000_000_000L, 1F, false);
        state.reset();
        assertFalse(state.consumeRestart());
        assertEquals(0F, state.advance(100_000_000_000L, 1F, false));
        state.observeTick(0, true);
        assertTrue(state.consumeRestart());
    }
}
