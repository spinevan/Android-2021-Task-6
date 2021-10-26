package ru.sinitsyndev.android_2021_task_6.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.media.MediaBrowserServiceCompat
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.sinitsyndev.android_2021_task_6.CHANNEL_ID
import ru.sinitsyndev.android_2021_task_6.LOG_TAG
import ru.sinitsyndev.android_2021_task_6.MY_CHANNEL_NAME
import ru.sinitsyndev.android_2021_task_6.MY_MEDIA_ROOT_ID
import ru.sinitsyndev.android_2021_task_6.NOTIFICATION_ID
import ru.sinitsyndev.android_2021_task_6.appComponent
import ru.sinitsyndev.android_2021_task_6.service.data.PlayListRepository
import ru.sinitsyndev.android_2021_task_6.service.data.Track
import javax.inject.Inject

// used guide
// https://developer.android.com/guide/topics/media-apps/audio-app/building-a-mediabrowserservice

class HardMediaService :
    MediaBrowserServiceCompat(),
    MediaPlayer.OnCompletionListener,
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnBufferingUpdateListener,
    AudioManager.OnAudioFocusChangeListener {

    @Inject
    lateinit var repository: PlayListRepository

    @Inject
    lateinit var notificator: Notificator

    private var notificationManager: NotificationManager? = null

    private var playList = mutableListOf<Track>()
    private var currentTrack: Track? = null

    private lateinit var stateBuilder: PlaybackStateCompat.Builder
    private var mediaPlayer: MediaPlayer = MediaPlayer()
    private var mMediaSessionCompat: MediaSessionCompat? = null

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var trackImage: Bitmap? = null

    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        Log.d(LOG_TAG, "!!!CoroutineExceptionHandler $exception")
    }

    override fun onCreate() {
        super.onCreate()

        appComponent.inject(this)

        playList = repository.getPlayList()!!.toMutableList()

        notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        // Create a MediaSessionCompat
        mMediaSessionCompat = MediaSessionCompat(baseContext, LOG_TAG).apply {

            // Enable callbacks from MediaButtons and TransportControls
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                    or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
            stateBuilder = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PLAY_PAUSE
                )

            setPlaybackState(stateBuilder.build())
            setCallback(heavyMediaSessionCallback)

            // Set the session's token so that client activities can communicate with it.
            setSessionToken(sessionToken)
        }

        currentTrack = playList.firstOrNull()

        currentTrack?.let {
            mMediaSessionCompat?.setMetadata(createMetadataFromTrack(it, trackImage))
        }

        initMediaPlayer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(LOG_TAG, "onStartCommand")
        return super.onStartCommand(intent, flags, startId)
    }

    private fun setMediaPlaybackState(state: Int) {
        val playBackStateBuilder = PlaybackStateCompat.Builder()
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            playBackStateBuilder.setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE
                    or PlaybackStateCompat.ACTION_PAUSE
                    or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    or PlaybackStateCompat.ACTION_STOP
                    or PlaybackStateCompat.ACTION_PREPARE
            )
        } else {
            playBackStateBuilder.setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE
                    or PlaybackStateCompat.ACTION_PLAY
                    or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    or PlaybackStateCompat.ACTION_PREPARE
            )
        }
        playBackStateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0f)
        mMediaSessionCompat!!.setPlaybackState(playBackStateBuilder.build())
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        Log.d(LOG_TAG, "onGetRoot")

        val rootExtras = Bundle().apply {
            putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED", true)
            putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 1)
        }

        return BrowserRoot(MY_MEDIA_ROOT_ID, rootExtras)
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

    private fun initMediaPlayer() {
        mediaPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )
        mediaPlayer.setVolume(1.0f, 1.0f)
        mediaPlayer.setOnCompletionListener(this)
        mediaPlayer.setOnPreparedListener(this)
    }

    private fun startForegroundAndShowNotification() {
        createChannel()
        startForeground(
            NOTIFICATION_ID,
            notificator.getNotification(
                createMetadataFromTrack(currentTrack!!, trackImage),
                PlaybackStateCompat.STATE_PLAYING,
                sessionToken!!,
                null
            )
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val notificationChannel = NotificationChannel(
                CHANNEL_ID, MY_CHANNEL_NAME, importance
            )
            notificationManager?.createNotificationChannel(notificationChannel)
        }
    }

    private val heavyMediaSessionCallback = object : MediaSessionCompat.Callback() {

        override fun onPlay() {
            super.onPlay()

            Log.d(LOG_TAG, "heavyMediaSessionCallback onPlay")

            currentTrack?.let {
                if (mMediaSessionCompat?.isActive == true) {
                    // play after pause
                    mediaPlayer.start()
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    notify(PlaybackStateCompat.STATE_PLAYING)
                } else {
                    mMediaSessionCompat?.isActive = true
                    startForegroundAndShowNotification()
                    setMediaPlaybackState(PlaybackStateCompat.STATE_BUFFERING)
                    preparePlayerAndPlay(it.trackUri)
                }
            }

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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                // Abandon audio focus
                am.abandonAudioFocusRequest(getAudioFocusRequest())
            }
        }

        override fun onPause() {
            Log.d(LOG_TAG, "heavyMediaSessionCallback onPause")

            mediaPlayer.pause()
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED)
            notify(PlaybackStateCompat.STATE_PAUSED)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                // Abandon audio focus
                am.abandonAudioFocusRequest(getAudioFocusRequest())
            }
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            Log.d(LOG_TAG, "heavyMediaSessionCallback onSkipToNext")
            playNextTrack(true)
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            Log.d(LOG_TAG, "heavyMediaSessionCallback onSkipToPrevious")
            playNextTrack(false)
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
    }

    private fun playNextTrack(direct: Boolean) {
        var trackNumber = playList.indexOf(currentTrack)
        trackImage = null
        if (direct) {
            trackNumber++
            if (trackNumber >= playList.size)
                trackNumber = 0
        } else {
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
                mediaPlayer.reset()
            }
            preparePlayerAndPlay(it.trackUri)
            mMediaSessionCompat?.setMetadata(createMetadataFromTrack(it, trackImage))
            notify(PlaybackStateCompat.STATE_PLAYING)
        }
    }

    private fun preparePlayerAndPlay(trackUri: String) {
        try {
            mediaPlayer.setDataSource(trackUri)
            // mediaPlayer.prepare()
            mediaPlayer.prepareAsync()
        } catch (e: Exception) {
            Log.d(LOG_TAG, "Exception preparePlayerAndPlay $e")
            stopAll()
        } finally {
            notifyWithImage(PlaybackStateCompat.STATE_BUFFERING)
        }
    }

    private fun stopAll() {
        mediaPlayer.release()
        mMediaSessionCompat?.isActive = false
        setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED)
        stopSelf()
        stopForeground(true)
    }

    override fun onCompletion(mp: MediaPlayer?) {
        Log.d(LOG_TAG, "MediaPlayer onCompletion")
        playNextTrack(true)
    }

    override fun onPrepared(mp: MediaPlayer?) {
        Log.d(LOG_TAG, "MediaPlayer onPrepared")

        try {
            if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getAudioFocus()
            } else {
                    true
                }
            ) {
                mediaPlayer.start()
            }
        } catch (e: java.lang.Exception) {
            Log.d(LOG_TAG, "Exception onPrepared $e")
            stopAll()
        } finally {
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
            notifyWithImage(PlaybackStateCompat.STATE_PLAYING)
        }
    }

    override fun onBufferingUpdate(mp: MediaPlayer?, percent: Int) {
        Log.d(LOG_TAG, "MediaPlayer onBufferingUpdate")
    }

    private fun notify(state: Int) {
        notificationManager?.notify(
            NOTIFICATION_ID,
            notificator.getNotification(
                createMetadataFromTrack(currentTrack!!, trackImage),
                state,
                sessionToken!!,
                trackImage
            )
        )
        currentTrack?.let {
            mMediaSessionCompat?.setMetadata(createMetadataFromTrack(it, trackImage))
        }
        if (trackImage == null) notifyWithImage(state)
    }

    private fun notifyWithImage(state: Int) {

        serviceScope.launch(errorHandler) {
            try {
                trackImage = withContext(Dispatchers.IO) { // background thread
                    return@withContext repository.resolveUriAsBitmap(
                        this@HardMediaService,
                        Uri.parse(currentTrack?.bitmapUri)
                    )
                }
            } finally {
                notificationManager?.notify(
                    NOTIFICATION_ID,
                    notificator.getNotification(
                        createMetadataFromTrack(currentTrack!!, trackImage),
                        state,
                        sessionToken!!,
                        trackImage
                    )
                )
                currentTrack?.let {
                    mMediaSessionCompat?.setMetadata(createMetadataFromTrack(it, trackImage))
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getAudioFocus(): Boolean {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        // Request audio focus for playback, this registers the afChangeListener
        val audioFocusRequest = getAudioFocusRequest()
        val result = am.requestAudioFocus(audioFocusRequest)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getAudioFocusRequest(): AudioFocusRequest {
        val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
            setOnAudioFocusChangeListener(this@HardMediaService)
            setAudioAttributes(
                AudioAttributes.Builder().run {
                    setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    build()
                }
            )
            build()
        }
        return audioFocusRequest
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN ->
                mediaPlayer.start()
            AudioManager.AUDIOFOCUS_LOSS -> {
                mediaPlayer.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                mediaPlayer.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer.pause()
            }
        }
    }
}
