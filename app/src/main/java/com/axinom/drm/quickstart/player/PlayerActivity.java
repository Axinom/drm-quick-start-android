package com.axinom.drm.quickstart.player;

import android.Manifest.permission;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.accessibility.CaptioningManager;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.Toast;

import com.axinom.drm.quickstart.R;
import com.axinom.drm.quickstart.callbacks.SmoothStreamingTestMediaDrmCallback;
import com.axinom.drm.quickstart.callbacks.WidevineTestMediaDrmCallback;
import com.axinom.drm.quickstart.player.DemoPlayer.RendererBuilder;
import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecTrackRenderer.DecoderInitializationException;
import com.google.android.exoplayer.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;
import com.google.android.exoplayer.drm.UnsupportedDrmException;
import com.google.android.exoplayer.metadata.id3.ApicFrame;
import com.google.android.exoplayer.metadata.id3.GeobFrame;
import com.google.android.exoplayer.metadata.id3.Id3Frame;
import com.google.android.exoplayer.metadata.id3.PrivFrame;
import com.google.android.exoplayer.metadata.id3.TextInformationFrame;
import com.google.android.exoplayer.metadata.id3.TxxxFrame;
import com.google.android.exoplayer.text.CaptionStyleCompat;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.text.SubtitleLayout;
import com.google.android.exoplayer.util.Util;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.List;

/**
 * An activity that plays media using {@link DemoPlayer}.
 */
