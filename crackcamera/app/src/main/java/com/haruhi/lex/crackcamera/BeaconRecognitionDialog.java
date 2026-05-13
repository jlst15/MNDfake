package com.haruhi.lex.crackcamera;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import static com.haruhi.lex.crackcamera.Notification.sendNotification;

/**
 * Red-state beacon sheet: timed phase transition then {@link MainActivity#onSwitchCamera(View)}
 * (→ yellow), or cancel without toggle. On auto-complete, posts
 * {@code policy_camera_allow_noti_comment} as a notification.
 * <p>
 * Window styling: {@link AlertDialogWindows#styleCenterWide(android.view.Window)}. Cancel clears
 * pending handler callbacks so timers do not fire after dismiss.
 */
final class BeaconRecognitionDialog {
    /** Delay after {@code show()} before switching from “setting” to “scanning” visuals. */
    static final long PHASE1_MS = 2800L;
    /** Delay after {@code show()} before invoking {@link MainActivity#onSwitchCamera(View)} and closing. */
    static final long AUTO_TOGGLE_MS = 5000L;

    private BeaconRecognitionDialog() {
    }

    /**
     * @param triggerView passed to {@link MainActivity#onSwitchCamera(View)} when the auto timer fires
     */
    static void show(final MainActivity activity, final View triggerView, final Handler mainHandler) {
        final Runnable[] pendingRunnables = new Runnable[2];

        View content = activity.getLayoutInflater().inflate(R.layout.dialog_beacon, null);
        final ProgressBar pbSetting = content.findViewById(R.id.pbBeaconSetting);
        final ProgressBar pbScan = content.findViewById(R.id.pbBeaconScan);
        final TextView tvSetting = content.findViewById(R.id.tvBeaconSetting);
        final TextView tvScanning = content.findViewById(R.id.tvBeaconScanning);
        if (pbScan != null) {
            pbScan.setVisibility(View.INVISIBLE);
        }

        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(content)
                .create();

        AlertDialogWindows.styleCenterWide(dialog.getWindow());

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface d) {
                clearPending(mainHandler, pendingRunnables);
            }
        });
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                pendingRunnables[0] = new Runnable() {
                    @Override
                    public void run() {
                        if (!dialog.isShowing()) {
                            return;
                        }
                        if (pbSetting != null) {
                            pbSetting.setVisibility(View.INVISIBLE);
                        }
                        if (pbScan != null) {
                            pbScan.setVisibility(View.VISIBLE);
                        }
                        if (tvSetting != null) {
                            tvSetting.setTextColor(ContextCompat.getColor(activity,
                                    R.color.common_mndmdm_text_sub));
                        }
                        if (tvScanning != null) {
                            tvScanning.setTextColor(ContextCompat.getColor(activity,
                                    R.color.common_txt_black));
                        }
                    }
                };
                pendingRunnables[1] = new Runnable() {
                    @Override
                    public void run() {
                        if (!dialog.isShowing()) {
                            return;
                        }
                        activity.onSwitchCamera(triggerView);
                        sendNotification(activity,
                                activity.getString(R.string.policy_camera_allow_noti_comment));
                        dialog.dismiss();
                    }
                };
                mainHandler.postDelayed(pendingRunnables[0], PHASE1_MS);
                mainHandler.postDelayed(pendingRunnables[1], AUTO_TOGGLE_MS);
            }
        });

        Button btnCancel = content.findViewById(R.id.btnBeaconCancel);
        if (btnCancel != null) {
            btnCancel.setText(R.string.mnfake_dialog_cancel);
            btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clearPending(mainHandler, pendingRunnables);
                    dialog.dismiss();
                }
            });
        }

        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private static void clearPending(Handler mainHandler, Runnable[] pendingRunnables) {
        for (int i = 0; i < pendingRunnables.length; i++) {
            if (pendingRunnables[i] != null) {
                mainHandler.removeCallbacks(pendingRunnables[i]);
                pendingRunnables[i] = null;
            }
        }
    }
}
