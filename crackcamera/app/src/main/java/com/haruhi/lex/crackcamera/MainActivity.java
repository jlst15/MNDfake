package com.haruhi.lex.crackcamera;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import static com.haruhi.lex.crackcamera.Notification.sendNotification;

public class MainActivity extends AppCompatActivity {

    /**
     * Delay NFC-off sheet until after first frame / init (see
     * {@link #maybeShowNfcOffBottomDialog}).
     */
    private static final long NFC_OFF_DIALOG_SHOW_DELAY_MS = 400L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AlertDialog nfcOffDialog;
    /**
     * NFC-off sheet at most once per process (survives rotation; resets when app is
     * killed).
     */
    private static boolean sNfcOffDialogShownThisProcess;

    public static final String CHANNEL_ID = "CAMCRACK";
    public static final int notificationId = 991991;
    Calendar suspendDate = Calendar.getInstance();
    Calendar startDate = Calendar.getInstance();
    Calendar currDate = Calendar.getInstance();

    TextView tvDay;
    TextView tvHour;
    TextView tvMin;
    TextView tvSec;

    static int[] startSets = { 2020, 0, 26, 17, 56 };
    static int[] endSets = { 2020, 9, 12, 14, 50, 1 };
    static int cDay;
    static int cHour;
    static int cMin;
    static int cSec;

    // Settings
    public static SharedPreferences sharedPref;
    public static SharedPreferences.Editor sharedEditor;
    public static final String PREF_NAME = "MNDFAKE_PREF";
    /**
     * Pref & UI branch: {@code true} = yellow blocked state (비콘, blocked sticker);
     * {@code false} = red untoggled (allow sticker).
     */
    public static boolean suspended = false;
    public static int mutex_user = 0;
    public static boolean init_screen = false;

