package com.focused.controll;

import com.focused.core.Session;
import com.focused.platform.WindowManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.util.Duration;

/**
 * SessionController — wires the Session model to the WindowManager platform layer.
 *
 * ── Responsibilities ──────────────────────────────────────────────────────────
 *
 * 1. Own the JavaFX Timeline (the once-per-second heartbeat).
 * 2. On each tick: delegate enforcement to a background thread so the
 *    FX thread is never blocked by Win32 calls.
 * 3. Notify the UI via callbacks when state changes (started, ticked, done).
 *
 * ── Threading model ───────────────────────────────────────────────────────────
 *
 * JavaFX Application Thread  →  Timeline fires tick()
 *                            →  submits Task to background thread
 * Background Thread          →  calls WindowManager.enforceLock() (Win32)
 * ── Why callbacks instead of direct View references? ──────────────────────────
 *
 * The controller does not import anything from com.focused.ui.
 * Instead it exposes Runnable callbacks that the View sets before starting
 * a session. This keeps the dependency arrow one-way:
 *
 *   View → Controller → Model
 *   View → Controller → Platform
 *
 * The controller never knows what the View looks like. This makes it
 * testable and replaceable. And allow my app to not have a circular depency chain
 */
public class SessionController {
    private final WindowManager windowManager; // Dependency Injection
    private Session currentSession;
    private Timeline heartbeat;

    // Callbacks (set by the View before starting)
    private java.util.function.Consumer<Session> onTick; // call javaFx Thread every thread
    private Runnable onDone; // call javaFx Thread when session finish

    public SessionController(WindowManager windowManager) {
        this.windowManager = windowManager;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Register the tick callback. Called every second with the live session.
     * Must be called before start().
     */
    public void setOnTick(java.util.function.Consumer<Session> onTick) {
        this.onTick = onTick;
    }

    /**
     * Register the completion callback. Called once when the session ends.
     * Must be called before start().
     */
    public void setOnDone(Runnable onDone) {
        this.onDone = onDone;
    }

    /**
     * Start a new focus session.
     *
     * @param targetWindow full window title (display only)
     * @param keyword      search term used for matching
     * @param seconds      session duration
     *
     * Precondition: no session is currently active.
     * The caller (View) is responsible for disabling the start button
     * while a session is active so this is never called twice.
     */
    public void start(String targetWindow, String keyword, int seconds) {
        if (currentSession != null && currentSession.isActive()) {
            throw new IllegalStateException(
                    "Cannot start a session while one is already active");
        }

        currentSession = new Session(targetWindow, keyword, seconds);
        currentSession.start();

        // Enforce immediately — don't wait one second for the first tick
        submitEnforcementTask();

        // Timeline fires every 1 second on the FX thread.
        // KeyFrame is JavaFX's way of saying "do this at time T".
        // Duration.seconds(1) with INDEFINITE cycle = repeating timer.
        heartbeat = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> tick())
        );
        heartbeat.setCycleCount(Timeline.INDEFINITE);
        heartbeat.play();
    }

    /**
     * Force-stop the current session early.
     * Safe to call even if no session is active.
     */
    public void stop() {
        if (currentSession == null) return;
        currentSession.stop();
        finish();
    }

    /**
     * Returns the current session, or null if none is active.
     * The View uses this to read initial state when rendering.
     */
    public Session getCurrentSession() {
        return currentSession;
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /**
     * Called every second by the Timeline on the FX thread.
     *
     * Design note: tick() does the minimum on the FX thread —
     * it advances the model clock and submits OS work to a background thread.
     * It does NOT call WindowManager directly.
     */
    private void tick() {
        boolean done = currentSession.tick();

        // Notify the View to update the countdown/progress ring.
        // We're already on the FX thread (Timeline fires here), so no
        // Platform.runLater() needed.
        if (onTick != null) {
            onTick.accept(currentSession);
        }

        if (done) {
            finish();
        } else {
            // Offload Win32 work to background thread
            submitEnforcementTask();
        }
    }

    /**
     * Submits the enforcement call to a background thread.
     *
     * Why Task and not a raw Thread?
     *
     * javafx.concurrent.Task is JavaFX's unit of background work.
     * It integrates with Platform.runLater() cleanly and handles
     * exceptions in a structured way.
     *
     * Reference: https://docs.oracle.com/javase/8/javafx/interoperability-tutorial/concurrency.htm
     *
     * Why not ScheduledExecutorService?
     * We already have a Timeline driving the heartbeat. Using a second
     * scheduling mechanism would create two clocks that can drift apart.
     * The Timeline fires the tick; the tick submits one Task. Simple.
     *
     * Trade-off: Task uses a daemon thread from a shared pool. If the
     * Win32 call somehow blocks longer than 1 second, a second Task
     * could launch before the first finishes. For Win32 ShowWindow()
     * calls this will never happen — they are synchronous and take
     * microseconds. If we ever add disk I/O here, we'd want a guard.
     */
    private void submitEnforcementTask() {
        // Capture session reference — lambdas must reference effectively final vars
        Session session = currentSession;

        Task<Void> enforcementTask = new Task<>() {
            @Override
            protected Void call() {
                // This runs on a background thread — safe to do OS work here
                windowManager.enforceLock(session);
                return null;
            }
        };

        // If enforcement throws (e.g. JNA error), log it — don't crash the app
        enforcementTask.setOnFailed(e ->
                System.err.println("[SessionController] Enforcement failed: "
                        + enforcementTask.getException().getMessage())
        );

        // Start the task on a daemon thread.
        // Daemon threads don't prevent JVM shutdown — correct for background work.
        Thread thread = new Thread(enforcementTask);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Tears down the heartbeat and releases all windows.
     * Called on the FX thread — either from tick() or stop().
     */
    private void finish() {
        if (heartbeat != null) {
            heartbeat.stop();
            heartbeat = null;
        }

        // Release windows on a background thread — same reasoning as enforcement
        Session session = currentSession;
        Task<Void> releaseTask = new Task<>() {
            @Override
            protected Void call() {
                windowManager.releaseAll();
                return null;
            }
        };

        // Notify View after release is complete — must post back to FX thread
        releaseTask.setOnSucceeded(e -> {
            if (onDone != null) onDone.run();
        });

        releaseTask.setOnFailed(e ->
                System.err.println("[SessionController] Release failed: "
                        + releaseTask.getException().getMessage())
        );

        Thread thread = new Thread(releaseTask);
        thread.setDaemon(true);
        thread.start();
    }
}

