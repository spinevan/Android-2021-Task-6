package ru.sinitsyndev.android_2021_task_6.service

import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import ru.sinitsyndev.android_2021_task_6.service.data.Track

fun createMediaItemFromTrack(track: Track): MediaBrowserCompat.MediaItem {

    val mediaDescriptionBuilder = MediaDescriptionCompat.Builder()
    mediaDescriptionBuilder.setMediaId(track.trackUri)
    mediaDescriptionBuilder.setTitle(track.title)
    mediaDescriptionBuilder.setIconUri(Uri.parse(track.bitmapUri))
    mediaDescriptionBuilder.setMediaUri(Uri.parse(track.trackUri))
    return MediaBrowserCompat.MediaItem(
        mediaDescriptionBuilder.build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
    )
}

fun createMetadataFromTrack(track: Track, trackImage: Bitmap?): MediaMetadataCompat {

    val metadata = MediaMetadataCompat.Builder()
    metadata.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
    metadata.putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
    metadata.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, track.trackUri)
    metadata.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, track.trackUri)
    metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, trackImage)
    return metadata.build()
}
