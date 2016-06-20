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

# Instructions to run sample project:
1. Open Android Studio and connect device to computer. Device's version must be at least Android 4.1 (API level 16).
2. Clone (VCS -> Checkout from Version Control -> GitHub from Android Studio Menu bar. Use https://github.com/Axinom/drm-quick-start-android.git as Git Repository URL in "Clone Repository" popup) or download and open (File -> Open from Android Studio Menu bar. Select extracted content of downloaded zip file in "Open File or Project popup") this Git repository (the one that you are currently reading).
3. Run application (Run -> Run 'app' from Android Studio Menu bar).