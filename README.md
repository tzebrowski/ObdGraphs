# Yet another Android OBD client application

## About

This is a simple Android application that demonstrates the usage of ObdMetrics java framework.


| View    |           |
| ------------ | ---- |
| Dash view    |     ![Alt text](./res/screen5.png?raw=true "Dash view") |
| Gauge view   |   ![Alt text](./res/screen4.png?raw=true "Gauge view")   |
| Metrics view |    ![Alt text](./res/screen1.png?raw=true "Metrics view")  |
| Debug view |    ![Alt text](./res/screen3.png?raw=true "Debug view")  |
| Preferences view |    ![Alt text](./res/screen2.png?raw=true "Preferences view")  |




### Integration with ObdMetrics

<details>
<summary>Code example</summary>
<p>

```kotlin
import android.util.Log
import org.openobd2.core.workflow.State
import org.openobd2.core.workflow.Workflow

internal class DataLogger {

    private var mode1: Workflow =
        WorkflowFactory.mode1().equationEngine("rhino")
            .ecuSpecific(
                EcuSpecific
                    .builder()
                    .initSequence(Mode1CommandGroup.INIT)
                    .pidFile("mode01.json").build()
            )
            .observer(metricsAggregator)
            .lifecycle(lifecycle)
            .commandFrequency(80)
            .initialize()

   fun start() {

    var adapterName = "OBDII"
    var selectedPids = pref.getStringSet("pref.pids.generic", emptySet())!!
    var batchEnabled: Boolean = PreferencesHelper.isBatchEnabled(context)
   
    var ctx = WorkflowContext.builder()
        .filter(selectedPids.map { s -> s.toLong() }.toSet())
        .batchEnabled(PreferencesHelper.isBatchEnabled(context))
        .connection(BluetoothConnection(device.toString())).build()
    mode1.start(ctx)
   
   }
   
   fun stop() {
    mode1.stop()
   }  
}
```

</p>
</details>
