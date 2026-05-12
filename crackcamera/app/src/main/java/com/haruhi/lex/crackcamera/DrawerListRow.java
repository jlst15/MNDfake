package com.haruhi.lex.crackcamera;

/** One row in the navigation drawer (see {@link MainActivity#setupDrawer()}). */
final class DrawerListRow {
    static final int KIND_SECTION = 0;
    static final int KIND_NAV = 1;
    static final int KIND_SWITCH = 2;
    static final int KIND_KV = 3;

    final int kind;
    final int titleRes;
    final int iconRes;
    /** Subtitle under title for {@link #KIND_SWITCH} (e.g. original *_dsc strings). */
    final int subtitleRes;
    /** Preference key for {@link #KIND_SWITCH}. */
    final String prefKey;
    /** Right-side value for {@link #KIND_KV}. */
    final String kvValue;

    private DrawerListRow(int kind, int titleRes, int iconRes, int subtitleRes, String prefKey, String kvValue) {
        this.kind = kind;
        this.titleRes = titleRes;
        this.iconRes = iconRes;
        this.subtitleRes = subtitleRes;
        this.prefKey = prefKey;
        this.kvValue = kvValue;
    }

    static DrawerListRow section(int titleRes) {
        return new DrawerListRow(KIND_SECTION, titleRes, 0, 0, null, null);
    }

    static DrawerListRow nav(int titleRes, int iconRes) {
        return new DrawerListRow(KIND_NAV, titleRes, iconRes, 0, null, null);
    }

    static DrawerListRow toggle(int titleRes, int subtitleRes, String prefKey, int iconRes) {
        return new DrawerListRow(KIND_SWITCH, titleRes, iconRes, subtitleRes, prefKey, null);
    }

    static DrawerListRow kv(int titleRes, String value) {
        return new DrawerListRow(KIND_KV, titleRes, 0, 0, null, value);
    }
}
