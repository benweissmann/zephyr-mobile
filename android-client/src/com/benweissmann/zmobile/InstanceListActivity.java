package com.benweissmann.zmobile;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.benweissmann.zmobile.components.ListHeader;
import com.benweissmann.zmobile.service.ZephyrService.ZephyrBinder;
import com.benweissmann.zmobile.service.callbacks.ZephyrCallback;
import com.benweissmann.zmobile.service.objects.Query;
import com.benweissmann.zmobile.service.objects.ZephyrInstance;

public class InstanceListActivity extends ZephyrgramSetActivity<ZephyrInstance> {
    public static final String ZEPHYR_CLASS_EXTRA = "instance_list_zephyr_class";
    private String className;
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.instance_list_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.instance_list_menu_compose:
            Intent intent = new Intent(this, ComposeActivity.class);
            intent.putExtra(ComposeActivity.CLASS_EXTRA, this.className);
            startActivityForResult(intent, 0);
            return true;
        case R.id.instance_list_menu_feedback:
            ComposeActivity.launchFeedback(this);
            return true;
        case R.id.instance_list_menu_refresh:
            this.update();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void setup() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            this.className = extras.getString(ZEPHYR_CLASS_EXTRA);
        }

        if (this.className == null) {
            throw new RuntimeException("InstanceListActivity didn't get a class");
        }
    }
    
    @Override
    protected void getItems(ZephyrBinder b,
                            ZephyrCallback<ZephyrInstance[]> callback) {
        
        b.fetchInstances(this.className, callback);
    }

    @Override
    protected void goToAll() {
        Intent intent = new Intent(this, ZephyrgramActivity.class);
        intent.putExtra(ZephyrgramActivity.QUERY_EXTRA, new Query().cls(this.className));
        startActivityForResult(intent, 0);
    }
    
    @Override
    protected List<ListHeader.Breadcrumb> getBreadcrumbs() {
        List<ListHeader.Breadcrumb> breadcrumbs = new ArrayList<ListHeader.Breadcrumb>();
        breadcrumbs.add(new ListHeader.Breadcrumb(this.className, null));
        
        return breadcrumbs;
    }
}
