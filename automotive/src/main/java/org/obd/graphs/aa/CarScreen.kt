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


private const val VIRTUAL_SCREEN_1_SETTINGS_CHANGED = "pref.aa.pids.profile_1.event.changed"
private const val VIRTUAL_SCREEN_2_SETTINGS_CHANGED = "pref.aa.pids.profile_2.event.changed"
private const val VIRTUAL_SCREEN_3_SETTINGS_CHANGED = "pref.aa.pids.profile_3.event.changed"
private const val VIRTUAL_SCREEN_4_SETTINGS_CHANGED = "pref.aa.pids.profile_4.event.changed"

internal class CarScreen(carContext: CarContext, val surfaceController: SurfaceController) :
    Screen(carContext),
    DefaultLifecycleObserver {

    private var renderingThread = RenderingThread(surfaceController)

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                VIRTUAL_SCREEN_1_SETTINGS_CHANGED -> {
                    if (carScreenSettings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_1) {
                        carScreenSettings.applyVirtualScreen1()
                        metricsCollector.configure()
                        surfaceController.renderFrame()
                    }
                }

                VIRTUAL_SCREEN_2_SETTINGS_CHANGED -> {
                    if (carScreenSettings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_2) {
                        carScreenSettings.applyVirtualScreen2()
                        metricsCollector.configure()
                        surfaceController.renderFrame()
                    }
                }

                VIRTUAL_SCREEN_3_SETTINGS_CHANGED -> {
                    if (carScreenSettings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_3) {
                        carScreenSettings.applyVirtualScreen3()
                        metricsCollector.configure()
                        surfaceController.renderFrame()
                    }
                }

                VIRTUAL_SCREEN_4_SETTINGS_CHANGED -> {
                    if (carScreenSettings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_4) {
                        carScreenSettings.applyVirtualScreen4()
                        metricsCollector.configure()
                        surfaceController.renderFrame()
                    }
                }

                PROFILE_CHANGED_EVENT -> {
                    metricsCollector.configure()
                    surfaceController.renderFrame()
                }

                DATA_LOGGER_CONNECTING_EVENT -> {
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
                    surfaceController.renderFrame()
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
            addAction(VIRTUAL_SCREEN_1_SETTINGS_CHANGED)
            addAction(VIRTUAL_SCREEN_2_SETTINGS_CHANGED)
            addAction(VIRTUAL_SCREEN_3_SETTINGS_CHANGED)
            addAction(VIRTUAL_SCREEN_4_SETTINGS_CHANGED)
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

    private fun createAction(iconResId: Int, iconColorTint: CarColor, func: () -> Unit): Action =
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

        .addAction(createAction(R.drawable.action_virtual_screen_1, CarColor.YELLOW) {
            carScreenSettings.applyVirtualScreen1()
            metricsCollector.configure()
            surfaceController.renderFrame()
        })
        .addAction(createAction(R.drawable.action_virtual_screen_2, CarColor.YELLOW) {
            carScreenSettings.applyVirtualScreen2()
            metricsCollector.configure()
            surfaceController.renderFrame()
        })

        .addAction(createAction(R.drawable.action_virtual_screen_3, CarColor.YELLOW) {
            carScreenSettings.applyVirtualScreen3()
            metricsCollector.configure()
            surfaceController.renderFrame()
        })
        .addAction(createAction(R.drawable.action_virtual_screen_4, CarColor.YELLOW) {
            carScreenSettings.applyVirtualScreen4()
            metricsCollector.configure()
            surfaceController.renderFrame()
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
            carToast(carContext, R.string.pref_aa_get_to_app_conf)
        })

        .addAction(createAction(R.drawable.action_exit, CarColor.RED) {
            try {
                stopDataLogging()
            } finally {
                Log.i(LOG_KEY, "Exiting the app. Closing the context")
                carContext.finishCarApp()
            }
        }).build()

    private fun stopDataLogging() {
        Log.i(LOG_KEY, "Stopping data logging process")
        renderingThread.stop()
        DataLoggerService.stop()
    }

    init {

        lifecycle.addObserver(this)
        dataLogger.observe(this) {
            metricsCollector.append(it)
        }

        if (dataLogger.isRunning()) {
            Log.i(LOG_KEY, "Data logger is running, connecting....")
            renderingThread.start()
        } else {
            Log.i(LOG_KEY, "Data logger is not running.")
        }
    }
}