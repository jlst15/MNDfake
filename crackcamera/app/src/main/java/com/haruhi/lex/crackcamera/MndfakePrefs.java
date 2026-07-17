package com.haruhi.lex.crackcamera;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Hidden developer toggles stored in {@link MainActivity#PREF_NAME}.
 */
final class MndfakePrefs {

    private static final String PREF_BEACON_NFC_REQUIRED = "mnfake_beacon_nfc_required";
    private static final String PREF_FLAG_SECURE = "mnfake_flag_secure";

    private MndfakePrefs() {
    }

    /** {@code true} = beacon sheet waits for NFC; {@code false} = auto-unlock after timer. */
    static boolean isBeaconNfcRequired(Context context) {
        return prefs(context).getBoolean(PREF_BEACON_NFC_REQUIRED, true);
    }

    static boolean toggleBeaconNfcRequired(Context context) {
        boolean next = !isBeaconNfcRequired(context);
        prefs(context).edit().putBoolean(PREF_BEACON_NFC_REQUIRED, next).apply();
        return next;
    }

    /** {@code true} = MainActivity blocks screenshots / recents previews via {@code FLAG_SECURE}. */
    static boolean isFlagSecureEnabled(Context context) {
        return prefs(context).getBoolean(PREF_FLAG_SECURE, true);
    }

    static boolean toggleFlagSecure(Context context) {
        boolean next = !isFlagSecureEnabled(context);
        prefs(context).edit().putBoolean(PREF_FLAG_SECURE, next).apply();
        return next;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);
    }
}
