package com.haruhi.lex.crackcamera;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Log viewer opened from the drawer "로그보기" row. Mirrors original resource names
 * {@code activity_log}, {@code lvLog}, {@code layout_list_log_item} declared in
 * {@code originalSource/res/values/public.xml} (layout binaries are not in this tree).
 */
public class LogActivity extends AppCompatActivity {

    private EditText etLogEditor;
    private Button btnLogEdit;
    private Button btnLogSave;
    private Button btnLogCancelEdit;
    private Button btnLogClose;
    private ListView lvLog;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        etLogEditor = findViewById(R.id.etLogEditor);
        btnLogEdit = findViewById(R.id.btnLogEdit);
        btnLogSave = findViewById(R.id.btnLogSave);
        btnLogCancelEdit = findViewById(R.id.btnLogCancelEdit);
        btnLogClose = findViewById(R.id.btnLogClose);
        lvLog = findViewById(R.id.lvLog);

        View.OnClickListener close = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEditMode) {
                    exitEditMode();
                } else {
                    finish();
                }
            }
        };

        View.OnLongClickListener editMode = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                enterEditMode();
                return true;
            }
        };

        ImageButton back = findViewById(R.id.btnLogBack);
        if (back != null) {
            back.setOnClickListener(close);
        }
        if (btnLogClose != null) {
            btnLogClose.setOnClickListener(close);
            btnLogClose.setOnLongClickListener(editMode);
        }

        if (btnLogSave != null) {
            btnLogSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveLogChanges();
                }
            });
        }

        if (btnLogCancelEdit != null) {
            btnLogCancelEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    exitEditMode();
                }
            });
        }
    }

    private void enterEditMode() {
        isEditMode = true;
        if (lvLog != null) {
            lvLog.setVisibility(View.GONE);
        }
        if (etLogEditor != null) {
            etLogEditor.setVisibility(View.VISIBLE);
            // Load current logs into editor
            List<AppEventLog.LogLine> lines = AppEventLog.readLines(this);
            StringBuilder sb = new StringBuilder();
            for (AppEventLog.LogLine line : lines) {
                sb.append(line.kind).append("\t").append(line.time).append("\t").append(line.message).append("\n");
            }
            etLogEditor.setText(sb.toString());
        }
        if (btnLogEdit != null) {
            btnLogEdit.setVisibility(View.GONE);
        }
        if (btnLogSave != null) {
            btnLogSave.setVisibility(View.VISIBLE);
        }
        if (btnLogCancelEdit != null) {
            btnLogCancelEdit.setVisibility(View.VISIBLE);
        }
    }

    private void exitEditMode() {
        isEditMode = false;
        if (lvLog != null) {
            lvLog.setVisibility(View.VISIBLE);
        }
        if (etLogEditor != null) {
            etLogEditor.setVisibility(View.GONE);
        }
        if (btnLogEdit != null) {
            btnLogEdit.setVisibility(View.GONE);
        }
        if (btnLogSave != null) {
            btnLogSave.setVisibility(View.GONE);
        }
        if (btnLogCancelEdit != null) {
            btnLogCancelEdit.setVisibility(View.GONE);
        }
    }

    private void saveLogChanges() {
        if (etLogEditor == null) return;

        String content = etLogEditor.getText().toString().trim();
        if (content.isEmpty()) return;

        // Parse and save the edited log entries
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.isEmpty()) continue;
            String[] parts = line.split("\t", -1);
            if (parts.length >= 3) {
                String kind = parts[0];
                String time = parts[1];
                String message = parts[2];
                AppEventLog.append(this, kind, time + "\t" + message);
            }
        }

        // Refresh and exit edit mode
        onResume();
        exitEditMode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<AppEventLog.LogLine> lines = AppEventLog.readLines(this);
        TextView empty = findViewById(R.id.tvLogEmpty);
        if (empty != null) {
            empty.setVisibility(lines.isEmpty() ? View.VISIBLE : View.GONE);
        }
        ListView lvLog = findViewById(R.id.lvLog);
        if (lvLog != null) {
            if (lvLog.getAdapter() instanceof LogListAdapter) {
                ((LogListAdapter) lvLog.getAdapter()).setLines(lines);
            } else {
                lvLog.setAdapter(new LogListAdapter(lines));
            }
        }
    }

    /** Stored as {@code yyyy-MM-dd HH:mm:ss}; display like original {@code [yy.MM.dd HH:mm:ss]}. */
    private static String bracketedTimeForDisplay(String stored) {
        if (stored == null || stored.isEmpty()) {
            return "";
        }
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            Date d = in.parse(stored);
            if (d == null) {
                return "[" + stored + "]";
            }
            SimpleDateFormat out = new SimpleDateFormat("yy.MM.dd HH:mm:ss", Locale.US);
            return "[" + out.format(d) + "]";
        } catch (ParseException e) {
            return "[" + stored + "]";
        }
    }

    private static final class LogListAdapter extends BaseAdapter {
        private List<AppEventLog.LogLine> lines;

        LogListAdapter(List<AppEventLog.LogLine> lines) {
            this.lines = lines != null ? new java.util.ArrayList<>(lines) : new java.util.ArrayList<AppEventLog.LogLine>();
        }

        void setLines(List<AppEventLog.LogLine> lines) {
            this.lines = lines != null ? new java.util.ArrayList<>(lines) : new java.util.ArrayList<AppEventLog.LogLine>();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return lines.size();
        }

        @Override
        public Object getItem(int position) {
            return lines.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_list_log_item, parent, false);
            }
            AppEventLog.LogLine line = lines.get(position);
            TextView tvTime = row.findViewById(R.id.tvLogTime);
            TextView tvMsg = row.findViewById(R.id.tvLogMessage);
            TextView tvMeta = row.findViewById(R.id.tvLogMeta);
            LinearLayout root = row.findViewById(R.id.llLogRowRoot);

            String release = Build.VERSION.RELEASE != null ? Build.VERSION.RELEASE : "";
            String footer = "Android " + release + " | " + BuildConfig.VERSION_NAME;

            if (tvTime != null) {
                tvTime.setText(bracketedTimeForDisplay(line.time));
            }
            if (tvMsg != null) {
                tvMsg.setText(line.message);
            }
            if (tvMeta != null) {
                tvMeta.setText(footer);
            }
            if (root != null) {
                if (line.isBlock()) {
                    root.setBackgroundResource(R.color.common_mndmdm_background_brown);
                } else {
                    root.setBackgroundResource(R.color.common_mndmdm_background_gray);
                }
            }
            return row;
        }
    }
}
