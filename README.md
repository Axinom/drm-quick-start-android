# drm-quick-start-android

This is an Android application that plays back DASH videos protected using Axinom DRM. The app works together with the [Axinom DRM quick start sample project](https://github.com/Axinom/drm-quick-start) which exposes web APIs providing data to the frontend.

![](Images/Relation%20to%20drm-quick-start.png)

You do not need to deploy the drm-quick-start project to use this sample, as it works against an Axinom-hosted deployment of the APIs by default. Your own deployment of the APIs is only needed if you wish to customize the data used by the application.

# Sample scenarios

This sample appliation can be used as an equivalent alternative to the web frontend in executing all the sample scenarios described by the [drm-quick-start sample project](https://github.com/Axinom/drm-quick-start). Refer to the latter for more information.

# Project structure

This project uses [ExoPlayer](https://github.com/google/ExoPlayer) to play back DASH videos protected using Axinom DRM. It is largely based on ExoPlayer sample code.

## Important files

[SampleChooserActivity.java](app/src/main/java/com/axinom/drm/quickstart/activity/SampleChooserActivity.java)

* Defines the URLs to the catalog API, authorization service API and the license server.
* Performs the API call to obtain video list from the catalog API.
* Performs the API call to obtain the license token from the authorization service API.

[WidevineMediaDrmCallback.java](app/src/main/java/com/axinom/drm/quickstart/callbacks/WidevineMediaDrmCallback.java)

* Implements communications with the Axinom DRM license server.
* Attaches the license token to license requests.

## Other major components

ExoPlayer demo application code is in the [player](app/src/main/java/com/axinom/drm/quickstart/player) package and is largely irrelevant to the functioning of Axinom DRM.

# Device compatibility

This project is compatible with devices running Android 4.4 or newer. This project is **not** compatible with the Android emulator.

# How to run the application

1. Open Android Studio and connect an Android 4.4 (or newer) device to computer. 
2. Clone or download this repository and open the project in Android Studio.
3. Run the application by selecting *Run -> Run 'app'* from the Android Studio menu bar.
