/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.axinom.drm.quickstart.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.axinom.drm.quickstart.R;
import com.axinom.drm.quickstart.application.BaseApp;
import com.axinom.drm.quickstart.models.Movie;
import com.axinom.drm.quickstart.requests.MoviesRequest;

import java.util.ArrayList;

/**
 * An activity for selecting from a number of samples.
 */
public class SampleChooserActivity extends Activity implements AdapterView.OnItemClickListener {

  private static final String TAG = SampleChooserActivity.class.getSimpleName();
  private static final String API_WEBSITE = "https://drm-quick-start.azurewebsites.net/api/website/videos";
  private static final String API_AUTH = "https://drm-quick-start.azurewebsites.net/api/authorization/";

  private ListView mListView;
  private ArrayList<String> mListItems = new ArrayList<>();
  private Movie[] mMovies = new Movie[0];
  private String mLicenseToken;


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.sample_chooser_activity);

    mListView = (ListView) findViewById(R.id.sample_list);

    makeMoviesRequest();
  }

  @Override
  protected void onResume() {
    super.onResume();
    mListView.setOnItemClickListener(this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    mListView.setOnItemClickListener(null);
  }

  private void makeMoviesRequest() {
    Request request = new MoviesRequest(API_WEBSITE,
            new Response.Listener<Movie[]>() {
              @Override
              public void onResponse(Movie[] response) {
                mMovies = response;
                for (int i = 0; i < mMovies.length; i++) {
                  mListItems.add(response[i].name);
                }

                ArrayAdapter adapter = new ArrayAdapter<String>(getApplicationContext(),
                        android.R.layout.simple_list_item_1, mListItems){
                  @Override
                  public View getView(int position, View convertView,
                                      ViewGroup parent) {
                    View view =super.getView(position, convertView, parent);

                    TextView textView=(TextView) view.findViewById(android.R.id.text1);

                    textView.setTextColor(Color.BLACK);

                    return view;
                  }
                };
                mListView.setAdapter(adapter);
              }
            },
            new Response.ErrorListener() {
              @Override
              public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Movies json was not loaded with error: " + error.getMessage());
              }
            }
    );
    BaseApp.requestQueue.add(request);
  }

  //
  private void makeAuthorizationRequest(final int position) {
    Request request = new StringRequest(Request.Method.GET,
            API_AUTH + mMovies[position].name.replace(" ", "%20"),
            new Response.Listener<String>() {
              @Override
              public void onResponse(String response) {
                mLicenseToken = response.substring(1,response.length()-1);
                startVideoActivity(position);
              }
            },
            new Response.ErrorListener() {
              @Override
              public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "License token was not loaded with error: " + error.getMessage());
              }
            });

    BaseApp.requestQueue.add(request);
  }

  private void startVideoActivity(int position){
    Intent intent = new Intent(this, PlayerActivity.class);
    intent.setData(Uri.parse(mMovies[position].url));
    intent.putExtra(PlayerActivity.LICENSE_TOKEN, mLicenseToken);
    startActivity(intent);
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    makeAuthorizationRequest(position);
  }
}
