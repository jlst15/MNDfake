package com.haruhi.lex.crackcamera;

import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * Long-press helper with a longer hold than the system default (~500ms), so hidden
 * gestures are less likely to be discovered by casual use.
 */
final class HiddenLongPress {
    /** Hold duration before a hidden long-press action fires. */
    static final int DURATION_MS = 3000;

    private HiddenLongPress() {
    }

    static void attach(View view, View.OnLongClickListener listener) {
        if (view == null || listener == null) {
            return;
        }
        view.setLongClickable(true);
        // Disable the framework long-click timeout; only our delayed handler should fire.
        view.setOnLongClickListener(null);

        final Handler handler = new Handler(Looper.getMainLooper());
        final int touchSlop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();

        view.setOnTouchListener(new View.OnTouchListener() {
            private boolean longPressFired;
            private float downX;
            private float downY;
            private final Runnable fireLongPress = new Runnable() {
                @Override
                public void run() {
                    longPressFired = true;
                    listener.onLongClick(view);
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        longPressFired = false;
                        downX = event.getX();
                        downY = event.getY();
                        handler.removeCallbacks(fireLongPress);
                        handler.postDelayed(fireLongPress, DURATION_MS);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(event.getX() - downX) > touchSlop
                                || Math.abs(event.getY() - downY) > touchSlop) {
                            handler.removeCallbacks(fireLongPress);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(fireLongPress);
                        if (!longPressFired) {
                            v.performClick();
                        }
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        handler.removeCallbacks(fireLongPress);
                        return true;
                    default:
                        return false;
                }
            }
        });
    }
}
