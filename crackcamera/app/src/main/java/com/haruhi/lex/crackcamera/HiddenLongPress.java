package com.haruhi.lex.crackcamera;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * Long-press helper with a longer hold than the system default (~500ms), so hidden
 * gestures are less likely to be discovered by casual use.
 * <p>
 * Touch events are forwarded to {@link View#onTouchEvent(MotionEvent)} so pressed/ripple
 * feedback still works; only the long-press timeout is customized.
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
        view.setClickable(true);
        // Disable the framework long-click timeout; only our delayed handler should fire.
        view.setLongClickable(false);
        view.setOnLongClickListener(null);
        view.setOnTouchListener(new TouchListener(view, listener));
    }

    private static final class TouchListener implements View.OnTouchListener, Runnable {
        private final View target;
        private final View.OnLongClickListener longClickListener;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final int touchSlop;

        private boolean callingThrough;
        private boolean longPressFired;
        private boolean fingerDown;
        private float downX;
        private float downY;

        TouchListener(View target, View.OnLongClickListener longClickListener) {
            this.target = target;
            this.longClickListener = longClickListener;
            this.touchSlop = ViewConfiguration.get(target.getContext()).getScaledTouchSlop();
        }

        @Override
        public void run() {
            if (!fingerDown || longPressFired) {
                return;
            }
            longPressFired = true;
            fingerDown = false;
            longClickListener.onLongClick(target);
            // Clear pressed feedback without delivering a click.
            dispatchCancel(target);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (callingThrough) {
                return false;
            }

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    longPressFired = false;
                    fingerDown = true;
                    downX = event.getX();
                    downY = event.getY();
                    handler.removeCallbacks(this);
                    handler.postDelayed(this, DURATION_MS);
                    return dispatchToView(v, event);
                case MotionEvent.ACTION_MOVE:
                    if (fingerDown && (Math.abs(event.getX() - downX) > touchSlop
                            || Math.abs(event.getY() - downY) > touchSlop)) {
                        fingerDown = false;
                        handler.removeCallbacks(this);
                    }
                    return dispatchToView(v, event);
                case MotionEvent.ACTION_UP:
                    handler.removeCallbacks(this);
                    fingerDown = false;
                    if (longPressFired) {
                        // Already handled as a long-press; don't fire click.
                        dispatchCancel(v);
                        return true;
                    }
                    return dispatchToView(v, event);
                case MotionEvent.ACTION_CANCEL:
                    handler.removeCallbacks(this);
                    fingerDown = false;
                    return dispatchToView(v, event);
                default:
                    return dispatchToView(v, event);
            }
        }

        private boolean dispatchToView(View v, MotionEvent event) {
            callingThrough = true;
            try {
                return v.onTouchEvent(event);
            } finally {
                callingThrough = false;
            }
        }

        private void dispatchCancel(View v) {
            long now = SystemClock.uptimeMillis();
            MotionEvent cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0, 0, 0);
            try {
                dispatchToView(v, cancel);
            } finally {
                cancel.recycle();
            }
        }
    }
}
