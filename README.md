Media Controller Test
=====================
Create a simple MediaController that connects to a MediaBrowserService
in order to test inter-app media controls.

This app works with the Universal Android Music Player sample,
or any other app that implements the media APIs.
https://github.com/googlesamples/android-UniversalMusicPlayer


Usage
=====

1. Select an app from the list of those presented.
   * Only apps that register a service with an intent filter action of
   "android.media.browse.MediaBrowserService" will be shown.
2. Select the type of action to perform to start the player. Options are:
   * Search: Sends the text provided as a search via _prepareFromSearch()_ or
   _playFromSearch()_.
   * Media ID: Sends the text provided as a media ID via _prepareFromMediaId()_ or
   _playFromMediaId()_.
   * URI: Sends the text provided as a URI via _prepareFromUri()_ or
   _playFromUri()_.
   * No Input: Calls the methods _prepare()_ or _play()_ directly.
3. Text below the ```PREPARE``` and ```PLAY``` buttons updates based on changes to
   the media player state via _onPlaybackStateChanged_ and _onMetadataChanged_ and
   includes the current player state reported via _PlaybackStateCompat.getState()_.
4. Swipe to the left to see typical media controls with the media's art as a
   background, if provided.
5. Press ```back``` to return to the list of media apps.

Via ADB
-------

It's also possible to launch the app via ADB and the Activity manager (am).

Usage: ```adb shell am start mediacontroller://<package name>?[search|id|uri=<value>]```

For example, to set up to play "Awakening" by _Silent Partner_ in UAMP, the following command
could be used:

```adb shell am start "mediacontroller://com.example.android.uamp?id=__BY_GENRE__/Rock\|-1679589699"```

Alternatively, it's possible to use extras to pass parameters, which is recommended when passing
parameters that include URI-like components:

Extra names:

- Package name : ```com.example.android.mediacontroller.PACKAGE_NAME```
- Search term : ```com.example.android.mediacontroller.SEARCH```
- Media ID : ```com.example.android.mediacontroller.MEDIA_ID```
- URI : ```com.example.android.mediacontroller.URI```

Another example with UAMP is to perform a search with the term "jazz?" one would use:

```adb shell am start -n com.example.android.mediacontroller/.MediaAppControllerActivity --es com.example.android.mediacontroller.PACKAGE_NAME "com.example.android.uamp" --es com.example.android.mediacontroller.SEARCH "jazz?"```

Verification
============

This tool displays the supported actions as reported by the MediaSession in the call to
[MediaSessionCompat.setPlaybackState()](https://developer.android.com/reference/android/support/v4/media/session/MediaSessionCompat.html#setPlaybackState(android.support.v4.media.session.PlaybackStateCompat))
as a list of prepare and play actions on the main screen. For actions that are not declared as
supported, it also colors the buttons red on the controller screen.

See the screenshots below for examples.

Screenshots
===========

![](screenshots/screenshots.png "Controls, URIs, Playback")


License
=======

Copyright 2017 Google Inc. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

