package ru.sinitsyndev.android_2021_task_6.service.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.HttpException
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.sinitsyndev.android_2021_task_6.LOG_TAG
import ru.sinitsyndev.android_2021_task_6.NOTIFICATION_LARGE_ICON_SIZE
import ru.sinitsyndev.android_2021_task_6.R
import ru.sinitsyndev.android_2021_task_6.service.interfaces.IPlayListRepository
import java.lang.reflect.Type
import javax.inject.Inject

class PlayListRepository @Inject constructor(private val context: Context) : IPlayListRepository {

    override fun getPlayList(): List<Track>? {

        val jsonString = loadFromJson()
        val moshi = Moshi.Builder().build()
        val listOfTracksType: Type = Types.newParameterizedType(List::class.java, Track::class.java)

        val adapter: JsonAdapter<List<Track>> = moshi.adapter(listOfTracksType)
        return adapter.fromJson(jsonString)
    }

    private fun loadFromJson(): String {
        return context.resources.openRawResource(R.raw.playlist)
            .bufferedReader().use { it.readText() }
    }

    suspend fun resolveUriAsBitmap(context: Context, uri: Uri): Bitmap? {

        return try {
            withContext(Dispatchers.IO) {
                Glide.with(context)
                    .asBitmap()
                    .load(uri)
                    .submit(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE)
                    .get()
            }
        } catch (e: HttpException) {
            Log.d(LOG_TAG, "resolveUriAsBitmap $e")
            null
        }
    }
}
