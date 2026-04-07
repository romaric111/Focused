package com.focused.core;

/**
 * Represents a single focus session.
 *
 * This is the heart of the app — pure logic, zero dependencies on
 * JavaFX, JNA, or Windows. It can be tested without running the app.
 *
 * A Session knows:
 *   - which window the user wants to focus on
 *   - how long the session lasts
 *   - what state it's in (IDLE, ACTIVE, DONE)
 *
 * A Session does NOT know:
 *   - how to minimize windows (that's WindowManager)
 *   - how to draw anything (that's MainView)
 *   - how to save anything (that's AppConfig)
 *
 * Adding a feature? Ask: does this belong in the domain (here),
 * the OS layer (platform/), the UI (ui/), or persistence (config/)?
 */
public class Session {

    // ── State ─────────────────────────────────────────────────────────────────

    public enum State {
        IDLE,    // not started
        ACTIVE,  // timer running, window locked
        DONE     // timer finished naturally
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private final String targetWindow;   // title of the window to lock to
    private final int    durationSeconds;// total duration of this session
    private State        state;
    private int          secondsLeft;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * @param targetWindow   the window title to lock the user to
     * @param durationSeconds how long the session should run
     */
    public Session(String targetWindow, int durationSeconds) {
        if (targetWindow == null || targetWindow.isBlank()) {
            throw new IllegalArgumentException("targetWindow cannot be blank");
        }
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("durationSeconds must be positive");
        }
        this.targetWindow    = targetWindow;
        this.durationSeconds = durationSeconds;
        this.secondsLeft     = durationSeconds;
        this.state           = State.IDLE;
    }

    // ── State transitions ─────────────────────────────────────────────────────

    /** Move from IDLE → ACTIVE. Throws if already active. */
    public void start() {
        if (state != State.IDLE) {
            throw new IllegalStateException("Session already started");
        }
        state = State.ACTIVE;
    }

    /**
     * Called once per second by the timer in the upper layer.
     * Returns true if the session just finished naturally.
     */
    public boolean tick() {
        if (state != State.ACTIVE) return false;
        secondsLeft = Math.max(0, secondsLeft - 1);
        if (secondsLeft == 0) {
            state = State.DONE;
            return true; // signal: session complete
        }
        return false;
    }

    /** Force-stop the session before the timer runs out. */
    public void stop() {
        if (state == State.ACTIVE) {
            state = State.DONE;
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getTargetWindow()    { return targetWindow; }
    public int    getDurationSeconds() { return durationSeconds; }
    public int    getSecondsLeft()     { return secondsLeft; }
    public State  getState()           { return state; }
    public boolean isActive()          { return state == State.ACTIVE; }

    /** Progress from 0.0 (just started) to 1.0 (done). Useful for the UI progress ring. */
    public double getProgress() {
        if (durationSeconds == 0) return 0;
        return 1.0 - ((double) secondsLeft / durationSeconds);
    }
}
