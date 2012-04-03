package com.benweissmann.zmobile.listadapters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.benweissmann.zmobile.ClassListActivity;
import com.benweissmann.zmobile.R;
import com.benweissmann.zmobile.service.objects.ZephyrClass;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ZephyrClassListAdapter extends ZephyrgramSetListAdapter<ZephyrClass>{
    public ZephyrClassListAdapter(ClassListActivity activity, ArrayList<ZephyrClass> items, Set<String> starred) {
        super(activity, R.layout.class_list_item, items);
    }
}
