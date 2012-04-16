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
import com.benweissmann.zmobile.service.objects.IQuery;
import com.benweissmann.zmobile.service.objects.Query;
import com.benweissmann.zmobile.service.objects.ZephyrClass;
import com.benweissmann.zmobile.service.objects.ZephyrInstance;
import com.benweissmann.zmobile.service.objects.ZephyrPersonals;
import com.benweissmann.zmobile.service.objects.ZephyrgramResultSet;
import com.benweissmann.zmobile.service.objects.Zephyrgram;
import com.benweissmann.zmobile.util.TextWrapper;

import de.timroes.axmlrpc.XMLRPCCallback;
import de.timroes.axmlrpc.XMLRPCClient;
import de.timroes.axmlrpc.XMLRPCException;
import de.timroes.axmlrpc.XMLRPCServerException;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class ZephyrService extends Service {
    // When in the emulator, 10.0.2.2 is the host machine
    public static final String XML_RPC_SERVER_URL = "https://linerva.mit.edu:49990";
    public static final int ZEPHYRGRAMS_PER_PAGE = 15;
    
    public static final String USER_NAME = "bsw";
    public static final String HOME_DOMAIN = "ATHENA.MIT.EDU";
    
    private static boolean isRunning = false; 
    private final IBinder binder = new ZephyrBinder();
    private XMLRPCHelper xmlRpcClient;
    
    /**
     * Class for clients to access. Because we know this service always runs in
     * the same process as its clients, we don't need to deal with IPC.
     */
    public class ZephyrBinder extends Binder {
        public void send(Activity activity, final Zephyrgram zephyrgram, final ZephyrStatusCallback callback) {
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
            
            String message = TextWrapper.wrap(zephyrgram.getBody());
            String cls = zephyrgram.getCls();
            String instance = zephyrgram.getInstance();
            String user = zephyrgram.getUser();
            
            if(cls == null || cls.equals("")) {
                cls = Zephyrgram.PERSONALS_CLASS;
            }
            
            if(instance == null || instance.equals("")) {
                instance = Zephyrgram.DEFAULT_INSTANCE;
            }
            
            xmlRpcClient.callAsync(activity, sendCallback, "messenger.send", message, cls, instance, user);
        }
        
        public void fetchClasses(Activity activity, final ZephyrCallback<ZephyrClass[]> callback) {
            XMLRPCCallback classesCallback = new XMLRPCCallback() {
                public void onResponse(long id, Object response) {
                    try {
                        Object[] classObjs = (Object[]) response;
                        int length = classObjs.length;
                        
                        ZephyrClass[] classes = new ZephyrClass[length];
                        
                        for (int i = 0; i < length; i++) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> classObj = (Map<String, Object>) classObjs[i];
                            String name = (String) classObj.get("cls");
                            int unreadCount = (Integer) classObj.get("unread");
                            int totalCount = (Integer) classObj.get("total");
                            boolean starred = (Boolean) classObj.get("starred");
                            
                            classes[i] = new ZephyrClass(name, unreadCount,
                                                         totalCount, starred);
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
            
            xmlRpcClient.callAsync(activity, classesCallback, "messenger.getClasses");
        }
        
        public void fetchInstances(Activity activity, final String cls,
                                   final ZephyrCallback<ZephyrInstance[]> callback) {
            XMLRPCCallback instancesCallback = new XMLRPCCallback() {
                public void onResponse(long id, Object response) {
                    try {
                        Object[] instanceObjs = (Object[]) response;
                        int length = instanceObjs.length;
                        
                        ZephyrInstance[] instances = new ZephyrInstance[length];
                        
                        for (int i = 0; i < length; i++) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> instanceObj = (Map<String, Object>) instanceObjs[i];
                            String name = (String) instanceObj.get("instance");
                            int unreadCount = (Integer) instanceObj.get("unread");
                            int totalCount = (Integer) instanceObj.get("total");
                            
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
            xmlRpcClient.callAsync(activity, instancesCallback,
                                   "messenger.getInstances", cls);
        }
        
        public void fetchPersonals(Activity activity, final ZephyrCallback<ZephyrPersonals[]> callback) {
            XMLRPCCallback personalsCallback = new XMLRPCCallback() {
                public void onResponse(long id, Object response) {
                    try {
                        Object[] personalsObjs = (Object[]) response;
                        int length = personalsObjs.length;
                        
                        ZephyrPersonals[] personals = new ZephyrPersonals[length];
                        
                        for (int i = 0; i < length; i++) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> personalsObj = (Map<String, Object>) personalsObjs[i];
                            String sender = (String) personalsObj.get("sender");
                            int unreadCount = (Integer) personalsObj.get("unread");
                            int totalCount = (Integer) personalsObj.get("total");
                            
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
            
            xmlRpcClient.callAsync(activity, personalsCallback,
                                   "messenger.getPersonals");
        }
        
        public void fetchZephyrgrams(final Activity activity, final IQuery query,
                                     final ZephyrCallback<ZephyrgramResultSet> callback) {
            this.fetchFilterId(activity, query, new ZephyrCallback<String>() {

                public void run(String filterId) {
                    ZephyrBinder.this.fetchStartingPage(activity, query, filterId,
                                                        callback);
                }

                public void onError(Exception e) {
                    Log.e("ZephyrBinder#fetchZephyrgrams",
                          "onError", e);
                    callback.onError(e);
                }
            });
        }
        
        private void fetchFilterId(Activity activity, IQuery query, 
                                   final ZephyrCallback<String> callback) {
            
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
                    callback.run(filterId);
                }
                
                public void onError(long id, XMLRPCException error) {
                    Log.e("ZephyrBinder#fetchFilterId", "xmlrpc exception",
                          error);
                    callback.onError(error);
                }
                
                public void onServerError(long id, XMLRPCServerException error) {
                    Log.e("ZephyrBinder#fetchFilterId",
                          "xmlrpc server exception", error);
                    callback.onError(error);
                }
                
            };
            
            Query[] clauses = query.queryArray();
            Object[] clauseMaps = new Object[clauses.length];
            
            for(int i = 0; i < clauses.length; i++) {
                clauseMaps[i] = this.makeFilterMap(clauses[i]);
            }
            
            xmlRpcClient.callAsync(activity, filterCallback,
                                   "messenger.filterMessages", clauseMaps);
        }
        
        private Map<String, Object> makeFilterMap(Query query) {
            Log.i("ZephyrService", query.toString());
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
            
            if (query.getUser() != null) {
                Log.i("ZephyrService", "putting in user " + query.getUser());
                filters.put("user", query.getUser());
            }
            
            return filters;
        }
        
        public void fetchPrevPage(Activity activity, ZephyrgramResultSet resultSet,
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
            
            this.fetchPage(activity, resultSet.getQuery(), resultSet.getFilterId(),
                           Math.max(0, resultSet.getOffset()- ZEPHYRGRAMS_PER_PAGE),
                           perPage, callback);
        }
        
        public void fetchNextPage(Activity activity, ZephyrgramResultSet resultSet,
                                  ZephyrCallback<ZephyrgramResultSet> callback) {
            Log.i("ZephyrService offset", ""+resultSet.getOffset());
            Log.i("ZephyrService pageLength", ""+resultSet.getPageLength());
            Log.i("ZephyrService per page", ""+ZEPHYRGRAMS_PER_PAGE);
            
            this.fetchPage(activity, resultSet.getQuery(), resultSet.getFilterId(),
                           resultSet.getOffset() + resultSet.getPageLength(),
                           ZEPHYRGRAMS_PER_PAGE, callback);
        }
        
        // gets either the page that starts with the most recent
        // unread message, or the page of most recent message if all messages
        // are read
        private void fetchStartingPage(final Activity activity, 
                                       final IQuery query,
                                       final String filterId,
                                       final ZephyrCallback<ZephyrgramResultSet> callback) {
            XMLRPCCallback offsetCallback = new XMLRPCCallback() {
                public void onResponse(long id, Object response) {
                    Object[] resultArray = (Object[]) response;
                    int offset = (Integer) resultArray[0];
                    int total = (Integer) resultArray[1];
                    
                    if (offset < 0) {
                        // fetch page of most recent
                        fetchPage(activity, query, filterId,
                                  Math.max(0, total - ZEPHYRGRAMS_PER_PAGE),
                                  ZEPHYRGRAMS_PER_PAGE, callback);
                    }
                    else {
                        fetchPage(activity, query, filterId, offset,
                                  ZEPHYRGRAMS_PER_PAGE, callback);
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
            
            xmlRpcClient.callAsync(activity, offsetCallback,
                                   "messenger.getOldestUnreadOffset", filterId);
        }
        
        private void fetchPage(Activity activity, 
                               final IQuery query,
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
                        String body = TextWrapper.unwrap((String) message.get("message"));
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
            
            xmlRpcClient.callAsync(activity, zephyrgramCallback, "messenger.get",
                                   filterId, offset, pageLength);
        }
        
        public void starClass(Activity activity, final String cls,
                              final ZephyrStatusCallback callback) {
            XMLRPCCallback starCallback = new XMLRPCCallback() {
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
                    Log.e("ZephyrBinder#starClass", "xmlrpc exception",
                          error);
                    callback.onError(error);
                }
                
                public void onServerError(long id, XMLRPCServerException error) {
                    Log.e("ZephyrBinder#starClass",
                          "xmlrpc server exception", error);
                    callback.onError(error);
                }
            };
            
            xmlRpcClient.callAsync(activity, starCallback,
                                   "preferences.starClass", cls);
        }
        
        public void unstarClass(Activity activity, final String cls,
                                final ZephyrStatusCallback callback) {
            XMLRPCCallback unstarCallback = new XMLRPCCallback() {
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
                    Log.e("ZephyrBinder#unstarClass", "xmlrpc exception",
                          error);
                    callback.onError(error);
                }
                
                public void onServerError(long id, XMLRPCServerException error) {
                    Log.e("ZephyrBinder#unstarClass",
                          "xmlrpc server exception", error);
                    callback.onError(error);
                }
            };
            
            xmlRpcClient.callAsync(activity, unstarCallback,
                                   "preferences.unstarClass", cls);
        }
        
        public void hideClass(Activity activity, final String cls,
                              final ZephyrStatusCallback callback) {
            XMLRPCCallback hideCallback = new XMLRPCCallback() {
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
                    Log.e("ZephyrBinder#hideClass", "xmlrpc exception",
                          error);
                    callback.onError(error);
                }
                
                public void onServerError(long id, XMLRPCServerException error) {
                    Log.e("ZephyrBinder#hideClass",
                          "xmlrpc server exception", error);
                    callback.onError(error);
                }
            };
            
            xmlRpcClient.callAsync(activity, hideCallback,
                                   "preferences.hideClass", cls);
        }
        
        public void markRead(Activity activity, ZephyrgramResultSet resultSet,
                             final ZephyrStatusCallback callback) {
            
            this.markFilterRead(activity, resultSet.getFilterId(),
                                resultSet.getOffset(), resultSet.getPageLength(),
                                callback);
        }
        
        public void markRead(final Activity activity, IQuery query,
                             final ZephyrStatusCallback callback) {
            
            this.fetchFilterId(activity, query, new ZephyrCallback<String>() {

                public void run(String filterId) {
                    markFilterRead(activity, filterId, 0, -1, callback);
                }

                public void onError(Exception e) {
                    Log.e("ZephyrBinder#markRead", "onError", e);
                    callback.onError(e);
                }
                
            });
        }
        
        private void markFilterRead(Activity activity, String filterId, int offset, int limit,
                                    final ZephyrStatusCallback callback) {
            XMLRPCCallback markCallback = new XMLRPCCallback() {
                public void onResponse(long id, Object result) {
                    @SuppressWarnings("unused")
                    Integer retVal = (Integer) result;
                    callback.onSuccess();
                }
                
                public void onError(long id, XMLRPCException error) {
                    Log.e("ZephyrBinder#markRead", "xmlrpc exception", error);
                    callback.onError(error);
                }
                
                public void onServerError(long id, XMLRPCServerException error) {
                    Log.e("ZephyrBinder#markRead", "xmlrpc server exception",
                          error);
                    callback.onError(error);
                }
            };
            
            xmlRpcClient.callAsync(activity, markCallback, "messenger.markFilterRead",
                                   filterId, offset, limit);
        }
    }
    
    @Override
    public void onCreate() {
        ZephyrService.isRunning = true;
        try {
            XMLRPCClient client = new XMLRPCClient(new URL(XML_RPC_SERVER_URL),
                                                   XMLRPCClient.FLAGS_NIL |
                                                   XMLRPCClient.FLAGS_SSL_IGNORE_INVALID_CERT |
                                                   XMLRPCClient.FLAGS_SSL_IGNORE_INVALID_HOST);
            this.xmlRpcClient = new XMLRPCHelper(client);
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