public class PlayerActivity extends Activity implements SurfaceHolder.Callback, OnClickListener,
        DemoPlayer.Listener, DemoPlayer.CaptionListener, DemoPlayer.Id3MetadataListener,
        AudioCapabilitiesReceiver.Listener {

  private static final String TAG = "PlayerActivity";

  // hardcoded license server urls
  public static final String WIDEVINE_LICENSE_SERVER = "widevine_license_server";
  public static final String PLAYREADY_LICENSE_SERVER = "playready_license_server";
  public static final String LICENSE_TOKEN = "license_token";

  private String mWidevineLicenseServer;
  private String mPlayreadyLicenseServer;
  private String mLicenseToken;

  // For use within demo app code.
  public static final String CONTENT_TYPE_EXTRA = "content_type";

  // For use when launching the demo app using adb.
  private static final String CONTENT_EXT_EXTRA = "type";

  private static final CookieManager defaultCookieManager;
  static {
    defaultCookieManager = new CookieManager();
    defaultCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
  }

  private MediaController mediaController;
  private View debugRootView;
  private View shutterView;
  private AspectRatioFrameLayout videoFrame;
  private SurfaceView surfaceView;
  private SubtitleLayout subtitleLayout;
  private Button retryButton;

  private DemoPlayer player;
  private boolean playerNeedsPrepare;

  private long playerPosition;
  private boolean enableBackgroundAudio;

  private Uri contentUri;
  private int contentType;

  private AudioCapabilitiesReceiver audioCapabilitiesReceiver;

  // Activity lifecycle

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.player_activity);
    View root = findViewById(R.id.root);
    root.setOnTouchListener(new OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
          toggleControlsVisibility();
        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
          view.performClick();
        }
        return true;
      }
    });
    root.setOnKeyListener(new OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE
                || keyCode == KeyEvent.KEYCODE_MENU) {
          return false;
        }
        return mediaController.dispatchKeyEvent(event);
      }
    });

    shutterView = findViewById(R.id.shutter);
    debugRootView = findViewById(R.id.controls_root);

    videoFrame = (AspectRatioFrameLayout) findViewById(R.id.video_frame);
    surfaceView = (SurfaceView) findViewById(R.id.surface_view);
    surfaceView.getHolder().addCallback(this);
    subtitleLayout = (SubtitleLayout) findViewById(R.id.subtitles);

    mediaController = new KeyCompatibleMediaController(this);
    mediaController.setAnchorView(root);
    retryButton = (Button) findViewById(R.id.retry_button);
    retryButton.setOnClickListener(this);

    CookieHandler currentHandler = CookieHandler.getDefault();
    if (currentHandler != defaultCookieManager) {
      CookieHandler.setDefault(defaultCookieManager);
    }

    audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(this, this);
    audioCapabilitiesReceiver.register();
  }

  @Override
  public void onNewIntent(Intent intent) {
    releasePlayer();
    playerPosition = 0;
    setIntent(intent);
  }

  @Override
  public void onStart() {
    super.onStart();
    if (Util.SDK_INT > 23) {
      onShown();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    if (Util.SDK_INT <= 23 || player == null) {
      onShown();
    }
  }

  private void onShown() {
    Intent intent = getIntent();
    contentUri = intent.getData();
    contentType = intent.getIntExtra(CONTENT_TYPE_EXTRA,
            inferContentType(contentUri, intent.getStringExtra(CONTENT_EXT_EXTRA)));

    mLicenseToken = intent.getStringExtra(LICENSE_TOKEN);
    mWidevineLicenseServer = intent.getStringExtra(WIDEVINE_LICENSE_SERVER);
    mPlayreadyLicenseServer = intent.getStringExtra(PLAYREADY_LICENSE_SERVER);

    configureSubtitleView();
    if (player == null) {
      if (!maybeRequestPermission()) {
        preparePlayer(true);
      }
    } else {
      player.setBackgrounded(false);
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if (Util.SDK_INT <= 23) {
      onHidden();
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    if (Util.SDK_INT > 23) {
      onHidden();
    }
  }

  private void onHidden() {
    if (!enableBackgroundAudio) {
      releasePlayer();
    } else {
      player.setBackgrounded(true);
    }
    shutterView.setVisibility(View.VISIBLE);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    audioCapabilitiesReceiver.unregister();
    releasePlayer();
  }

  // OnClickListener methods

  @Override
  public void onClick(View view) {
    if (view == retryButton) {
      preparePlayer(true);
    }
  }

  // AudioCapabilitiesReceiver.Listener methods

  @Override
  public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
    if (player == null) {
      return;
    }
    boolean backgrounded = player.getBackgrounded();
    boolean playWhenReady = player.getPlayWhenReady();
    releasePlayer();
    preparePlayer(playWhenReady);
    player.setBackgrounded(backgrounded);
  }

  // Permission request listener method

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                         int[] grantResults) {
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      preparePlayer(true);
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
  private boolean maybeRequestPermission() {
    if (requiresPermission(contentUri)) {
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

  // Internal methods

  public RendererBuilder getRendererBuilder() {
    String userAgent = Util.getUserAgent(this, "ExoPlayerDemo");
    switch (contentType) {
      case Util.TYPE_SS:
        return new SmoothStreamingRendererBuilder(this, userAgent, contentUri.toString(),
                new SmoothStreamingTestMediaDrmCallback(mPlayreadyLicenseServer, mLicenseToken));
      case Util.TYPE_DASH:
        return new DashRendererBuilder(this, userAgent, contentUri.toString(),
                new WidevineTestMediaDrmCallback(mWidevineLicenseServer, mLicenseToken));
      case Util.TYPE_HLS:
        return new HlsRendererBuilder(this, userAgent, contentUri.toString());
      case Util.TYPE_OTHER:
        return new ExtractorRendererBuilder(this, userAgent, contentUri);
      default:
        throw new IllegalStateException("Unsupported type: " + contentType);
    }
  }

  private void preparePlayer(boolean playWhenReady) {
    if (player == null) {
      player = new DemoPlayer(getRendererBuilder());
      player.addListener(this);
      player.setCaptionListener(this);
      player.setMetadataListener(this);
      player.seekTo(playerPosition);
      playerNeedsPrepare = true;
      mediaController.setMediaPlayer(player.getPlayerControl());
      mediaController.setEnabled(true);
    }
    if (playerNeedsPrepare) {
      player.prepare();
      playerNeedsPrepare = false;
      updateButtonVisibilities();
    }
    player.setSurface(surfaceView.getHolder().getSurface());
    player.setPlayWhenReady(playWhenReady);
  }

  private void releasePlayer() {
    if (player != null) {
      playerPosition = player.getCurrentPosition();
      player.release();
      player = null;
    }
  }

  // DemoPlayer.Listener implementation

  @Override
  public void onStateChanged(boolean playWhenReady, int playbackState) {
    if (playbackState == ExoPlayer.STATE_ENDED) {
      showControls();
    }
    updateButtonVisibilities();
  }

  @Override
  public void onError(Exception e) {
    String errorString = null;
    if (e instanceof UnsupportedDrmException) {
      // Special case DRM failures.
      UnsupportedDrmException unsupportedDrmException = (UnsupportedDrmException) e;
      errorString = getString(Util.SDK_INT < 18 ? R.string.error_drm_not_supported
              : unsupportedDrmException.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
              ? R.string.error_drm_unsupported_scheme : R.string.error_drm_unknown);
    } else if (e instanceof ExoPlaybackException
            && e.getCause() instanceof DecoderInitializationException) {
      // Special case for decoder initialization failures.
      DecoderInitializationException decoderInitializationException =
              (DecoderInitializationException) e.getCause();
      if (decoderInitializationException.decoderName == null) {
        if (decoderInitializationException.getCause() instanceof DecoderQueryException) {
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
    }
    if (errorString != null) {
      Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_LONG).show();
    }
    playerNeedsPrepare = true;
    updateButtonVisibilities();
    showControls();
  }

  @Override
  public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                 float pixelWidthAspectRatio) {
    shutterView.setVisibility(View.GONE);
    videoFrame.setAspectRatio(
            height == 0 ? 1 : (width * pixelWidthAspectRatio) / height);
  }

  // User controls

  private void updateButtonVisibilities() {
    retryButton.setVisibility(playerNeedsPrepare ? View.VISIBLE : View.GONE);
  }

  private void toggleControlsVisibility()  {
    if (mediaController.isShowing()) {
      mediaController.hide();
      debugRootView.setVisibility(View.GONE);
    } else {
      showControls();
    }
  }

  private void showControls() {
    mediaController.show(0);
    debugRootView.setVisibility(View.VISIBLE);
  }

  // DemoPlayer.CaptionListener implementation

  @Override
  public void onCues(List<Cue> cues) {
    subtitleLayout.setCues(cues);
  }

  // DemoPlayer.MetadataListener implementation

  @Override
  public void onId3Metadata(List<Id3Frame> id3Frames) {
    for (Id3Frame id3Frame : id3Frames) {
      if (id3Frame instanceof TxxxFrame) {
        TxxxFrame txxxFrame = (TxxxFrame) id3Frame;
        Log.i(TAG, String.format("ID3 TimedMetadata %s: description=%s, value=%s", txxxFrame.id,
                txxxFrame.description, txxxFrame.value));
      } else if (id3Frame instanceof PrivFrame) {
        PrivFrame privFrame = (PrivFrame) id3Frame;
        Log.i(TAG, String.format("ID3 TimedMetadata %s: owner=%s", privFrame.id, privFrame.owner));
      } else if (id3Frame instanceof GeobFrame) {
        GeobFrame geobFrame = (GeobFrame) id3Frame;
        Log.i(TAG, String.format("ID3 TimedMetadata %s: mimeType=%s, filename=%s, description=%s",
                geobFrame.id, geobFrame.mimeType, geobFrame.filename, geobFrame.description));
      } else if (id3Frame instanceof ApicFrame) {
        ApicFrame apicFrame = (ApicFrame) id3Frame;
        Log.i(TAG, String.format("ID3 TimedMetadata %s: mimeType=%s, description=%s",
                apicFrame.id, apicFrame.mimeType, apicFrame.description));
      } else if (id3Frame instanceof TextInformationFrame) {
        TextInformationFrame textInformationFrame = (TextInformationFrame) id3Frame;
        Log.i(TAG, String.format("ID3 TimedMetadata %s: description=%s", textInformationFrame.id,
                textInformationFrame.description));
      } else {
        Log.i(TAG, String.format("ID3 TimedMetadata %s", id3Frame.id));
      }
    }
  }

  // SurfaceHolder.Callback implementation

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    if (player != null) {
      player.setSurface(holder.getSurface());
    }
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    // Do nothing.
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    if (player != null) {
      player.blockingClearSurface();
    }
  }

  private void configureSubtitleView() {
    CaptionStyleCompat style;
    float fontScale;
    if (Util.SDK_INT >= 19) {
      style = getUserCaptionStyleV19();
      fontScale = getUserCaptionFontScaleV19();
    } else {
      style = CaptionStyleCompat.DEFAULT;
      fontScale = 1.0f;
    }
    subtitleLayout.setStyle(style);
    subtitleLayout.setFractionalTextSize(SubtitleLayout.DEFAULT_TEXT_SIZE_FRACTION * fontScale);
  }

  @TargetApi(19)
  private float getUserCaptionFontScaleV19() {
    CaptioningManager captioningManager =
            (CaptioningManager) getSystemService(Context.CAPTIONING_SERVICE);
    return captioningManager.getFontScale();
  }

  @TargetApi(19)
  private CaptionStyleCompat getUserCaptionStyleV19() {
    CaptioningManager captioningManager =
            (CaptioningManager) getSystemService(Context.CAPTIONING_SERVICE);
    return CaptionStyleCompat.createFromCaptionStyle(captioningManager.getUserStyle());
  }

  /**
   * Makes a best guess to infer the type from a media {@link Uri} and an optional overriding file
   * extension.
   *
   * @param uri The {@link Uri} of the media.
   * @param fileExtension An overriding file extension.
   * @return The inferred type.
   */
  private static int inferContentType(Uri uri, String fileExtension) {
    String lastPathSegment = !TextUtils.isEmpty(fileExtension) ? "." + fileExtension
            : uri.getLastPathSegment();
    return Util.inferContentType(lastPathSegment);
  }

  private static final class KeyCompatibleMediaController extends MediaController {

    private MediaPlayerControl playerControl;

    public KeyCompatibleMediaController(Context context) {
      super(context);
    }

    @Override
    public void setMediaPlayer(MediaPlayerControl playerControl) {
      super.setMediaPlayer(playerControl);
      this.playerControl = playerControl;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
      int keyCode = event.getKeyCode();
      if (playerControl.canSeekForward() && (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
              || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
          playerControl.seekTo(playerControl.getCurrentPosition() + 15000); // milliseconds
          show();
        }
        return true;
      } else if (playerControl.canSeekBackward() && (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND
              || keyCode == KeyEvent.KEYCODE_DPAD_LEFT)) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
          playerControl.seekTo(playerControl.getCurrentPosition() - 5000); // milliseconds
          show();
        }
        return true;
      }
      return super.dispatchKeyEvent(event);
    }
  }

}
