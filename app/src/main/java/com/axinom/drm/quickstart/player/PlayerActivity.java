package com.axinom.drm.quickstart.player;

import android.Manifest.permission;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.axinom.drm.quickstart.R;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.Util;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

import androidx.annotation.NonNull;

/**
 * An activity that plays media using {@link DemoPlayer}.
 */
public class PlayerActivity extends Activity implements DemoPlayer.Listener {

  private static final String TAG = "PlayerActivity";

  public static final String WIDEVINE_LICENSE_SERVER = "widevine_license_server";
  public static final String LICENSE_TOKEN = "license_token";

  private Uri mContentUri;
  private String mWidevineLicenseServer;
  private String mLicenseToken;
  // save last playback position on suspend
  private long mPlayerPosition;
  private boolean mPlayerStartOnPrepared;

  private static final CookieManager sDefaultCookieManager;
  static {
    sDefaultCookieManager = new CookieManager();
    sDefaultCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
  }

  private PlayerView mPlayerView;
  private DemoPlayer mPlayer;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.player_activity);
    mPlayerView = findViewById(R.id.player_view);
    CookieHandler currentHandler = CookieHandler.getDefault();
    if (currentHandler != sDefaultCookieManager) {
      CookieHandler.setDefault(sDefaultCookieManager);
    }
    handleIntent(getIntent());
  }

  private void handleIntent(Intent intent){
    Log.d(TAG, "handleIntent() called with: intent = [" + intent + "]");
    mContentUri = intent.getData();
    mLicenseToken = intent.getStringExtra(LICENSE_TOKEN);
    mWidevineLicenseServer = intent.getStringExtra(WIDEVINE_LICENSE_SERVER);
    mPlayerPosition = 0;
    mPlayerStartOnPrepared = true;
  }

  @Override
  public void onNewIntent(Intent intent) {
    Log.d(TAG, "onNewIntent() called with: intent = [" + intent + "]");
    releasePlayer();
    handleIntent(intent);
  }

  @Override
  public void onResume() {
    super.onResume();
    restorePlayer();
  }

  private void restorePlayer() {
    Log.d(TAG, "restorePlayer() called");
    if (mPlayer == null) {
      if (!requestPermissionsIfNeeded()) {
        preparePlayer();
      }
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.d(TAG, "onPause() called");
    releasePlayer();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    releasePlayer();
  }


  // Permission request listener method
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      preparePlayer();
    } else {
      Toast.makeText(getApplicationContext(), R.string.storage_permission_denied,
              Toast.LENGTH_LONG).show();
      finish();
    }
  }

  // Permission management methods

  /**
   * Checks whether it is necessary to ask for permission to read storage. If necessary, it also
   * requests permission.
   *
   * @return true if a permission request is made. False if it is not necessary.
   */
  @TargetApi(23)
  private boolean requestPermissionsIfNeeded() {
    Log.d(TAG, "requestPermissionsIfNeeded() called");
    if (requiresPermission(mContentUri)) {
      requestPermissions(new String[] {permission.READ_EXTERNAL_STORAGE}, 0);
      return true;
    } else {
      return false;
    }
  }

  @TargetApi(23)
  private boolean requiresPermission(Uri uri) {
    return Util.SDK_INT >= 23
            && Util.isLocalFileUri(uri)
            && checkSelfPermission(permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED;
  }

  private void preparePlayer() {
    Log.d(TAG, "preparePlayer() called");
    DemoPlayer.Params params = new DemoPlayer.Params();
    params.contentUri = mContentUri;
    params.axDrmMessage = mLicenseToken;
    params.licenseServer = mWidevineLicenseServer;
    params.startPosition = mPlayerPosition;
    params.startOnPrepared = mPlayerStartOnPrepared;
    if (mPlayer == null) {
      mPlayer = new DemoPlayer(this);
      mPlayer.addListener(this);
      mPlayer.prepare(params, mPlayerView);
    }
  }

  private void releasePlayer() {
    Log.d(TAG, "releasePlayer() called");
    if (mPlayer != null) {
      mPlayerPosition = mPlayer.getCurrentPosition();
      mPlayer.release();
      mPlayer = null;
      mPlayerStartOnPrepared = false;
    } else {
      mPlayerPosition = 0;
    }
  }

  // DemoPlayer.Listener implementation
  @Override
  public void onPlayerError(Exception e) {
    Log.d(TAG, "onPlayerError() called with: e = [" + e + "]");
    String errorString;
    if (e instanceof UnsupportedDrmException) {
      // Special case DRM failures.
      UnsupportedDrmException unsupportedDrmException = (UnsupportedDrmException) e;
      errorString = getString(unsupportedDrmException.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
              ? R.string.error_drm_unsupported_scheme : R.string.error_drm_unknown);
    } else if (e instanceof ExoPlaybackException
            && e.getCause() instanceof MediaCodecRenderer.DecoderInitializationException) {
      // Special case for decoder initialization failures.
      MediaCodecRenderer.DecoderInitializationException decoderInitializationException =
              (MediaCodecRenderer.DecoderInitializationException) e.getCause();
      if (decoderInitializationException.decoderName == null) {
        if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
          errorString = getString(R.string.error_querying_decoders);
        } else if (decoderInitializationException.secureDecoderRequired) {
          errorString = getString(R.string.error_no_secure_decoder,
                  decoderInitializationException.mimeType);
        } else {
          errorString = getString(R.string.error_no_decoder,
                  decoderInitializationException.mimeType);
        }
      } else {
        errorString = getString(R.string.error_instantiating_decoder,
                decoderInitializationException.decoderName);
      }
    } else if (e instanceof DemoPlayer.UnsupportedFormatException){
      errorString = getString(R.string.error_unsupported_file_format);
    } else {
      errorString = getString(R.string.error_player_unknown, e.getMessage());
    }
    showAlertDialog(errorString);
  }

  private void showAlertDialog(String message){
    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
    alertDialogBuilder.setTitle("Error");
    alertDialogBuilder
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("OK",new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog,int id) {
                finish();
              }
            });
    AlertDialog alertDialog = alertDialogBuilder.create();
    alertDialog.show();
  }

}
