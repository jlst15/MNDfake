package com.haruhi.lex.crackcamera;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;

import static com.haruhi.lex.crackcamera.MainActivity.CHANNEL_ID;
import static com.haruhi.lex.crackcamera.MainActivity.PREF_NAME;
import static com.haruhi.lex.crackcamera.MainActivity.notificationId;

public class Notification extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        System.out.println("신호 청취"+intent.getAction());
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){

            System.out.println("BOOT 신호 청취");
            sendPrivate(context);
            System.out.println("알림 전송");
        }
    }
    public void sendPrivate(Context context) {
        sendNotification(context);
    }

    /**
     * Posts {@code policy_camera_deny_noti_comment} only while persisted {@code status}
     * is {@code true} (blocked / yellow; same key as {@link MainActivity#saveSetting}).
     * Used for boot, delete toolbar, and cold start when the app was left in blocked state.
     */
    public static void sendNotification(Context context) {
        if (!isPersistedCameraBlocked(context)) {
            return;
        }
        sendNotification(context, context.getString(R.string.policy_camera_deny_noti_comment));
    }

    private static boolean isPersistedCameraBlocked(Context context) {
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean("status", false);
    }
    /**
     * Posts to {@link #CHANNEL_ID} / {@link MainActivity#notificationId} with custom body text.
     */
    public static void sendNotification(Context context, CharSequence contentText) {
        Intent intent = new Intent(context, Notification.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        int pendingIntentFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? PendingIntent.FLAG_IMMUTABLE
                : 0;
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags);
        postChannelAndNotify(context, pendingIntent, contentText);
    }

    private static void postChannelAndNotify(Context context, PendingIntent pendingIntent, CharSequence contentText) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .setColor(Color.parseColor("#ff009688"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.app_name);
            String description = context.getString(R.string.app_name);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }
}
