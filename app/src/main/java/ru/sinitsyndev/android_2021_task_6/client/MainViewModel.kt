package ru.sinitsyndev.android_2021_task_6.client

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.sinitsyndev.android_2021_task_6.LOG_TAG
import ru.sinitsyndev.android_2021_task_6.MainActivity
import ru.sinitsyndev.android_2021_task_6.service.HardMediaService

class MainViewModel(application: Application): AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private var activity: MainActivity? = null

    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var mediaController: MediaControllerCompat

    val state: MutableLiveData<PlaybackStateCompat?> = MutableLiveData(null)
    val metadata: MutableLiveData<MediaMetadataCompat?> = MutableLiveData(null)
    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val canInput: MutableLiveData<Boolean> = MutableLiveData(true)

    private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {

            // Get the token for the MediaSession
            mediaBrowser.sessionToken.also { token ->

                // Create a MediaControllerCompat
                mediaController = MediaControllerCompat(
                    getApplication(), // Context
                    token
                )

                // Save the controller
                MediaControllerCompat.setMediaController(activity!!, mediaController)
            }

            // Finish building the UI
            buildTransportControls()
        }

        override fun onConnectionSuspended() {
            // The Service has crashed. Disable transport controls until it automatically reconnects
        }

        override fun onConnectionFailed() {
            // The Service has refused our connection
        }
    }

    private var controllerCallback = object : MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            Log.d(LOG_TAG, "onMetadataChanged ${metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)}")

        }

        override fun onPlaybackStateChanged(_state: PlaybackStateCompat?) {
            Log.d(LOG_TAG, "onPlaybackStateChanged ${_state.toString()}")
            state.postValue(_state)
            if (_state?.state == PlaybackStateCompat.STATE_PLAYING
                || _state?.state == PlaybackStateCompat.STATE_STOPPED
                || _state?.state == PlaybackStateCompat.STATE_PAUSED
                || _state?.state == PlaybackStateCompat.STATE_ERROR
            ) {
                isLoading.value = false
                canInput.value = true
            }
        }

        override fun onAudioInfoChanged(info: MediaControllerCompat.PlaybackInfo?) {
            super.onAudioInfoChanged(info)
            Log.d(LOG_TAG, "onAudioInfoChanged")
        }
    }

    private fun buildTransportControls() {
        mediaController = MediaControllerCompat.getMediaController(activity as Activity)

        Log.d(LOG_TAG, "initial Metadata ${mediaController.metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)}")

        // Register a Callback to stay in sync
        mediaController.registerCallback(controllerCallback)

        Log.d(LOG_TAG, "initial Metadata ${mediaController.metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)}")
        // Register a Callback to stay in sync
        mediaController.registerCallback(controllerCallback)
    }

    override fun onCleared() {
        super.onCleared()
        mediaBrowser.disconnect()
    }

    fun initMediaBrowserConnector(mActivity: MainActivity) {
        activity = mActivity
        mediaBrowser = MediaBrowserCompat(
            getApplication(),
            ComponentName(getApplication(), HardMediaService::class.java),
            connectionCallbacks,
            null // optional Bundle
        )
        mediaBrowser.connect()
    }

    fun playPause() {
        isLoading.value = true
        canInput.value = false
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val pbState = mediaController.playbackState.state
                Log.d(LOG_TAG, pbState.toString())
                //Log.d(LOG_TAG, mediaBrowser.root)

                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                    mediaController.transportControls.pause()
                    //binding.playBtn.text = "Play"
                } else {
                    mediaController.transportControls.play()
                    //binding.playBtn.text = "Pause"
                }
            }
        }
    }

    fun skipToPrevious() {
        isLoading.value = true
        canInput.value = false
        mediaController.transportControls.skipToPrevious()
    }

    fun skipToNext() {
        isLoading.value = true
        canInput.value = false
        mediaController.transportControls.skipToNext()
    }
}