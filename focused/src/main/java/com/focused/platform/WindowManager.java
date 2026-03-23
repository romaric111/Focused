package com.focused.platform;

import com.focused.core.Session;

import java.util.List;

/**
 * WindowManager — the only class allowed to talk to the Windows OS.
 *
 * All JNA / Win32 API code lives here and ONLY here.
 * The rest of the app calls this class; it never calls user32.dll directly.
 *
 * Why this matters:
 *   If we ever support macOS or Linux, we swap this one class.
 *   Nothing else changes.
 *
 * Session 2 will implement these methods.
 * For now they are stubs so the project compiles cleanly.
 */
public class WindowManager {

    // ── Window info ───────────────────────────────────────────────────────────

    /**
     * A lightweight snapshot of a visible window.
     * Immutable — we never mutate window state through this object.
     */
    public record WindowInfo(String title, long hwnd) {
        @Override public String toString() { return title; }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns all visible, user-facing windows currently open.
     * Excludes tool windows, our own process, and the console that launched us.
     *
     * TODO: implement in Session 2.
     */
    public List<WindowInfo> getVisibleWindows() {
        throw new UnsupportedOperationException("Implement in Session 2");
    }

    /**
     * Enforces the lock for the given session:
     *   - minimizes every window whose title does NOT match session.getTargetWindow()
     *   - brings the target window to the front
     *
     * Called once per second while a session is ACTIVE.
     *
     * TODO: implement in Session 2.
     */
    public void enforceLock(Session session) {
        throw new UnsupportedOperationException("Implement in Session 2");
    }

    /**
     * Restores all windows to their normal state.
     * Called when a session ends (naturally or force-stopped).
     *
     * TODO: implement in Session 2.
     */
    public void releaseAll() {
        throw new UnsupportedOperationException("Implement in Session 2");
    }
}
