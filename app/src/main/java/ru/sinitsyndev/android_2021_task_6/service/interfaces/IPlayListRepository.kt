package ru.sinitsyndev.android_2021_task_6.service.interfaces

import ru.sinitsyndev.android_2021_task_6.service.data.Track

interface IPlayListRepository {

    fun getPlayList(): List<Track>?

}