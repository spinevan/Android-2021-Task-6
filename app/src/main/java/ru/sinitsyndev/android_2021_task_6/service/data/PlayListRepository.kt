package ru.sinitsyndev.android_2021_task_6.service.data

import android.content.res.Resources
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import ru.sinitsyndev.android_2021_task_6.R
import ru.sinitsyndev.android_2021_task_6.service.interfaces.IPlayListRepository
import java.lang.reflect.Type

class PlayListRepository(private val resources: Resources): IPlayListRepository {

    override fun getPlayList(): List<Track>? {

        val jsonString = loadFromJson()

        val moshi = Moshi.Builder().build()
//        val listOfTracksType: Type = Types.newParameterizedType(List::class.java, Track::class.java)
        //val jsonAdapter: List<Track> = moshi.adapter(listOfTracksType)

        val listOfTracksType: Type = Types.newParameterizedType( List::class.java, Track::class.java)

        val adapter: JsonAdapter<List<Track>> = moshi.adapter(listOfTracksType)
        return adapter.fromJson(jsonString)
    }

    private fun loadFromJson(): String {

        return resources.openRawResource(R.raw.playlist)
            .bufferedReader().use { it.readText() }

    }
}