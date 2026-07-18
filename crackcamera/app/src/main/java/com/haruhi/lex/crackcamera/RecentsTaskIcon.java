package com.haruhi.lex.crackcamera;

import android.app.Activity;
import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.core.content.ContextCompat;

/**
 * Best-effort Recents/overview icon. OneUI often still uses the adaptive package
 * icon in overview, so launcher art padding is the reliable fix; this covers
 * OEMs that honor {@link ActivityManager.TaskDescription}.
 */
final class RecentsTaskIcon {
    private static final int BACKGROUND_COLOR = 0xFF252526;

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
        activity.setTaskDescription(new ActivityManager.TaskDescription(label, icon, BACKGROUND_COLOR));
    }

    private static Bitmap createIcon(Activity activity) {
        Drawable art = ContextCompat.getDrawable(activity, R.drawable.mnfake_launcher_icon_art);
        if (art == null) {
            return null;
        }
        ActivityManager am = (ActivityManager) activity.getSystemService(Activity.ACTIVITY_SERVICE);
        int sizePx = am != null ? am.getLauncherLargeIconSize() : 0;
        if (sizePx <= 0) {
            sizePx = Math.round(72f * activity.getResources().getDisplayMetrics().density);
        }
        if (sizePx < 48) {
            sizePx = 48;
        }
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(BACKGROUND_COLOR);
        // Full-bleed: art already has safe-zone padding baked in.
        art.setBounds(0, 0, sizePx, sizePx);
        art.draw(canvas);
        return bitmap;
    }
}
