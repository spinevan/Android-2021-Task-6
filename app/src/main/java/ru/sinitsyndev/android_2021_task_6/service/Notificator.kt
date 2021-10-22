package ru.sinitsyndev.android_2021_task_6.service

import android.app.NotificationManager
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import ru.sinitsyndev.android_2021_task_6.CHANNEL_ID
import androidx.media.session.MediaButtonReceiver

import android.R.attr.description

import android.R
import android.R.attr
import android.app.Notification
import android.app.PendingIntent
import android.content.Context

import androidx.core.content.ContextCompat
import android.content.Intent

import ru.sinitsyndev.android_2021_task_6.MainActivity


class Notificator(private val service: Context){

    private val REQUEST_CODE = 501

    private val playAction = NotificationCompat.Action(
        R.drawable.ic_media_play,
        "Play",
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_PLAY)
    )

    private val pauseAction = NotificationCompat.Action(
        R.drawable.ic_media_pause,
        "Play",
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_PAUSE)
    )

    private val prevAction = NotificationCompat.Action(
        R.drawable.ic_media_previous,
        "Prev",
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
    )

    private val nextAction = NotificationCompat.Action(
        R.drawable.ic_media_next,
        "Next",
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
    )

    fun getNotification(metadata: MediaMetadataCompat, state: Int, token: MediaSessionCompat.Token): Notification {
        val isPlaying = state == PlaybackStateCompat.STATE_PLAYING
        val builder = buildNotification(state, token, isPlaying, metadata.description)

        return builder.build()
    }

    private fun buildNotification(state: Int,
                                  token: MediaSessionCompat.Token,
                                  isPlaying: Boolean,
                                  description: MediaDescriptionCompat
    ): NotificationCompat.Builder {

        val builder = NotificationCompat.Builder(service, CHANNEL_ID)
        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(token)
                .setShowActionsInCompactView(
                    0,
                    1,
                    2
                )
                .setShowCancelButton(true)
                .setCancelButtonIntent(
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        service,
                        PlaybackStateCompat.ACTION_STOP
                    )
                )
        )
            .setColor(ContextCompat.getColor(service, R.color.background_light))
            .setSmallIcon(R.drawable.sym_def_app_icon)
            .setContentIntent(createContentIntent()) //
            .setContentTitle(description.title) //
            .setContentText(description.subtitle)
            .setSilent(true)
//            .setLargeIcon(
//                MusicLibrary.getAlbumBitmap(
//                    service,
//                    attr.description.getMediaId()
//                )
//            )
            // When notification is deleted (when playback is paused and notification can be
            // deleted) fire MediaButtonPendingIntent with ACTION_STOP.
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    service, PlaybackStateCompat.ACTION_STOP
                )
            ) // Show controls on lock screen even when user hides sensitive content.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        builder.addAction(prevAction)

        if (isPlaying) {
            builder.addAction(pauseAction)
        } else {
            builder.addAction(playAction)
        }

        builder.addAction(nextAction)

        return builder
    }

    private fun createContentIntent(): PendingIntent {
        val openIntent = Intent(service, MainActivity::class.java)
        openIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(
            service, REQUEST_CODE, openIntent, PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

}