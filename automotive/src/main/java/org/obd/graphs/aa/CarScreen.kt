package org.obd.graphs.aa

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.*
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.bl.collector.CarMetricsCollector
import org.obd.graphs.bl.datalogger.*
import org.obd.graphs.renderer.Fps

private const val LOG_KEY = "CarScreen"
private const val VIRTUAL_SCREEN_1_SETTINGS_CHANGED = "pref.aa.pids.profile_1.event.changed"
private const val VIRTUAL_SCREEN_2_SETTINGS_CHANGED = "pref.aa.pids.profile_2.event.changed"
private const val VIRTUAL_SCREEN_3_SETTINGS_CHANGED = "pref.aa.pids.profile_3.event.changed"
private const val VIRTUAL_SCREEN_4_SETTINGS_CHANGED = "pref.aa.pids.profile_4.event.changed"
const val SURFACE_DESTROYED_EVENT = "car.event.surface.destroyed"
const val SURFACE_AREA_CHANGED_EVENT = "car.event.surface.area_changed"
const val SURFACE_BROKEN_EVENT = "car.event.surface_broken.event"


internal class CarScreen(carContext: CarContext,
                         private val surfaceController: SurfaceController,
                         private val settings: ScreenSettings,
                         private val metricsCollector: CarMetricsCollector,
                         private val fps: Fps
) :
    Screen(carContext),
    DefaultLifecycleObserver {

    private val renderingThread: RenderingThread = RenderingThread(
        renderAction = { surfaceController.renderFrame() },
        perfFrameRate = {
            settings.getSurfaceFrameRate()
        }
    )

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                SURFACE_BROKEN_EVENT -> {
                    Log.d(LOG_KEY,"Received event about ")
                    renderingThread.stop()
                    fps.stop()
                    carContext.finishCarApp()
                }
                MAIN_ACTIVITY_EVENT_DESTROYED -> {
                    Log.d(LOG_KEY, "Main activity has been destroyed.")
                    invalidate()
                }
                MAIN_ACTIVITY_EVENT_PAUSE -> {
                    Log.d(LOG_KEY, "Main activity is going to the background.")
                    invalidate()
                }
                SURFACE_DESTROYED_EVENT -> {
                    renderingThread.stop()
                    fps.stop()
                }
                SURFACE_AREA_CHANGED_EVENT -> {
                    if (!renderingThread.isRunning() && dataLogger.isRunning()) {
                        renderingThread.start()
                        fps.start()
                    }
                }

                VIRTUAL_SCREEN_1_SETTINGS_CHANGED -> {
                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_1) {
                        settings.applyVirtualScreen1()
                        metricsCollector.applyFilter(settings.getSelectedPIDs())
                        surfaceController.renderFrame()
                    }
                }

                VIRTUAL_SCREEN_2_SETTINGS_CHANGED -> {
                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_2) {
                        settings.applyVirtualScreen2()
                        metricsCollector.applyFilter(settings.getSelectedPIDs())
                        surfaceController.renderFrame()
                    }
                }

                VIRTUAL_SCREEN_3_SETTINGS_CHANGED -> {
                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_3) {
                        settings.applyVirtualScreen3()
                        metricsCollector.applyFilter(settings.getSelectedPIDs())
                        surfaceController.renderFrame()
                    }
                }

                VIRTUAL_SCREEN_4_SETTINGS_CHANGED -> {
                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_4) {
                        settings.applyVirtualScreen4()
                        metricsCollector.applyFilter(settings.getSelectedPIDs())
                        surfaceController.renderFrame()
                    }
                }

                PROFILE_CHANGED_EVENT -> {
                    metricsCollector.applyFilter(settings.getSelectedPIDs())
                    surfaceController.renderFrame()
                }

                DATA_LOGGER_CONNECTING_EVENT -> {
                    toast.show(carContext, R.string.main_activity_toast_connection_connecting)
                }

                DATA_LOGGER_NO_NETWORK_EVENT -> {
                    toast.show(carContext, R.string.main_activity_toast_connection_no_network)
                }

                DATA_LOGGER_ERROR_EVENT -> {
                    toast.show(carContext, R.string.main_activity_toast_connection_error)
                }

                DATA_LOGGER_STOPPED_EVENT -> {
                    toast.show(carContext, R.string.main_activity_toast_connection_stopped)
                    renderingThread.stop()
                    surfaceController.renderFrame()
                    fps.stop()
                }

                DATA_LOGGER_CONNECTED_EVENT -> {
                    toast.show(carContext, R.string.main_activity_toast_connection_established)
                    renderingThread.start()
                    fps.start()
                }

                DATA_LOGGER_ERROR_CONNECT_EVENT -> {
                    toast.show(carContext, R.string.main_activity_toast_connection_connect_error)
                }

                DATA_LOGGER_ADAPTER_NOT_SET_EVENT -> {
                    toast.show(carContext, R.string.main_activity_toast_adapter_is_not_selected)
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
            addAction(SURFACE_DESTROYED_EVENT)
            addAction(SURFACE_AREA_CHANGED_EVENT)
            addAction(MAIN_ACTIVITY_EVENT_DESTROYED)
            addAction(MAIN_ACTIVITY_EVENT_PAUSE)
            addAction(SURFACE_BROKEN_EVENT)
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

        .addAction(createAction(R.drawable.action_virtual_screen_1, mapColor(settings.colorTheme().actionsBtnVirtualScreensColor)) {
            settings.applyVirtualScreen1()
            metricsCollector.applyFilter(settings.getSelectedPIDs())
            surfaceController.renderFrame()
        })
        .addAction(createAction(R.drawable.action_virtual_screen_2, mapColor(settings.colorTheme().actionsBtnVirtualScreensColor)) {
            settings.applyVirtualScreen2()
            metricsCollector.applyFilter(settings.getSelectedPIDs())
            surfaceController.renderFrame()
        })

        .addAction(createAction(R.drawable.action_virtual_screen_3, mapColor(settings.colorTheme().actionsBtnVirtualScreensColor)) {
            settings.applyVirtualScreen3()
            metricsCollector.applyFilter(settings.getSelectedPIDs())
            surfaceController.renderFrame()
        })
        .addAction(createAction(R.drawable.action_virtual_screen_4, mapColor(settings.colorTheme().actionsBtnVirtualScreensColor)) {
            settings.applyVirtualScreen4()
            metricsCollector.applyFilter(settings.getSelectedPIDs())
            surfaceController.renderFrame()
        })
        .build()

    private fun actions(): ActionStrip = ActionStrip.Builder()
        .addAction(createAction(R.drawable.actions_connect, mapColor(settings.colorTheme().actionsBtnConnectColor)) {
            dataLogger.start()
        })
        .addAction(createAction(R.drawable.action_disconnect,mapColor(settings.colorTheme().actionsBtnDisconnectColor)) {
            toast.show(carContext, R.string.toast_connection_disconnect)
            stopDataLogging()
        })

        .addAction(createAction(R.drawable.config, CarColor.BLUE) {
            sendBroadcastEvent(AA_EDIT_PREF_SCREEN)
            toast.show(carContext, R.string.pref_aa_get_to_app_conf)
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
        fps.stop()
        renderingThread.stop()
        dataLogger.stop()
    }

    private fun mapColor(color: Int): CarColor =
        when (color){
            Color.BLUE -> CarColor.BLUE
            Color.GREEN -> CarColor.GREEN
            Color.YELLOW -> CarColor.YELLOW
            else -> CarColor.PRIMARY
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