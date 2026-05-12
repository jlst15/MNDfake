package com.haruhi.lex.crackcamera;

import android.os.Build;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the left drawer list for “red” (allowed UI) vs “yellow” (blocked UI); the latter inserts
 * GPS checkout under 진동알림. See {@link MainActivity#refreshDrawerMenuMode(boolean)}.
 */
final class DrawerMenuRows {
    private DrawerMenuRows() {
    }

    static List<DrawerListRow> buildRedRows() {
        final String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER : "";
        final String model = Build.MODEL != null ? Build.MODEL : "";
        final String osRelease = Build.VERSION.RELEASE != null ? Build.VERSION.RELEASE : "";
        final List<DrawerListRow> drawerRows = new ArrayList<>();
        drawerRows.add(DrawerListRow.section(R.string.drawer_section_app));
        drawerRows.add(DrawerListRow.nav(R.string.drawer_menu_user_guide, R.drawable.img_ico_menu_guide));
        drawerRows.add(DrawerListRow.nav(R.string.drawer_menu_log, R.drawable.img_ico_menu_log));
        drawerRows.add(DrawerListRow.toggle(R.string.mndmdm_common_list_item_alert_sound,
                R.string.mndmdm_common_list_item_alert_sound_dsc, DrawerPrefs.ALERT_SOUND,
                R.drawable.img_ico_menu_voice));
        drawerRows.add(DrawerListRow.toggle(R.string.mndmdm_common_list_item_alert_vibration,
                R.string.mndmdm_common_list_item_alert_vibration_dsc, DrawerPrefs.ALERT_VIBRATION,
                R.drawable.img_ico_menu_vibration));
        drawerRows.add(DrawerListRow.section(R.string.drawer_section_system));
        drawerRows.add(DrawerListRow.kv(R.string.drawer_menu_device_manufacture, manufacturer));
        drawerRows.add(DrawerListRow.kv(R.string.drawer_menu_device_model, model));
        drawerRows.add(DrawerListRow.kv(R.string.drawer_menu_device_os_version, osRelease));
        return drawerRows;
    }

    /**
     * Yellow (blocked): same list as red, with {@link R.string#drawer_menu_gps_checkout}
     * under 진동알림.
     */
    static List<DrawerListRow> buildYellowRows() {
        List<DrawerListRow> rows = buildRedRows();
        rows.add(5, DrawerListRow.nav(R.string.drawer_menu_gps_checkout, R.drawable.img_ico_menu_gps));
        return rows;
    }
}
