package com.haruhi.lex.crackcamera;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Persisted event list for {@link LogActivity}. Lines use
 * {@code kind\ttimestamp\tmessage[\tversion]} ({@link #PREF_KEY_LINES_V2}) so rows can style
 * “카메라 차단” entries like the original app. {@code version} is optional and overrides the
 * app version shown in the log-item footer.
 */
final class AppEventLog {
    /** {@code block} = brown row (차단); {@code allow} = 허용; {@code other} = install etc. */
    static final String KIND_BLOCK = "block";
    static final String KIND_ALLOW = "allow";
    static final String KIND_OTHER = "other";

    private static final String PREF_KEY_LINES_V2 = "mnfake_app_event_log_v2";
    private static final int MAX_LINES = 120;

    private AppEventLog() {
    }

    static void append(Context context, String kind, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        String k = kind != null ? kind : KIND_OTHER;
        Context app = context.getApplicationContext();
        SharedPreferences sp = app.getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String line = k + "\t" + ts + "\t" + sanitize(message) + "\t" + sanitize(BuildConfig.VERSION_NAME);
        String existing = sp.getString(PREF_KEY_LINES_V2, "");
        String combined = line + "\n" + existing;
        String[] parts = combined.split("\n", -1);
        StringBuilder out = new StringBuilder();
        int n = Math.min(parts.length, MAX_LINES);
        for (int i = 0; i < n; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(parts[i]);
        }
        sp.edit().putString(PREF_KEY_LINES_V2, out.toString()).apply();
    }

    private static String sanitize(String message) {
        if (message == null) {
            return "";
        }
        return message.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    static List<LogLine> readLines(Context context) {
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(MainActivity.PREF_NAME,
                Context.MODE_PRIVATE);
        String raw = sp.getString(PREF_KEY_LINES_V2, "");
        return parseStoredText(raw);
    }

    static void writeLines(Context context, List<LogLine> lines) {
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(MainActivity.PREF_NAME,
                Context.MODE_PRIVATE);
        sp.edit().putString(PREF_KEY_LINES_V2, serializeLines(lines)).apply();
    }

    /**
     * One line per entry: {@code kind<TAB>yyyy-MM-dd HH:mm:ss<TAB>message[<TAB>version]}.
     * Version is always written so editors can change the footer app version.
     */
    static String toEditableText(List<LogLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (LogLine line : lines) {
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(line.kind).append('\t').append(line.time).append('\t').append(line.message)
                    .append('\t').append(line.displayVersion());
        }
        return out.toString();
    }

    static List<LogLine> parseEditableText(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = text.split("\n", -1);
        List<LogLine> list = new ArrayList<>(parts.length);
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            list.add(parseOneLine(trimmed));
        }
        return list;
    }

    static List<LogLine> parseStoredText(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = raw.split("\n");
        List<LogLine> list = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            list.add(parseTabFields(part.split("\t", -1), part));
        }
        return list;
    }

    private static String serializeLines(List<LogLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        int n = Math.min(lines.size(), MAX_LINES);
        for (int i = 0; i < n; i++) {
            LogLine line = lines.get(i);
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(line.kind).append('\t').append(line.time).append('\t').append(sanitize(line.message));
            if (line.version != null && !line.version.isEmpty()) {
                out.append('\t').append(sanitize(line.version));
            }
        }
        return out.toString();
    }

    private static LogLine parseOneLine(String line) {
        if (line.startsWith("[")) {
            int end = line.indexOf(']');
            if (end > 1) {
                String timeDisplay = line.substring(1, end).trim();
                String rest = line.substring(end + 1).trim();
                String message = rest;
                String version = "";
                // Optional " | version" or tab-separated version after the message.
                int tab = rest.lastIndexOf('\t');
                if (tab >= 0) {
                    message = rest.substring(0, tab).trim();
                    version = rest.substring(tab + 1).trim();
                } else {
                    int pipe = rest.lastIndexOf(" | ");
                    if (pipe >= 0) {
                        String maybeVersion = rest.substring(pipe + 3).trim();
                        // Only treat as version when it looks like an app version, not message text.
                        if (looksLikeVersion(maybeVersion)) {
                            message = rest.substring(0, pipe).trim();
                            version = maybeVersion;
                        }
                    }
                }
                return new LogLine(inferKind(message), displayTimeToStored(timeDisplay), message, version);
            }
        }
        return parseTabFields(line.split("\t", -1), line);
    }

    private static LogLine parseTabFields(String[] seg, String rawFallback) {
        if (seg.length >= 4) {
            return new LogLine(seg[0], seg[1], seg[2], joinFrom(seg, 3));
        }
        if (seg.length == 3) {
            return new LogLine(seg[0], seg[1], seg[2], "");
        }
        if (seg.length == 2) {
            if (looksLikeTimestamp(seg[0])) {
                return new LogLine(inferKind(seg[1]), seg[0], seg[1], "");
            }
            return new LogLine(seg[0], "", seg[1], "");
        }
        return new LogLine(inferKind(rawFallback), "", rawFallback, "");
    }

    private static String joinFrom(String[] parts, int start) {
        if (start >= parts.length) {
            return "";
        }
        if (start == parts.length - 1) {
            return parts[start];
        }
        StringBuilder out = new StringBuilder(parts[start]);
        for (int i = start + 1; i < parts.length; i++) {
            out.append('\t').append(parts[i]);
        }
        return out.toString();
    }

    private static String inferKind(String message) {
        if (message != null) {
            if (message.contains("차단")) {
                return KIND_BLOCK;
            }
            if (message.contains("허용")) {
                return KIND_ALLOW;
            }
        }
        return KIND_OTHER;
    }

    private static boolean looksLikeTimestamp(String value) {
        return value != null && value.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }

    private static boolean looksLikeVersion(String value) {
        return value != null && value.matches("\\d+(?:\\.\\d+)+.*");
    }

    private static String displayTimeToStored(String display) {
        if (display == null || display.isEmpty()) {
            return "";
        }
        try {
            SimpleDateFormat in = new SimpleDateFormat("yy.MM.dd HH:mm:ss", Locale.US);
            Date d = in.parse(display);
            if (d != null) {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(d);
            }
        } catch (Exception ignored) {
        }
        if (looksLikeTimestamp(display)) {
            return display;
        }
        return display;
    }

    static final class LogLine {
        final String kind;
        final String time;
        final String message;
        /** Optional app version override for the log-item footer; empty uses {@link BuildConfig#VERSION_NAME}. */
        final String version;

        LogLine(String kind, String time, String message) {
            this(kind, time, message, "");
        }

        LogLine(String kind, String time, String message, String version) {
            this.kind = kind != null ? kind : KIND_OTHER;
            this.time = time != null ? time : "";
            this.message = message != null ? message : "";
            this.version = version != null ? version : "";
        }

        String displayVersion() {
            return version.isEmpty() ? BuildConfig.VERSION_NAME : version;
        }

        boolean isBlock() {
            return KIND_BLOCK.equals(kind);
        }

        boolean isAllow() {
            return KIND_ALLOW.equals(kind);
        }
    }
}
