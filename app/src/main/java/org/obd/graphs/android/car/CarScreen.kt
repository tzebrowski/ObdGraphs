package org.obd.graphs.android.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.lifecycle.DefaultLifecycleObserver
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.MetricsAggregator
import org.obd.graphs.bl.datalogger.slowPIDs
import org.obd.graphs.ui.common.MetricsProvider


class CarScreen(carContext: CarContext) : Screen(carContext),
    DefaultLifecycleObserver {

    override fun onGetTemplate(): Template {
        val data = MetricsProvider().findMetrics(slowPIDs())
        if (data.size == 0 ) {
            val paneBuilder = Pane.Builder()
            paneBuilder.setLoading(true)
            return PaneTemplate.Builder(paneBuilder.build())
                .setHeaderAction(Action.BACK)
                .setTitle(carContext.getString(R.string.car_hardware_info))
                .build()
        }else {

            val paneBuilder = Pane.Builder()

            data.forEach {
                val sensorRow: Row.Builder = Row.Builder()
                    .setTitle(CarText.Builder(it.command.pid.description).build())

                val info = StringBuilder()
                info.append("value = ${it.valueToString()}\n")
                info.append("min = max = \n")
                sensorRow.addText(info)
                paneBuilder.addRow(sensorRow.build())
            }

            return PaneTemplate.Builder(paneBuilder.build())
                .setHeaderAction(Action.BACK)
                .setTitle(carContext.getString(R.string.car_hardware_info))
                .build()
        }
    }

    init {
        lifecycle.addObserver(this)
        val data = MetricsProvider().findMetrics(slowPIDs())
        MetricsAggregator.metrics.observe(this) {
            it?.let {
                data.find { c -> c.command.pid.id ==  it.command.pid.id }?.let {
                    invalidate()
                }
            }
        }
    }
}