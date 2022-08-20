package org.obd.graphs.aa

import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.DataLogger
import org.obd.graphs.bl.datalogger.DataLoggerService
import org.obd.graphs.bl.datalogger.MetricsAggregator
import org.obd.graphs.setCarContext
import org.obd.graphs.ui.common.MetricsProvider
import org.obd.graphs.ui.common.toNumber
import org.obd.graphs.ui.preferences.Prefs
import org.obd.graphs.ui.preferences.getStringSet


private const val LOG_KEY = "CarScreen"

class CarScreen(carContext: CarContext) : Screen(carContext),
    DefaultLifecycleObserver {

    override fun onGetTemplate(): Template {
        val data = MetricsProvider().findMetrics(aaPIDs())
        if (data.size == 0) {
            return PaneTemplate.Builder(Pane.Builder().setLoading(true).build())
                .setHeaderAction(Action.BACK)
                .setTitle(carContext.getString(R.string.pref_aa_car_no_pids_selected))
                .build()
        } else {
            try {
                val histogram = DataLogger.instance.diagnostics().histogram()
                val listBuilder = ItemList.Builder()

                data.forEach {
                    val info = StringBuilder().apply {
                        append("value=${it.valueToString()}\n")
                    }
                    histogram.findBy(it.command.pid).let { hist ->
                        info.append("min=${it.toNumber(hist.min)}")
                        info.append(" max=${it.toNumber(hist.max)}")
                        info.append(" avg=${it.toNumber(hist.mean)}")
                    }

                    listBuilder.addItem(
                        Row.Builder()
                            .setTitle(
                                CarText.Builder(it.command.pid.description.replace("\n", " "))
                                    .build()
                            )
                            .addText(colorize(info))
                            .build()
                    )
                }

                return ListTemplate.Builder()
                    .setSingleList(listBuilder.build())
                    .setTitle(carContext.getString(R.string.pref_aa_car_hardware_info))
                    .setHeaderAction(Action.BACK)
                    .setActionStrip(actions())
                    .build()

            } catch (e: Exception) {
                Log.e(LOG_KEY, "Failed to build Template", e)
                return PaneTemplate.Builder(Pane.Builder().setLoading(true).build())
                    .setHeaderAction(Action.BACK)
                    .setTitle(carContext.getString(R.string.pref_aa_car_hardware_info))
                    .build()
            }
        }
    }

    private fun actions(): ActionStrip = ActionStrip.Builder()
        .addAction(Action.Builder()
            .setIcon(
                CarIcon.Builder(
                    IconCompat.createWithResource(
                        carContext,
                        R.drawable.actions_connect
                    )
                ).build()
            )
            .setOnClickListener {
                DataLoggerService.start()
            }.build())
        .addAction(Action.Builder()
            .setIcon(
                CarIcon.Builder(
                    IconCompat.createWithResource(
                        carContext,
                        R.drawable.action_disconnect
                    )
                ).build()
            )
            .setOnClickListener {
                DataLoggerService.stop()

            }.build())
        .build()

    private fun aaPIDs() =
        Prefs.getStringSet("pref.aa.pids.selected").map { s -> s.toLong() }.toSet()


    init {

        setCarContext(carContext)

        lifecycle.addObserver(this)
        val aaPIDs = aaPIDs()

        Log.i(LOG_KEY, "Selected PIDs = $aaPIDs")

        val data = MetricsProvider().findMetrics(aaPIDs)
        MetricsAggregator.metrics.observe(this) {
            it?.let {
                data.find { c -> c.command.pid.id == it.command.pid.id }?.let {
                    invalidate()
                }
            }
        }
    }
}