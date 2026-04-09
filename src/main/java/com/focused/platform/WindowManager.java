package com.focused.platform;

import com.focused.core.Session;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.ArrayList;

/**
 * WindowManager will interact with windows.
 */
public class WindowManager {
    interface User32 extends StdCallLibrary{
        User32 INSTANCE = Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);

        interface WNDENUMPROC extends StdCallLibrary.StdCallCallback{
            boolean callback(HWND hwnd, Pointer data);
        }
       boolean EnumWindows(WNDENUMPROC lpEnumFunc, Pointer lParam);
       boolean IsWindowVisible(HWND hwnd);
       int GetWindowTextW(HWND hwnd, char[] lpString, int nMaxCount);
       int GetWindowTextLengthW(HWND hwnd);
       boolean ShowWindow(HWND hwnd, int nCmdShow);
       boolean SetForegroundWindow(HWND hwnd);
       int GetWindowLongW(HWND hwnd, int nIndex);
       int GetWindowThreadProcessId(HWND hwnd, IntByReference lpdwProcessId);

       int SW_MINIMIZE      = 6;
       int SW_RESTORE       = 9;
       int GWWL_EXSTYLE     = -20;
       int WS_EX_TOOLWINDOW = 0x00000080;
    }


     // We take the windows info
    //A snapshot of a visible window.
    public record WindowInfo(String title, long hwnd) {
        @Override public String toString() { return title; }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns all visible, user-facing windows currently open.
     * Excludes tool windows, our own process, and the console that launched us.
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
