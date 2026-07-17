package com.haruhi.lex.crackcamera;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AppEventLogParseTest {
    @Test
    public void parseEditableText_readsOptionalVersionColumn() {
        List<AppEventLog.LogLine> lines = AppEventLog.parseEditableText(
                "block\t2025-07-16 14:30:00\t카메라 차단 | 수동\t9.9.9\n"
                        + "allow\t2025-07-16 14:31:00\t카메라 허용 | 비콘\n");

        assertEquals(2, lines.size());
        assertEquals("block", lines.get(0).kind);
        assertEquals("2025-07-16 14:30:00", lines.get(0).time);
        assertEquals("카메라 차단 | 수동", lines.get(0).message);
        assertEquals("9.9.9", lines.get(0).version);

        assertEquals("allow", lines.get(1).kind);
        assertEquals("", lines.get(1).version);
    }

    @Test
    public void toEditableText_includesVersionColumn() {
        List<AppEventLog.LogLine> lines = AppEventLog.parseEditableText(
                "other\t2025-07-16 14:32:00\tInstall date/time updated.\t2.0.1");
        String editable = AppEventLog.toEditableText(lines);
        assertTrue(editable.contains("\t2.0.1"));
        assertEquals(lines.get(0).message, AppEventLog.parseEditableText(editable).get(0).message);
        assertEquals("2.0.1", AppEventLog.parseEditableText(editable).get(0).version);
    }

    @Test
    public void parseEditableText_acceptsCommaSeparatedFields() {
        List<AppEventLog.LogLine> lines = AppEventLog.parseEditableText(
                "block,2025-07-16 14:30:00,카메라 차단 | 수동,9.9.9\n"
                        + "allow, 2025-07-16 14:31:00, 카메라 허용 | 비콘\n"
                        + "other,2025-07-16 14:32:00,hello, world note,2.0.1\n");

        assertEquals(3, lines.size());

        assertEquals("block", lines.get(0).kind);
        assertEquals("2025-07-16 14:30:00", lines.get(0).time);
        assertEquals("카메라 차단 | 수동", lines.get(0).message);
        assertEquals("9.9.9", lines.get(0).version);

        assertEquals("allow", lines.get(1).kind);
        assertEquals("2025-07-16 14:31:00", lines.get(1).time);
        assertEquals("카메라 허용 | 비콘", lines.get(1).message);
        assertEquals("", lines.get(1).version);

        assertEquals("other", lines.get(2).kind);
        assertEquals("hello, world note", lines.get(2).message);
        assertEquals("2.0.1", lines.get(2).version);
    }
}
