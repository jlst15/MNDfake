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
 * Red-state beacon sheet: timed phase transition, then waits for an NFC tag before
 * {@link MainActivity#onSwitchCamera(View)} (→ yellow). Cancel dismisses without toggle.
 * On NFC unlock, posts {@code policy_camera_allow_noti_comment} as a notification.
 * NFC is handled only while this sheet is open ({@link BeaconNfcUnlock}); the activity is
 * not registered for system {@code TECH_DISCOVERED}, so idle tags do not relaunch it.
 * <p>
 * Window styling: {@link AlertDialogWindows#styleCenterWide(android.view.Window)}. Cancel clears
 * pending handler callbacks so timers do not fire after dismiss.
 */
final class BeaconRecognitionDialog {
    /** Delay after {@code show()} before switching from “setting” to “scanning” visuals. */
    static final long PHASE1_MS = 2800L;
    /** Auto-unlock delay when {@link MndfakePrefs#isBeaconNfcRequired} is {@code false}. */
    static final long AUTO_TOGGLE_MS = 5000L;

    private BeaconRecognitionDialog() {
    }

    static boolean dispatchNfcIntent(android.content.Intent intent) {
        return BeaconNfcUnlock.dispatchIntent(intent);
    }

    /**
     * @param triggerView passed to {@link MainActivity#onSwitchCamera(View)} when an NFC tag is read
     */
    static void show(final MainActivity activity, final View triggerView, final Handler mainHandler) {
        final boolean nfcRequired = MndfakePrefs.isBeaconNfcRequired(activity);
        final Runnable[] pendingRunnables = new Runnable[2];
        final BeaconNfcUnlock[] nfcUnlock = new BeaconNfcUnlock[1];
        final boolean[] unlocked = {false};

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

        final Runnable completeUnlock = new Runnable() {
            @Override
            public void run() {
                if (unlocked[0] || !dialog.isShowing()) {
                    return;
                }
                unlocked[0] = true;
                clearPending(mainHandler, pendingRunnables);
                if (nfcUnlock[0] != null) {
                    nfcUnlock[0].stop();
                    nfcUnlock[0] = null;
                }
                activity.onSwitchCamera(triggerView);
                sendNotification(activity,
                        activity.getString(R.string.policy_camera_allow_noti_comment));
                dialog.dismiss();
            }
        };

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface d) {
                clearPending(mainHandler, pendingRunnables);
                if (nfcUnlock[0] != null) {
                    nfcUnlock[0].stop();
                    nfcUnlock[0] = null;
                }
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
                mainHandler.postDelayed(pendingRunnables[0], PHASE1_MS);

                if (nfcRequired) {
                    nfcUnlock[0] = new BeaconNfcUnlock(activity, mainHandler, completeUnlock);
                    nfcUnlock[0].start();
                } else {
                    pendingRunnables[1] = completeUnlock;
                    mainHandler.postDelayed(pendingRunnables[1], AUTO_TOGGLE_MS);
                }
            }
        });

        Button btnCancel = content.findViewById(R.id.btnBeaconCancel);
        if (btnCancel != null) {
            btnCancel.setText(R.string.mnfake_dialog_cancel);
            btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clearPending(mainHandler, pendingRunnables);
                    if (nfcUnlock[0] != null) {
                        nfcUnlock[0].stop();
                        nfcUnlock[0] = null;
                    }
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
