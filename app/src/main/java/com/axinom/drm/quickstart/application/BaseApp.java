package com.axinom.drm.quickstart.application;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;


public class BaseApp extends Application {

    public static RequestQueue requestQueue;

    @Override
    public void onCreate() {
        super.onCreate();
        setInstance(this);

        requestQueue = Volley.newRequestQueue(this);
        
        hasConnection(getApplicationContext());
    }

    public static void cancelAllRequestForTag(final Object tag) {
        BaseApp.requestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return tag.equals(request.getTag());
            }
        });
    }

    private static BaseApp application;
    private static void setInstance(BaseApp app) {
        application = app;
    }
    public static BaseApp instance(){
        return application;
    }
    
    public static boolean hasConnection = false;

    public static boolean hasConnection(Context cntx){
        hasConnection = IsNetworkAvailable(cntx);
        return hasConnection;
    }

    public static boolean IsNetworkAvailable(Context c) {
        ConnectivityManager cm = (ConnectivityManager) c
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return (info != null);
    }
}
