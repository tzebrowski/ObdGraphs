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
import org.obd.graphs.renderer.Identity


private enum class DefaultScreen(private val code: Int): Identity {
    NOT_SET(-1);

    override fun id(): Int {
        return this.code
    }
}

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

    private var screenId : Identity = SurfaceRendererType.GIULIA
    private val surfaceRendererController = SurfaceRendererController(carContext, settings, metricsCollector, fps, query)

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            when (intent?.action) {
                AA_VIRTUAL_SCREEN_RENDERER_CHANGED_EVENT -> surfaceRendererController.allocateSurfaceRenderer(getSurfaceRendererType())
                AA_VIRTUAL_SCREEN_REFRESH_EVENT -> renderFrame()

                AA_REFRESH_EVENT -> {
                    Log.i(LOG_TAG,"Received forced refresh screen event for screen ${screenId}. " +
                            "Is renderer: ${isSurfaceRendererScreen(screenId)}")

                    if (isSurfaceRendererScreen(screenId)) {

                        if (screenId == SurfaceRendererType.GAUGE) {
                            when (settings.getGaugeRendererSetting().getCurrentVirtualScreen()) {
                                VIRTUAL_SCREEN_1 -> applyVirtualScreen1()
                                VIRTUAL_SCREEN_2 -> applyVirtualScreen2()
                                VIRTUAL_SCREEN_3 -> applyVirtualScreen3()
                                VIRTUAL_SCREEN_4 -> applyVirtualScreen4()
                            }
                        } else if (screenId == SurfaceRendererType.GIULIA) {
                            when (settings.getGiuliaRendererSetting().getCurrentVirtualScreen()) {
                                VIRTUAL_SCREEN_1 -> applyVirtualScreen1()
                                VIRTUAL_SCREEN_2 -> applyVirtualScreen2()
                                VIRTUAL_SCREEN_3 -> applyVirtualScreen3()
                                VIRTUAL_SCREEN_4 -> applyVirtualScreen4()
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
                    if (settings.getGiuliaRendererSetting().getCurrentVirtualScreen() == VIRTUAL_SCREEN_1 ||
                        settings.getGaugeRendererSetting().getCurrentVirtualScreen() == VIRTUAL_SCREEN_1) {
                        applyVirtualScreen1()
                    }
                    updateQuery()
                    renderFrame()
                }

                VIRTUAL_SCREEN_2_SETTINGS_CHANGED -> {

                    if (settings.getGiuliaRendererSetting().getCurrentVirtualScreen() == VIRTUAL_SCREEN_2 ||
                        settings.getGaugeRendererSetting().getCurrentVirtualScreen() == VIRTUAL_SCREEN_2) {
                        applyVirtualScreen2()
                    }

                    updateQuery()
                    renderFrame()
                }

                VIRTUAL_SCREEN_3_SETTINGS_CHANGED -> {

                    if (settings.getGiuliaRendererSetting().getCurrentVirtualScreen() == VIRTUAL_SCREEN_3 ||
                        settings.getGaugeRendererSetting().getCurrentVirtualScreen() == VIRTUAL_SCREEN_3) {
                        applyVirtualScreen3()
                    }

                    updateQuery()
                    renderFrame()
                }

                VIRTUAL_SCREEN_4_SETTINGS_CHANGED -> {

                    if (settings.getGiuliaRendererSetting().getCurrentVirtualScreen() == VIRTUAL_SCREEN_4 ||
                        settings.getGaugeRendererSetting().getCurrentVirtualScreen() == VIRTUAL_SCREEN_4) {
                        applyVirtualScreen4()
                    }

                    updateQuery()
                    renderFrame()
                }

                PROFILE_CHANGED_EVENT -> {
                    updateQuery()
                    surfaceRendererController.allocateSurfaceRenderer(getSurfaceRendererType())
                    renderFrame()
                }

                PROFILE_RESET_EVENT -> {
                    updateQuery()
                    surfaceRendererController.allocateSurfaceRenderer(getSurfaceRendererType())
                    renderFrame()
                }
            }
        }
    }

    fun getLifecycleObserver() = surfaceRendererController

    fun resetSurfaceRenderer(){
        screenId = DefaultScreen.NOT_SET
    }


    fun switchSurfaceRenderer(screenId: Identity) {
        this.screenId = screenId
        Log.i(LOG_TAG, "Switch to new surface renderer screen: ${this.screenId} and updating query...")

        when (this.screenId as SurfaceRendererType){
            SurfaceRendererType.GIULIA, SurfaceRendererType.GAUGE -> {

                metricsCollector.applyFilter(enabled = getSelectedPIDs())

                if (dataLoggerPreferences.instance.individualQueryStrategyEnabled) {
                    query.setStrategy(QueryStrategyType.INDIVIDUAL_QUERY_FOR_EACH_VIEW)
                    query.update(metricsCollector.getMetrics().map { p -> p.source.command.pid.id }.toSet())
                } else {
                    query.setStrategy(QueryStrategyType.SHARED_QUERY)
                }

                dataLogger.updateQuery(query = query)
                surfaceRendererController.allocateSurfaceRenderer(screenId as SurfaceRendererType)
            }


            SurfaceRendererType.DRAG_RACING -> {
                dataLogger.updateQuery(query = query.apply {
                    setStrategy(QueryStrategyType.DRAG_RACING_QUERY)
                })
                surfaceRendererController.allocateSurfaceRenderer(surfaceRendererType = SurfaceRendererType.DRAG_RACING)
            }

            SurfaceRendererType.TRIP_INFO -> {

                dataLogger.updateQuery(query = query.apply {
                    setStrategy(QueryStrategyType.TRIP_INFO_QUERY)
                })
                surfaceRendererController.allocateSurfaceRenderer(surfaceRendererType = SurfaceRendererType.TRIP_INFO)
            }
        }

        renderFrame()
    }

    override fun actionStartDataLogging(){
        Log.e(LOG_TAG, "1 Action start data logging for $screenId")
        when (screenId) {
            SurfaceRendererType.GIULIA , SurfaceRendererType.GAUGE -> {
                if (dataLoggerPreferences.instance.individualQueryStrategyEnabled) {
                    query.setStrategy(QueryStrategyType.INDIVIDUAL_QUERY_FOR_EACH_VIEW)
                    query.update(metricsCollector.getMetrics().map { p -> p.source.command.pid.id }.toSet())
                } else {
                    query.setStrategy(QueryStrategyType.SHARED_QUERY)
                }
                dataLogger.start(query)
            }

            SurfaceRendererType.DRAG_RACING ->
                dataLogger.start(query.apply{
                    setStrategy(QueryStrategyType.DRAG_RACING_QUERY)
                })

            SurfaceRendererType.TRIP_INFO ->
                dataLogger.start(query.apply{
                    setStrategy(QueryStrategyType.TRIP_INFO_QUERY)
                })
        }
    }

    fun isSurfaceRendererScreen(identity: Identity) = identity is SurfaceRendererType
    override fun getFeatureDescription(): List<FeatureDescription>  = mutableListOf(
        FeatureDescription(SurfaceRendererType.DRAG_RACING, R.drawable.action_drag_race, carContext.getString(R.string.available_features_drag_race_screen_title)),

        FeatureDescription(SurfaceRendererType.GAUGE, R.drawable.action_gauge, carContext.getString(R.string.available_features_gauge_screen_title)),
        FeatureDescription(SurfaceRendererType.GIULIA, R.drawable.action_giulia_metics, carContext.getString(R.string.available_features_giulia_screen_title))).apply {
            if (settings.getTripInfoScreenSettings().viewEnabled) {
                add(FeatureDescription(SurfaceRendererType.TRIP_INFO, R.drawable.action_giulia,  carContext.getString(R.string.available_features_trip_info_screen_title)))
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

        if (screenId == SurfaceRendererType.GIULIA || screenId == SurfaceRendererType.GAUGE) {
            getVerticalActionStrip()?.let {
                template = template.setMapActionStrip(it)
            }
        }

        return template.setActionStrip(getHorizontalActionStrip()).build()
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

        if (isSurfaceRendererScreen(screenId)) {

            val selectedPIDs = getSelectedPIDs()

            metricsCollector.applyFilter(enabled = selectedPIDs)


            if (screenId == SurfaceRendererType.DRAG_RACING) {
                Log.i(LOG_TAG, "Updating query for  DRAG_RACING_SCREEN_ID screen")

                query.setStrategy(QueryStrategyType.DRAG_RACING_QUERY)
                metricsCollector.applyFilter(enabled = query.getIDs())
                Log.i(LOG_TAG, "User selection PIDs=${query.getIDs()}")
                dataLogger.updateQuery(query)

            } else if (screenId == SurfaceRendererType.TRIP_INFO) {
                Log.i(LOG_TAG, "Updating query for  TRIP_INFO_SCREEN_ID screen")

                query.setStrategy(QueryStrategyType.TRIP_INFO_QUERY)
                metricsCollector.applyFilter(enabled = query.getIDs())
                Log.i(LOG_TAG, "User selection PIDs=${query.getIDs()}")
                dataLogger.updateQuery(query)

            } else if (dataLoggerPreferences.instance.individualQueryStrategyEnabled) {
                Log.i(LOG_TAG, "Updating query for  individualQueryStrategyEnabled")

                metricsCollector.applyFilter(enabled = selectedPIDs, order = settings.getPIDsSortOrder())

                query.setStrategy(QueryStrategyType.INDIVIDUAL_QUERY_FOR_EACH_VIEW)
                query.update(metricsCollector.getMetrics().map { p -> p.source.command.pid.id }.toSet())
                Log.i(LOG_TAG, "User selection PIDs=${selectedPIDs}")

                dataLogger.updateQuery(query)
            } else {
                Log.i(LOG_TAG, "Updating query to SHARED_QUERY strategy")

                query.setStrategy(QueryStrategyType.SHARED_QUERY)
                val query = query.getIDs()
                val intersection = selectedPIDs.filter { query.contains(it) }.toSet()

                Log.i(LOG_TAG, "Query=$query,user selection=$selectedPIDs, intersection=$intersection")

                metricsCollector.applyFilter(enabled = intersection, order = settings.getPIDsSortOrder())
            }
        } else {
            Log.i(LOG_TAG, "Do not update the query. It's not surface renderer screen.")
        }
    }

    private fun applyVirtualScreen1() =  if (this.screenId == SurfaceRendererType.GIULIA) {
        settings.getGiuliaRendererSetting().applyVirtualScreen1()
    } else {
        settings.getGaugeRendererSetting().applyVirtualScreen1()
    }

    private fun applyVirtualScreen2() =  if (this.screenId == SurfaceRendererType.GIULIA) {
        settings.getGiuliaRendererSetting().applyVirtualScreen2()
    } else {
        settings.getGaugeRendererSetting().applyVirtualScreen2()
    }

    private fun applyVirtualScreen3() =  if (this.screenId == SurfaceRendererType.GIULIA) {
        settings.getGiuliaRendererSetting().applyVirtualScreen3()
    } else {
        settings.getGaugeRendererSetting().applyVirtualScreen3()
    }

    private fun applyVirtualScreen4() =  if (this.screenId == SurfaceRendererType.GIULIA) {
        settings.getGiuliaRendererSetting().applyVirtualScreen4()
    } else {
        settings.getGaugeRendererSetting().applyVirtualScreen4()
    }


    private fun getSelectedPIDs(): Set<Long>  =  if (this.screenId == SurfaceRendererType.GIULIA) {
            settings.getGiuliaRendererSetting().selectedPIDs
        } else {
            settings.getGaugeRendererSetting().selectedPIDs
        }

    private fun getVerticalActionStrip(): ActionStrip? {

        var added = false
        var builder = ActionStrip.Builder()

        if (settings.isVirtualScreenEnabled(1)) {
            added = true

            builder = builder.addAction(createAction(carContext, R.drawable.action_virtual_screen_1, actionStripColor(VIRTUAL_SCREEN_1)) {
                parent.invalidate()
                applyVirtualScreen1()
                updateQuery()
                renderFrame()
            })
        }

        if (settings.isVirtualScreenEnabled(2)) {

            added = true
            builder = builder.addAction(createAction(carContext, R.drawable.action_virtual_screen_2, actionStripColor(VIRTUAL_SCREEN_2)) {
                parent.invalidate()
                applyVirtualScreen2()
                updateQuery()
                renderFrame()
            })
        }

        if (settings.isVirtualScreenEnabled(3)) {

            added = true
            builder = builder.addAction(createAction(carContext, R.drawable.action_virtual_screen_3, actionStripColor(VIRTUAL_SCREEN_3)) {
                parent.invalidate()
                applyVirtualScreen3()
                updateQuery()
                renderFrame()
            })
        }

        if (settings.isVirtualScreenEnabled(4)) {
            added = true

            builder = builder.addAction(createAction(carContext, R.drawable.action_virtual_screen_4, actionStripColor(VIRTUAL_SCREEN_4)) {
                parent.invalidate()
                applyVirtualScreen4()
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

    private fun actionStripColor(key: String): CarColor = if (screenId  == SurfaceRendererType.GAUGE &&  settings.getGaugeRendererSetting().getCurrentVirtualScreen() == key
        || screenId  == SurfaceRendererType.GIULIA &&  settings.getGiuliaRendererSetting().getCurrentVirtualScreen() == key) {
        CarColor.GREEN
    } else {
        mapColor(settings.getColorTheme().actionsBtnVirtualScreensColor)
    }

    private fun getSurfaceRendererType (): SurfaceRendererType =
        if (screenId is SurfaceRendererType) screenId as SurfaceRendererType else  SurfaceRendererType.GIULIA

}