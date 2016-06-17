# drm-quick-start-android
Android frontend for the DRM quick start sample

Important files:
1. SampleChooserActivity (https://github.com/Axinom/drm-quick-start-android/blob/master/app/src/main/java/com/axinom/drm/quickstart/activity/SampleChooserActivity.java)
    1. contains URLs to website API, authorization service and license servers
    2. request to obtain video list from website API
    3. request to obtain license token from authorization service API

2. SmoothStreamingRendererBuilder (https://github.com/Axinom/drm-quick-start-android/blob/master/app/src/main/java/com/axinom/drm/quickstart/callbacks/SmoothStreamingTestMediaDrmCallback.java)
    1. request to obtain key from license server for Smooth Streaming content

3. SmoothStreamingRendererBuilder (https://github.com/Axinom/drm-quick-start-android/blob/master/app/src/main/java/com/axinom/drm/quickstart/callbacks/WidevineTestMediaDrmCallback.java)
    1. request to obtain key from license server for Widevine content


Not important files regarding Axinom DRM:
Files from package called "player" (https://github.com/Axinom/drm-quick-start-android/tree/master/app/src/main/java/com/axinom/drm/quickstart/player)
These classes are just Exoplayer related.
