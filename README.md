# Yet another Android OBD client application

## About

This is a simple Android application that demonstrates the usage of OpenElm327 java framework.


| View    |           |
| ------------ | ---- |
| Dash view    |     ![Alt text](./res/screen5.png?raw=true "Dash view") |
| Gauge view   |   ![Alt text](./res/screen4.png?raw=true "Gauge view")   |
| Metrics view |    ![Alt text](./res/screen1.png?raw=true "Metrics view")  |
| Debug view |    ![Alt text](./res/screen3.png?raw=true "Debug view")  |
| Preferences view |    ![Alt text](./res/screen2.png?raw=true "Preferences view")  |




### Integration with OpenElm327

```kotlin
import android.util.Log
import org.openobd2.core.workflow.State
import org.openobd2.core.workflow.Workflow

internal class DataLogger {

    private lateinit var workflow: Workflow
    private lateinit var device: String

    init {
        Thread.currentThread().contextClassLoader
            .getResourceAsStream("generic.json").use {
                workflow = Workflow.mode1()
                    .source(it)
                    .evaluationEngine("rhino")
                    .subscriber(ModelChangePublisher())
                    .state( object : State {
                        override fun starting() {
                            Log.i("DATA_LOGGER_DL", "Start collecting process for Device: $device")
                        }
                        override fun completed() {
                            Log.i("DATA_LOGGER_DL", "Collecting process completed for Device: $device")
                        }

                        override fun stopping() {
                            Log.i("DATA_LOGGER_DL", "Stop collecting process for Device: $device")
                        }
                    })
                    .build()
            }
    }

    fun stop() {
        workflow.stop()
    }

    fun start(btDeviceName: String) {
        this.device = btDeviceName
        workflow.start(BluetoothConnection(btDeviceName))
    }
}
```


