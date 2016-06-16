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
package com.axinom.drm.quickstart.callbacks;

import android.annotation.TargetApi;
import android.media.MediaDrm.KeyRequest;
import android.media.MediaDrm.ProvisionRequest;

import com.google.android.exoplayer.drm.MediaDrmCallback;
import com.google.android.exoplayer.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer.util.Util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Demo {@link StreamingDrmSessionManager} for smooth streaming test content.
 */
@TargetApi(18)
public class SmoothStreamingTestMediaDrmCallback implements MediaDrmCallback {

  private String mLicenseServer, mAxDrmMessage;

  public SmoothStreamingTestMediaDrmCallback(String licenseServer, String axDrmMessage) {
    mLicenseServer = licenseServer;
    mAxDrmMessage = axDrmMessage;
  }

  private static Map<String, String> KEY_REQUEST_PROPERTIES;
  {
    HashMap<String, String> keyRequestProperties = new HashMap<>();
    keyRequestProperties.put("Content-Type", "text/xml");
    keyRequestProperties.put("SOAPAction", mLicenseServer);
    keyRequestProperties.put("X-AxDRM-Message", mAxDrmMessage);
    KEY_REQUEST_PROPERTIES = keyRequestProperties;
  }

  @Override
  public byte[] executeProvisionRequest(UUID uuid, ProvisionRequest request) throws IOException {
    String url = request.getDefaultUrl() + "&signedRequest=" + new String(request.getData());
    return Util.executePost(url, null, null);
  }

  @Override
  public byte[] executeKeyRequest(UUID uuid, KeyRequest request) throws Exception {
    String url = request.getDefaultUrl();
    return Util.executePost(url, request.getData(), KEY_REQUEST_PROPERTIES);
  }

}
