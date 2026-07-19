package com.haruhi.lex.crackcamera;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

/**
 * Renders {@link DrawerListRow} items for {@link MainActivity#refreshDrawerMenuMode(boolean)}.
 */
final class DrawerMenuAdapter extends BaseAdapter {
    private final Context context;
    private final List<DrawerListRow> rows;
    private final SharedPreferences sharedPref;
    private final SharedPreferences.Editor sharedEditor;

    DrawerMenuAdapter(Context context, List<DrawerListRow> rows,
            SharedPreferences sharedPref, SharedPreferences.Editor sharedEditor) {
        this.context = context;
        this.rows = rows;
        this.sharedPref = sharedPref;
        this.sharedEditor = sharedEditor;
    }

    @Override
    public int getViewTypeCount() {
        return 4;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).kind;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public Object getItem(int position) {
        return rows.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final DrawerListRow row = rows.get(position);
        if (row.kind == DrawerListRow.KIND_SECTION) {
            View v = convertView;
            if (v == null || v.findViewById(R.id.tvDrawerSectionTitle) == null) {
                v = LayoutInflater.from(context).inflate(R.layout.layout_drawer_section_header, parent, false);
            }
            TextView tv = v.findViewById(R.id.tvDrawerSectionTitle);
            if (tv != null) {
                tv.setText(row.titleRes);
            }
            return v;
        }
        if (row.kind == DrawerListRow.KIND_KV) {
            View v = convertView;
            if (v == null || v.findViewById(R.id.tvKvValue) == null) {
                v = LayoutInflater.from(context).inflate(R.layout.layout_drawer_item_system_kv, parent, false);
            }
            TextView tvT = v.findViewById(R.id.tvKvTitle);
            TextView tvV = v.findViewById(R.id.tvKvValue);
            if (tvT != null) {
                tvT.setText(row.titleRes);
            }
            if (tvV != null) {
                tvV.setText(row.kvValue != null ? row.kvValue : "");
            }
            return v;
        }
        if (row.kind == DrawerListRow.KIND_NAV) {
            View itemView = convertView;
            if (itemView == null || itemView.findViewById(R.id.tvDrawerSectionTitle) != null
                    || itemView.findViewById(R.id.tvKvValue) != null) {
                itemView = LayoutInflater.from(context).inflate(R.layout.layout_list_drawer_item, parent, false);
            }
            TextView tvTitle = itemView.findViewById(R.id.tvTitle);
            if (tvTitle != null) {
                tvTitle.setText(row.titleRes);
            }
            ImageView ivIcon = itemView.findViewById(R.id.ivIcon);
            if (ivIcon != null) {
                ivIcon.setBackgroundResource(row.iconRes);
                ivIcon.setVisibility(View.VISIBLE);
            }
            View sw = itemView.findViewById(R.id.swState);
            if (sw != null) {
                sw.setVisibility(View.GONE);
            }
            View tvVal = itemView.findViewById(R.id.tvValue);
            if (tvVal != null) {
                tvVal.setVisibility(View.GONE);
            }
            View ivDetail = itemView.findViewById(R.id.ivDetail);
            if (ivDetail != null) {
                ivDetail.setVisibility(View.VISIBLE);
            }
            return itemView;
        }
        View itemView = convertView;
        if (itemView == null || itemView.findViewById(R.id.tvDrawerSectionTitle) != null
                || itemView.findViewById(R.id.tvKvValue) != null) {
            itemView = LayoutInflater.from(context).inflate(R.layout.layout_list_drawer_item, parent, false);
        }
        TextView tvTitle = itemView.findViewById(R.id.tvTitle);
        if (tvTitle != null) {
            tvTitle.setText(row.titleRes);
        }
        ImageView ivIcon = itemView.findViewById(R.id.ivIcon);
        if (ivIcon != null) {
            if (row.iconRes != 0) {
                ivIcon.setBackgroundResource(row.iconRes);
                ivIcon.setVisibility(View.VISIBLE);
            } else {
                ivIcon.setVisibility(View.GONE);
            }
        }
        TextView tvVal = itemView.findViewById(R.id.tvValue);
        if (tvVal != null) {
            if (row.subtitleRes != 0) {
                tvVal.setText(row.subtitleRes);
                tvVal.setVisibility(View.VISIBLE);
            } else {
                tvVal.setVisibility(View.GONE);
            }
        }
        // Two-line switch rows run taller than the original; tighten text padding only here.
        View textCol = itemView.findViewById(R.id.llDrawerItemText);
        if (textCol != null) {
            int padH = Math.round(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 10f, context.getResources().getDisplayMetrics()));
            int padV = Math.round(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 5f, context.getResources().getDisplayMetrics()));
            textCol.setPadding(padH, padV, padH, padV);
        }
        View ivDetail = itemView.findViewById(R.id.ivDetail);
        if (ivDetail != null) {
            ivDetail.setVisibility(View.GONE);
        }
        Switch sw = (Switch) itemView.findViewById(R.id.swState);
        if (sw != null) {
            sw.setVisibility(View.VISIBLE);
            sw.setFocusable(false);
            sw.setClickable(true);
            // Keep row height to the custom switch art; OEM themes often force 48dp minHeight.
            sw.setMinimumWidth(0);
            sw.setMinimumHeight(0);
            boolean on = sharedPref != null && sharedPref.getBoolean(row.prefKey, true);
            sw.setOnCheckedChangeListener(null);
            sw.setChecked(on);
            sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (sharedEditor != null) {
                        sharedEditor.putBoolean(row.prefKey, isChecked);
                        sharedEditor.apply();
                    }
                }
            });
        }
        return itemView;
    }
}
