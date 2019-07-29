package com.axinom.drm.quickstart.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.axinom.drm.quickstart.R;
import com.axinom.drm.quickstart.application.BaseApp;
import com.axinom.drm.quickstart.player.PlayerActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

// An activity for selecting samples.
public class SampleChooserActivity extends Activity implements AdapterView.OnItemClickListener {

	private enum RequestStatus { NONE, LOADING, LOADED, ERROR }
	private static final String TAG = SampleChooserActivity.class.getSimpleName();

	// URL to the catalog API
	private static final String API_CATALOG = "https://drm-quick-start.azurewebsites.net/api/catalog/videos";
	// URL to the authorization service API
	private static final String API_AUTH = "https://drm-quick-start.azurewebsites.net/api/authorization/";

	// hardcoded license server URL
	private static final String WIDEVINE_LICENSE_SERVER = "https://drm-widevine-licensing.axtest.net/AcquireLicense";

	private ListView mListView;
	private ArrayAdapter<String> mListAdapter;
	private final ArrayList<String> mVideoNames = new ArrayList<>();
	private final ArrayList<String> mVideoUrls = new ArrayList<>();
	private String mLicenseToken;
	private RequestStatus mListRequestStatus;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sample_chooser_activity);
		mListView = findViewById(R.id.sample_list);
		mListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
		mListView.setAdapter(mListAdapter);
		mListRequestStatus = RequestStatus.NONE;
	}

	@Override
	protected void onResume() {
		super.onResume();
		mListView.setOnItemClickListener(this);
		if (mListRequestStatus == RequestStatus.NONE){
			makeMoviesRequest();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mListView.setOnItemClickListener(null);
	}

	private void showToast(String errorMessage){
		Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
	}

	// Let's first populate the video list.
	// Called when activity is starting.
	private void makeMoviesRequest() {
		Log.d(TAG, "makeMoviesRequest() called");
		JsonArrayRequest request = new JsonArrayRequest (Request.Method.GET, API_CATALOG, null,
				new Response.Listener<JSONArray>() {
			@Override
			public void onResponse(JSONArray response) {
				// Adding video URLs and names to lists from json array response.
				for (int i = 0; i < response.length(); i++) {
					try {
						JSONObject jsonObject = response.getJSONObject(i);
						mVideoUrls.add(jsonObject.getString("url"));
						mVideoNames.add(jsonObject.getString("name"));
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				mListRequestStatus = RequestStatus.LOADED;
				mListAdapter.clear();
				mListAdapter.addAll(mVideoNames);
				mListAdapter.notifyDataSetChanged();
			}
			}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				Log.d(TAG, "Movie list was not loaded, error: " + error.getMessage());
				showToast(getString(R.string.error_video_list));
				mListRequestStatus = RequestStatus.ERROR;
			}
		});
		mListRequestStatus = RequestStatus.LOADING;
		BaseApp.requestQueue.add(request);
	}

	// Called when the user clicks on the link to play a video.
	// First, we need to request a license token from the authorization service.
	// This will prove to the license server that we have the right to play the video.
	private void makeAuthorizationRequest(final int position) {
		Request request = new StringRequest(Request.Method.GET,
				API_AUTH + android.net.Uri.encode(mVideoNames.get(position)),
			new Response.Listener<String>() {
			@Override
			public void onResponse(String response) {
				// We got a license token! We are all set to start playback.
				// We just pass it on to the player activity started in startVideoActivity
				// method and have it take care of the rest.
				mLicenseToken = response.substring(1,response.length()-1);
				startVideoActivity(position);
			}
			}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				Log.d(TAG, "License token was not loaded with error: " + error.getMessage());
				showToast(getString(R.string.error_drm_token));
			}
		});
		BaseApp.requestQueue.add(request);
	}

	private void startVideoActivity(int position){
		Intent intent = new Intent(this, PlayerActivity.class);
		intent.setData(Uri.parse(mVideoUrls.get(position)));
		intent.putExtra(PlayerActivity.LICENSE_TOKEN, mLicenseToken);
		intent.putExtra(PlayerActivity.WIDEVINE_LICENSE_SERVER, WIDEVINE_LICENSE_SERVER);
		startActivity(intent);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		makeAuthorizationRequest(position);
	}

}
