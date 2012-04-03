package com.benweissmann.zmobile;

import java.util.ArrayList;
import java.util.List;

import com.benweissmann.zmobile.components.ListHeader;
import com.benweissmann.zmobile.components.ListHeader.Breadcrumb;
import com.benweissmann.zmobile.components.LoadFlipper;
import com.benweissmann.zmobile.listadapters.ZephyrgramListAdapter;
import com.benweissmann.zmobile.service.ZephyrService;
import com.benweissmann.zmobile.service.ZephyrServiceBridge;
import com.benweissmann.zmobile.service.ZephyrService.ZephyrBinder;
import com.benweissmann.zmobile.service.callbacks.BinderCallback;
import com.benweissmann.zmobile.service.callbacks.ZephyrCallback;
import com.benweissmann.zmobile.service.objects.Query;
import com.benweissmann.zmobile.service.objects.Zephyrgram;
import com.benweissmann.zmobile.service.objects.ZephyrgramResultSet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ZephyrgramActivity extends Activity {
    public static final String QUERY_EXTRA = "zephyrgram_activity_query";

    private Query query = null;
    private ZephyrgramResultSet startResultSet = null;
    private ZephyrgramResultSet endResultSet = null;
    private boolean fetching = false;
    private ArrayList<Zephyrgram> zephyrgrams = null;
    private ZephyrgramListAdapter adapter = null;
    
    private boolean autoloadNext = false;
    private boolean autoloadPrev = false;
    
    private boolean atEnd = false;
    
    private static final int PREV_VIEW_HEIGHT_DP = 30;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            this.query = (Query) extras.getSerializable(QUERY_EXTRA);
        }

        if (this.query == null) {
            throw new RuntimeException(
                    "InstanceListActivity didn't get a query");
        }
        
        // add breadcrumbs
        ListHeader.populate(ZephyrgramActivity.this, getBreadcrumbs());

        getFirstPage();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.zephyrgrams_list_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.zephyrgrams_list_menu_compose:
            Intent intent = new Intent(this, ComposeActivity.class);
            
            if(this.query.getCls() != null && this.query.getCls().equals(Zephyrgram.PERSONALS_CLASS)) {
                intent.putExtra(ComposeActivity.SELECT_PERSONAL_EXTRA, true);
                
                if(this.query.getSender() != null) {
                    intent.putExtra(ComposeActivity.PERSONAL_TO_EXTRA, this.query.getSender());
                }
            }
            else {
                if(this.query.getCls() != null) {
                    intent.putExtra(ComposeActivity.CLASS_EXTRA, this.query.getCls());
                }
                
                if(this.query.getInstance() != null) {
                    intent.putExtra(ComposeActivity.INSTANCE_EXTRA, this.query.getInstance());
                }
            }
            
            startActivityForResult(intent, 0);
            return true;
        case R.id.zephyrgrams_list_menu_feedback:
            ComposeActivity.launchFeedback(this);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onRestart() {
        super.onRestart();
        
        if(this.atEnd) {
            this.getNextPage();
        }
    }

    private void getFirstPage() {
        if (this.fetching) {
            return;
        }
        this.fetching = true;
        
        LoadFlipper.flipToLoader(this);

        ZephyrServiceBridge.getBinder(this, new BinderCallback() {
            public void run(ZephyrBinder binder, final Runnable onComplete) {
                binder.fetchZephyrgrams(ZephyrgramActivity.this.query,
                        new ZephyrCallback<ZephyrgramResultSet>() {
                            public void run(final ZephyrgramResultSet result) {
                                ZephyrgramActivity.this.startResultSet = result;
                                ZephyrgramActivity.this.endResultSet = result;

                                ZephyrgramActivity.this.initList(result
                                        .getZephyrgrams());
                                
                                onComplete.run();
                            }

                            public void onError(Throwable e) {
                                Log.e("ZephyrgramActivity",
                                        "got error callback in ZephyrgramActivity#getFirstPage",
                                        e);
                                
                                onComplete.run();
                            }
                        });
            }
        });
    }
    
    private void initList(final List<Zephyrgram> initialZephyrgrams) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                ListView listView = (ListView) findViewById(R.id.list_view);
                
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                
                // add next page
                listView.addFooterView(vi.inflate(R.layout.next_zephyrgrams_list_item, null));
                
                // add prev page
                listView.addHeaderView(vi.inflate(R.layout.prev_zephyrgrams_list_item, null));
                
                zephyrgrams = new ArrayList<Zephyrgram>(initialZephyrgrams);
                
                adapter = new ZephyrgramListAdapter(ZephyrgramActivity.this, zephyrgrams);
                
                listView.setDivider(null);
                listView.setAdapter(adapter);
                
                if(!initialZephyrgrams.isEmpty() && initialZephyrgrams.get(0).isRead()) {
                    // if the first zephyr is read, they're all read, so scroll
                    // to the bottom.
                    listView.setSelectionFromTop(adapter.getCount()+1, 0);
                }
                
                if(initialZephyrgrams.size() < ZephyrService.ZEPHYRGRAMS_PER_PAGE) {
                    // we're underfull, so we must be at the end
                    atEnd = true;
                }
                
                listView.setOnItemClickListener(new ListView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view,
                            int position, long id) {
                        
                        if(position == 0) {
                            // prev page button
                            getPrevPage();
                        }
                        else if(position == adapter.getCount()+1) {
                            // next page button
                            getNextPage();
                        }
                        else {
                            ZephyrgramActivity.this.onItemClick(zephyrgrams.get(position-1));
                        }
                    }
                });
                
                listView.setOnScrollListener(new OnScrollListener() {
                    
                    public void onScroll(AbsListView view, int firstVisibleItem,
                                         int visibleItemCount, int totalItemCount) {
                        
                        if(firstVisibleItem == 0) {
                            if(autoloadPrev) {
                                getPrevPage();
                            }
                            autoloadNext = true;
                            autoloadPrev = false;
                        }
                        else if(firstVisibleItem + visibleItemCount >= totalItemCount) {
                            if(autoloadNext) {
                                getNextPage();
                            }
                            autoloadNext = false;
                            autoloadPrev = true;
                        }
                        else {
                            autoloadNext = true;
                            autoloadPrev = true;
                        }
                        
                    }
                
                    public void onScrollStateChanged(AbsListView view, int scrollState) {
                    }
                });
                
                LoadFlipper.flipToContent(ZephyrgramActivity.this);
                
                ZephyrgramActivity.this.fetching = false;
            }
        });
    }

    private void getNextPage() {
        if (this.fetching) {
            return;
        }
        this.fetching = true;
        
        this.runOnUiThread(new Runnable() {
            public void run() {
                ProgressBar spinner = (ProgressBar) findViewById(R.id.next_zephyrgrams_spinner);
                spinner.setVisibility(View.VISIBLE);
                
                TextView nextLabel = (TextView) findViewById(R.id.next_zephyrgrams_item);
                nextLabel.setText(getString(R.string.next_zephyrgrams_loading));
            }
        });

        ZephyrServiceBridge.getBinder(this, new BinderCallback() {
            public void run(ZephyrBinder binder, final Runnable onComplete) {
                binder.fetchNextPage(ZephyrgramActivity.this.endResultSet,
                        new ZephyrCallback<ZephyrgramResultSet>() {
                            public void run(final ZephyrgramResultSet result) {
                                ZephyrgramActivity.this.appendAllToEnd(result);
                                onComplete.run();
                            }

                            public void onError(Throwable e) { 
                                Log.e("ZephyrgramActivity",
                                        "got error callback in ZephyrgramActivity#getNextPage",
                                        e);
                                onComplete.run();
                            }
                        });
            }
        });
    }

    private void getPrevPage() {
        if (this.fetching) {
            return;
        }
        this.fetching = true;
        
        this.runOnUiThread(new Runnable() {
            public void run() {
                ProgressBar spinner = (ProgressBar) findViewById(R.id.prev_zephyrgrams_spinner);
                spinner.setVisibility(View.VISIBLE);
                
                TextView prevLabel = (TextView) findViewById(R.id.prev_zephyrgrams_item);
                prevLabel.setText(getString(R.string.prev_zephyrgrams_loading));
            }
        });

        ZephyrServiceBridge.getBinder(this, new BinderCallback() {
            public void run(ZephyrBinder binder, final Runnable onComplete) {
                binder.fetchPrevPage(ZephyrgramActivity.this.startResultSet,
                        new ZephyrCallback<ZephyrgramResultSet>() {
                            public void run(final ZephyrgramResultSet result) {
                                ZephyrgramActivity.this.appendAllToStart(result);
                                onComplete.run();
                            }

                            public void onError(Throwable e) {
                                Log.e("ZephyrgramActivity",
                                        "got error callback in ZephyrgramActivity#getPrevPage",
                                        e);
                                onComplete.run();
                            }
                        });
            }
        });
    }

    private void appendAllToEnd(final ZephyrgramResultSet resultSet) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                if(resultSet.getPageLength() > 0) {
                    zephyrgrams.addAll(resultSet.getZephyrgrams());
                    adapter.notifyDataSetChanged();
                    endResultSet = resultSet;
                    atEnd = false;
                }
                else {
                    CharSequence text = getString(R.string.no_next_zephyrgrams);
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(ZephyrgramActivity.this, text, duration);
                    toast.setGravity(Gravity.BOTTOM|Gravity.CENTER, 0, 0);
                    toast.show();
                    
                    // we're at the end
                    atEnd = true;
                }
                
                ZephyrgramActivity.this.fetching = false;
                
                ProgressBar spinner = (ProgressBar) findViewById(R.id.next_zephyrgrams_spinner);
                spinner.setVisibility(View.INVISIBLE);
                
                TextView nextLabel = (TextView) findViewById(R.id.next_zephyrgrams_item);
                nextLabel.setText(getString(R.string.next_zephyrgrams));
            }
        });

    }

    private void appendAllToStart(final ZephyrgramResultSet resultSet) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                if(resultSet.getPageLength() > 0) {
                    zephyrgrams.addAll(0, resultSet.getZephyrgrams());
                    adapter.notifyDataSetChanged();
                    ListView list = (ListView) findViewById(R.id.list_view);
                    list.setSelectionFromTop(resultSet.getZephyrgrams().size() + 1, getPrevViewHeight());
                    startResultSet = resultSet;
                }
                else {
                    CharSequence text = getString(R.string.no_prev_zephyrgrams);
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(ZephyrgramActivity.this, text, duration);
                    toast.setGravity(Gravity.TOP|Gravity.CENTER, 0, 0);
                    toast.show();
                }
                
                ZephyrgramActivity.this.fetching = false;
                
                ProgressBar spinner = (ProgressBar) findViewById(R.id.prev_zephyrgrams_spinner);
                spinner.setVisibility(View.INVISIBLE);
                
                TextView prevLabel = (TextView) findViewById(R.id.prev_zephyrgrams_item);
                prevLabel.setText(getString(R.string.prev_zephyrgrams));
            }
        });

    }
    
    private List<Breadcrumb> getBreadcrumbs() {
        List<Breadcrumb> breadcrumbs = new ArrayList<Breadcrumb>();
        
        if(this.query.getCls() == null) {
            // we're in Home -> All
            breadcrumbs.add(new Breadcrumb(getString(R.string.all_label), null));
        }
        else if(this.query.getCls().equals(Zephyrgram.PERSONALS_CLASS)) {
            // we're in Home -> Personals
            breadcrumbs.add(new Breadcrumb(getString(R.string.personal_label),
                                               PersonalsListActivity.class));
            
            if(this.query.getSender() == null) {
                // we're in Home -> Personals -> All
                breadcrumbs.add(new Breadcrumb(getString(R.string.all_label), null));
            }
            else {
                // we're in Home -> Personals -> <sender>
                breadcrumbs.add(new Breadcrumb(this.query.getSender(), null));
            }
        }
        else {
            // we're in Home -> <class>
            breadcrumbs.add(new Breadcrumb(this.query.getCls(), InstanceListActivity.class));
            
            if(this.query.getInstance() == null) {
                // we're in Home -> <class> -> All
                breadcrumbs.add(new Breadcrumb(getString(R.string.all_label), null));
            }
            else {
                // we're in Home -> <class> -> <instance>
                breadcrumbs.add(new Breadcrumb(this.query.getInstance(), null));
            }
        }
        
        return breadcrumbs;
    }
    
    private int getPrevViewHeight() {
        Resources r = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                                    PREV_VIEW_HEIGHT_DP,
                                                    r.getDisplayMetrics()));
    }
    
    private void onItemClick(Zephyrgram z) {
        Intent intent = new Intent(this, ComposeActivity.class);
            
        if(z.isPersonal()) {
            intent.putExtra(ComposeActivity.SELECT_PERSONAL_EXTRA, true);
            
            if(z.isFromMe()) {
                intent.putExtra(ComposeActivity.PERSONAL_TO_EXTRA, z.getUser());
            }
            else {
                intent.putExtra(ComposeActivity.PERSONAL_TO_EXTRA, z.getSender());
            }
        }
        else {
            intent.putExtra(ComposeActivity.CLASS_EXTRA, z.getCls());
            intent.putExtra(ComposeActivity.INSTANCE_EXTRA, z.getInstance());
            intent.putExtra(ComposeActivity.PERSONAL_TO_EXTRA, z.getSender());
        }
        
            
        startActivityForResult(intent, 0);
    }
}
