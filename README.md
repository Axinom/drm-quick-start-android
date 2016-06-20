# drm-quick-start-android
Android frontend for the DRM quick start sample

# Important files:
* SampleChooserActivity (https://github.com/Axinom/drm-quick-start-android/blob/master/app/src/main/java/com/axinom/drm/quickstart/activity/SampleChooserActivity.java):
    * Contains URLs to website API, authorization service and license servers.
    * Request to obtain video list from website API.
    * Request to obtain license token from authorization service API.


* WidevineMediaDrmCallback (https://github.com/Axinom/drm-quick-start-android/blob/master/app/src/main/java/com/axinom/drm/quickstart/callbacks/WidevineMediaDrmCallback.java):
    * Request to obtain key from license server for Widevine content.

# Not important files regarding Axinom DRM:
Files from package called "player" (https://github.com/Axinom/drm-quick-start-android/tree/master/app/src/main/java/com/axinom/drm/quickstart/player). These classes are just Exoplayer related.