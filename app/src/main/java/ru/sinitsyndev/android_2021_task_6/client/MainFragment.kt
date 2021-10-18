package ru.sinitsyndev.android_2021_task_6.client

import android.app.Activity
import android.content.ComponentName
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.sinitsyndev.android_2021_task_6.LOG_TAG
import ru.sinitsyndev.android_2021_task_6.R
import ru.sinitsyndev.android_2021_task_6.databinding.FragmentMainBinding
import ru.sinitsyndev.android_2021_task_6.service.HardMediaService

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var mediaController: MediaControllerCompat

    private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {

            // Get the token for the MediaSession
            mediaBrowser.sessionToken.also { token ->

                // Create a MediaControllerCompat
                mediaController = MediaControllerCompat(
                    context, // Context
                    token
                )

                // Save the controller
                MediaControllerCompat.setMediaController(context as Activity, mediaController)
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
            //metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            Log.d(LOG_TAG, "onPlaybackStateChanged ${state.toString()}")
        }

        override fun onAudioInfoChanged(info: MediaControllerCompat.PlaybackInfo?) {
            super.onAudioInfoChanged(info)
            Log.d(LOG_TAG, "onAudioInfoChanged")
        }
    }

    fun buildTransportControls() {
        mediaController = MediaControllerCompat.getMediaController(context as Activity)

//        binding.selectFirst.setOnClickListener {
//            mediaController.transportControls.skipToNext()
//        }

        binding.playBtn.setOnClickListener {
            val pbState = mediaController.playbackState.state
            Log.d(LOG_TAG, pbState.toString())
            //Log.d(LOG_TAG, mediaBrowser.root)

            if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                mediaController.transportControls.pause()
                binding.playBtn.text = "Play"
            } else {
                mediaController.transportControls.play()
                binding.playBtn.text = "Pause"
            }
        }

        binding.NextBtn.setOnClickListener {
            mediaController.transportControls.skipToNext()
            binding.playBtn.text = "Pause"
        }

        binding.prevBtn.setOnClickListener {
            mediaController.transportControls.skipToPrevious()
            binding.playBtn.text = "Pause"
        }

        // Display the initial state
//        val metadata = mediaController.metadata
//        val pbState = mediaController.playbackState

        //Log.d(LOG_TAG, "metadata " + metadata.toString())
        Log.d(LOG_TAG, "initial Metadata ${mediaController.metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)}")
        // Register a Callback to stay in sync
        mediaController.registerCallback(controllerCallback)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mediaBrowser = MediaBrowserCompat(
            context,
            context?.let { ComponentName(it, HardMediaService::class.java) },
            connectionCallbacks,
            null // optional Bundle
        )

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mediaBrowser.connect()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaBrowser.disconnect()
        _binding = null

    }

    override fun onDestroy() {
        super.onDestroy()
//        mediaBrowser.disconnect()
    }

}