    private int pendingInstallYear;
    private int pendingInstallMonth;
    private int pendingInstallDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pruneOverlappedBottomToolbarRows();
        Button denyBtn = findViewById(R.id.btnCameraDeny);
        if (denyBtn != null) {
            denyBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    loadSetting();
                    if (!init_screen) {
                        onSwitchCamera(v);
                    } else if (!suspended) {
                        // Red only: beacon sheet → timer/cancel (→ yellow).
                        BeaconRecognitionDialog.show(MainActivity.this, v, mainHandler);
                    } else {
                        // Yellow only: dark OK/Cancel sheet → 확인 (→ red).
                        MainStyledDialogs.showCameraToggleStyledConfirm(MainActivity.this, v, mainHandler);
                    }
                }
            });
        }
        ImageView hiddenLogoTap = findViewById(R.id.ivUserState);
        if (hiddenLogoTap != null) {
            hiddenLogoTap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickMND(v);
                }
            });
        }
        System.out.println("완전시작");
        sharedPref = getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sharedEditor = sharedPref.edit();
        setupDrawer();
        tvDay = findViewById(R.id.tvDay);
        tvHour = findViewById(R.id.tvHour);
        tvMin = findViewById(R.id.tvMin);
        tvSec = findViewById(R.id.tvSec);
        cDay = 0;
        cHour = 0;
        cMin = 0;
        cSec = 0;

        loadSetting();
        System.out.println("로드전: " + suspendDate.get(Calendar.HOUR) + ":" + suspendDate.get(Calendar.MINUTE) + ":"
                + suspendDate.get(Calendar.SECOND));
        startDate.set(startSets[0], startSets[1], startSets[2], startSets[3], startSets[4]);
        suspendDate.set(endSets[0], endSets[1], endSets[2], endSets[3], endSets[4], endSets[5]);
        System.out.println("로드후: " + suspendDate.get(Calendar.HOUR) + ":" + suspendDate.get(Calendar.MINUTE) + ":"
                + suspendDate.get(Calendar.SECOND));
        // System.out.println("날짜차이:" + cDay + "일 " + cHour + "시 " + cMin + "분");
        updatePanel();

        suspended = !suspended;
        init_screen = false;
        onSwitchCamera(findViewById(R.id.btnCameraDeny));
        init_screen = true;
        // if(mutex_user==0)
        a1.start();
        mutex_user++;
        sendNotification(getApplicationContext());
        System.out.println("설정 끝 " + mutex_user);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sNfcOffDialogShownThisProcess) {
            return;
        }
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                maybeShowNfcOffBottomDialog();
            }
        }, NFC_OFF_DIALOG_SHOW_DELAY_MS);
    }

    @Override
    protected void onPause() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        super.onPause();
    }

    /**
     * If the device has NFC and it is disabled, show a bottom sheet matching
     * {@code originalSource} strings
     * {@code mndmdm_common_popup_nfc_off_title/desc}.
     * OEM-specific “mode” is not exposed in the public API;
     * {@link NfcAdapter#isEnabled()} matches the off-NFC case.
     */
    private void maybeShowNfcOffBottomDialog() {
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);
        if (nfc == null) {
            return;
        }
        if (nfc.isEnabled()) {
            return;
        }
        if (nfcOffDialog != null && nfcOffDialog.isShowing()) {
            return;
        }
        if (sNfcOffDialogShownThisProcess) {
            return;
        }

        View content = getLayoutInflater().inflate(R.layout.dialog_nfc_settings_warning, null);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .create();

        AlertDialogWindows.styleBottomSheet(dialog.getWindow());

        Button dismissBtn = content.findViewById(R.id.btnNfcDismiss);
        if (dismissBtn != null) {
            dismissBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface d) {
                nfcOffDialog = null;
            }
        });

        nfcOffDialog = dialog;
        dialog.show();
        sNfcOffDialogShownThisProcess = true;
    }

    public Thread a1 = new Thread() {
        public void run() {
            while (true) {
                updateTime();
                // System.out.println("실행 ");
                try {
                    sleep(999);
                } catch (InterruptedException e) {
                    mutex_user--;
                    e.printStackTrace();
                    break;
                }
            }
        }
    };

    public void onClickDelButton(View v) {
        sendNotification(getApplicationContext());
    }

    public void updateTime() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int[] sara = getDiff();
                // System.out.println(sara[0]+ ": "+sara[1]+ ": "+sara[2]+ ": "+sara[3]);
                if (sara[0] != cDay) {
                    cDay = sara[0];
                    String pl = "" + cDay;
                    tvDay.setText(pl);
                }
                if (sara[1] != cHour) {
                    cHour = sara[1];
                    tvHour.setText(two(cHour));

                }
                if (sara[2] != cMin) {
                    cMin = sara[2];
                    tvMin.setText(two(cMin));
                }
                if (sara[3] != cSec) {
                    cSec = sara[3];
                    tvSec.setText(two(cSec));
                }

            }

        });
    }

    public int[] getDiff() {
        // 일 시 분 초
        long start = suspendDate.getTimeInMillis();
        currDate = Calendar.getInstance();
        // System.out.println("기준시:
        // "+suspendDate.get(Calendar.HOUR)+":"+suspendDate.get(Calendar.MINUTE)+":"+suspendDate.get(Calendar.SECOND));
        // System.out.println("기준시: "+endSets[3]+":"+endSets[4]+":"+endSets[5]);
        long currTime = currDate.getTimeInMillis();
        long lapsed = currTime - start;
        // System.out.println("계산중 "+currTime+" - "+suspendDate.getTimeInMillis()+" =
        // "+lapsed);
        // 3601
        int pool = (int) (lapsed / 1000);
        int seconds = pool % 60; // 1 초
        pool /= 60; // 60 분
        // System.out.println(seconds+"초");
        int minutes = pool % 60; // 0분
        pool /= 60; // 1시간
        int hour = pool % 24; // 1시간
        pool /= 24; // 0일
        int days = pool; // 0일

        // System.out.println(days+" , "+ hour+" : "+minutes+" : "+seconds);
        return new int[] { days, hour, minutes, seconds };
    }

    public void updatePanel() {
        loadSetting();
        TextView inst = findViewById(R.id.tvInstallDate);
        TextView susp = findViewById(R.id.tvCheckInTim);
        int iy = startSets[0];
        if (iy > 2000)
            iy -= 2000;
        int sy = endSets[0];
        if (sy > 2000)
            sy -= 2000;

        String start = two(iy) + "." + two(startSets[1] + 1) + "." + two(startSets[2]) + " " + two(startSets[3]) + ":"
                + two(startSets[4]);
        String end = two(sy) + "." + two(endSets[1] + 1) + "." + two(endSets[2]) + " " + two(endSets[3]) + ":"
                + two(endSets[4]);
        System.out.println("설치 " + start);
        System.out.println("차단 " + end);

        inst.setText(start);
        susp.setText(end);
    }

    public String two(int a) {
        if (a == 0) {
            return "00";
        } else if (a < 10) {
            return "0" + a;
        } else {
            return "" + a;
        }
    }

    public static void saveSetting() {
        sharedEditor.putBoolean("status", suspended);

        sharedEditor.putInt("startY", startSets[0]);
        sharedEditor.putInt("startM", startSets[1]);
        sharedEditor.putInt("startD", startSets[2]);
        sharedEditor.putInt("startH", startSets[3]);
        sharedEditor.putInt("startMin", startSets[4]);

        sharedEditor.putInt("stopY", endSets[0]);
        sharedEditor.putInt("stopM", endSets[1]);
        sharedEditor.putInt("stopD", endSets[2]);
        sharedEditor.putInt("stopH", endSets[3]);
        sharedEditor.putInt("stopMin", endSets[4]);
        sharedEditor.putInt("stopSec", endSets[5]);

        sharedEditor.commit();
        // sharedEditor.apply();
    }

    public static void loadSetting() {
        suspended = sharedPref.getBoolean("status", false);
        startSets[0] = sharedPref.getInt("startY", 2020);
        startSets[1] = sharedPref.getInt("startM", 0);
        startSets[2] = sharedPref.getInt("startD", 26);
        startSets[3] = sharedPref.getInt("startH", 17);
        startSets[4] = sharedPref.getInt("startMin", 20);

        endSets[0] = sharedPref.getInt("stopY", 2020);
        endSets[1] = sharedPref.getInt("stopM", 9);
        endSets[2] = sharedPref.getInt("stopD", 12);
        endSets[3] = sharedPref.getInt("stopH", 12);
        endSets[4] = sharedPref.getInt("stopMin", 0);
        endSets[5] = sharedPref.getInt("stopSec", 0);

    }

    /**
     * Resolve views optional for legacy ids not present in the copied fragment
     * layout.
     */
    private <T extends View> T optionalViewById(String name, Class<T> clazz) {
        int id = getResources().getIdentifier(name, "id", getPackageName());
        if (id == 0) {
            return null;
        }
        View v = findViewById(id);
        if (v == null || !clazz.isInstance(v)) {
            return null;
        }
        return clazz.cast(v);
    }

    /**
     * Left drawer: {@link R.layout#drawer_left_panel} only. Red vs yellow differs
     * only in
     * {@link DrawerMenuRows#buildYellowRows()} (one extra item under 진동알림).
     */
    private void setupDrawer() {
        final DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ImageButton menuBtn = findViewById(R.id.ivMenu);
        if (menuBtn != null && drawerLayout != null) {
            menuBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }
        Button btMenuLegacy = optionalViewById("bt_menu", Button.class);
        if (btMenuLegacy != null && drawerLayout != null) {
            btMenuLegacy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }
        ImageButton topDeleteBtn = findViewById(R.id.btnDeleteApp);
        if (topDeleteBtn != null) {
            topDeleteBtn.bringToFront();
            topDeleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Same control as drawer footer: always open uninstall sheet (yellow mode shows
                    // alert icon here but tap still matches original delete affordance).
                    if (drawerLayout != null) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                    if (suspended) {
                        showUninstallConfirmDialog();
                    }
                }
            });
        }
        View drawerRoot = findViewById(R.id.left_drawer);
        wireDrawerChrome(drawerLayout, drawerRoot, R.id.btnClose, R.id.btnDelete);
        ListView lvDrawerMenu = findViewById(R.id.lvDrawerMenu);
        if (lvDrawerMenu == null) {
            return;
        }
        refreshDrawerMenuMode(suspended);
    }

    private void wireDrawerChrome(final DrawerLayout drawerLayout, View drawerRoot, int closeId, int deleteId) {
        if (drawerRoot == null || drawerLayout == null) {
            return;
        }
        View closeBtn = drawerRoot.findViewById(closeId);
        if (closeBtn != null) {
            closeBtn.bringToFront();
            closeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
            });
        }
        Button deleteFooter = drawerRoot.findViewById(deleteId);
        if (deleteFooter != null) {
            deleteFooter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    showUninstallConfirmDialog();
                }
            });
        }
    }

    private void showUninstallConfirmDialog() {
        MainStyledDialogs.showUninstallConfirm(this);
    }

    void startSystemUninstallForThisApp() {
        Uri uri = Uri.parse("package:" + getPackageName());
        Intent intent = new Intent(Intent.ACTION_DELETE, uri);
        startActivity(intent);
    }

    /**
     * Pass {@code true} when the UI matches yellow (blocked) mode — same moment as
     * {@link #onSwitchCamera}'s {@code if (suspended)} branch before any
     * {@code init_screen} flip.
     */
    private void refreshDrawerMenuMode(boolean blockedUi) {
        final DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ListView lvDrawerMenu = findViewById(R.id.lvDrawerMenu);
        if (lvDrawerMenu == null) {
            return;
        }
        final List<DrawerListRow> rows = blockedUi ? DrawerMenuRows.buildYellowRows() : DrawerMenuRows.buildRedRows();
        lvDrawerMenu.setAdapter(new DrawerMenuAdapter(this, rows, sharedPref, sharedEditor));
        lvDrawerMenu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DrawerListRow r = rows.get(position);
                if (r.kind == DrawerListRow.KIND_SECTION || r.kind == DrawerListRow.KIND_KV) {
                    return;
                }
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
            }
        });
    }

    private void pruneOverlappedBottomToolbarRows() {
        ViewGroup rlButton = findViewById(R.id.rlButton);
        if (rlButton == null || rlButton.getChildCount() <= 1)
            return;
        // Original APK toggles visibility per mode; we keep only the primary denial
        // button like the old crackcamera UI.
        for (int i = 1; i < rlButton.getChildCount(); i++) {
            rlButton.getChildAt(i).setVisibility(View.GONE);
        }
    }

    public void onClickMND(View view) {
        ImageView logo = findViewById(R.id.ivUserState);
        if (logo == null)
            return;
        loadSetting();
        logo.setVisibility(View.GONE);
        showInstallDatePicker();
    }

    private void restoreInstallSecretLogo() {
        ImageView logo = findViewById(R.id.ivUserState);
        if (logo != null) {
            logo.setVisibility(View.VISIBLE);
        }
    }

    private void showInstallDatePicker() {
        final boolean[] picked = new boolean[] { false };
        DatePickerDialog dialog = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(android.widget.DatePicker unused, int year, int month, int dayOfMonth) {
                picked[0] = true;
                pendingInstallYear = year;
                pendingInstallMonth = month;
                pendingInstallDay = dayOfMonth;
                showInstallTimePicker();
            }
        }, startSets[0], startSets[1], startSets[2]);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface unusedDialog) {
                if (!picked[0]) {
                    restoreInstallSecretLogo();
                }
            }
        });
        dialog.show();
    }

    private void showInstallTimePicker() {
        final boolean[] picked = new boolean[] { false };
        TimePickerDialog dialog = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(android.widget.TimePicker unused, int hourOfDay, int minute) {
                picked[0] = true;
                startSets[0] = pendingInstallYear;
                startSets[1] = pendingInstallMonth;
                startSets[2] = pendingInstallDay;
                startSets[3] = hourOfDay;
                startSets[4] = minute;
                saveSetting();
                updatePanel();
                restoreInstallSecretLogo();
            }
        }, startSets[3], startSets[4], true);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface unusedDialog) {
                if (!picked[0]) {
                    restoreInstallSecretLogo();
                }
            }
        });
        dialog.show();
    }

    public void onSwitchCamera(View view) {
        Button toggleButton = findViewById(R.id.btnCameraDeny);
        RelativeLayout rl1 = findViewById(R.id.rlTitle);
        RelativeLayout rl2 = findViewById(R.id.rlSubTitle);
        LinearLayout ll1 = findViewById(R.id.llCheckInTime);
        LinearLayout ll2 = findViewById(R.id.llDelayTime);

        ImageView sticker = findViewById(R.id.ivCameraSticker);
        ProgressBar prg = findViewById(R.id.pdProgress);
        ImageButton topRightToolbar = findViewById(R.id.btnDeleteApp);

        Button btl = optionalViewById("bt_menu", Button.class);
        Button btr = optionalViewById("bt_del", Button.class);
        float factor = getApplicationContext().getResources().getDisplayMetrics().density;

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

        if (suspended) {
            System.out.println("허용상태");
            // 카메라 허용 -> 차단
            // System.out.println("화면구성중 "+suspendDate.getTimeInMillis());

            endSets[0] = suspendDate.get(Calendar.YEAR);
            endSets[1] = suspendDate.get(Calendar.MONDAY);
            endSets[2] = suspendDate.get(Calendar.DATE);
            endSets[3] = suspendDate.get(Calendar.HOUR_OF_DAY);
            endSets[4] = suspendDate.get(Calendar.MINUTE);
            endSets[5] = suspendDate.get(Calendar.SECOND);
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

            if (rl1 != null)
                rl1.setBackgroundResource(R.drawable.img_bg_user_soldier);
            if (rl2 != null)
                rl2.setBackgroundResource(R.drawable.img_bg_user_soldier_sub);
            if (ll1 != null)
                ll1.setVisibility(View.VISIBLE);
            if (sticker != null) {
                sticker.setVisibility(View.VISIBLE);
                sticker.setImageResource(R.drawable.img_policy_state_camera_block);
            }
            if (ll2 != null)
                ll2.setVisibility(View.VISIBLE);
            if (prg != null)
                prg.setVisibility(View.VISIBLE);
        } else {
            System.out.println("허용상태");
            // 카메라 차단 -> 허용
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
                topRightToolbar.setContentDescription(getString(R.string.common___delete));
            }

            if (rl1 != null)
                rl1.setBackgroundResource(R.drawable.img_bg_user_out);
            if (rl2 != null)
                rl2.setBackgroundResource(R.drawable.img_bg_user_out_sub);
            if (ll1 != null)
                ll1.setVisibility(View.GONE);
            if (sticker != null) {
                sticker.setVisibility(View.VISIBLE);
                sticker.setImageResource(R.drawable.img_policy_state_camera_allow);
            }
            if (ll2 != null)
                ll2.setVisibility(View.INVISIBLE);
            if (prg != null)
                prg.setVisibility(View.INVISIBLE);
        }
        final boolean drawerMenuYellow = suspended;
        if (init_screen) {
            boolean wasYellow = suspended;
            suspendDate = Calendar.getInstance();
            suspended = !suspended;
            if (wasYellow && !suspended) {
                vibrateLongYellowToRed();
            }
            saveSetting();
        }
        updatePanel();
        refreshDrawerMenuMode(drawerMenuYellow);
    }

    /**
     * Long pulse when leaving yellow (blocked) for red allow UI (init_screen toggle
     * only).
     */
    private void vibrateLongYellowToRed() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        final long ms = 1000L;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(ms);
        }
    }
}