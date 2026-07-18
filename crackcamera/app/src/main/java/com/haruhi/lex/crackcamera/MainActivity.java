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
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import static com.haruhi.lex.crackcamera.Notification.sendNotification;

/**
 * Primary screen: camera deny toggle, navigation drawer, NFC / uninstall /
 * beacon dialogs, and
 * hidden install-date flow (long-press agent version at bottom) and
 * screenshot-block toggle (long-press toolbar delete).
 * <p>
 * Supporting types (same package, behavior unchanged):
 * <ul>
 * <li>{@link DrawerMenuRows}, {@link DrawerMenuAdapter}, {@link DrawerListRow}
 * — drawer list
 * content and adapter</li>
 * <li>{@link AlertDialogWindows} — shared {@link android.app.AlertDialog}
 * window sizing and
 * gravity</li>
 * <li>{@link MainStyledDialogs} — uninstall sheet and yellow→red camera
 * confirmation sheet</li>
 * <li>{@link BeaconRecognitionDialog} — red-state beacon sheet; NFC or auto timer per
 * {@link MndfakePrefs#isBeaconNfcRequired}; phase {@link BeaconRecognitionDialog#PHASE1_MS},
 * auto {@link BeaconRecognitionDialog#AUTO_TOGGLE_MS}</li>
 * <li>{@link MainCameraUi} — toolbar / header visuals driven by
 * {@link #onSwitchCamera(View)}</li>
 * </ul>
 */
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
    private int pendingBlockYear;
    private int pendingBlockMonth;
    private int pendingBlockDay;
    /** Matches painted toolbar/header mode (yellow blocked vs red allow), not post-flip {@link #suspended}. */
    private boolean blockedUiVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyFlagSecureFromPrefs();
        setContentView(R.layout.activity_main);
        RecentsTaskIcon.apply(this);
        TextView tvAgentVersion = findViewById(R.id.tvAgentVersion);
        if (tvAgentVersion != null) {
            tvAgentVersion.setText(BuildConfig.VERSION_NAME);
            HiddenLongPress.attach(tvAgentVersion, new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    loadSetting();
                    showInstallDatePicker();
                    return true;
                }
            });
        }
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
            HiddenLongPress.attach(hiddenLogoTap, new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    boolean nfcRequired = MndfakePrefs.toggleBeaconNfcRequired(MainActivity.this);
                    Toast.makeText(MainActivity.this,
                            nfcRequired ? R.string.mnfake_beacon_nfc_required_on
                                    : R.string.mnfake_beacon_nfc_required_off,
                            Toast.LENGTH_SHORT).show();
                    return true;
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
        /* Cold start only: opposite of no-arg boot/delete gate — skip pristine install (no status key). */
        if (sharedPref.contains("status") && !sharedPref.getBoolean("status", false)) {
            sendNotification(getApplicationContext(), getString(R.string.policy_camera_deny_noti_comment));
        }
        // if(mutex_user==0)
        a1.start();
        mutex_user++;
        System.out.println("설정 끝 " + mutex_user);
    }

    @Override
    protected void onResume() {
        super.onResume();
        RecentsTaskIcon.apply(this);
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        BeaconRecognitionDialog.dispatchNfcIntent(intent);
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
     *
     * @see AlertDialogWindows#styleBottomSheet(android.view.Window)
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
     * Package-visible for {@link MainCameraUi}.
     */
    <T extends View> T optionalViewById(String name, Class<T> clazz) {
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
            HiddenLongPress.attach(topDeleteBtn, new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (blockedUiVisible) {
                        showBlockDatePicker();
                    } else {
                        boolean secure = MndfakePrefs.toggleFlagSecure(MainActivity.this);
                        applyFlagSecureFromPrefs();
                        Toast.makeText(MainActivity.this,
                                secure ? R.string.mnfake_flag_secure_on
                                        : R.string.mnfake_flag_secure_off,
                                Toast.LENGTH_SHORT).show();
                    }
                    return true;
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

    private void applyFlagSecureFromPrefs() {
        if (MndfakePrefs.isFlagSecureEnabled(this)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    /**
     * Bottom-sheet uninstall confirmation; delegates to
     * {@link MainStyledDialogs#showUninstallConfirm(MainActivity)}.
     */
    private void showUninstallConfirmDialog() {
        MainStyledDialogs.showUninstallConfirm(this);
    }

    /**
     * Launches the system uninstall UI for this package. Package-visible for
     * {@link MainStyledDialogs}.
     */
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
                if (r.kind == DrawerListRow.KIND_NAV && r.titleRes == R.string.drawer_menu_log) {
                    startActivity(new Intent(MainActivity.this, LogActivity.class));
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

    private void showInstallDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(android.widget.DatePicker unused, int year, int month, int dayOfMonth) {
                pendingInstallYear = year;
                pendingInstallMonth = month;
                pendingInstallDay = dayOfMonth;
                showInstallTimePicker();
            }
        }, startSets[0], startSets[1], startSets[2]);
        dialog.show();
    }

    private void showInstallTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(android.widget.TimePicker unused, int hourOfDay, int minute) {
                startSets[0] = pendingInstallYear;
                startSets[1] = pendingInstallMonth;
                startSets[2] = pendingInstallDay;
                startSets[3] = hourOfDay;
                startSets[4] = minute;
                saveSetting();
                updatePanel();
                AppEventLog.append(MainActivity.this, AppEventLog.KIND_OTHER,
                        getString(R.string.mnfake_log_event_install_updated));
            }
        }, startSets[3], startSets[4], true);
        dialog.show();
    }

    private void showBlockDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(android.widget.DatePicker unused, int year, int month, int dayOfMonth) {
                pendingBlockYear = year;
                pendingBlockMonth = month;
                pendingBlockDay = dayOfMonth;
                showBlockTimePicker();
            }
        }, endSets[0], endSets[1], endSets[2]);
        dialog.show();
    }

    private void showBlockTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(android.widget.TimePicker unused, int hourOfDay, int minute) {
                endSets[0] = pendingBlockYear;
                endSets[1] = pendingBlockMonth;
                endSets[2] = pendingBlockDay;
                endSets[3] = hourOfDay;
                endSets[4] = minute;
                endSets[5] = 0;
                suspendDate.set(endSets[0], endSets[1], endSets[2], endSets[3], endSets[4], endSets[5]);
                saveSetting();
                updatePanel();
                AppEventLog.append(MainActivity.this, AppEventLog.KIND_OTHER,
                        getString(R.string.mnfake_log_event_block_updated));
            }
        }, endSets[3], endSets[4], true);
        dialog.show();
    }

    /**
     * Applies camera blocked vs allowed UI and optionally flips {@link #suspended}
     * when
     * {@link #init_screen} is true (user-driven toggle). Startup uses
     * {@code init_screen == false}
     * so the first call only syncs visuals from prefs.
     * <p>
     * Order matters: {@link MainCameraUi#applyToggleVisuals(MainActivity)} paints
     * from the
     * <em>current</em> {@code suspended} flag; {@code drawerMenuYellow} snapshots
     * that flag for
     * {@link #refreshDrawerMenuMode(boolean)} before {@code suspended} may be
     * toggled inside the
     * {@code init_screen} block.
     *
     * @param view optional trigger (e.g. deny button); may be {@code null} on
     *             synthetic calls
     */
    public void onSwitchCamera(View view) {
        // Refresh lock anchor before painting so the yellow branch copies "now" into endSets
        // (previously endSets lagged by one toggle because suspendDate was updated after the copy).
        if (init_screen) {
            suspendDate = Calendar.getInstance();
        }
        MainCameraUi.applyToggleVisuals(this);
        final boolean drawerMenuYellow = suspended;
        blockedUiVisible = drawerMenuYellow;
        if (init_screen) {
            boolean wasYellow = suspended;
            suspended = !suspended;
            if (wasYellow && !suspended) {
                vibrateLongYellowToRed();
            }
            saveSetting();
            if (!suspended) {
                AppEventLog.append(MainActivity.this, AppEventLog.KIND_BLOCK,
                        getString(R.string.mnfake_log_camera_block_manual));
            } else {
                AppEventLog.append(MainActivity.this, AppEventLog.KIND_ALLOW,
                        getString(R.string.mnfake_log_camera_allow_beacon));
            }
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
        final long ms = 1100L;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(ms);
        }
    }
}