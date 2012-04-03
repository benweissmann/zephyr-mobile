package com.benweissmann.zmobile.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.benweissmann.zmobile.service.callbacks.ZephyrCallback;
import com.benweissmann.zmobile.service.callbacks.ZephyrStatusCallback;
import com.benweissmann.zmobile.service.objects.Query;
import com.benweissmann.zmobile.service.objects.ZephyrClass;
import com.benweissmann.zmobile.service.objects.ZephyrInstance;
import com.benweissmann.zmobile.service.objects.ZephyrPersonals;
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
    public static final String XML_RPC_SERVER_URL = "http://bsw.mit.edu:9000";
    public static final int ZEPHYRGRAMS_PER_PAGE = 15;
    
    public static final String USER_NAME = "ME";
    
    private static boolean isRunning = false;
    private final IBinder binder = new ZephyrBinder();
    private XMLRPCClient xmlRpcClient;
    
    /**
     * Class for clients to access. Because we know this service always runs in
     * the same process as its clients, we don't need to deal with IPC.
     */
    public class ZephyrBinder extends Binder {
        public void send(final Zephyrgram zephyrgram, final ZephyrStatusCallback callback) {
            XMLRPCCallback sendCallback = new XMLRPCCallback() {
                public void onResponse(long id, Object result) {
                    boolean response = (Boolean) result;
                    if(response) {
                        callback.onSuccess();
                    }
                    else {
                        callback.onFailure();
                    }
                }
                
                public void onError(long id, XMLRPCException error) {
                    Log.e("ZephyrBinder#send", "xmlrpc exception",
                          error);
                    callback.onError(error);
                }
                
                public void onServerError(long id, XMLRPCServerException error) {
                    Log.e("ZephyrBinder#send",
                          "xmlrpc server exception", error);
                    callback.onError(error);
                }
            };
            
            String message = zephyrgram.getBody();
            String cls = zephyrgram.getCls();
            String instance = zephyrgram.getInstance();
            String user = zephyrgram.getUser();
            
            if(cls == null || cls.equals("")) {
                cls = Zephyrgram.PERSONALS_CLASS;
            }
            
            if(instance == null || instance.equals("")) {
                instance = Zephyrgram.DEFAULT_INSTANCE;
            }
            
            xmlRpcClient.callAsync(sendCallback, "messenger.send", message, cls, instance, user);
        }
        
        public void fetchClasses(final ZephyrCallback<ZephyrClass[]> callback) {
            XMLRPCCallback classesCallback = new XMLRPCCallback() {
                public void onResponse(long id, Object response) {
                    try {
                        Object[] classObjs = (Object[]) response;
                        int length = classObjs.length;
                        
                        ZephyrClass[] classes = new ZephyrClass[length];
                        
                        for (int i = 0; i < length; i++) {
                            Object[] classObj = (Object[]) classObjs[i];
                            String name = (String) classObj[0];
                            Object[] counts = (Object[]) classObj[1];
                            int unreadCount = (Integer) counts[0];
                            int totalCount = (Integer) counts[1];
                            
                            classes[i] = new ZephyrClass(name, unreadCount,
                                                         totalCount);
                        }
                        
                        callback.run(classes);
                    }
                    catch (ClassCastException e) {
                        callback.onError(new MalformedServerResponseException(e));
                        return;
                    }
                }
                
                public void onError(long id, XMLRPCException error) {
                    Log.e("ZephyrBinder#fetchClasses", "xmlrpc exception",
                          error);
                    callback.onError(error);
                }
                
                public void onServerError(long id, XMLRPCServerException error) {
                    Log.e("ZephyrBinder#fetchClasses",
                          "xmlrpc server exception", error);
                    callback.onError(error);
                }
            };
            
            xmlRpcClient.callAsync(classesCallback, "messenger.getClasses");
        }
        
        public void fetchInstances(final String cls,
                                   final ZephyrCallback<ZephyrInstance[]> callback) {
            XMLRPCCallback instancesCallback = new XMLRPCCallback() {
                public void onResponse(long id, Object response) {
                    try {
                        Object[] instanceObjs = (Object[]) response;
                        int length = instanceObjs.length;
                        
                        ZephyrInstance[] instances = new ZephyrInstance[length];
                        
                        for (int i = 0; i < length; i++) {
                            Object[] instanceObj = (Object[]) instanceObjs[i];
                            String name = (String) instanceObj[0];
                            Object[] counts = (Object[]) instanceObj[1];
                            int unreadCount = (Integer) counts[0];
                            int totalCount = (Integer) counts[1];
                            
                            instances[i] = new ZephyrInstance(cls, name,
                                                              unreadCount,
                                                              totalCount);
                        }
                        
                        callback.run(instances);
                    }
                    catch (ClassCastException e) {
                        callback.onError(new MalformedServerResponseException(e));
                        return;
                    }
                }
                
                public void onError(long id, XMLRPCException error) {
                    Log.e("ZephyrBinder#fetchInstances", "xmlrpc exception",
                          error);
                    callback.onError(error);
                }
                
                public void onServerError(long id, XMLRPCServerException error) {
                    Log.e("ZephyrBinder#fetchInstances",
                          "xmlrpc server exception", error);
                    callback.onError(error);
                }
            };
            xmlRpcClient.callAsync(instancesCallback, "messenger.getInstances",
                                   cls);
        }
        
        public void fetchPersonals(final ZephyrCallback<ZephyrPersonals[]> callback) {
            XMLRPCCallback personalsCallback = new XMLRPCCallback() {
                public void onResponse(long id, Object response) {
                    try {
                        Object[] personalsObjs = (Object[]) response;
                        int length = personalsObjs.length;
                        
                        ZephyrPersonals[] personals = new ZephyrPersonals[length];
                        
                        for (int i = 0; i < length; i++) {
                            Object[] personalsObj = (Object[]) personalsObjs[i];
                            String sender = (String) personalsObj[0];
                            Object[] counts = (Object[]) personalsObj[1];
                            int unreadCount = (Integer) counts[0];
                            int totalCount = (Integer) counts[1];
                            
                            personals[i] = new ZephyrPersonals(sender, unreadCount, totalCount);
                        }
                        
                        callback.run(personals);
                    }
                    catch (ClassCastException e) {
                        callback.onError(new MalformedServerResponseException(e));
                        return;
                    }
                }
                
                public void onError(long id, XMLRPCException error) {
                    Log.e("ZephyrBinder#fetchPersonals", "xmlrpc exception",
                          error);
                    callback.onError(error);
                }
                
                public void onServerError(long id, XMLRPCServerException error) {
                    Log.e("ZephyrBinder#fetchPersonals",
                          "xmlrpc server exception", error);
                    callback.onError(error);
                }
            };
            
            xmlRpcClient.callAsync(personalsCallback, "messenger.getPersonals");
        }
        
        public void fetchZephyrgrams(final Query query,
                                     final ZephyrCallback<ZephyrgramResultSet> callback) {
            XMLRPCCallback filterCallback = new XMLRPCCallback() {
                public void onResponse(long id, Object response) {
                    String filterId;
                    try {
                        filterId = (String) response;
                    }
                    catch (ClassCastException e) {
                        callback.onError(new MalformedServerResponseException(e));
                        return;
                    }
                    
                    // return result set for first page
                    ZephyrBinder.this.fetchStartingPage(query, filterId,
                                                        callback);
                }
                
                public void onError(long id, XMLRPCException error) {
                    Log.e("ZephyrBinder#fetchZephyrgrams", "xmlrpc exception",
                          error);
                    callback.onError(error);
                }
                
                public void onServerError(long id, XMLRPCServerException error) {
                    Log.e("ZephyrBinder#fetchZephyrgrams",
                          "xmlrpc server exception", error);
                    callback.onError(error);
                }
                
            };
            
            Map<String, Object> filters = new HashMap<String, Object>();
            
            if (query.getCls() != null) {
                filters.put("cls", query.getCls());
            }
            
            if (query.getInstance() != null) {
                filters.put("instance", query.getInstance());
            }
            
            if (query.getSender() != null) {
                filters.put("sender", query.getSender());
            }
            
            if (query.getText() != null) {
                filters.put("message", query.getText());
            }
            
            xmlRpcClient.callAsync(filterCallback, "messenger.filterMessages",
                                   filters);
        }
        
        public void fetchPrevPage(ZephyrgramResultSet resultSet,
                                  ZephyrCallback<ZephyrgramResultSet> callback) {
            if (resultSet.getOffset() == 0) {
                // if we're at the start, return an empty result
                callback.run(new ZephyrgramResultSet(resultSet.getQuery(),
                                                     resultSet.getFilterId(),
                                                     0,
                                                     new ArrayList<Zephyrgram>()));
                
                return;
            }
            
            int perPage = Math.min(ZEPHYRGRAMS_PER_PAGE, resultSet.getOffset());
            
            this.fetchPage(resultSet.getQuery(), resultSet.getFilterId(),
                           Math.max(0, resultSet.getOffset()- ZEPHYRGRAMS_PER_PAGE),
                           perPage, callback);
        }
        
        public void fetchNextPage(ZephyrgramResultSet resultSet,
                                  ZephyrCallback<ZephyrgramResultSet> callback) {
            this.fetchPage(resultSet.getQuery(), resultSet.getFilterId(),
                           resultSet.getOffset() + resultSet.getPageLength(),
                           ZEPHYRGRAMS_PER_PAGE, callback);
        }
        
        // gets either the page that starts with the most recent
        // unread message, or the page of most recent message if all messages
        // are read
        private void fetchStartingPage(final Query query,
                                       final String filterId,
                                       final ZephyrCallback<ZephyrgramResultSet> callback) {
            XMLRPCCallback offsetCallback = new XMLRPCCallback() {
                public void onResponse(long id, Object response) {
                    Object[] resultArray = (Object[]) response;
                    int offset = (Integer) resultArray[0];
                    int total = (Integer) resultArray[1];
                    
                    if (offset < 0) {
                        // fetch page of most recent
                        fetchPage(query, filterId, total - ZEPHYRGRAMS_PER_PAGE,
                                  ZEPHYRGRAMS_PER_PAGE, callback);
                    }
                    else {
                        fetchPage(query, filterId, offset, ZEPHYRGRAMS_PER_PAGE,
                                  callback);
                    }
                }
                
                public void onError(long id, XMLRPCException error) {
                    Log.e("ZephyrBinder#requestPage", "xmlrpc exception", error);
                    callback.onError(error);
                }
                
                public void onServerError(long id, XMLRPCServerException error) {
                    Log.e("ZephyrBinder#requestPage",
                          "xmlrpc server exception", error);
                    callback.onError(error);
                }
            };
            
            xmlRpcClient.callAsync(offsetCallback,
                                   "messenger.getOldestUnreadOffset", filterId);
        }
        
        private void fetchPage(final Query query,
                               final String filterId,
                               final int offset,
                               final int pageLength,
                               final ZephyrCallback<ZephyrgramResultSet> callback) {
            
            XMLRPCCallback zephyrgramCallback = new XMLRPCCallback() {
                @SuppressWarnings("unchecked")
                public void onResponse(long id, Object response) {
                    Object[] messages;
                    
                    Map<String, Object> responseMap = (Map<String, Object>) response;
                    
                    try {
                        messages = (Object[]) responseMap.get("messages");
                    }
                    catch (ClassCastException e) {
                        callback.onError(new MalformedServerResponseException(e));
                        return;
                    }
                    
                    List<Zephyrgram> zephyrgrams = new ArrayList<Zephyrgram>();
                    
                    for (Object messageObj : messages) {
                        Map<String, Object> message = (Map<String, Object>) messageObj;
                        String cls = (String) message.get("cls");
                        String instance = (String) message.get("instance");
                        String body = (String) message.get("message");
                        String user = (String) message.get("user");
                        String sender = (String) message.get("sender");
                        Boolean read = (Boolean) message.get("read");
                        Date timestamp = (Date) message.get("timestamp");
                        
                        zephyrgrams.add(new Zephyrgram(cls, instance, sender,
                                                       timestamp, read, user,
                                                       body));
                    }
                    
                    ZephyrgramResultSet results = new ZephyrgramResultSet(query,
                                                                          filterId,
                                                                          offset,
                                                                          zephyrgrams);
                    callback.run(results);
                }
                
                public void onError(long id, XMLRPCException error) {
                    Log.e("ZephyrBinder#requestPage", "xmlrpc exception", error);
                    callback.onError(error);
                }
                
                public void onServerError(long id, XMLRPCServerException error) {
                    Log.e("ZephyrBinder#requestPage",
                          "xmlrpc server exception", error);
                    callback.onError(error);
                }
            };
            
            xmlRpcClient.callAsync(zephyrgramCallback, "messenger.get",
                                   filterId, offset, pageLength);
        }
    }
    
    @Override
    public void onCreate() {
        ZephyrService.isRunning = true;
        try {
            this.xmlRpcClient = new XMLRPCClient(new URL(XML_RPC_SERVER_URL),
                                                 XMLRPCClient.FLAGS_NIL);
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
