import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.obd.graphs.bl.collector.InMemoryCarMetricsCollector
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.DataLogger
import org.obd.graphs.bl.datalogger.DataLoggerService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun dataLoggerService(): DataLogger {
        return  DataLoggerService()
    }


    @Provides
    @Singleton
    fun metricsCollector(dataLogger: DataLogger): MetricsCollector {
        return  InMemoryCarMetricsCollector(dataLogger)
    }

}