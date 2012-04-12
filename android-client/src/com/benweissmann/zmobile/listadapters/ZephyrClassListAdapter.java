package com.benweissmann.zmobile.listadapters;

import java.util.ArrayList;
import java.util.Set;

import android.view.View;
import android.view.ViewGroup;

import com.benweissmann.zmobile.ClassListActivity;
import com.benweissmann.zmobile.R;
import com.benweissmann.zmobile.service.objects.ZephyrClass;

public class ZephyrClassListAdapter extends ZephyrgramSetListAdapter<ZephyrClass>{
    public ZephyrClassListAdapter(ClassListActivity activity, ArrayList<ZephyrClass> items, Set<String> starred) {
        super(activity, R.layout.class_list_item, items);
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = super.getView(position, convertView, parent);
        
        ZephyrClass cls = this.getItem(position);
        
        if(cls.isStarred()) {
            v.findViewById(R.id.class_star_off).setVisibility(View.GONE);
        }
        else {
            v.findViewById(R.id.class_star_on).setVisibility(View.GONE);
        }
        
        return v;
    }
}
