package org.obd.graphs.aa.screen.nav

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.AA_VIRTUAL_SCREEN_REFRESH_EVENT
import org.obd.graphs.AA_VIRTUAL_SCREEN_RENDERER_CHANGED_EVENT
import org.obd.graphs.aa.*
import org.obd.graphs.aa.screen.*
import org.obd.graphs.aa.screen.CarScreen
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.profile.PROFILE_CHANGED_EVENT
import org.obd.graphs.profile.PROFILE_RESET_EVENT
import org.obd.graphs.renderer.Fps


private const val HIGH_FREQ_PID_SELECTION_CHANGED_EVENT = "pref.pids.generic.high.event.changed"
private const val LOW_FREQ_PID_SELECTION_CHANGED_EVENT = "pref.pids.generic.low.event.changed"

internal class SurfaceScreen(
    carContext: CarContext,
    settings: CarSettings,
    metricsCollector: MetricsCollector,
    fps: Fps,
    private val screenNavigator: ScreenNavigator,
    private val parent: NavTemplateCarScreen
) : CarScreen(carContext, settings, metricsCollector, fps) {

    private val surfaceController = SurfaceController(carContext, settings, metricsCollector, fps, query)

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            when (intent?.action) {
                AA_VIRTUAL_SCREEN_RENDERER_CHANGED_EVENT -> surfaceController.allocateSurfaceRender()
                AA_VIRTUAL_SCREEN_REFRESH_EVENT -> renderFrame()

                HIGH_FREQ_PID_SELECTION_CHANGED_EVENT -> {
                    applyMetricsFilter()
                    renderFrame()
                }

                LOW_FREQ_PID_SELECTION_CHANGED_EVENT -> {
                    applyMetricsFilter()
                    renderFrame()
                }
                VIRTUAL_SCREEN_1_SETTINGS_CHANGED -> {
                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_1) {
                        settings.applyVirtualScreen1()
                    }
                    applyMetricsFilter()
                    renderFrame()
                }

                VIRTUAL_SCREEN_2_SETTINGS_CHANGED -> {

                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_2) {
                        settings.applyVirtualScreen2()
                    }

                    applyMetricsFilter()
                    renderFrame()
                }

                VIRTUAL_SCREEN_3_SETTINGS_CHANGED -> {

                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_3) {
                        settings.applyVirtualScreen3()
                    }
                    applyMetricsFilter()
                    renderFrame()
                }

                VIRTUAL_SCREEN_4_SETTINGS_CHANGED -> {

                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_4) {
                        settings.applyVirtualScreen4()
                    }

                    applyMetricsFilter()
                    renderFrame()
                }

                PROFILE_CHANGED_EVENT -> {
                    applyMetricsFilter()
                    surfaceController.allocateSurfaceRender()
                    renderFrame()
                }

                PROFILE_RESET_EVENT -> {
                    applyMetricsFilter()
                    surfaceController.allocateSurfaceRender()
                    renderFrame()
                }

            }
        }
    }

    fun getLifecycleObserver() = surfaceController
    fun toggleSurfaceRenderer(screenId: Int) {
        surfaceController.toggleSurfaceRenderer(screenId)
        renderFrame()
    }

    fun renderFrame() {
        if (isAllowedFrameRendering()) {
            surfaceController.renderFrame()
        }
    }

    override fun onCarConfigurationChanged() {
        surfaceController.onCarConfigurationChanged()
    }

    override fun onGetTemplate(): Template {
        var template = NavigationTemplate.Builder()

        if (screenNavigator.isVirtualScreensEnabled()) {
            getVerticalActionStrip()?.let {
                template = template.setMapActionStrip(it)
            }
        }

        return template.setActionStrip(
            getHorizontalActionStrip(
                screenId = screenNavigator.getCurrentScreenId()
            )
        ).build()
    }

    override fun onCreate(owner: LifecycleOwner) {
        carContext.registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(HIGH_FREQ_PID_SELECTION_CHANGED_EVENT)
            addAction(LOW_FREQ_PID_SELECTION_CHANGED_EVENT)
            addAction(VIRTUAL_SCREEN_1_SETTINGS_CHANGED)
            addAction(VIRTUAL_SCREEN_2_SETTINGS_CHANGED)
            addAction(VIRTUAL_SCREEN_3_SETTINGS_CHANGED)
            addAction(VIRTUAL_SCREEN_4_SETTINGS_CHANGED)
            addAction(PROFILE_CHANGED_EVENT)
            addAction(PROFILE_RESET_EVENT)
            addAction(AA_VIRTUAL_SCREEN_REFRESH_EVENT)
            addAction(AA_VIRTUAL_SCREEN_RENDERER_CHANGED_EVENT)
        })
    }

    override fun onDestroy(owner: LifecycleOwner) {
        carContext.unregisterReceiver(broadcastReceiver)
    }

    private fun applyMetricsFilter() {
        if (dataLoggerPreferences.instance.queryForEachViewStrategyEnabled) {
            Log.i(LOG_KEY, "User selection PIDs=${settings.getSelectedPIDs()}")

            metricsCollector.applyFilter(enabled = settings.getSelectedPIDs(), order = settings.getPIDsSortOrder())

            query.setStrategy(QueryStrategyType.INDIVIDUAL_QUERY_FOR_EACH_VIEW)
            query.update(metricsCollector.getMetrics().map { p -> p.source.command.pid.id }.toSet())
            dataLogger.updateQuery(query)
        } else {
            query.setStrategy(QueryStrategyType.SHARED_QUERY)
            val query = query.getIDs()
            val selection = settings.getSelectedPIDs()
            val intersection = selection.filter { query.contains(it) }.toSet()

            Log.i(LOG_KEY, "Query=$query,user selection=$selection, intersection=$intersection")

            metricsCollector.applyFilter(enabled = intersection, order = settings.getPIDsSortOrder())
        }
    }

    private fun getVerticalActionStrip(): ActionStrip? {

        var added = false
        var builder = ActionStrip.Builder()

        if (settings.isVirtualScreenEnabled(1)) {
            added = true

            builder = builder.addAction(createAction(carContext,R.drawable.action_virtual_screen_1, actionStripColor(VIRTUAL_SCREEN_1)) {
                parent.invalidate()
                settings.applyVirtualScreen1()
                applyMetricsFilter()
                renderFrame()
            })
        }

        if (settings.isVirtualScreenEnabled(2)) {

            added = true
            builder = builder.addAction(createAction(carContext,R.drawable.action_virtual_screen_2, actionStripColor(VIRTUAL_SCREEN_2)) {
                parent.invalidate()
                settings.applyVirtualScreen2()
                applyMetricsFilter()
                renderFrame()
            })
        }

        if (settings.isVirtualScreenEnabled(3)) {

            added = true
            builder = builder.addAction(createAction(carContext,R.drawable.action_virtual_screen_3, actionStripColor(VIRTUAL_SCREEN_3)) {
                parent.invalidate()
                settings.applyVirtualScreen3()
                applyMetricsFilter()
                renderFrame()
            })
        }

        if (settings.isVirtualScreenEnabled(4)) {
            added = true

            builder = builder.addAction(createAction(carContext,R.drawable.action_virtual_screen_4, actionStripColor(VIRTUAL_SCREEN_4)) {
                parent.invalidate()
                settings.applyVirtualScreen4()
                applyMetricsFilter()
                renderFrame()
            })
        }
        return if (added) {
            builder.build()
        } else {
            null
        }
    }

    private fun actionStripColor(key: String): CarColor = if (settings.getCurrentVirtualScreen() == key) {
        CarColor.GREEN
    } else {
        mapColor(settings.colorTheme().actionsBtnVirtualScreensColor)
    }

    private fun isAllowedFrameRendering() = screenNavigator.getCurrentScreenId() == GIULIA_SCREEN_ID ||
            screenNavigator.getCurrentScreenId() == DRAG_RACING_SCREEN_ID
}