package com.haruhi.lex.crackcamera;

import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

/**
 * Shared {@link android.app.AlertDialog} window styling for transparent backgrounds and consistent
 * width/gravity. Used by NFC and uninstall sheets ({@link #styleBottomSheet}) and the beacon dialog
 * ({@link #styleCenterWide}).
 */
final class AlertDialogWindows {
    private AlertDialogWindows() {
    }

    /** Bottom sheet: transparent, full width, anchored bottom. */
    static void styleBottomSheet(Window window) {
        if (window == null) {
            return;
        }
        window.setBackgroundDrawableResource(android.R.color.transparent);
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);
        window.setGravity(Gravity.BOTTOM);
    }

    /**
     * Center card, full width (e.g. beacon dialog). Apply before {@code show()} so the first
     * layout is correct.
     */
    static void styleCenterWide(Window window) {
        if (window == null) {
            return;
        }
        window.setBackgroundDrawableResource(android.R.color.transparent);
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.CENTER;
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);
    }
}
