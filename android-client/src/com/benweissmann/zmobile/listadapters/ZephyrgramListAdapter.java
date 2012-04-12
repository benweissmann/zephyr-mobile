package com.benweissmann.zmobile.listadapters;

import java.util.ArrayList;

import com.benweissmann.zmobile.R;
import com.benweissmann.zmobile.service.objects.Zephyrgram;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ZephyrgramListAdapter extends ArrayAdapter<Zephyrgram> {
    private ArrayList<Zephyrgram> zephyrgrams;
    
    public ZephyrgramListAdapter(Context context, ArrayList<Zephyrgram> zephyrgrams) {
        super(context, R.layout.zephyrgram_list_item, zephyrgrams);
        this.zephyrgrams = zephyrgrams;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.zephyrgram_list_item, null);
        }
        
        Zephyrgram z = this.zephyrgrams.get(position);
        
        LinearLayout tripletLayout = (LinearLayout) v.findViewById(R.id.zephyrgram_triplet);
        LinearLayout personalHeaderLayout = (LinearLayout) v.findViewById(R.id.zephyrgram_personal_header);
        
        if(z.isPersonal()) {
            tripletLayout.setVisibility(View.GONE);
            personalHeaderLayout.setVisibility(View.VISIBLE);
            
            TextView toMeLabel = (TextView) v.findViewById(R.id.personal_zephyr_to_me_prefix_text);
            TextView fromMeLabel = (TextView) v.findViewById(R.id.personal_zephyr_from_me_prefix_text);
            
            TextView senderView = (TextView) v.findViewById(R.id.zephyrgram_personal_sender);
            
            if(z.isFromMe()) {
                toMeLabel.setVisibility(View.GONE);
                fromMeLabel.setVisibility(View.VISIBLE);
                senderView.setText(z.getUser());
            }
            else {
                toMeLabel.setVisibility(View.VISIBLE);
                fromMeLabel.setVisibility(View.GONE);
                senderView.setText(z.getSender());
            }
        }
        else {
            tripletLayout.setVisibility(View.VISIBLE);
            personalHeaderLayout.setVisibility(View.GONE);
            
            TextView clsView = (TextView) v.findViewById(R.id.zephyrgram_class);
            TextView instanceView = (TextView) v.findViewById(R.id.zephyrgram_instance);
            TextView senderView = (TextView) v.findViewById(R.id.zephyrgram_sender);
            
            clsView.setText(z.getCls());
            instanceView.setText(z.getInstance());
            senderView.setText(z.getSender());
        }
        
        
        TextView timeView = (TextView) v.findViewById(R.id.zephyrgram_time);
        TextView bodyView = (TextView) v.findViewById(R.id.zephyrgram_body);
        
        timeView.setText(z.getTime());
        bodyView.setText(z.getBody());
        
        return v;
    }
}
