package com.haruhi.lex.crackcamera;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
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
 * Log viewer opened from the drawer “로그보기” row. Mirrors original resource names
 * {@code activity_log}, {@code lvLog}, {@code layout_list_log_item} declared in
 * {@code originalSource/res/values/public.xml} (layout binaries are not in this tree).
 */
public class LogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        View.OnClickListener close = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        };
        ImageButton back = findViewById(R.id.btnLogBack);
        if (back != null) {
            back.setOnClickListener(close);
        }
        Button btnClose = findViewById(R.id.btnLogClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(close);
        }

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
