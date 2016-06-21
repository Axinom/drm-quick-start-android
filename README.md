# drm-quick-start-android
Android frontend for the DRM quick start sample

# Important files:
* SampleChooserActivity (https://github.com/Axinom/drm-quick-start-android/blob/master/app/src/main/java/com/axinom/drm/quickstart/activity/SampleChooserActivity.java):
    * Contains URLs to catalog API, authorization service and license servers.
    * Request to obtain video list from website API.
    * Request to obtain license token from authorization service API.


* WidevineMediaDrmCallback (https://github.com/Axinom/drm-quick-start-android/blob/master/app/src/main/java/com/axinom/drm/quickstart/callbacks/WidevineMediaDrmCallback.java):
    * Request to obtain key from license server for Widevine content.

# Not important files regarding Axinom DRM:
Files from package called "player" (https://github.com/Axinom/drm-quick-start-android/tree/master/app/src/main/java/com/axinom/drm/quickstart/player). These classes are just ExoPlayer related.

# Instructions to run sample project:
1. Open Android Studio and connect device to computer. Device's version must be at least Android 4.3 (API level 18).
2. Clone or download and open this Git repository (the one that you are currently reading) in Android Studio.
3. Run application (Run -> Run 'app' from Android Studio Menu bar).