/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.mediacontroller;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

/**
 * This class connects to a {@link android.support.v4.media.MediaBrowserServiceCompat}
 * in order to get a {@link MediaControllerCompat}.
 * Buttons are displayed on screen so that the user can exercise
 * the {@link android.support.v4.media.session.MediaSessionCompat.Callback}
 * methods of the media app.
 * <p>
 * Example: If you install the UAMP app and this Monkey Test app, you will be able
 * to test UAMP media controls.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    // Key names used for saving/restoring instance state.
    private static final String STATE_APP_DETAILS_KEY =
            "com.example.android.mediacontroller.STATE_APP_DETAILS_KEY";
    private static final String STATE_URI_KEY =
            "com.example.android.mediacontroller.STATE_URI_KEY";

    /**
     * Key name for {@link Intent} extras.
     */
    private static final String APP_DETAILS_EXTRA =
            "com.example.android.mediacontroller.APP_DETAILS_EXTRA";

    private MediaAppDetails mMediaAppDetails;
    private MediaControllerCompat mController;
    private MediaBrowserCompat mBrowser;

    private EditText mUriInput;
    private TextView mMediaInfoText;

    /**
     * Builds an {@link Intent} to launch this Activity with a set of extras.
     *
     * @param activity   The Activity building the Intent.
     * @param appDetails The app details about the media app to connect to.
     * @return An Intent that can be used to start the Activity.
     */
    public static Intent buildIntent(final Activity activity,
                                     final MediaAppDetails appDetails) {
        final Intent intent = new Intent(activity, MainActivity.class);
        intent.putExtra(APP_DETAILS_EXTRA, appDetails);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mUriInput = (EditText) findViewById(R.id.uri_id_query);
        mMediaInfoText = (TextView) findViewById(R.id.media_info);

        if (savedInstanceState != null) {
            mMediaAppDetails = savedInstanceState.getParcelable(STATE_APP_DETAILS_KEY);
            mUriInput.setText(savedInstanceState.getString(STATE_URI_KEY));
        }

        handleIntent(getIntent());
        setupButtons();
        setupMediaController();

        findViewById(R.id.reconnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setupMediaController();
            }
        });
        findViewById(R.id.update_media_info_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mediaInfoStr = fetchMediaInfo();
                if (mediaInfoStr != null) {
                    mMediaInfoText.setText(mediaInfoStr);
                }
            }
        });

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setIcon(new BitmapDrawable(getResources(), mMediaAppDetails.icon));
            actionBar.setTitle(mMediaAppDetails.appName);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        if (intent.getData() != null) {
            String uri = intent.getData().toString();
            mUriInput.setText(uri);
        }

        final Bundle extras = intent.getExtras();
        if (extras != null && extras.containsKey(APP_DETAILS_EXTRA)) {
            mMediaAppDetails = extras.getParcelable(APP_DETAILS_EXTRA);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);

        out.putParcelable(STATE_APP_DETAILS_KEY, mMediaAppDetails);
        out.putString(STATE_URI_KEY, mUriInput.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mMediaAppDetails = savedInstanceState.getParcelable(STATE_APP_DETAILS_KEY);
        mUriInput.setText(savedInstanceState.getString(STATE_URI_KEY));
    }

    private void setupMediaController() {
        if (mBrowser != null) {
            mBrowser.disconnect();
            mBrowser = null;
            mController = null;
        }

        mBrowser = new MediaBrowserCompat(this, mMediaAppDetails.mediaServiceComponentName,
                new MyConnectionCallback(), null);
        mBrowser.connect();
    }

    private void setupButtons() {
        LinearLayout buttonList = (LinearLayout) findViewById(R.id.activity_main);
        for (final Action action : Action.createActions(this)) {
            Button button = new Button(this);
            button.setText(action.getName());
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mController != null) {
                        String id = mUriInput.getText().toString();
                        action.getMediaControllerAction().run(mController, id, null);
                    }
                }
            });
            buttonList.addView(button);
        }
    }

    @Nullable
    private String fetchMediaInfo() {
        if (mController == null) {
            Log.e(TAG, "Failed to update media info, null MediaController.");
            return null;
        }

        PlaybackStateCompat playbackState = mController.getPlaybackState();
        if (playbackState == null) {
            Log.e(TAG, "Failed to update media info, null PlaybackState.");
            return null;
        }

        Map<String, String> mediaInfos = new HashMap<>();
        mediaInfos.put(getString(R.string.info_state_string),
                String.valueOf(playbackState.getState()));

        MediaMetadataCompat mediaMetadata = mController.getMetadata();
        if (mediaMetadata != null) {
            addMediaInfo(
                    mediaInfos,
                    getString(R.string.info_title_string),
                    mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
            addMediaInfo(
                    mediaInfos,
                    getString(R.string.info_artist_string),
                    mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
            addMediaInfo(
                    mediaInfos,
                    getString(R.string.info_album_string),
                    mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM));
        }
        return mediaInfos.toString();
    }

    private void addMediaInfo(Map<String, String> mediaInfos, String key, String value) {
        if (!TextUtils.isEmpty(value)) {
            mediaInfos.put(key, value);
        }
    }

    private class MyConnectionCallback extends MediaBrowserCompat.ConnectionCallback {

        @Override
        public void onConnected() {
            try {
                mController = new MediaControllerCompat(
                        MainActivity.this,
                        mBrowser.getSessionToken());

                mController.registerCallback(new MediaControllerCompat.Callback() {
                    @Override
                    public void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
                        String newText = "PlaybackState changed!";
                        String mediaInfoStr = fetchMediaInfo();
                        if (mediaInfoStr != null) {
                            mMediaInfoText.setText(newText + "\n" + mediaInfoStr);
                        }
                    }
                });
                Log.d(TAG, "MediaControllerCompat created");
                findViewById(R.id.status_info).setVisibility(View.GONE);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to connect with session token: " + e);
                String msg = getString(R.string.media_controller_failed_msg);
                ((TextView) findViewById(R.id.status_info)).setText(msg);
                findViewById(R.id.status_info).setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onConnectionSuspended() {
            Log.d(TAG, "MediaBrowser connection suspended");
            String msg = getString(R.string.connection_suspended_msg);
            ((TextView) findViewById(R.id.status_info)).setText(msg);
            findViewById(R.id.status_info).setVisibility(View.VISIBLE);
        }

        @Override
        public void onConnectionFailed() {
            Log.e(TAG, "MediaBrowser connection failed");
            String msg = getString(R.string.connection_failed_msg);
            ((TextView) findViewById(R.id.status_info)).setText(msg);
            findViewById(R.id.status_info).setVisibility(View.VISIBLE);
        }
    }
}
