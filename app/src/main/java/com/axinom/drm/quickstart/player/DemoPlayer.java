package com.axinom.drm.quickstart.player;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A wrapper around {@link ExoPlayer} that provides a higher level interface.
 */
class DemoPlayer implements ExoPlayer.EventListener, VideoListener, TextOutput {

  public static class Params {
    Uri contentUri;
    String licenseServer;
    String axDrmMessage;
    long startPosition = 0;
    boolean startOnPrepared = true;
  }

  public static class UnsupportedFormatException extends Exception { }

  /**
   * Listener interface for player events.
   */
  public interface Listener {
    void onPlayerError(Exception e);
  }

  private static final String TAG = DemoPlayer.class.getSimpleName();
  private static final String PLAYER_APP_NAME = "ExoPlayerDemo";

  private SimpleExoPlayer mPlayer;
  private boolean mPlayerIsCreated = false;
  private Context mContext;

  private final CopyOnWriteArrayList<Listener> mListeners;


  public DemoPlayer(Context context) {
    mContext = context;
    mListeners = new CopyOnWriteArrayList<>();
  }

  private boolean isCreated(){
    return mPlayerIsCreated;
  }

  private void playerCreate(Params params){
    TrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory();
    DefaultTrackSelector trackSelector = new DefaultTrackSelector(trackSelectionFactory);
    // TODO: more parameters?
    trackSelector.setParameters(new DefaultTrackSelector.ParametersBuilder().setPreferredTextLanguage("eng"));


    DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(mContext);
    try {
      mPlayerIsCreated = false;
      if (Util.inferContentType(params.contentUri) != C.TYPE_DASH){
          throw new UnsupportedFormatException();
      }
      DrmSessionManager<FrameworkMediaCrypto> sessionManager = buildDrmSessionManager(
              mContext, params.licenseServer, params.axDrmMessage);
      mPlayer = ExoPlayerFactory.newSimpleInstance(mContext, renderersFactory, trackSelector, sessionManager);
      mPlayer.addListener(this);
      mPlayerIsCreated = true;
    } catch (UnsupportedDrmException | UnsupportedFormatException | NullPointerException e){
      Log.d(TAG, "playerCreate() exception = " + e.getMessage());
      dispatchPlayerError(e);
    }
  }


  private void dispatchPlayerError(Exception e){
    for (Listener listener : mListeners) {
      listener.onPlayerError(e);
    }
  }

  public void addListener(Listener listener) {
    mListeners.add(listener);
  }

  private MediaSource buildMediaSource(Context context, Uri videoUri){
    Log.d(TAG, "buildMediaSource() called with: context = [" + context + "], videoUri = [" + videoUri + "]");
    String userAgent = Util.getUserAgent(context, PLAYER_APP_NAME);
    DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, userAgent);
    return new DashMediaSource.Factory(
            new DefaultDashChunkSource.Factory(dataSourceFactory), dataSourceFactory)
            .createMediaSource(videoUri);
  }

  private HttpDataSource.Factory buildHttpDataSourceFactory(Context context) {
    return new DefaultHttpDataSourceFactory(Util.getUserAgent(context,
            PLAYER_APP_NAME), null);
  }

  private DefaultDrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManager(Context context,
                                                                                String licenseUrl, String drmToken)
        throws UnsupportedDrmException {
    HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(licenseUrl,
            buildHttpDataSourceFactory(context));
    if (drmToken != null) {
        drmCallback.setKeyRequestProperty("X-AxDRM-Message", drmToken);
    }

    return new DefaultDrmSessionManager<>(C.WIDEVINE_UUID, FrameworkMediaDrm.newInstance(C.WIDEVINE_UUID),
            drmCallback, null);

  }

  public void prepare(Params params, PlayerView playerView) {
    Log.d(TAG, "prepare() called with: params = [" + params + "]");
    playerRelease();
    playerCreate(params);
    if (isCreated()) {
      mPlayer.setPlayWhenReady(params.startOnPrepared);
      playerView.setPlayer(mPlayer);
      mPlayer.prepare(buildMediaSource(mContext, params.contentUri));
      if (params.startPosition != 0){
        mPlayer.seekTo(params.startPosition);
      }
    }
  }

  private void playerRelease(){
    if (mPlayer != null) {
      mPlayer.release();
      mPlayer = null;
    }
  }


  public void release() {
    playerRelease();
    mContext = null;
  }

  public long getCurrentPosition() {
    if (mPlayerIsCreated) return mPlayer.getCurrentPosition();
    return 0;
  }

  private void dumpSelectedTracks() {
    TrackSelectionArray trackSelectionArray = mPlayer.getCurrentTrackSelections();
    for (int i = 0; i < trackSelectionArray.length; i++){
      TrackSelection trackSelection = trackSelectionArray.get(i);
      if (trackSelection != null) {
        Log.d(TAG, "trackSelection, index = " + i + ", selected format = " + trackSelection.getSelectedFormat());
      }
    }
  }

  private void dumpAvailableTracks(){
    TrackGroupArray trackGroups = mPlayer.getCurrentTrackGroups();

    for (int i = 0; i < trackGroups.length; i++){
      TrackGroup trackGroup = trackGroups.get(i);
      for (int trackIndex  = 0; trackIndex < trackGroup.length; trackIndex++){
        Format format =  trackGroup.getFormat(trackIndex);
        Log.d(TAG, "group = " + i + ", track = " + trackIndex + ", format = " + format);
      }
    }
  }

  @Override
  public void onPlayerStateChanged(boolean playWhenReady, int state) {
    Log.d(TAG, "onPlayerStateChanged() called with: playWhenReady = [" + playWhenReady + "], state = [" + state + "]");
    switch (state){
      case Player.STATE_IDLE:
        Log.d(TAG, "idle");
        break;
      case Player.STATE_BUFFERING:
        Log.d(TAG, "buffering");
        break;
      case Player.STATE_READY:
        Log.d(TAG, "ready");
        dumpAvailableTracks();
        dumpSelectedTracks();
        break;
      case Player.STATE_ENDED:
        Log.d(TAG, "ended");
        break;
      default:
        Log.d(TAG, "unknown state");
    }
  }

  @Override
  public void onPlayerError(ExoPlaybackException exception) {
    Log.d(TAG, "onPlayerError() called with: exception = [" + exception + "]");
    dispatchPlayerError(exception);
  }


  @Override
  public void onCues(List<Cue> cues) {
    Log.d(TAG, "onCues() called with: cues = [" + cues + "]");
  }

}
