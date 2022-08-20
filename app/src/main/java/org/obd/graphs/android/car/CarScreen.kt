package org.obd.graphs.android.car

import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.lifecycle.DefaultLifecycleObserver
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.DataLogger
import org.obd.graphs.bl.datalogger.MetricsAggregator
import org.obd.graphs.setCarContext
import org.obd.graphs.ui.common.MetricsProvider
import org.obd.graphs.ui.preferences.Prefs
import org.obd.graphs.ui.preferences.getStringSet
import java.lang.ref.WeakReference

private const val LOG_KEY = "CarScreen"

class CarScreen(carContext: CarContext) : Screen(carContext),
    DefaultLifecycleObserver {

    override fun onGetTemplate(): Template {
        val data = MetricsProvider().findMetrics(aaPIDs())
        if (data.size == 0 ) {
            return PaneTemplate.Builder(Pane.Builder().setLoading(true).build())
                .setHeaderAction(Action.BACK)
                .setTitle(carContext.getString(R.string.car_hardware_info))
                .build()
        } else {
            try {
                val paneBuilder = Pane.Builder()
                val histogram = DataLogger.instance.diagnostics().histogram()

                data.forEach {
                    val sensorRow: Row.Builder =
                        Row.Builder().setTitle(CarText.Builder(it.command.pid.description).build())

                    val info = StringBuilder().apply {
                        append("value=${it.valueToString()}\n")
                    }

                    histogram.findBy(it.command.pid).let{ hist ->
                        info.append("min=${hist.min}")
                        info.append(" max=${hist.max}")
                        info.append(" avg=${hist.mean} \n")
                    }
                    val spannableString = colorize(info)

                    sensorRow.addText(spannableString)
                    paneBuilder.addRow(sensorRow.build())
                }

                return PaneTemplate.Builder(paneBuilder.build())
                    .setHeaderAction(Action.BACK)
                    .setTitle(carContext.getString(R.string.car_hardware_info))
                    .build()

            }catch (e: Exception) {
                Log.e(LOG_KEY,"Failed to build Template",e)
                return PaneTemplate.Builder(Pane.Builder().setLoading(true).build())
                    .setHeaderAction(Action.BACK)
                    .setTitle(carContext.getString(R.string.car_hardware_info))
                    .build()
            }
        }
    }

    private fun aaPIDs() = Prefs.getStringSet("pref.aa.pids.selected").map { s -> s.toLong() }.toSet()


    init {

        setCarContext(carContext)

        lifecycle.addObserver(this)
        val aaPIDs = aaPIDs()

        Log.i(LOG_KEY,"Selected PIDs = $aaPIDs")

        val data = MetricsProvider().findMetrics(aaPIDs)
        MetricsAggregator.metrics.observe(this) {
            it?.let {
                data.find { c -> c.command.pid.id ==  it.command.pid.id }?.let {
                    invalidate()
                }
            }
        }
    }
}