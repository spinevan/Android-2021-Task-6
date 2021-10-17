package ru.sinitsyndev.android_2021_task_6.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import ru.sinitsyndev.android_2021_task_6.LOG_TAG
import android.content.res.AssetFileDescriptor
import android.os.ResultReceiver
import android.util.Log
import java.io.IOException
import java.lang.IllegalStateException
import android.content.BroadcastReceiver
import androidx.core.app.NotificationManagerCompat

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.*
import android.media.browse.MediaBrowser
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.core.app.NotificationCompat

import androidx.media.session.MediaButtonReceiver
import ru.sinitsyndev.android_2021_task_6.CHANNEL_ID
import ru.sinitsyndev.android_2021_task_6.NOTIFICATION_ID


private const val MY_MEDIA_ROOT_ID = "media_root_id"
private const val MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id"

//used guide
//https://developer.android.com/guide/topics/media-apps/audio-app/building-a-mediabrowserservice

class HardMediaService: MediaBrowserServiceCompat(), MediaPlayer.OnCompletionListener {

    private var notificationManager: NotificationManager? = null

    private val builder by lazy {
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Test")
            .setGroup("Test")
            .setGroupSummary(false)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            //.setContentIntent(getPendingIntent())
            .setSilent(true)
            .setSmallIcon(R.drawable.sym_def_app_icon)
    }

    private lateinit var stateBuilder: PlaybackStateCompat.Builder
    private var mediaPlayer: MediaPlayer = MediaPlayer()
    private var mMediaSessionCompat: MediaSessionCompat? = null

    private val mNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                mediaPlayer.pause()
            //}
        }
    }


    override fun onCreate() {
        super.onCreate()

        notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        // Create a MediaSessionCompat
        mMediaSessionCompat = MediaSessionCompat(baseContext, LOG_TAG).apply {

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

        val testMetadata = MediaMetadataCompat.Builder()
        testMetadata.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Artist1")
        testMetadata.putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Song title 1")
        testMetadata.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "https://freepd.com/music/Coy Koi.mp3")
        testMetadata.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, "https://freepd.com/music/Coy Koi.mp3")
        mMediaSessionCompat?.setMetadata(testMetadata.build())

        initMediaPlayer()

    }

    private fun setMediaPlaybackState(state: Int) {
        val playBackstateBuilder = PlaybackStateCompat.Builder()
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            playBackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PAUSE)
        } else {
            playBackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PLAY)
        }
        playBackstateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0f)
        mMediaSessionCompat!!.setPlaybackState(playBackstateBuilder.build())
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        Log.d(LOG_TAG, "onGetRoot")

        val rootExtras = Bundle().apply {
            putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED", true)
            putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 1)
        }

        return  BrowserRoot(MY_MEDIA_ROOT_ID, rootExtras)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
//        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()
//        //val testItem = MediaBrowserCompat.MediaItem()
//        result.sendResult(mediaItems)
        Log.d(LOG_TAG, "onLoadChildren")

        val mediaItem1 = createMediaItem("https://freepd.com/music/Coy Koi.mp3",
            "test",
            Uri.parse("https://www.pelicanwater.com/blog/wp-content/uploads/2019/02/cropped-Snow_Blog_Jan_2019-01.jpg"),
            Uri.parse("https://freepd.com/music/Coy Koi.mp3")
        )
        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()
        mediaItems.add(mediaItem1)

        result.sendResult(mediaItems)

    }

    private fun createMediaItem(
        mediaId: String,
        folderName: String,
        iconUri: Uri,
        mediaUri: Uri
    ): MediaBrowserCompat.MediaItem {
        val mediaDescriptionBuilder = MediaDescriptionCompat.Builder()
        mediaDescriptionBuilder.setMediaId(mediaId)
        mediaDescriptionBuilder.setTitle(folderName)
        mediaDescriptionBuilder.setIconUri(iconUri)
        mediaDescriptionBuilder.setMediaUri(mediaUri)
        return MediaBrowserCompat.MediaItem(
            mediaDescriptionBuilder.build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }

//    private fun startService() {
//        Intent(this, HardMediaService::class.java).also { intent ->
//            startService(intent)
//        }
//    }

    private fun initMediaPlayer() {
        //mediaPlayer = MediaPlayer()
        //mediaPlayer?.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        mediaPlayer.setVolume(1.0f, 1.0f)
        mediaPlayer.setOnCompletionListener(this)
    }

    private fun startForegroundAndShowNotification() {
        createChannel()
        //val notification = getNotification("content")
        //startForeground(NOTIFICATION_ID, notification)
        startForeground(NOTIFICATION_ID, builder.build() )

//        val intent = Intent(this, HardMediaService::class.java)
//        startService(intent)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "test media"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val notificationChannel = NotificationChannel(
                CHANNEL_ID, channelName, importance
            )
            notificationManager?.createNotificationChannel(notificationChannel)
        }
    }


    private fun getNotification(content: String) = builder.setContentText(content).build()

    private val heavyMediaSessionCallback = object: MediaSessionCompat.Callback() {

        override fun onPlay() {
            super.onPlay()

            Log.d(LOG_TAG, "heavyMediaSessionCallback onPlay")

            mMediaSessionCompat?.isActive = true

           //startService()
            startForegroundAndShowNotification()
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)

            val testMetadata = MediaMetadataCompat.Builder()
            testMetadata.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Artist1")
            testMetadata.putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Song title 1")
            testMetadata.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "https://freepd.com/music/Coy Koi.mp3")
            testMetadata.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, "https://freepd.com/music/Coy Koi.mp3")
            mMediaSessionCompat?.setMetadata(testMetadata.build())

            mediaPlayer.setDataSource("https://freepd.com/music/Coy Koi.mp3")
            mediaPlayer.prepare()
            mediaPlayer.start()

            //showPlayingNotification()
            //mMediaPlayer?.start()

