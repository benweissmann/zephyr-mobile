package com.benweissmann.zmobile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.benweissmann.zmobile.listadapters.ZephyrClassListAdapter;
import com.benweissmann.zmobile.listadapters.ZephyrgramSetListAdapter;
import com.benweissmann.zmobile.service.ZephyrService.ZephyrBinder;
import com.benweissmann.zmobile.service.ZephyrServiceBridge;
import com.benweissmann.zmobile.service.callbacks.BinderCallback;
import com.benweissmann.zmobile.service.callbacks.ZephyrCallback;
import com.benweissmann.zmobile.service.callbacks.ZephyrStatusCallback;
import com.benweissmann.zmobile.service.objects.Query;
import com.benweissmann.zmobile.service.objects.ZephyrClass;
import com.benweissmann.zmobile.service.objects.Zephyrgram;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView;
import android.widget.ListView;

public class ClassListActivity extends ZephyrgramSetActivity<ZephyrClass> {
    private View personalsListItem;
    private Set<String> starred;
    
    private static final String STARRED_SERALIZER_DELIMITER = Character.toString((char) 30); // ASCII record separator
    private static final String STARRED_KEY = "starred_classes";
    
    @Override
    public void setup() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        String[] starredItems = prefs.getString(STARRED_KEY, "").split(STARRED_SERALIZER_DELIMITER);
        this.starred = new HashSet<String>(Arrays.asList(starredItems));
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.class_list_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.class_list_menu_compose:
            Intent intent = new Intent(this, ComposeActivity.class);
            startActivityForResult(intent, 0);
            return true;
        case R.id.class_list_menu_feedback:
            ComposeActivity.launchFeedback(this);
            return true;
        case R.id.class_list_menu_refresh:
            this.update();
            return true;
        case R.id.class_list_menu_settings:
            // TODO: implement this
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void getItems(ZephyrBinder b,
                            ZephyrCallback<ZephyrClass[]> callback) {
        
        b.fetchClasses(callback);
    }

    @Override
    protected void goToAll() {
        Intent intent = new Intent(this, ZephyrgramActivity.class);
        intent.putExtra(ZephyrgramActivity.QUERY_EXTRA, new Query());
        startActivityForResult(intent, 0);
    }
    
    protected void goToPersonals() {
        Intent intent = new Intent(this, PersonalsListActivity.class);
        startActivityForResult(intent, 0);
    }
    
    @Override
    protected void goToItem(ZephyrClass item) {
        Intent intent = new Intent(this, InstanceListActivity.class);
        intent.putExtra(InstanceListActivity.ZEPHYR_CLASS_EXTRA, item.getName());
        startActivityForResult(intent, 0);
    }
    
    @Override
    protected void dispatchClick(int position, ZephyrgramSetListAdapter<ZephyrClass> listAdapter) {
        if(position == 0) {
            this.goToAll();
        }
        else if(position == 1) {
            this.goToPersonals();
        }
        else {
            this.goToItem(listAdapter.getItem(position-2));
        }
    }

    @Override
    protected void addHeaderViews(ListView listView) {
        super.addHeaderViews(listView);
        
        LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = vi.inflate(R.layout.zephyrgram_set_list_item, null);
        
        this.personalsListItem = v;
        
        TextView labelView = (TextView) v.findViewById(R.id.item_label);
        labelView.setText(getString(R.string.personal_label));
        
        listView.addHeaderView(v);
    }
    
    @Override
    protected void refreshHeaderViews(ListView listView, ArrayList<ZephyrClass> items) {
        super.refreshHeaderViews(listView, items);
        
        int unreadCount = 0;
        
        for(int i = 0; i < items.size(); i++) {
            if(items.get(i).isPersonals()) {
                unreadCount = items.get(i).getUnreadCount();
                items.remove(i);
                break;
            }
        }
        
        if(unreadCount > 0) {
            this.personalsListItem.findViewById(R.id.item_unread_count_wrapper).setVisibility(View.VISIBLE);
            TextView unreadCountView = (TextView) this.personalsListItem.findViewById(R.id.item_unread_count);
            unreadCountView.setText(Integer.toString(unreadCount));
        }
        else {
            this.personalsListItem.findViewById(R.id.item_unread_count_wrapper).setVisibility(View.INVISIBLE);
        }
    }
    
