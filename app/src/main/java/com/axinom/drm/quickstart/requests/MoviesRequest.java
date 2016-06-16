package com.axinom.drm.quickstart.requests;

import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.axinom.drm.quickstart.models.Movie;

import java.io.IOException;

// Volley request for movies feed loading.
public class MoviesRequest extends BaseJsonRequest<Movie[]> {

    private  static final RetryPolicy mRetryPolicy = new RetryPolicy() {
        @Override public int getCurrentTimeout() {
            return 5000;
        }

        @Override public int getCurrentRetryCount() {
            return 0;
        }

        @Override public void retry(VolleyError error) throws VolleyError { }
    };

    public MoviesRequest(String url, Response.Listener<Movie[]> listener, Response.ErrorListener errorListener) {
        super(url, Movie[].class, listener, errorListener);
        setRetryPolicy(mRetryPolicy);
    }

    @Override
    protected Movie[] handleResponseModel(Movie[] model) throws IOException {
        if(model != null) return model;
        return null;
    }
}