//            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            // Request audio focus for playback, this registers the afChangeListener

//             val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
//                //setOnAudioFocusChangeListener(afChangeListener)
//                setAudioAttributes(AudioAttributes.Builder().run {
//                    setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
//                    build()
//                })
//                build()
//            }
//            val result = am.requestAudioFocus(audioFocusRequest)
//            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
//                // Start the service
//                startService(Intent(applicationContext, HardMediaService::class.java))
//                // Set the session active  (and update metadata and state)
//                mediaSession?.isActive = true
//                // start the player (custom call)
//                player.start()
//                // Register BECOME_NOISY BroadcastReceiver
//                //registerReceiver(myNoisyAudioStreamReceiver, intentFilter)
//                // Put the service in the foreground, post notification
//                service.startForeground(id, myPlayerNotification)
//            }
        }

        override fun onStop() {
            Log.d(LOG_TAG, "heavyMediaSessionCallback onStop")
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
            Log.d(LOG_TAG, "heavyMediaSessionCallback onPause")
//            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//            // Update metadata and state
//            // pause the player (custom call)
//            player.pause()
//            // unregister BECOME_NOISY BroadcastReceiver
//            unregisterReceiver(myNoisyAudioStreamReceiver)
//            // Take the service out of the foreground, retain the notification
//            service.stopForeground(false)
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            Log.d(LOG_TAG, "heavyMediaSessionCallback onSkipToNext")

            val testMetadata = MediaMetadataCompat.Builder()
            testMetadata.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Artist1")
            testMetadata.putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Song title 1")
            testMetadata.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "https://freepd.com/music/Coy Koi.mp3")
            testMetadata.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, "https://freepd.com/music/Coy Koi.mp3")
            mMediaSessionCompat?.setMetadata(testMetadata.build())

        }

        override fun onPrepare() {
            super.onPrepare()
            Log.d(LOG_TAG, "heavyMediaSessionCallback onPrepare")

        }

    }

    private fun showPlayingNotification() {
//        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this@HardMediaService)
//        builder.addAction(
//            NotificationCompat.Action(
//                R.drawable.ic_media_pause,
//                "Pause",
//                MediaButtonReceiver.buildMediaButtonPendingIntent(
//                    this,
//                    PlaybackStateCompat.ACTION_PLAY_PAUSE
//                )
//            )
//        )
//        builder.setStyle(
//            androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0)
//                .setMediaSession(mMediaSessionCompat!!.sessionToken)
//        )
//        builder.setSmallIcon(R.mipmap.sym_def_app_icon)
//        NotificationManagerCompat.from(this@HardMediaService).notify(1, builder.build())
    }

    override fun onCompletion(mp: MediaPlayer?) {
        Log.d(LOG_TAG, "MediaPlayer onCompletion")
        mediaPlayer.release()
    }
}