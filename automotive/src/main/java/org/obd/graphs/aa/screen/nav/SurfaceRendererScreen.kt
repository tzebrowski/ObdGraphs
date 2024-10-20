package org.obd.graphs.aa.screen.nav

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.*
import org.obd.graphs.aa.*
import org.obd.graphs.aa.screen.*
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.profile.PROFILE_CHANGED_EVENT
import org.obd.graphs.profile.PROFILE_RESET_EVENT
import org.obd.graphs.renderer.Fps
import org.obd.graphs.renderer.SurfaceRendererType

private const val NOT_SET = -1
private const val GIULIA_SCREEN_ID = 0
private const val DRAG_RACING_SCREEN_ID = 1
private const val TRIP_INFO_SCREEN_ID = 3

private const val LOW_FREQ_PID_SELECTION_CHANGED_EVENT = "pref.pids.generic.low.event.changed"
private const val LOG_TAG = "SurfaceRendererScreen"

internal class SurfaceRendererScreen(
    carContext: CarContext,
    settings: CarSettings,
    metricsCollector: MetricsCollector,
    fps: Fps,
    private val parent: NavTemplateCarScreen
) : CarScreen(carContext, settings, metricsCollector, fps) {

    private val query = Query.instance()

    override fun query(): Query  = query


    private var screenId = GIULIA_SCREEN_ID
    private val surfaceRendererController = SurfaceRendererController(carContext, settings, metricsCollector, fps, query)

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            when (intent?.action) {
                AA_VIRTUAL_SCREEN_RENDERER_CHANGED_EVENT -> surfaceRendererController.allocateSurfaceRenderer()
                AA_VIRTUAL_SCREEN_REFRESH_EVENT -> renderFrame()

                AA_REFRESH_EVENT -> {
                    Log.i(LOG_TAG,"Received forced refresh screen event for screen ${screenId}. Is renderer: ${isSurfaceRendererScreen(screenId)}")
                    if (isSurfaceRendererScreen(screenId)) {

                        if (screenId == GIULIA_SCREEN_ID) {
                            when (settings.getCurrentVirtualScreen()) {
                                VIRTUAL_SCREEN_1 -> settings.applyVirtualScreen1()
                                VIRTUAL_SCREEN_2 -> settings.applyVirtualScreen2()
                                VIRTUAL_SCREEN_3 -> settings.applyVirtualScreen3()
                                VIRTUAL_SCREEN_4 -> settings.applyVirtualScreen4()
                            }
                        }

                        updateQuery()
                        renderFrame()
                    }
                }

                AA_TRIP_INFO_PID_SELECTION_CHANGED_EVENT -> {
                    updateQuery()
                    renderFrame()
                }

                AA_HIGH_FREQ_PID_SELECTION_CHANGED_EVENT -> {
                    updateQuery()
                    renderFrame()
                }

                LOW_FREQ_PID_SELECTION_CHANGED_EVENT -> {
                    updateQuery()
                    renderFrame()
                }
                VIRTUAL_SCREEN_1_SETTINGS_CHANGED -> {
                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_1) {
                        settings.applyVirtualScreen1()
                    }
                    updateQuery()
                    renderFrame()
                }

                VIRTUAL_SCREEN_2_SETTINGS_CHANGED -> {

                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_2) {
                        settings.applyVirtualScreen2()
                    }

                    updateQuery()
                    renderFrame()
                }

                VIRTUAL_SCREEN_3_SETTINGS_CHANGED -> {

                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_3) {
                        settings.applyVirtualScreen3()
                    }
                    updateQuery()
                    renderFrame()
                }

                VIRTUAL_SCREEN_4_SETTINGS_CHANGED -> {

                    if (settings.getCurrentVirtualScreen() == VIRTUAL_SCREEN_4) {
                        settings.applyVirtualScreen4()
                    }

                    updateQuery()
                    renderFrame()
                }

                PROFILE_CHANGED_EVENT -> {
                    updateQuery()
                    surfaceRendererController.allocateSurfaceRenderer()
                    renderFrame()
                }

                PROFILE_RESET_EVENT -> {
                    updateQuery()
                    surfaceRendererController.allocateSurfaceRenderer()
                    renderFrame()
                }
            }
        }
    }

    fun getLifecycleObserver() = surfaceRendererController

    fun resetSurfaceRenderer(){
        screenId = NOT_SET
    }

    fun switchSurfaceRenderer(newScreen: Int) {
        screenId = newScreen
        Log.e(LOG_TAG, "Switch to new surface renderer screen: $screenId and updating query")

        when (screenId){
            GIULIA_SCREEN_ID -> {
                metricsCollector.applyFilter(enabled = settings.getSelectedPIDs())

                if (dataLoggerPreferences.instance.individualQueryStrategyEnabled) {
                    query.setStrategy(QueryStrategyType.INDIVIDUAL_QUERY_FOR_EACH_VIEW)
                    query.update(metricsCollector.getMetrics().map { p -> p.source.command.pid.id }.toSet())
                } else {
                    query.setStrategy(QueryStrategyType.SHARED_QUERY)
                }

                dataLogger.updateQuery(query = query)
                surfaceRendererController.allocateSurfaceRenderer()
            }

            DRAG_RACING_SCREEN_ID -> {
                dataLogger.updateQuery(query = query.apply {
                    setStrategy(QueryStrategyType.DRAG_RACING_QUERY)
                })
                surfaceRendererController.allocateSurfaceRenderer(surfaceRendererType = SurfaceRendererType.DRAG_RACING)
            }

            TRIP_INFO_SCREEN_ID -> {

                dataLogger.updateQuery(query = query.apply {
                    setStrategy(QueryStrategyType.TRIP_INFO_QUERY)
                })
                surfaceRendererController.allocateSurfaceRenderer(surfaceRendererType = SurfaceRendererType.TRIP_INFO)
            }
        }

        renderFrame()
    }

    override fun actionStartDataLogging(){
        Log.i(LOG_TAG, "Action start data logging")
        when (screenId) {
            GIULIA_SCREEN_ID -> {
                if (dataLoggerPreferences.instance.individualQueryStrategyEnabled) {
                    query.setStrategy(QueryStrategyType.INDIVIDUAL_QUERY_FOR_EACH_VIEW)
                    query.update(metricsCollector.getMetrics().map { p -> p.source.command.pid.id }.toSet())
                } else {
                    query.setStrategy(QueryStrategyType.SHARED_QUERY)
                }
                dataLogger.start(query)
            }

            DRAG_RACING_SCREEN_ID ->
                dataLogger.start(query.apply{
                    setStrategy(QueryStrategyType.DRAG_RACING_QUERY)
                })

            TRIP_INFO_SCREEN_ID ->
                dataLogger.start(query.apply{
                    setStrategy(QueryStrategyType.TRIP_INFO_QUERY)
                })
        }
    }

    fun isSurfaceRendererScreen(newScreen: Int) =
        newScreen == GIULIA_SCREEN_ID || newScreen == DRAG_RACING_SCREEN_ID || newScreen == TRIP_INFO_SCREEN_ID

    override fun getFeatureDescription(): List<FeatureDescription>  = mutableListOf(
        FeatureDescription(DRAG_RACING_SCREEN_ID, R.drawable.action_drag_race_screen, carContext.getString(R.string.available_features_drag_race_screen_title)),
        FeatureDescription(GIULIA_SCREEN_ID, R.drawable.action_giulia, carContext.getString(R.string.available_features_giulia_screen_title))).apply {
            if (settings.getTripInfoScreenSettings().viewEnabled) {
                add(FeatureDescription(TRIP_INFO_SCREEN_ID, R.drawable.action_giulia,  carContext.getString(R.string.available_features_trip_info_screen_title)))
            }
    }


    fun renderFrame() {
        if (isSurfaceRendererScreen(screenId)) {
            surfaceRendererController.renderFrame()
        }
    }

    override fun onCarConfigurationChanged() {
        if (isSurfaceRendererScreen(screenId)) {
            surfaceRendererController.renderFrame()
        }
    }

    override fun onGetTemplate(): Template {
        var template = NavigationTemplate.Builder()

        if (screenId == GIULIA_SCREEN_ID) {
            getVerticalActionStrip()?.let {
                template = template.setMapActionStrip(it)
            }
        }

        return template.setActionStrip(
            getHorizontalActionStrip(
                screenId = screenId
            )
        ).build()
    }

    override fun onCreate(owner: LifecycleOwner) {
        registerReceiver(carContext,broadcastReceiver) {
            it.addAction(AA_HIGH_FREQ_PID_SELECTION_CHANGED_EVENT)
            it.addAction(LOW_FREQ_PID_SELECTION_CHANGED_EVENT)
            it.addAction(VIRTUAL_SCREEN_1_SETTINGS_CHANGED)
            it.addAction(VIRTUAL_SCREEN_2_SETTINGS_CHANGED)
            it.addAction(VIRTUAL_SCREEN_3_SETTINGS_CHANGED)
            it.addAction(VIRTUAL_SCREEN_4_SETTINGS_CHANGED)
            it.addAction(PROFILE_CHANGED_EVENT)
            it.addAction(PROFILE_RESET_EVENT)
            it.addAction(AA_VIRTUAL_SCREEN_REFRESH_EVENT)
            it.addAction(AA_VIRTUAL_SCREEN_RENDERER_CHANGED_EVENT)
            it.addAction(AA_REFRESH_EVENT)
            it.addAction(AA_TRIP_INFO_PID_SELECTION_CHANGED_EVENT)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        carContext.unregisterReceiver(broadcastReceiver)
    }

    private fun updateQuery() {

        Log.e(LOG_TAG,"Updating query")

        if (isSurfaceRendererScreen(screenId)) {
            if (screenId == DRAG_RACING_SCREEN_ID) {
                Log.i(LOG_TAG, "Updating query for  DRAG_RACING_SCREEN_ID screen")

                query.setStrategy(QueryStrategyType.DRAG_RACING_QUERY)
                metricsCollector.applyFilter(enabled = query.getIDs())
                Log.i(LOG_TAG, "User selection PIDs=${query.getIDs()}")
                dataLogger.updateQuery(query)

            } else if (screenId == TRIP_INFO_SCREEN_ID) {
                Log.i(LOG_TAG, "Updating query for  TRIP_INFO_SCREEN_ID screen")

                query.setStrategy(QueryStrategyType.TRIP_INFO_QUERY)
                metricsCollector.applyFilter(enabled = query.getIDs())
                Log.i(LOG_TAG, "User selection PIDs=${query.getIDs()}")
                dataLogger.updateQuery(query)

            } else if (dataLoggerPreferences.instance.individualQueryStrategyEnabled) {
                Log.i(LOG_TAG, "Updating query for  individualQueryStrategyEnabled")

                metricsCollector.applyFilter(enabled = settings.getSelectedPIDs(), order = settings.getPIDsSortOrder())

                query.setStrategy(QueryStrategyType.INDIVIDUAL_QUERY_FOR_EACH_VIEW)
                query.update(metricsCollector.getMetrics().map { p -> p.source.command.pid.id }.toSet())
                Log.i(LOG_TAG, "User selection PIDs=${settings.getSelectedPIDs()}")

                dataLogger.updateQuery(query)
            } else {
                Log.i(LOG_TAG, "Updating query for default query")

                query.setStrategy(QueryStrategyType.SHARED_QUERY)
                val query = query.getIDs()
                val selection = settings.getSelectedPIDs()
                val intersection = selection.filter { query.contains(it) }.toSet()

                Log.i(LOG_TAG, "Query=$query,user selection=$selection, intersection=$intersection")

                metricsCollector.applyFilter(enabled = intersection, order = settings.getPIDsSortOrder())
            }
        }
    }

    private fun getVerticalActionStrip(): ActionStrip? {

        var added = false
        var builder = ActionStrip.Builder()

        if (settings.isVirtualScreenEnabled(1)) {
            added = true

            builder = builder.addAction(createAction(carContext, R.drawable.action_virtual_screen_1, actionStripColor(VIRTUAL_SCREEN_1)) {
                parent.invalidate()
                settings.applyVirtualScreen1()
                updateQuery()
                renderFrame()
            })
        }

        if (settings.isVirtualScreenEnabled(2)) {

            added = true
            builder = builder.addAction(createAction(carContext, R.drawable.action_virtual_screen_2, actionStripColor(VIRTUAL_SCREEN_2)) {
                parent.invalidate()
                settings.applyVirtualScreen2()
                updateQuery()
                renderFrame()
            })
        }

        if (settings.isVirtualScreenEnabled(3)) {

            added = true
            builder = builder.addAction(createAction(carContext, R.drawable.action_virtual_screen_3, actionStripColor(VIRTUAL_SCREEN_3)) {
                parent.invalidate()
                settings.applyVirtualScreen3()
                updateQuery()
                renderFrame()
            })
        }

        if (settings.isVirtualScreenEnabled(4)) {
            added = true

            builder = builder.addAction(createAction(carContext, R.drawable.action_virtual_screen_4, actionStripColor(VIRTUAL_SCREEN_4)) {
                parent.invalidate()
                settings.applyVirtualScreen4()
                updateQuery()
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
        mapColor(settings.getColorTheme().actionsBtnVirtualScreensColor)
    }
}