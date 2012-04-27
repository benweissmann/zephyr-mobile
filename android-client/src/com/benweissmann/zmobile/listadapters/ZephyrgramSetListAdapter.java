package com.benweissmann.zmobile.listadapters;

import java.util.ArrayList;

import com.benweissmann.zmobile.R;
import com.benweissmann.zmobile.service.objects.ZephyrgramSet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ZephyrgramSetListAdapter<T extends ZephyrgramSet> extends ArrayAdapter<T> {
    private ArrayList<T> items;
    private int layoutId;

    protected ZephyrgramSetListAdapter(Context context, int layoutId, ArrayList<T> items) {
        super(context, layoutId, items);
        this.items = items;
        this.layoutId = layoutId;
    }
    
    public ZephyrgramSetListAdapter(Context context, ArrayList<T> items) {
        this(context, R.layout.zephyrgram_set_list_item, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        T item = items.get(position);
        String label = item.getName();
        int unreadCount = item.getUnreadCount();
        
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(this.layoutId, null);
        }
        
        TextView labelView = (TextView) v.findViewById(R.id.item_label);
        labelView.setText(label);

        if(unreadCount > 0) {
            TextView unreadCountView = (TextView) v.findViewById(R.id.item_unread_count);
            LinearLayout unreadCountWrapper = (LinearLayout) v.findViewById(R.id.item_unread_count_wrapper);
            unreadCountView.setText(String.valueOf(unreadCount));
            unreadCountWrapper.setVisibility(View.VISIBLE);
        }
        else {
            LinearLayout unreadCountWrapper = (LinearLayout) v.findViewById(R.id.item_unread_count_wrapper);
            unreadCountWrapper.setVisibility(View.INVISIBLE);
        }
        
        return v;
    }
}