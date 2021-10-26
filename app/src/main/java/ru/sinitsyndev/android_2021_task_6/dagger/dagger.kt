package ru.sinitsyndev.android_2021_task_6.dagger

import android.app.Application
import android.content.Context
import dagger.Component
import dagger.Module
import dagger.Provides
import ru.sinitsyndev.android_2021_task_6.MainActivity
import ru.sinitsyndev.android_2021_task_6.client.MainFragment
import ru.sinitsyndev.android_2021_task_6.service.HardMediaService
import ru.sinitsyndev.android_2021_task_6.service.Notificator
import ru.sinitsyndev.android_2021_task_6.service.data.PlayListRepository
import javax.inject.Singleton

@Component(modules = [AppModule::class])
interface AppComponent {
    fun inject(activity: MainActivity)
    fun inject(hardMediaService: HardMediaService)
    fun inject(mainFragment: MainFragment)
}

@Module
class AppModule(private val application: Application) {
    @Provides
    @Singleton
    fun providesApplication(): Application = application

    @Provides
    @Singleton
    fun providesApplicationContext(): Context = application

    @Provides
    fun providePlayListRepository(): PlayListRepository {
        return PlayListRepository(application.applicationContext)
    }

    @Provides
    fun provideNotificator(): Notificator {
        return Notificator(application.applicationContext)
    }
}
