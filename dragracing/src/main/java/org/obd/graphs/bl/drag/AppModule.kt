package org.obd.graphs.bl.drag

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providerDragRacingRegistry(): DragRacingResultRegistry {
        return InMemoryDragRacingRegistry()
    }

    @Provides
    @Singleton
    fun providerSimplyImpl(): SimpleInt {
        return SimpleImpl()
    }
}