package com.haruhi.lex.crackcamera;

import android.app.AlertDialog;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import static com.haruhi.lex.crackcamera.Notification.sendNotification;

/**
 * Bottom-sheet dialogs shared with {@link MainActivity} (uninstall, yellow→red camera confirm).
 * Both use {@link AlertDialogWindows#styleBottomSheet(android.view.Window)} for consistent layout.
 */
final class MainStyledDialogs {
    /**
     * After styled confirm (확인) dismisses, wait before {@link MainActivity#onSwitchCamera(View)}
     * so the dismiss animation finishes first.
     */
    private static final long CAMERA_CONFIRM_POST_DISMISS_MS = 1000L;

    private MainStyledDialogs() {
    }

    /**
     * Same presentation as NFC-off sheet; 확인 launches system uninstall
     * ({@link android.content.Intent#ACTION_DELETE} with {@code package:} URI).
     */
    static void showUninstallConfirm(final MainActivity activity) {
        View content = activity.getLayoutInflater().inflate(R.layout.dialog_uninstall_confirm, null);
        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(content)
                .create();

        AlertDialogWindows.styleBottomSheet(dialog.getWindow());

        Button btnCancel = content.findViewById(R.id.btnUninstallCancel);
        Button btnOk = content.findViewById(R.id.btnUninstallOk);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }
        if (btnOk != null) {
            btnOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    activity.startSystemUninstallForThisApp();
                }
            });
        }

        dialog.show();
    }

    /**
     * Yellow state: bottom sheet titled “카메라 차단”. After 확인, posts
     * {@code policy_camera_deny_noti_comment} then {@link MainActivity#onSwitchCamera(View)} (→ red).
     */
    static void showCameraToggleStyledConfirm(final MainActivity activity, final View triggerView,
            final Handler mainHandler) {
        View content = activity.getLayoutInflater().inflate(R.layout.dialog_camera_toggle_confirm, null);
        TextView tvTitle = content.findViewById(R.id.tvConfirmTitle);
        TextView tvMessage = content.findViewById(R.id.tvConfirmMessage);
        if (tvTitle != null) {
            tvTitle.setText(R.string.mnfake_dialog_camera_block_title);
        }
        if (tvMessage != null) {
            tvMessage.setText(R.string.mnfake_dialog_camera_block_message);
        }

        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(content)
                .create();

        AlertDialogWindows.styleBottomSheet(dialog.getWindow());

        View btnCancel = content.findViewById(R.id.btnConfirmCancel);
        View btnOk = content.findViewById(R.id.btnConfirmOk);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }
        if (btnOk != null) {
            btnOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    mainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            activity.onSwitchCamera(triggerView);
                            sendNotification(activity,
                                    activity.getString(R.string.policy_camera_deny_noti_comment));
                        }
                    }, CAMERA_CONFIRM_POST_DISMISS_MS);
                }
            });
        }

        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }
}
