package com.haruhi.lex.crackcamera;

import android.app.Activity;
import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.core.content.ContextCompat;

/**
 * Sets a non-adaptive bitmap for Recents/overview so launcher adaptive-icon
 * insets can be tuned without shrinking the task switcher icon.
 */
final class RecentsTaskIcon {
    private static final int BACKGROUND_COLOR = 0xFF252526;
    /** Padding around the emblem inside the Recents bitmap (fraction of edge). */
    private static final float INSET_FRACTION = 0.12f;

    private RecentsTaskIcon() {
    }

    static void apply(Activity activity) {
        if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        Bitmap icon = createIcon(activity);
        if (icon == null) {
            return;
        }
        String label = activity.getString(R.string.app_name);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            activity.setTaskDescription(new ActivityManager.TaskDescription.Builder()
                    .setLabel(label)
                    .setIcon(icon)
                    .setPrimaryColor(BACKGROUND_COLOR)
                    .build());
        } else {
            activity.setTaskDescription(new ActivityManager.TaskDescription(label, icon, BACKGROUND_COLOR));
        }
    }

    private static Bitmap createIcon(Activity activity) {
        Drawable art = ContextCompat.getDrawable(activity, R.drawable.mnfake_launcher_icon_art);
        if (art == null) {
            return null;
        }
        int sizePx = Math.round(72f * activity.getResources().getDisplayMetrics().density);
        if (sizePx < 48) {
            sizePx = 48;
        }
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(BACKGROUND_COLOR);
        int inset = Math.round(sizePx * INSET_FRACTION);
        art.setBounds(inset, inset, sizePx - inset, sizePx - inset);
        art.draw(canvas);
        return bitmap;
    }
}
