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

import android.R.drawable
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.res.Resources
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
import ru.sinitsyndev.android_2021_task_6.service.data.PlayListRepository
import ru.sinitsyndev.android_2021_task_6.service.data.Track


private const val MY_MEDIA_ROOT_ID = "media_root_id"

//used guide
//https://developer.android.com/guide/topics/media-apps/audio-app/building-a-mediabrowserservice

class HardMediaService: MediaBrowserServiceCompat(), MediaPlayer.OnCompletionListener,
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnBufferingUpdateListener {

    private var notificationManager: NotificationManager? = null

    private var repository: PlayListRepository? = null
    private var playList = mutableListOf<Track>()
    private var currentTrack: Track? = null

//    private val builder by lazy {
//        NotificationCompat.Builder(this, CHANNEL_ID)
//            .setContentTitle("Test")
//            .setGroup("Test")
//            .setGroupSummary(false)
//            .setDefaults(NotificationCompat.DEFAULT_ALL)
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            //.setContentIntent(getPendingIntent())
//            .setSilent(true)
//            .setSmallIcon(drawable.sym_def_app_icon)
//    }

    private val notificator by lazy {
        Notificator(this)
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

        repository = PlayListRepository(applicationContext.resources)
        playList = repository!!.getPlayList()!!.toMutableList()


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

        currentTrack = playList.firstOrNull()

        currentTrack?.let {
           mMediaSessionCompat?.setMetadata(createMetadataFromTrack(it))
        }

        initMediaPlayer()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(LOG_TAG, "onStartCommand")
        return super.onStartCommand(intent, flags, startId)
    }

    private fun setMediaPlaybackState(state: Int) {
        val playBackstateBuilder = PlaybackStateCompat.Builder()
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            playBackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PAUSE)
        } else {
            playBackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE
                    or PlaybackStateCompat.ACTION_PLAY
                    or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    or PlaybackStateCompat.ACTION_STOP
            )
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
        Log.d(LOG_TAG, "onLoadChildren")

        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()
        playList.forEach {
            mediaItems.add(createMediaItemFromTrack(it))
        }

        result.sendResult(mediaItems)

    }

    private fun createMediaItemFromTrack(track: Track): MediaBrowserCompat.MediaItem {
        val mediaDescriptionBuilder = MediaDescriptionCompat.Builder()
        mediaDescriptionBuilder.setMediaId(track.trackUri)
        mediaDescriptionBuilder.setTitle(track.title)
        mediaDescriptionBuilder.setIconUri(Uri.parse(track.bitmapUri))
        mediaDescriptionBuilder.setMediaUri(Uri.parse(track.trackUri))
        return MediaBrowserCompat.MediaItem(
            mediaDescriptionBuilder.build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }

    private fun createMetadataFromTrack(track: Track):MediaMetadataCompat {
        val metadata = MediaMetadataCompat.Builder()
        metadata.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
        metadata.putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
        metadata.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, track.trackUri)
        metadata.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, track.trackUri)
        return metadata.build()
    }


    private fun initMediaPlayer() {
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        mediaPlayer.setVolume(1.0f, 1.0f)
        mediaPlayer.setOnCompletionListener(this)
        mediaPlayer.setOnPreparedListener(this)
    }

    private fun startForegroundAndShowNotification() {
        createChannel()
//        startForeground(NOTIFICATION_ID, builder.build() )
        startForeground(NOTIFICATION_ID, notificator.getNotification(createMetadataFromTrack(currentTrack!!),
            PlaybackStateCompat.STATE_PLAYING,
            sessionToken!!
        ) )
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


    //private fun getNotification(content: String) = builder.setContentText(content).build()

    private val heavyMediaSessionCallback = object: MediaSessionCompat.Callback() {

        override fun onPlay() {
            super.onPlay()

            Log.d(LOG_TAG, "heavyMediaSessionCallback onPlay")

            currentTrack?.let {
                if (mMediaSessionCompat?.isActive == true) {
                    //play after pause
                    mediaPlayer.start()
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
                } else {
                    mMediaSessionCompat?.isActive = true
                    startForegroundAndShowNotification()
                    setMediaPlaybackState(PlaybackStateCompat.STATE_BUFFERING)
                    mediaPlayer.setDataSource(it.trackUri)
                    mediaPlayer.prepare()
                    mediaPlayer.start()
                }
            }

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

            mediaPlayer.stop()
            mediaPlayer.release()
            mMediaSessionCompat?.isActive = false
            setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED)
            stopSelf()
            stopForeground(true)

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

            mediaPlayer.pause()
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED)

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
            playNextTrack(true)
            //mMediaSessionCompat?.setMetadata(testMetadata.build())

        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            playNextTrack(false)
            Log.d(LOG_TAG, "heavyMediaSessionCallback onSkipToPrevious")
        }

        override fun onFastForward() {
            Log.d(LOG_TAG, "heavyMediaSessionCallback onFastForward")
            super.onFastForward()
        }

        override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
            Log.d(LOG_TAG, "heavyMediaSessionCallback onCommand")
            super.onCommand(command, extras, cb)
        }

        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            Log.d(LOG_TAG, "heavyMediaSessionCallback onMediaButtonEvent")
            return super.onMediaButtonEvent(mediaButtonEvent)
        }

        override fun onPrepare() {
            super.onPrepare()
            Log.d(LOG_TAG, "heavyMediaSessionCallback onPrepare")

        }

        override fun onSkipToQueueItem(id: Long) {
            super.onSkipToQueueItem(id)
            Log.d(LOG_TAG, "heavyMediaSessionCallback onSkipToQueueItem")
        }

    }

    private fun playNextTrack( direct: Boolean) {
        var trackNumber = playList.indexOf(currentTrack)
        if (direct) {
            trackNumber++
            if (trackNumber >= playList.size)
                trackNumber = 0
        }else{
            if (trackNumber > 0)
                trackNumber--
        }
        currentTrack = playList[trackNumber]
        currentTrack?.let {

            setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT)

            if (mMediaSessionCompat?.isActive == false) {
                startForegroundAndShowNotification()
            } else {
                mMediaSessionCompat?.isActive = true

                setMediaPlaybackState(PlaybackStateCompat.STATE_BUFFERING)
                //mediaPlayer.stop()
                mediaPlayer.reset()
                mediaPlayer.setDataSource(it.trackUri)
                mediaPlayer.prepare()
                mediaPlayer.start()
            }

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
        mMediaSessionCompat?.isActive = false
        setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED)
        stopSelf()
        stopForeground(true)
    }

    override fun onPrepared(mp: MediaPlayer?) {
        Log.d(LOG_TAG, "onPrepared")
        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
    }

    override fun onBufferingUpdate(mp: MediaPlayer?, percent: Int) {
        Log.d(LOG_TAG, "onBufferingUpdate")
    }
}