package com.axinom.drm.quickstart.callbacks;

import android.annotation.TargetApi;
import android.media.MediaDrm.KeyRequest;
import android.media.MediaDrm.ProvisionRequest;

import com.google.android.exoplayer.drm.MediaDrmCallback;
import com.google.android.exoplayer.util.Util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A {@link MediaDrmCallback} for Widevine content.
 */
@TargetApi(18)
public class WidevineMediaDrmCallback implements MediaDrmCallback {

  private final String mLicenseServer, mAxDrmMessage;

  public WidevineMediaDrmCallback(String licenseServer, String axDrmMessage) {
    // license server URL has hardcoded value of: "https://drm-widevine-licensing.axtest.net/AcquireLicense"
    // defined in SampleChooserActivity class
    mLicenseServer = licenseServer;
    mAxDrmMessage = axDrmMessage;
  }

  @Override
  public byte[] executeProvisionRequest(UUID uuid, ProvisionRequest request) throws IOException {
    String url = request.getDefaultUrl() + "&signedRequest=" + new String(request.getData());
    return Util.executePost(url, null, null);
  }

  // Requesting key from license server using license token obtained from the authorization service
  @Override
  public byte[] executeKeyRequest(UUID uuid, KeyRequest request) throws IOException {
    Map<String, String> requestProperties = new HashMap<>();
    requestProperties.put("X-AxDRM-Message", mAxDrmMessage);
    return Util.executePost(mLicenseServer, request.getData(), requestProperties);
  }
}
