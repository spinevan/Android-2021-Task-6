package ru.sinitsyndev.android_2021_task_6.service

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import ru.sinitsyndev.android_2021_task_6.LOG_TAG
import android.media.MediaPlayer
import android.content.res.AssetFileDescriptor
import android.os.ResultReceiver
import java.io.IOException
import java.lang.IllegalStateException


private const val MY_MEDIA_ROOT_ID = "media_root_id"
private const val MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id"

//used guide
//https://developer.android.com/guide/topics/media-apps/audio-app/building-a-mediabrowserservice

class HardMediaService: MediaBrowserServiceCompat() {

    private var mediaSession: MediaSessionCompat? = null
    private lateinit var stateBuilder: PlaybackStateCompat.Builder
    private val mMediaPlayer: MediaPlayer? = null
    private val mMediaSessionCompat: MediaSessionCompat? = null

    override fun onCreate() {
        super.onCreate()

        // Create a MediaSessionCompat
        mediaSession = MediaSessionCompat(baseContext, LOG_TAG).apply {

            // Enable callbacks from MediaButtons and TransportControls
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                    or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
            stateBuilder = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
            setPlaybackState(stateBuilder.build())

            // MySessionCallback() has methods that handle callbacks from a media controller
            setCallback(heavyMediaSessionCallback)

            // Set the session's token so that client activities can communicate with it.
            setSessionToken(sessionToken)
        }

    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return  BrowserRoot(MY_MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        TODO("Not yet implemented")
    }

    private val heavyMediaSessionCallback = object: MediaSessionCompat.Callback() {
        override fun onPlay() {
//            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//            // Request audio focus for playback, this registers the afChangeListener
//
//            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
//                setOnAudioFocusChangeListener(afChangeListener)
//                setAudioAttributes(AudioAttributes.Builder().run {
//                    setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
//                    build()
//                })
//                build()
//            }
//            val result = am.requestAudioFocus(audioFocusRequest)
//            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
//                // Start the service
//                startService(Intent(context, MediaBrowserService::class.java))
//                // Set the session active  (and update metadata and state)
//                mediaSession.isActive = true
//                // start the player (custom call)
//                player.start()
//                // Register BECOME_NOISY BroadcastReceiver
//                registerReceiver(myNoisyAudioStreamReceiver, intentFilter)
//                // Put the service in the foreground, post notification
//                service.startForeground(id, myPlayerNotification)
//            }
        }

        override fun onStop() {
//            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//            // Abandon audio focus
//            am.abandonAudioFocusRequest(audioFocusRequest)
//            unregisterReceiver(myNoisyAudioStreamReceiver)
//            // Stop the service
//            service.stopSelf()
//            // Set the session inactive  (and update metadata and state)
//            mediaSession.isActive = false
//            // stop the player (custom call)
//            player.stop()
//            // Take the service out of the foreground
//            service.stopForeground(false)
        }

        override fun onPause() {
//            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//            // Update metadata and state
//            // pause the player (custom call)
//            player.pause()
//            // unregister BECOME_NOISY BroadcastReceiver
//            unregisterReceiver(myNoisyAudioStreamReceiver)
//            // Take the service out of the foreground, retain the notification
//            service.stopForeground(false)
        }
    }
}