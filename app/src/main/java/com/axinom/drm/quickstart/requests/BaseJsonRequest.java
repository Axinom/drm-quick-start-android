package com.axinom.drm.quickstart.requests;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.axinom.drm.quickstart.BuildConfig;
import com.axinom.drm.quickstart.requests.additional.NoRetryPolicy;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class BaseJsonRequest<T> extends Request<T> {

    private static final String TAG = BaseJsonRequest.class.getSimpleName();
    public static final String CONTENT_TYPE = "application/json;charset=UTF-8";
    private static Map<String, String> headers;

    protected final ObjectMapper mapper = new ObjectMapper();
    protected final Class<T> mClass;
    private final Response.Listener<T> mListener;
    private static final RetryPolicy mRetryPolicy = new NoRetryPolicy();

    private long startTime;
    private boolean mLogJson = false;

    @SuppressWarnings("unused")
    public BaseJsonRequest(String url, Class<T> responseClass, Listener<T> listener, ErrorListener errorListener) {
        this(Request.Method.GET, url, responseClass, listener, errorListener);
    }

    public BaseJsonRequest(int method, String url, Class<T> responseClass, Listener<T> listener, ErrorListener errorListener) {
        super(method, url, errorListener);
        mClass = responseClass;
        mListener = listener;
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        mapper.configure(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, false);
        mapper.configure(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS, true);
        startTime = System.nanoTime();
        setRetryPolicy(mRetryPolicy);
        setShouldCache(false);
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            long parsingStart = System.nanoTime();
            String json = new String(
                    response.data, HttpHeaderParser.parseCharset(response.headers, "UTF-8"));
            try{
                if (BuildConfig.DEBUG && mLogJson) Log.d("RequestInfo", "received json: \n"+new JSONObject(json).toString(2));
            }catch (JSONException ignored) { }

            T model = mapper.readValue(json, mClass);
            long handlingStart = System.nanoTime();
            model = handleResponseModel(model);
            Log.d("LoadingTime", "["+ mClass +"] "
                    + "completed in "+String.valueOf(TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS)) + " milliseconds"
                    + ", parsed in "+String.valueOf(TimeUnit.MILLISECONDS.convert(System.nanoTime() - parsingStart, TimeUnit.NANOSECONDS)) + " milliseconds"
                    + ", handled in "+String.valueOf(TimeUnit.MILLISECONDS.convert(System.nanoTime() - handlingStart, TimeUnit.NANOSECONDS)) + " milliseconds");
            if(model != null)
                return Response.success(model, HttpHeaderParser.parseCacheHeaders(response));
            else
                return Response.error(new VolleyError("Response model is null"));
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error in parsing response: " + e.toString());
            return Response.error(new ParseError(e));
        } catch (JsonParseException e) {
            Log.e(TAG, "Error in parsing response: " + e.toString());
            return Response.error(new ParseError(e));
        } catch (JsonMappingException e) {
            Log.e(TAG, "Error in parsing response: " + e.toString());
            return Response.error(new ParseError(e));
        } catch (IOException e) {
            Log.e(TAG, "Error in parsing response: " + e.toString());
            return Response.error(new ParseError(e));
        }
    }

    public static Map<String, String> getRequestHeaders(){
        if(headers == null) {
            headers = new HashMap<>();
            headers.put("Content-Type", CONTENT_TYPE);
            headers.put("Accept", CONTENT_TYPE);
        }
        return headers;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return getRequestHeaders();
    }

    protected abstract T handleResponseModel(T model) throws IOException;

    @Override
    protected void deliverResponse(T response) {
        Log.d(TAG, "delivering response");
        mListener.onResponse(response);
    }

    @SuppressWarnings("unused")
    public void setLogJson(boolean value) {
        mLogJson = value;
    }
}
