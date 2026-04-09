package com.focused.core;

/**
 * Manage the user focus session.
 * A Session knows:
 *   - which window the user wants to focus on
 *   - how long the session lasts
 *   - The state is in (IDLE, ACTIVE, DONE)
 */
public class Session {
    public enum State {
        IDLE,
        ACTIVE,
        DONE
    }


    private final String targetWindow;
    private final int    durationSeconds;
    private State        state;
    private int          secondsLeft;
    private final String  keyword;

    /**
     * @param targetWindow   the window title to lock the user to
     * @param durationSeconds how long the session should run
     * @param keyword
     */
    public Session(String targetWindow, String keyword,int durationSeconds) {
        if (targetWindow == null || targetWindow.isBlank()) {
            throw new IllegalArgumentException("targetWindow cannot be blank");
        }
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("durationSeconds must be positive");
        }
        if (keyword==null || keyword.isBlank()){
            throw new IllegalArgumentException("keyword can't be be null or blank");
        }
        this.targetWindow    = targetWindow;
        this.durationSeconds = durationSeconds;
        this.secondsLeft     = durationSeconds;
        this.state           = State.IDLE;
        this.keyword         = keyword.toLowerCase();
    }

    //State transitions
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

    public void stop() {
        if (state == State.ACTIVE) {
            state = State.DONE;
        }
    }
    //Matches method
    public boolean matches(String windowTitle){
        if (windowTitle==null) return false;
        return windowTitle.toLowerCase().contains(keyword);
    }

    //Getters

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
