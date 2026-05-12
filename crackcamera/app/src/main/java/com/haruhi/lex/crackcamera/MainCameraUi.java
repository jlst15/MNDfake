package com.haruhi.lex.crackcamera;

import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.util.Calendar;

/**
 * Camera blocked vs allowed toolbar / header visuals for {@link MainActivity#onSwitchCamera(View)}.
 * Reads {@link MainActivity#suspended} and {@link MainActivity#suspendDate}; writes
 * {@link MainActivity#endSets} when painting the blocked (yellow) branch.
 */
final class MainCameraUi {
    private MainCameraUi() {
    }

    /**
     * Updates views to match current {@link MainActivity#suspended} (yellow blocked vs red allow).
     * Does not flip {@code suspended} or persist — caller handles state after painting.
     */
    static void applyToggleVisuals(MainActivity activity) {
        Button toggleButton = activity.findViewById(R.id.btnCameraDeny);
        RelativeLayout rl1 = activity.findViewById(R.id.rlTitle);
        RelativeLayout rl2 = activity.findViewById(R.id.rlSubTitle);
        LinearLayout ll1 = activity.findViewById(R.id.llCheckInTime);
        LinearLayout ll2 = activity.findViewById(R.id.llDelayTime);

        ImageView sticker = activity.findViewById(R.id.ivCameraSticker);
        ProgressBar prg = activity.findViewById(R.id.pdProgress);
        ImageButton topRightToolbar = activity.findViewById(R.id.btnDeleteApp);

        Button btl = activity.optionalViewById("bt_menu", Button.class);
        Button btr = activity.optionalViewById("bt_del", Button.class);
        float factor = activity.getApplicationContext().getResources().getDisplayMetrics().density;

        // Yellow UI: toolbar icons match originalSource —
        // drawable-xhdpi/img_common_btn_alert.png is 53×49px
        // (~26.5×24.5dp); drawer_base.xml uses wrap_content + 5dp pad (was oversized at
        // 33×33dp square).
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                (int) (26 * factor + 0.5f), (int) (25 * factor + 0.5f));
        LinearLayout.LayoutParams letParams = new LinearLayout.LayoutParams((int) (48 * factor), (int) (35 * factor));
        iconParams.gravity = Gravity.CENTER;
        letParams.gravity = Gravity.CENTER;

        int marginEndPx = (int) (10 * factor + 0.5f);

        if (MainActivity.suspended) {
            System.out.println("허용상태");
            Calendar suspendDate = activity.suspendDate;
            MainActivity.endSets[0] = suspendDate.get(Calendar.YEAR);
            MainActivity.endSets[1] = suspendDate.get(Calendar.MONDAY);
            MainActivity.endSets[2] = suspendDate.get(Calendar.DATE);
            MainActivity.endSets[3] = suspendDate.get(Calendar.HOUR_OF_DAY);
            MainActivity.endSets[4] = suspendDate.get(Calendar.MINUTE);
            MainActivity.endSets[5] = suspendDate.get(Calendar.SECOND);
            if (toggleButton != null) {
                toggleButton.setText(R.string.mnfake_camera_toggle_beacon);
                toggleButton.setBackgroundResource(R.drawable.mnfake_btn_pill_grey);
                toggleButton.setTextColor(0xffffffff);
            }

            if (btl != null && btr != null) {
                btr.setLayoutParams(iconParams);
                btl.setLayoutParams(iconParams);
                btr.setBackgroundResource(R.drawable.img_common_btn_alert);
                btl.setBackgroundResource(R.drawable.menu);
            }
            if (topRightToolbar != null) {
                RelativeLayout.LayoutParams trLp = new RelativeLayout.LayoutParams(iconParams.width, iconParams.height);
                trLp.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
                trLp.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                trLp.setMarginEnd(marginEndPx);
                topRightToolbar.setLayoutParams(trLp);
                topRightToolbar.setBackgroundResource(R.drawable.img_common_btn_alert);
                topRightToolbar.setContentDescription(null);
            }

            if (rl1 != null) {
                rl1.setBackgroundResource(R.drawable.img_bg_user_soldier);
            }
            if (rl2 != null) {
                rl2.setBackgroundResource(R.drawable.img_bg_user_soldier_sub);
            }
            if (ll1 != null) {
                ll1.setVisibility(View.VISIBLE);
            }
            if (sticker != null) {
                sticker.setVisibility(View.VISIBLE);
                sticker.setImageResource(R.drawable.img_policy_state_camera_block);
            }
            if (ll2 != null) {
                ll2.setVisibility(View.VISIBLE);
            }
            if (prg != null) {
                prg.setVisibility(View.VISIBLE);
            }
        } else {
            System.out.println("허용상태");
            if (toggleButton != null) {
                toggleButton.setText(R.string.mndmdm_common_camera_deny);
                toggleButton.setBackgroundResource(R.drawable.style_mndmdm_btn_deny);
            }
            if (btl != null && btr != null) {
                btr.setLayoutParams(letParams);
                btl.setLayoutParams(letParams);
                btr.setBackgroundResource(R.drawable.img_common_drawer_delete);
                btl.setBackgroundResource(R.drawable.img_common_drawer_menu);
            }
            if (topRightToolbar != null) {
                RelativeLayout.LayoutParams trLp = new RelativeLayout.LayoutParams(letParams.width, letParams.height);
                trLp.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
                trLp.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                trLp.setMarginEnd(marginEndPx);
                topRightToolbar.setLayoutParams(trLp);
                topRightToolbar.setBackgroundResource(R.drawable.img_common_drawer_delete);
                topRightToolbar.setContentDescription(activity.getString(R.string.common___delete));
            }

            if (rl1 != null) {
                rl1.setBackgroundResource(R.drawable.img_bg_user_out);
            }
            if (rl2 != null) {
                rl2.setBackgroundResource(R.drawable.img_bg_user_out_sub);
            }
            if (ll1 != null) {
                ll1.setVisibility(View.GONE);
            }
            if (sticker != null) {
                sticker.setVisibility(View.VISIBLE);
                sticker.setImageResource(R.drawable.img_policy_state_camera_allow);
            }
            if (ll2 != null) {
                ll2.setVisibility(View.INVISIBLE);
            }
            if (prg != null) {
                prg.setVisibility(View.INVISIBLE);
            }
        }
    }
}
