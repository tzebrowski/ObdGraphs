package org.obd.graphs.aa

import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.DataLoggerService
import org.obd.graphs.bl.datalogger.MetricsAggregator
import org.obd.graphs.ui.common.MetricsProvider
import org.obd.graphs.ui.preferences.Prefs
import org.obd.graphs.ui.preferences.getStringSet

class CarScreen(carContext: CarContext) : Screen(carContext),
    DefaultLifecycleObserver {

    override fun onGetTemplate(): Template {
        return if (MetricsProvider().findMetrics(aaPIDs()).isEmpty()) {
            PaneTemplate.Builder(Pane.Builder().setLoading(true).build())
                .setHeaderAction(Action.BACK)
                .setTitle(carContext.getString(R.string.pref_aa_car_no_pids_selected))
                .build()
        } else {
            return try {
                NavigationTemplate.Builder()
                    .setActionStrip(actions())
                    .build()
            } catch (e: Exception) {
                Log.e(LOG_KEY, "Failed to build template", e)
                PaneTemplate.Builder(Pane.Builder().setLoading(true).build())
                    .setHeaderAction(Action.BACK)
                    .setTitle(carContext.getString(R.string.pref_aa_car_error))
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
                ).setTint(CarColor.GREEN).build()
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
                ).setTint(CarColor.RED).build()
            )
            .setOnClickListener {
                DataLoggerService.stop()
            }.build())
        .build()


    init {
        lifecycle.addObserver(this)
    }
}