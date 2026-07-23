package com.crest.client.ui;

import java.util.ArrayList;
import java.util.List;

public class FocusManager {
    private static final List<Focusable> focusables = new ArrayList<>();
    private static Focusable focused;
    private static int focusIndex = -1;

    public interface Focusable {
        void onFocusGained();
        void onFocusLost();
        boolean onKeyPressed(int key, int scan, int mods);
        boolean acceptsFocus();
        int getFocusPriority();
    }

    public static void register(Focusable f) {
        if (!focusables.contains(f)) {
            focusables.add(f);
            focusables.sort((a, b) -> Integer.compare(b.getFocusPriority(), a.getFocusPriority()));
        }
    }

    public static void unregister(Focusable f) {
        focusables.remove(f);
        if (focused == f) {
            focused = null;
            focusIndex = -1;
        }
    }

    public static void clear() {
        focusables.clear();
        focused = null;
        focusIndex = -1;
    }

    public static Focusable getFocused() {
        return focused;
    }

    public static boolean focusNext() {
        if (focusables.isEmpty()) return false;
        int start = focusIndex;
        int idx = focusIndex;
        do {
            idx = (idx + 1) % focusables.size();
            if (focusables.get(idx).acceptsFocus()) {
                setFocus(idx);
                return true;
            }
        } while (idx != start);
        return false;
    }

    public static boolean focusPrevious() {
        if (focusables.isEmpty()) return false;
        int start = focusIndex;
        int idx = focusIndex;
        do {
            idx = (idx - 1 + focusables.size()) % focusables.size();
            if (focusables.get(idx).acceptsFocus()) {
                setFocus(idx);
                return true;
            }
        } while (idx != start);
        return false;
    }

    public static void focusFirst() {
        for (int i = 0; i < focusables.size(); i++) {
            if (focusables.get(i).acceptsFocus()) {
                setFocus(i);
                return;
            }
        }
    }

    private static void setFocus(int idx) {
        if (focused != null) focused.onFocusLost();
        focusIndex = idx;
        focused = focusables.get(idx);
        focused.onFocusGained();
    }

    public static boolean handleKeyPress(int key, int scan, int mods) {
        if (focused != null && focused.onKeyPressed(key, scan, mods)) return true;
        if (key == 258) {
            if ((mods & 1) != 0) return focusPrevious();
            return focusNext();
        }
        return false;
    }

    public static void clearFocus() {
        if (focused != null) {
            focused.onFocusLost();
            focused = null;
            focusIndex = -1;
        }
    }
}
