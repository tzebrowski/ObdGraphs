package org.obd.graphs.aa

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.*

class CarScreen(carContext: CarContext, surfaceController: SurfaceController) : Screen(carContext),
    DefaultLifecycleObserver {

    internal var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            when (intent?.action) {
                PROFILE_CHANGED_EVENT -> {
                    surfaceController.configure()
                }

                DATA_LOGGER_CONNECTING_EVENT -> {
                    surfaceController.configure()
                    carToast(getCarContext(),R.string.main_activity_toast_connection_connecting)
                }

                DATA_LOGGER_NO_NETWORK_EVENT -> {
                    carToast(getCarContext(),R.string.main_activity_toast_connection_no_network)
                }

                DATA_LOGGER_ERROR_EVENT -> {
                    carToast(getCarContext(),R.string.main_activity_toast_connection_error)
                }


                DATA_LOGGER_STOPPED_EVENT -> {
                    carToast(getCarContext(),R.string.main_activity_toast_connection_stopped)
                }

                DATA_LOGGER_CONNECTED_EVENT -> {
                    carToast(getCarContext(),R.string.main_activity_toast_connection_established)
                }

                DATA_LOGGER_ERROR_CONNECT_EVENT -> {
                    carToast(getCarContext(),R.string.main_activity_toast_connection_connect_error)
                }

                DATA_LOGGER_ADAPTER_NOT_SET_EVENT -> {
                    carToast(getCarContext(),R.string.main_activity_toast_adapter_is_not_selected)
                }
            }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        carContext.registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(DATA_LOGGER_ADAPTER_NOT_SET_EVENT)
            addAction(DATA_LOGGER_CONNECTING_EVENT)
            addAction(DATA_LOGGER_STOPPED_EVENT)
            addAction(DATA_LOGGER_STOPPING_EVENT)
            addAction(DATA_LOGGER_ERROR_EVENT)
            addAction(DATA_LOGGER_CONNECTED_EVENT)
            addAction(DATA_LOGGER_NO_NETWORK_EVENT)
            addAction(DATA_LOGGER_ERROR_CONNECT_EVENT)
            addAction(PROFILE_CHANGED_EVENT)
        })
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)

        carContext.unregisterReceiver(broadcastReceiver)
    }

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
        .addAction(Action.Builder()
            .setTitle(carContext.getString(R.string.pref_aa_action_exit))
            .setOnClickListener {
                carContext.finishCarApp()
            }
            .build())
        .build()

    init {
        lifecycle.addObserver(this)
        MetricsAggregator.metrics.observe(this) {
            surfaceController.render(it)
        }
    }
}