package com.benweissmann.zmobile;

import java.util.ArrayList;
import java.util.List;

import com.benweissmann.zmobile.components.ListHeader;
import com.benweissmann.zmobile.service.ZephyrService.ZephyrBinder;
import com.benweissmann.zmobile.service.callbacks.ZephyrCallback;
import com.benweissmann.zmobile.service.objects.Query;
import com.benweissmann.zmobile.service.objects.ZephyrPersonals;
import com.benweissmann.zmobile.service.objects.Zephyrgram;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class PersonalsListActivity extends ZephyrgramSetActivity<ZephyrPersonals> {
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.personals_list_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.personals_list_menu_compose:
            Intent intent = new Intent(this, ComposeActivity.class);
            intent.putExtra(ComposeActivity.SELECT_PERSONAL_EXTRA, true);
            startActivityForResult(intent, 0);
            return true;
        case R.id.personals_list_menu_feedback:
            ComposeActivity.launchFeedback(this);
            return true;
        case R.id.personals_list_menu_refresh:
            this.update();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void getItems(ZephyrBinder b,
                            ZephyrCallback<ZephyrPersonals[]> callback) {
        
        b.fetchPersonals(callback);
    }

    @Override
    protected void goToAll() {
        Intent intent = new Intent(this, ZephyrgramActivity.class);
        intent.putExtra(ZephyrgramActivity.QUERY_EXTRA, new Query().cls(Zephyrgram.PERSONALS_CLASS));
        startActivityForResult(intent, 0);
    }

    @Override
    protected List<ListHeader.Breadcrumb> getBreadcrumbs() {
        List<ListHeader.Breadcrumb> breadcrumbs = new ArrayList<ListHeader.Breadcrumb>();
        breadcrumbs.add(new ListHeader.Breadcrumb(getString(R.string.personal_label), null));
        
        return breadcrumbs;
    }
}
