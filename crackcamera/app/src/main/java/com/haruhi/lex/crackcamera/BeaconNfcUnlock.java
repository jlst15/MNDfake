package com.haruhi.lex.crackcamera;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Build;
import android.os.Handler;

/**
 * Listens for any NFC tag while the beacon recognition sheet is open.
 * Uses reader mode on API 19+ and foreground dispatch on older releases.
 */
final class BeaconNfcUnlock {

    private static final int READER_FLAGS =
            NfcAdapter.FLAG_READER_NFC_A
                    | NfcAdapter.FLAG_READER_NFC_B
                    | NfcAdapter.FLAG_READER_NFC_F
                    | NfcAdapter.FLAG_READER_NFC_V
                    | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;

    private static BeaconNfcUnlock sActive;

    private final MainActivity activity;
    private final Handler mainHandler;
    private final Runnable onTagDetected;
    private boolean active;
    private boolean completed;

    BeaconNfcUnlock(MainActivity activity, Handler mainHandler, Runnable onTagDetected) {
        this.activity = activity;
        this.mainHandler = mainHandler;
        this.onTagDetected = onTagDetected;
    }

    static boolean dispatchIntent(Intent intent) {
        BeaconNfcUnlock unlock = sActive;
        return unlock != null && unlock.handleIntent(intent);
    }

    void start() {
        if (active) {
            return;
        }
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(activity);
        if (adapter == null || !adapter.isEnabled()) {
            return;
        }
        active = true;
        sActive = this;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            adapter.enableReaderMode(activity, readerCallback, READER_FLAGS, null);
        } else {
            enableForegroundDispatch(adapter);
        }
    }

    void stop() {
        if (!active) {
            return;
        }
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(activity);
        if (adapter != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                adapter.disableReaderMode(activity);
            } else {
                adapter.disableForegroundDispatch(activity);
            }
        }
        active = false;
        if (sActive == this) {
            sActive = null;
        }
    }

    boolean handleIntent(Intent intent) {
        if (!active || completed || intent == null) {
            return false;
        }
        String action = intent.getAction();
        if (!NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                && !NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                && !NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            return false;
        }
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            return false;
        }
        notifyDetected();
        return true;
    }

    private final NfcAdapter.ReaderCallback readerCallback = new NfcAdapter.ReaderCallback() {
        @Override
        public void onTagDiscovered(Tag tag) {
            notifyDetected();
        }
    };

    private void notifyDetected() {
        if (completed) {
            return;
        }
        completed = true;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                onTagDetected.run();
            }
        });
    }

    private void enableForegroundDispatch(NfcAdapter adapter) {
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingFlags |= PendingIntent.FLAG_MUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                activity, 0, new Intent(activity, activity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                pendingFlags);
        IntentFilter tagDiscovered = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter techDiscovered = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter ndefDiscovered = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndefDiscovered.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException ignored) {
        }
        IntentFilter[] filters = new IntentFilter[]{tagDiscovered, techDiscovered, ndefDiscovered};
        String[][] techLists = new String[][]{
                new String[]{NfcA.class.getName()},
                new String[]{NfcB.class.getName()},
                new String[]{NfcF.class.getName()},
                new String[]{NfcV.class.getName()}
        };
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techLists);
    }
}
