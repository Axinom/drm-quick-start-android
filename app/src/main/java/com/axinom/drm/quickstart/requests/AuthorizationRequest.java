//package com.axinom.drm.quickstart.requests;
//
//import com.android.volley.Response;
//import com.android.volley.RetryPolicy;
//import com.android.volley.VolleyError;
//import com.axinom.drm.quickstart.models.LicenseToken;
//
//import java.io.IOException;
//
//// Volley request for movies feed loading.
//public class AuthorizationRequest extends BaseJsonRequest<LicenseToken> {
//
//    private  static final RetryPolicy mRetryPolicy = new RetryPolicy() {
//        @Override public int getCurrentTimeout() {
//            return 5000;
//        }
//
//        @Override public int getCurrentRetryCount() {
//            return 0;
//        }
//
//        @Override public void retry(VolleyError error) throws VolleyError { }
//    };
//
//    public AuthorizationRequest(String url, Response.Listener<LicenseToken> listener, Response.ErrorListener errorListener) {
//        super(url, LicenseToken.class, listener, errorListener);
//        setRetryPolicy(mRetryPolicy);
//    }
//
//    @Override
//    protected LicenseToken handleResponseModel(LicenseToken model) throws IOException {
//        if(model != null) return model;
//        return null;
//    }
//}

package com.axinom.drm.quickstart.requests;

import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;

import java.io.IOException;

// Volley request for receiving license token.
public class AuthorizationRequest extends BaseJsonRequest<String> {

    private  static final RetryPolicy mRetryPolicy = new RetryPolicy() {
        @Override public int getCurrentTimeout() {
            return 5000;
        }

        @Override public int getCurrentRetryCount() {
            return 0;
        }

        @Override public void retry(VolleyError error) throws VolleyError { }
    };

    public AuthorizationRequest(String url, Response.Listener<String> listener, Response.ErrorListener errorListener) {
        super(url, String.class, listener, errorListener);
        setRetryPolicy(mRetryPolicy);
    }

    @Override
    protected String handleResponseModel(String model) throws IOException {
        if(model != null) return model;
        return null;
    }
}