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
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.axinom.drm.quickstart.R;
import com.axinom.drm.quickstart.application.BaseApp;
import com.axinom.drm.quickstart.player.PlayerActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

// An activity for selecting samples.
public class SampleChooserActivity extends Activity implements AdapterView.OnItemClickListener {

	private static final String TAG = SampleChooserActivity.class.getSimpleName();

	// URL to the website API
	private static final String API_WEBSITE = "https://drm-quick-start.azurewebsites.net/api/website/videos";
	// URL to the authorization service API
	private static final String API_AUTH = "https://drm-quick-start.azurewebsites.net/api/authorization/";

	// hardcoded license server URLs
	public static final String WIDEVINE_LICENSE_SERVER = "https://drm-widevine-licensing.axtest.net/AcquireLicense";
	public static final String PLAYREADY_LICENSE_SERVER = "https://drm-playready-licensing.axtest.net/AcquireLicense";

	private ListView mListView;
	private ArrayList<String> mVideoNames = new ArrayList<>();
	private ArrayList<String> mVideoUrls = new ArrayList<>();
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

	// Let's first populate the video list.
	// Called when activity is starting.
	private void makeMoviesRequest() {
		JsonArrayRequest request = new JsonArrayRequest (Request.Method.GET, API_WEBSITE, null,
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
				ArrayAdapter adapter = new ArrayAdapter<String>(getApplicationContext(),
				android.R.layout.simple_list_item_1, mVideoNames){
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
			}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				Log.d(TAG, "Movies json was not loaded with error: " + error.getMessage());
			}
		});
		BaseApp.requestQueue.add(request);
	}

	// Called when the user clicks on the link to play a video.
	// First, we need to request a license token from the authorization service.
	// This will prove to the license server that we have the right to play the video.
	private void makeAuthorizationRequest(final int position) {
		Request request = new StringRequest(Request.Method.GET,
				API_AUTH + encodeURIComponent(mVideoNames.get(position)),
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
			}
		});
		BaseApp.requestQueue.add(request);
	}

	private void startVideoActivity(int position){
		Intent intent = new Intent(this, PlayerActivity.class);
		intent.setData(Uri.parse(mVideoUrls.get(position)));
		intent.putExtra(PlayerActivity.LICENSE_TOKEN, mLicenseToken);
		intent.putExtra(PlayerActivity.WIDEVINE_LICENSE_SERVER, WIDEVINE_LICENSE_SERVER);
		intent.putExtra(PlayerActivity.PLAYREADY_LICENSE_SERVER, PLAYREADY_LICENSE_SERVER);
		startActivity(intent);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		makeAuthorizationRequest(position);
	}

	public static String encodeURIComponent(String component)   {
		String result;
		try {
			result = URLEncoder.encode(component, "UTF-8")
					.replaceAll("\\%28", "(")
					.replaceAll("\\%29", ")")
					.replaceAll("\\+", "%20")
					.replaceAll("\\%27", "'")
					.replaceAll("\\%21", "!")
					.replaceAll("\\%7E", "~");
		} catch (UnsupportedEncodingException e) {
			result = component;
		}
			return result;
		}
	}