    @Override
    protected ZephyrgramSetListAdapter<ZephyrClass> getListAdapter(ArrayList<ZephyrClass> items) {
        int nextStarredIndex = 0;
        
        for(int i = 0; i < items.size(); i++) {
            if(starred.contains(items.get(i))) {
                items.add(nextStarredIndex++, items.remove(i));
            }
        }
        
        return new ZephyrClassListAdapter(this, items, this.starred);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        MenuInflater inflater = getMenuInflater();
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        
        if(info.position < 2) {
            inflater.inflate(R.menu.class_list_limited_context_menu, menu);
        }
        else {
            inflater.inflate(R.menu.class_list_context_menu, menu);
            
            ZephyrClass cls = this.getCurrentListAdapter().getItem(info.position-2);
            if(cls.isStarred()) {
                menu.removeItem(R.id.class_list_star_class);
            }
            else {
                menu.removeItem(R.id.class_list_unstar_class);
            }
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        boolean allSelected = false;
        boolean personalsSelected = false;
        ZephyrClass cls = null;
        
        if (info.position == 0) {
            allSelected = true;
        }
        else if(info.position == 1) {
            personalsSelected = true;
        }
        else {
            cls = this.getCurrentListAdapter().getItem(info.position-2);
        }
        
        switch (item.getItemId()) {
            case R.id.class_list_star_class:
                if(cls != null) {
                    this.starClass(cls.getName());
                }
                return true;
            case R.id.class_list_unstar_class:
                if(cls != null) {
                    this.unstarClass(cls.getName());
                }
                return true;
            case R.id.class_list_hide_item:
                if(cls != null) {
                    this.hideClass(cls.getName());
                }
                return true;
            case R.id.class_list_mark_all_read:
                if(allSelected) {
                    this.markRead(new Query());
                }
                else if(personalsSelected) {
                    this.markRead(new Query().cls(Zephyrgram.PERSONALS_CLASS));
                }
                else {
                    this.markRead(cls.getQuery());
                }
        }
        
        return super.onContextItemSelected(item);
    }
    
    private void starClass(final String clsName) {
        ZephyrServiceBridge.getBinder(this, new BinderCallback() {
            public void run(ZephyrBinder binder, Runnable onComplete) {
                binder.starClass(clsName, new ZephyrStatusCallback() {
                    
                    public void onSuccess() {
                        update();
                    }
                    
                    public void onFailure() {
                        showFailToast();
                    }
                    
                    public void onError(Throwable e) {
                        showFailToast();
                    }
                });
            }
        });
    }
    
    private void unstarClass(final String clsName) {
        ZephyrServiceBridge.getBinder(this, new BinderCallback() {
            public void run(ZephyrBinder binder, Runnable onComplete) {
                binder.unstarClass(clsName, new ZephyrStatusCallback() {
                    public void onSuccess() {
                        update();
                    }
                    
                    public void onFailure() {
                        showFailToast();
                    }
                    
                    public void onError(Throwable e) {
                        showFailToast();
                    }
                });
            }
        });
    }
    
    private void hideClass(final String clsName) {
        ZephyrServiceBridge.getBinder(this, new BinderCallback() {
            public void run(ZephyrBinder binder, Runnable onComplete) {
                binder.hideClass(clsName, new ZephyrStatusCallback() {
                    public void onSuccess() {
                        update();
                    }
                    
                    public void onFailure() {
                        showFailToast();
                    }
                    
                    public void onError(Throwable e) {
                        showFailToast();
                    }
                });
                onComplete.run();
            }
        });
    }
}
