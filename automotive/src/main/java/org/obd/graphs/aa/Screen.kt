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
import org.obd.graphs.AA_EDIT_PREF_SCREEN
import org.obd.graphs.bl.datalogger.*
import org.obd.graphs.sendBroadcastEvent


internal class Screen(carContext: CarContext, val surfaceController: SurfaceController) :
    Screen(carContext),
    DefaultLifecycleObserver {

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                PROFILE_CHANGED_EVENT -> {
                    surfaceController.configure()
                }

                DATA_LOGGER_CONNECTING_EVENT -> {
                    surfaceController.configure()
                    carToast(getCarContext(), R.string.main_activity_toast_connection_connecting)
                }

                DATA_LOGGER_NO_NETWORK_EVENT -> {
                    carToast(getCarContext(), R.string.main_activity_toast_connection_no_network)
                }

                DATA_LOGGER_ERROR_EVENT -> {
                    carToast(getCarContext(), R.string.main_activity_toast_connection_error)
                }

                DATA_LOGGER_STOPPED_EVENT -> {
                    carToast(getCarContext(), R.string.main_activity_toast_connection_stopped)
                    renderingThread.stop()
                }

                DATA_LOGGER_CONNECTED_EVENT -> {
                    carToast(getCarContext(), R.string.main_activity_toast_connection_established)
                    renderingThread.start()
                }

                DATA_LOGGER_ERROR_CONNECT_EVENT -> {
                    carToast(getCarContext(), R.string.main_activity_toast_connection_connect_error)
                }

                DATA_LOGGER_ADAPTER_NOT_SET_EVENT -> {
                    carToast(getCarContext(), R.string.main_activity_toast_adapter_is_not_selected)
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
        return try {
            NavigationTemplate.Builder()
                .setMapActionStrip(profilesActionStrip())
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
    private fun createAction(iconResId: Int, iconColorTint: CarColor, func: () -> Unit): Action  =
        Action.Builder()
            .setIcon(
                CarIcon.Builder(
                    IconCompat.createWithResource(
                        carContext,
                        iconResId
                    )
                ).setTint(iconColorTint).build()
            )
            .setOnClickListener {
                func()
            }.build()

    private fun profilesActionStrip(): ActionStrip = ActionStrip.Builder()

        .addAction(createAction(R.drawable.action_profile_1, CarColor.YELLOW) {
            carScreenSettings.setProfile1()
            surfaceController.configure()
            surfaceController.render()
        })
        .addAction(createAction(R.drawable.action_profile_2, CarColor.YELLOW) {
            carScreenSettings.setProfile2()
            surfaceController.configure()
            surfaceController.render()
        })

        .addAction(createAction(R.drawable.action_profile_3, CarColor.YELLOW) {
            carScreenSettings.setProfile3()
            surfaceController.configure()
            surfaceController.render()
        })
        .addAction(createAction(R.drawable.action_profile_4, CarColor.YELLOW) {
            carScreenSettings.setProfile4()
            surfaceController.configure()
            surfaceController.render()
        })
        .build()

    private fun actions(): ActionStrip = ActionStrip.Builder()
        .addAction(createAction(R.drawable.actions_connect, CarColor.GREEN) {
            DataLoggerService.start()
        })
        .addAction(createAction(R.drawable.action_disconnect, CarColor.BLUE) {
            stopDataLogging()
        })

        .addAction(createAction(R.drawable.config, CarColor.BLUE) {
            sendBroadcastEvent(AA_EDIT_PREF_SCREEN)
            carToast(carContext,R.string.pref_aa_get_to_app_conf)
        })

        .addAction(createAction(R.drawable.action_exit,CarColor.RED) {
            stopDataLogging()
            carContext.finishCarApp()
        }).build()

    private fun stopDataLogging() {
        Log.i(LOG_KEY,"Stopping data logging process.")
        renderingThread.stop()
        DataLoggerService.stop()
    }

    private var renderingThread = RenderingThread(surfaceController)

    init {

        lifecycle.addObserver(this)
        dataLogger.observe(this) {
            metricsCollector.collect(it)
        }

        if (dataLogger.isRunning()){
            Log.i(LOG_KEY,"Data logger is running, connecting.................")
            renderingThread.start()
        } else {
            Log.i(LOG_KEY,"Data logger is not running.")
        }
    }
}