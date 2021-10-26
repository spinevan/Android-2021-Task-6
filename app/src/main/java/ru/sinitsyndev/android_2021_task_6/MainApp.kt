package ru.sinitsyndev.android_2021_task_6

import android.app.Application
import android.content.Context
import ru.sinitsyndev.android_2021_task_6.dagger.AppComponent
import ru.sinitsyndev.android_2021_task_6.dagger.AppModule
import ru.sinitsyndev.android_2021_task_6.dagger.DaggerAppComponent

class MainApp: Application() {
    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build()
    }
}

val Context.appComponent: AppComponent
    get() = when (this) {
        is MainApp -> appComponent
        else -> applicationContext.appComponent
    }