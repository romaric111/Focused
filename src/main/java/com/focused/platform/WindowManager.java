package com.focused.platform;

import com.focused.core.Session;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import java.awt.*;
import java.util.*;
import java.util.List;

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

            // Gets the show state of a window (minimized, maximized, normal)
             // WINDOWPLACEMENT is a structure — we only need the showCmd field
          // which sits at byte offset 8 (two ints before it: length and flags)
        boolean GetWindowPlacement(HWND hwnd, int[] lpwndpl);

        int SW_SHOWMINIMIZED = 2;
        int SW_SHOWNORMAL    = 1;
        int SW_SHOWNOACTIVATE = 4;

       int SW_MINIMIZE      = 6;
       int SW_RESTORE       = 9;
       int GWWL_EXSTYLE     = -20;
       int WS_EX_TOOLWINDOW = 0x00000080;
    }


     // We take the windows info
    //A snapshot of a visible window.
    public record WindowInfo(String title, HWND hwnd) {
        @Override public String toString() { return title; }
    }

    // EXCLUDE MY OWN PROCESS SO  THE APP DON'T BLOCK ITSELF
    private static final Set<Integer> OWN_PIDS = buildOwnPids();

    private static Set<Integer>buildOwnPids(){
        Set<Integer>pids=new HashSet<>();
        pids.add((int) ProcessHandle.current().pid());
        ProcessHandle.current()
                .parent()
                .ifPresent(p->pids.add((int) p.pid()));
        return Collections.unmodifiableSet(pids);
    }

    /**
     * Returns all visible, user-facing windows currently open.
     * Excludes tool windows, our own process, and the console that launched them.
     */
    public List<WindowInfo> getVisibleWindows() {
        List<WindowInfo> result = new ArrayList<>();
        User32.INSTANCE.EnumWindows((hwnd, data) ->{
            if (!User32.INSTANCE.IsWindowVisible(hwnd)) return true;

            int len = User32.INSTANCE.GetWindowTextLengthW(hwnd);
            if(len==0) return true;

            char[] buf = new char[len +1];
            User32.INSTANCE.GetWindowTextW(hwnd, buf,buf.length);
            String title = new String(buf).trim();
            if (title.isEmpty()) return true;

            int exStyle = User32.INSTANCE.GetWindowLongW(hwnd, User32.GWWL_EXSTYLE);
            if ((exStyle & User32.WS_EX_TOOLWINDOW) !=0) return true;

            IntByReference pidRef = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, pidRef);
            if (OWN_PIDS.contains(pidRef.getValue())) return true;

            result.add(new WindowInfo(title, hwnd));
            return true;
        }, null);
    return  Collections.unmodifiableList(result);

    }

    //Minimise every screen outside of the screen chosen
    public void enforceLock(Session session) {
        for (WindowInfo w: getVisibleWindows()){
            if(session.matches(w.title())){
                // Only intervene if the window got minimized
                // If it's normal or maximized, leave it exactly as the user left it
                if (isMinimized(w.hwnd())) {
                    User32.INSTANCE.ShowWindow(w.hwnd(), User32.SW_RESTORE);
                    User32.INSTANCE.SetForegroundWindow(w.hwnd());
                }
            }else{
                User32.INSTANCE.ShowWindow(w.hwnd(), User32.SW_MINIMIZE);
            }
        }

    }

   /**
    * Returns true if the window is currently in a minimized state.
    * GetWindowPlacement fills a WINDOWPLACEMENT structure.
    * We pass it as a raw int array — the structure layout is:
    *   [0] length (must be set to structure size before calling)
    *   [1] flags
    *   [2] showCmd  ← this is what we need
    *   [3] ptMinPosition.x
    *   [4] ptMinPosition.y
    *   [5] ptMaxPosition.x
    *   [6] ptMaxPosition.y
    *   [7..10] rcNormalPosition (RECT — 4 ints)
    * Win32 WINDOWPLACEMENT reference:
    * https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-windowplacement
    */
    private boolean isMinimized(HWND hwnd) {
        int[] placement = new int[11];
        placement[0] = placement.length * 4; // set length field (bytes)
        User32.INSTANCE.GetWindowPlacement(hwnd, placement);
        return placement[2] == User32.SW_SHOWMINIMIZED;
    }

    //At the done state all windows are released
    public void releaseAll() {
        for(WindowInfo w:getVisibleWindows()){
            User32.INSTANCE.ShowWindow(w.hwnd(), User32.SW_RESTORE);
        }
    }
}
