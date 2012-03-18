package com.benweissmann.zmobile.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.benweissmann.zmobile.service.callbacks.ZephyrCallback;
import com.benweissmann.zmobile.service.objects.Query;
import com.benweissmann.zmobile.service.objects.ZephyrClass;
import com.benweissmann.zmobile.service.objects.ZephyrgramResultSet;
import com.benweissmann.zmobile.service.objects.Zephyrgram;

import de.timroes.axmlrpc.XMLRPCCallback;
import de.timroes.axmlrpc.XMLRPCClient;
import de.timroes.axmlrpc.XMLRPCException;
import de.timroes.axmlrpc.XMLRPCServerException;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class ZephyrService extends Service {
    public static final String XML_RPC_SERVER_URL = "http://10.0.2.2:9000";
    public static final int ZEPHYRGRAMS_PER_PAGE = 2;

    private static boolean isRunning = false;
    private final IBinder binder = new ZephyrBinder();
    private XMLRPCClient xmlRpcClient;

    /**
     * Class for clients to access. Because we know this service always runs in
     * the same process as its clients, we don't need to deal with IPC.
     */
    public class ZephyrBinder extends Binder {
        public void fetchClasses(final ZephyrCallback<ZephyrClass[]> callback) {
            XMLRPCCallback classesCallback = new XMLRPCCallback() {
                public void onResponse(long id, Object response) {
                    try {
                        Object[] classObjs = (Object[]) response;
                        int length = classObjs.length;
                        
                        ZephyrClass[] classes = new ZephyrClass[length];
                        
                        for(int i = 0; i < length; i++) {
                            Object[] classObj = (Object[]) classObjs[i];
                            String name = (String) classObj[0];
                            Object[] counts = (Object[]) classObj[1];
                            int unreadCount = (Integer) counts[0];
                            int readCount = (Integer) counts[1];
                            
                            classes[i] = new ZephyrClass(name, unreadCount, readCount);
                        }
                        
                        callback.run(classes);
                    }
                    catch (ClassCastException e) {
                        callback.onError(new MalformedServerResponseException(e));
                        return;
                    }
                }

                public void onError(long id, XMLRPCException error) {
                    Log.e("ZephyrBinder#fetchClasses", "xmlrpc exception", error);
                    callback.onError(error);
                }

                public void onServerError(long id, XMLRPCServerException error) {
                    Log.e("ZephyrBinder#fetchClasses", "xmlrpc server exception", error);
                    callback.onError(error);
                }
            };
            
            xmlRpcClient.callAsync(classesCallback, "messenger.getClasses");
        }
        
        public void fetchZephyrgrams(final Query query, final ZephyrCallback<ZephyrgramResultSet> callback) {
            XMLRPCCallback filterCallback = new XMLRPCCallback() {
                public void onResponse(long id, Object response) {
                    String filterId;
                    try {
                        filterId = (String) ((Object[]) response)[0];
                    }
                    catch (ClassCastException e) {
                        callback.onError(new MalformedServerResponseException(e));
                        return;
                    }

                    // return result set for first page
                    ZephyrBinder.this.fetchPage(query, filterId, 0, callback);
                }

                public void onError(long id, XMLRPCException error) {
                    Log.e("ZephyrBinder#fetchZephyrgrams", "xmlrpc exception", error);
                    callback.onError(error); 
                }

                public void onServerError(long id, XMLRPCServerException error) {
                    Log.e("ZephyrBinder#fetchZephyrgrams", "xmlrpc server exception", error);
                    callback.onError(error);
                }

            };
            
            Map<String, Object> filters = new HashMap<String, Object>();
            
            if(query.getCls() != null) {
                filters.put("cls", query.getCls());
            }
            
            if(query.getInstance() != null) {
                filters.put("instance", query.getInstance());
            }
            
            if(query.getSender() != null) {
                filters.put("sender", query.getSender());
            }
            
            if(query.getText() != null) {
                filters.put("message", query.getText());
            }

            xmlRpcClient.callAsync(filterCallback, "messenger.filterMessages", filters);
        }
        
        public void fetchNextPage(ZephyrgramResultSet resultSet, ZephyrCallback<ZephyrgramResultSet> callback) {
            this.fetchPage(resultSet.getQuery(), resultSet.getFilterId(), resultSet.getPage() + 1, callback);
        }
        
        public void fetchPrevPage(ZephyrgramResultSet resultSet, ZephyrCallback<ZephyrgramResultSet> callback) {
            this.fetchPage(resultSet.getQuery(), resultSet.getFilterId(), resultSet.getPage() - 1, callback);
        }
        
        public void fetchPage(ZephyrgramResultSet resultSet, int page, ZephyrCallback<ZephyrgramResultSet> callback) {
            this.fetchPage(resultSet.getQuery(), resultSet.getFilterId(), page, callback);
        }

        private void fetchPage(final Query query, final String filterId, final int page, final ZephyrCallback<ZephyrgramResultSet> callback) {
            
            XMLRPCCallback zephyrgramCallback = new XMLRPCCallback() {
                @SuppressWarnings("unchecked")
                public void onResponse(long id, Object response) {
                    Object[] messages;
                    int resultLength;
                    
                    Map<String, Object> responseMap = (Map<String, Object>) response;
                    
                    try {
                        messages = (Object[]) responseMap.get("messages");
                        resultLength = (Integer) responseMap.get("count");
                    }
                    catch (ClassCastException e) {
                        callback.onError(new MalformedServerResponseException(e));
                        return;
                    }
                    
                    List<Zephyrgram> zephyrgrams = new ArrayList<Zephyrgram>();
                    
                    for(Object messageObj : messages) {
                        Map<String, Object> message = (Map<String, Object>) messageObj;
                        String cls = (String) message.get("cls");
                        String instance = (String) message.get("instance");
                        String body = (String) message.get("message");
                        String user = (String) message.get("user");
                        String sender = (String) message.get("sender");
                        Boolean read = (Boolean) message.get("read");
                        
                        Double secondsSinceEpoch = (Double) message.get("timestamp");
                        Date timestamp = new Date(secondsSinceEpoch.longValue() * 1000L);
                        
                        zephyrgrams.add(new Zephyrgram(cls, instance, sender, timestamp, read, user, body));
                    }
                    
                    ZephyrgramResultSet results = new ZephyrgramResultSet(query, filterId, page, resultLength, zephyrgrams);
                    callback.run(results);
                }

                public void onError(long id, XMLRPCException error) {
                    Log.e("ZephyrBinder#requestPage", "xmlrpc exception", error);
                    callback.onError(error);
                }

                public void onServerError(long id, XMLRPCServerException error) {
                    Log.e("ZephyrBinder#requestPage", "xmlrpc server exception", error);
                    callback.onError(error);
                }
            };
            
            xmlRpcClient.callAsync(zephyrgramCallback, "messenger.get", filterId, page*ZEPHYRGRAMS_PER_PAGE, ZEPHYRGRAMS_PER_PAGE);
        }
    }

    @Override
    public void onCreate() {
        ZephyrService.isRunning = true;
        try {
            this.xmlRpcClient = new XMLRPCClient(new URL(XML_RPC_SERVER_URL), XMLRPCClient.FLAGS_NIL);
        }
        catch (MalformedURLException e) {
            Log.e("ZephyrService", "Failed to create URL for XML RPC server", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("ZephyrService", "Received start id " + startId + ": " + intent);

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        ZephyrService.isRunning = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public static boolean isRunning() {
        return ZephyrService.isRunning;
    }
}
