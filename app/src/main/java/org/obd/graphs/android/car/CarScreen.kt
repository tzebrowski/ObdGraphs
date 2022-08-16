package org.obd.graphs.android.car

import android.text.Spannable
import android.text.SpannableString
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.lifecycle.DefaultLifecycleObserver
import org.obd.graphs.CarApplicationContext
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.DataLogger
import org.obd.graphs.bl.datalogger.MetricsAggregator
import org.obd.graphs.bl.datalogger.slowPIDs
import org.obd.graphs.ui.common.MetricsProvider
import java.lang.ref.WeakReference

class CarScreen(carContext: CarContext) : Screen(carContext),
    DefaultLifecycleObserver {

    override fun onGetTemplate(): Template {
        val data = MetricsProvider().findMetrics(slowPIDs())
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
                    val histogram = histogram.findBy(it.command.pid)
                    val info = StringBuilder()
                    info.append("value=${it.valueToString()}\n")
                    info.append("min=${histogram.min}")
                    info.append(" max=${histogram.max}")
                    info.append(" avg=${histogram.mean} \n")

                    val spannableString = colorize(info)

                    sensorRow.addText(spannableString)
                    paneBuilder.addRow(sensorRow.build())
                }

                return PaneTemplate.Builder(paneBuilder.build())
                    .setHeaderAction(Action.BACK)
                    .setTitle(carContext.getString(R.string.car_hardware_info))
                    .build()

            }catch (e: Exception) {
                Log.e("CarScreen","Failed to build Template",e)
                return PaneTemplate.Builder(Pane.Builder().setLoading(true).build())
                    .setHeaderAction(Action.BACK)
                    .setTitle(carContext.getString(R.string.car_hardware_info))
                    .build()
            }
        }
    }

    private fun colorize(info: StringBuilder): SpannableString = SpannableString(info).apply {
            var txt = "value="
            var i = indexOf(txt)

            setSpan(
                ForegroundCarColorSpan.create(CarColor.GREEN)
                , i, i + txt.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            txt = "min="
            i = indexOf(txt)

            setSpan(
                ForegroundCarColorSpan.create(CarColor.GREEN), i, i + txt.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )


            txt = "max="
            i = indexOf(txt)

            setSpan(
                ForegroundCarColorSpan.create(CarColor.GREEN), i, i + txt.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            txt = "avg="
            i = indexOf(txt)

            setSpan(
                ForegroundCarColorSpan.create(CarColor.GREEN), i, i + txt.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }


    init {
        CarApplicationContext = WeakReference(carContext)
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