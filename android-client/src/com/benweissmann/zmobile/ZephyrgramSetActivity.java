package com.benweissmann.zmobile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.benweissmann.zmobile.components.ListHeader;
import com.benweissmann.zmobile.components.LoadFlipper;
import com.benweissmann.zmobile.listadapters.ZephyrgramSetListAdapter;
import com.benweissmann.zmobile.service.ZephyrServiceBridge;
import com.benweissmann.zmobile.service.ZephyrService.ZephyrBinder;
import com.benweissmann.zmobile.service.callbacks.BinderCallback;
import com.benweissmann.zmobile.service.callbacks.ZephyrCallback;
import com.benweissmann.zmobile.service.objects.ZephyrgramSet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public abstract class ZephyrgramSetActivity<T extends ZephyrgramSet> extends Activity {
    private View allListItem;
    
    /**
     * Called when this activity is created. If you need to do extra setup,
     * override setup().
     */
    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);
        
        this.setup();
        ListHeader.populate(this, this.getBreadcrumbs());
        this.addHeaderViews((ListView) findViewById(R.id.list_view));

        update();
    }
    
    /**
     * Called by onCreate. By default, does nothing, but can be override
     * to do subclass-specific onCreate actions, like unpacking extras.
     */
    protected void setup() {}
    
    /**
     * Asynchronous method to fetch the ZephyrgramSets for this list.
     * @param b        The binder for the ZephyrService
     * @param callback A ZephyrCallback that needs to be called with the
     *                 ZephyrgramSets for this list.   
     */
    protected abstract void getItems(ZephyrBinder b, ZephyrCallback<T[]> callback);
    
    /**
     * Called when the user selects the "All" item for this list.
     */
    protected abstract void goToAll();
    
    /**
     * Called when the user selects an item from this list. This implementation
     * goes to the ZephyrgramAcivity with the query returned from
     * item.getQuery()
     * @param item The item selected
     */
    protected void goToItem(T item) {
        Intent intent = new Intent(this, ZephyrgramActivity.class);
        intent.putExtra(ZephyrgramActivity.QUERY_EXTRA, item.getQuery());
        startActivityForResult(intent, 0);
    }
    
    /**
     * Calls the appropriate method when the user taps an item from the list.
     * This implementation calls goToAll for position 0, and
     * goToItem for all other positions. If you override addHeaderViews to
     * add additional headers, you must override this method to correctly
     * dispatch.
     * @param position position of the pressed list item
     */
    protected void dispatchClick(int position, ZephyrgramSetListAdapter<T> listAdapter) {
        if(position == 0) {
            goToAll();
        }
        else {
            goToItem(listAdapter.getItem(position - 1));
        }
    }

    /**
     * Adds header views to the list. This implementation adds an "all" item
     * with an unread count equal to the sum of item.getUnreadCount()
     * for item in items.
     * 
     * Note that this method is called only once (when the Activity is created).
     * refreshHeaderViews is called on refresh as well as when the activity
     * is created. Use this method to add the header views and then use
     * refreshHeaderViews to set unread counts, etc.
     * @param listView
     */
    protected void addHeaderViews(ListView listView) {
        LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        // add "All"
        
        View v = vi.inflate(R.layout.zephyrgram_set_list_item, null);
        
        this.allListItem = v;
        
        TextView labelView = (TextView) v.findViewById(R.id.item_label);
        labelView.setText(getString(R.string.all_label));
        
        listView.addHeaderView(v);
    }
    
    /**
     * Returns the breadcrumbs for this activity.
     * @return
     */
    protected List<ListHeader.Breadcrumb> getBreadcrumbs() {
        return new ArrayList<ListHeader.Breadcrumb>();
    }
    
    /**
     * Adds header views to the list. This implementation adds an "all" item
     * with an unread count equal to the sum of item.getUnreadCount()
     * for item in items.
     * 
     * This method is called BEFORE the items array is passed to the listadapter,
     * so you can safely modify the items list, e.g. to remove the
     * "message" class.
     * 
     * @param listView
     * @param items
     */
    protected void refreshHeaderViews(ListView listView, ArrayList<T> items) {
        int unreadCount = 0;
        
        for(T item : items) {
            unreadCount += item.getUnreadCount();
        }
        
        if(unreadCount > 0) {
            this.allListItem.findViewById(R.id.item_unread_count_wrapper).setVisibility(View.VISIBLE);
            TextView unreadCountView = (TextView) this.allListItem.findViewById(R.id.item_unread_count);
            unreadCountView.setText(Integer.toString(unreadCount));
        }
        else {
            this.allListItem.findViewById(R.id.item_unread_count_wrapper).setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Returns a list adapter for this activity. This can be overridden to
     * return a subclass of ZephyrgramSetListAdapter.
     * @param items The items to populate the adapter with.
     */
    protected ZephyrgramSetListAdapter<T> getListAdapter(ArrayList<T> items) {
        return new ZephyrgramSetListAdapter<T>(this, items);
    }

    /**
     * Fetches a fresh list of ZephyrgramSets and displays them.
     */
    protected void update() {
        LoadFlipper.flipToLoader(this);
        
        ZephyrServiceBridge.getBinder(this, new BinderCallback() {
            public void run(ZephyrBinder binder, final Runnable onComplete) {
                getItems(binder, new ZephyrCallback<T[]>() {
                    public void run(final T[] itemArray) {
                        updateItems(itemArray);
                        
                        onComplete.run();
                    }

                    public void onError(Throwable e) {
                        Log.e("ZephyrgramSetActivity",
                              "got error callback in ZephyrgramSetActivity#update",
                              e);
                        
                        onComplete.run();
                    }
                });
            }
        });
    }

    private void updateItems(final T[] itemArray) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                ListView listView = (ListView) findViewById(R.id.list_view);
                ArrayList<T> items = new ArrayList<T>(Arrays.asList(itemArray));
                
                refreshHeaderViews(listView, items);
                final ZephyrgramSetListAdapter<T> listAdapter = getListAdapter(items);
                listView.setAdapter(listAdapter);
                
                listView.setOnItemClickListener(new OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view,
                            int position, long id) {
                        
                        dispatchClick(position, listAdapter);
                    }
                });
                
                LoadFlipper.flipToContent(ZephyrgramSetActivity.this);
            }
        });
    }
}
