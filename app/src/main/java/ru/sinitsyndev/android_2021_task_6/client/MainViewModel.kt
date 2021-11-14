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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import ru.sinitsyndev.android_2021_task_6.LOG_TAG
import ru.sinitsyndev.android_2021_task_6.MainActivity
import ru.sinitsyndev.android_2021_task_6.service.HardMediaService

@SuppressLint("StaticFieldLeak")
class MainViewModel(application: Application, val activity: MainActivity) : AndroidViewModel(application) {

    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var mediaController: MediaControllerCompat

    val state: MutableLiveData<PlaybackStateCompat?> = MutableLiveData(null)
    val metadata: MutableLiveData<MediaMetadataCompat?> = MutableLiveData(null)
    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)

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
                activity.let{ MediaControllerCompat.setMediaController(it, mediaController) }
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

        override fun onMetadataChanged(newMetadata: MediaMetadataCompat?) {
            Log.d(LOG_TAG, "onMetadataChanged ${newMetadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)}")
            metadata.value = newMetadata
        }

        override fun onPlaybackStateChanged(newState: PlaybackStateCompat?) {
            Log.d(LOG_TAG, "onPlaybackStateChanged $newState")
            state.postValue(newState)

            when (newState?.state) {
                PlaybackStateCompat.STATE_PLAYING -> isLoading.value = false
                PlaybackStateCompat.STATE_STOPPED -> isLoading.value = false
                PlaybackStateCompat.STATE_PAUSED -> isLoading.value = false
                PlaybackStateCompat.STATE_ERROR -> isLoading.value = false
                PlaybackStateCompat.STATE_BUFFERING -> isLoading.value = true
                else -> Log.d(LOG_TAG, "Another state ${newState?.state}")
            }
        }

        override fun onAudioInfoChanged(info: MediaControllerCompat.PlaybackInfo?) {
            super.onAudioInfoChanged(info)
            Log.d(LOG_TAG, "onAudioInfoChanged")
        }
    }

    init {
        initMediaBrowserConnector()
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
        disconnect()
    }

    private fun initMediaBrowserConnector() {
        mediaBrowser = MediaBrowserCompat(
            getApplication(),
            ComponentName(getApplication(), HardMediaService::class.java),
            connectionCallbacks,
            null // optional Bundle
        )
        mediaBrowser.connect()
    }

    fun playPause() {
        val pbState = mediaController.playbackState.state
        Log.d(LOG_TAG, pbState.toString())

        if (pbState == PlaybackStateCompat.STATE_PLAYING) {
            mediaController.transportControls.pause()
        } else {
            mediaController.transportControls.play()
        }
    }

    fun skipToPrevious() {
        mediaController.transportControls.skipToPrevious()
    }

    fun skipToNext() {
        mediaController.transportControls.skipToNext()
    }

    fun disconnect() {
        mediaController.transportControls.stop()
        mediaBrowser.disconnect()
    }
}

class MainViewModelFactory @AssistedInject constructor(
    private val application: Application,
    @Assisted("activity") private val activity: MainActivity
): ViewModelProvider.AndroidViewModelFactory(application) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainViewModel(application, activity) as T
    }

    @AssistedFactory
    interface Factory  {

        fun create(@Assisted("activity") activity: MainActivity): MainViewModelFactory
    }
}
