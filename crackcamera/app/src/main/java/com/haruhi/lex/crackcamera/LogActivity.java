package com.haruhi.lex.crackcamera;

import android.content.Intent;
import android.net.Uri;
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
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    private static final int REQUEST_OPEN_LOG_FILE = 1001;

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
            HiddenLongPress.attach(btnLogClose, editMode);
        }

        TextView tvLogTitle = findViewById(R.id.tvLogTitle);
        if (tvLogTitle != null) {
            HiddenLongPress.attach(tvLogTitle, new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    openLogFilePicker();
                    return true;
                }
            });
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_OPEN_LOG_FILE || resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            toast(R.string.mnfake_log_load_failed);
            return;
        }
        loadLogFromUri(uri);
    }

    private void openLogFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        Intent chooser = Intent.createChooser(intent, getString(R.string.mnfake_log_load));
        try {
            startActivityForResult(chooser, REQUEST_OPEN_LOG_FILE);
        } catch (Exception e) {
            toast(R.string.mnfake_log_load_failed);
        }
    }

    private void loadLogFromUri(Uri uri) {
        String text = readTextFromUri(uri);
        if (text == null) {
            toast(R.string.mnfake_log_load_failed);
            return;
        }
        List<AppEventLog.LogLine> lines = AppEventLog.parseEditableText(text);
        AppEventLog.writeLines(this, lines);
        toast(R.string.mnfake_log_load_ok);
        if (isEditMode && etLogEditor != null) {
            etLogEditor.setText(AppEventLog.toEditableText(lines));
        } else {
            refreshLogList();
        }
    }

    private String readTextFromUri(Uri uri) {
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (out.length() > 0) {
                    out.append('\n');
                }
                out.append(line);
            }
            return out.toString();
        } catch (IOException e) {
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void toast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    private void enterEditMode() {
        isEditMode = true;
        if (lvLog != null) {
            lvLog.setVisibility(View.GONE);
        }
        if (etLogEditor != null) {
            etLogEditor.setVisibility(View.VISIBLE);
            etLogEditor.setText(AppEventLog.toEditableText(AppEventLog.readLines(this)));
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
        refreshLogList();
    }

    private void saveLogChanges() {
        if (etLogEditor == null) {
            return;
        }
        List<AppEventLog.LogLine> lines = AppEventLog.parseEditableText(etLogEditor.getText().toString());
        AppEventLog.writeLines(this, lines);
        toast(R.string.mnfake_log_save_ok);
        exitEditMode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isEditMode) {
            refreshLogList();
        }
    }

    private void refreshLogList() {
        List<AppEventLog.LogLine> lines = AppEventLog.readLines(this);
        TextView empty = findViewById(R.id.tvLogEmpty);
        if (empty != null) {
            empty.setVisibility(lines.isEmpty() ? View.VISIBLE : View.GONE);
        }
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
            String footer = "Android " + release + " | " + line.displayVersion();

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
