package org.obd.graphs.aa.screen.nav

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.aa.*
import org.obd.graphs.aa.screen.*
import org.obd.graphs.aa.screen.CarScreen
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.*
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.renderer.Fps
import org.obd.metrics.pid.PIDsGroup
import org.obd.metrics.pid.PidDefinition

internal class RoutinesScreen (carContext: CarContext,
                      settings: CarSettings,
                      metricsCollector: MetricsCollector,
                      fps: Fps,
                      private val screenNavigator: ScreenNavigator
) : CarScreen(carContext, settings, metricsCollector, fps) {

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ROUTINE_WORKFLOW_NOT_RUNNING_EVENT -> toast.show(carContext, R.string.routine_workflow_is_not_running)
                ROUTINE_UNKNOWN_STATUS_EVENT ->  toast.show(carContext, R.string.routine_unknown_error)
                ROUTINE_EXECUTION_FAILED_EVENT -> toast.show(carContext, R.string.routine_execution_failed)
                ROUTINE_EXECUTED_SUCCESSFULLY_EVENT -> toast.show(carContext, R.string.routine_executed_successfully)
                ROUTINE_EXECUTION_NO_DATA_RECEIVED_EVENT -> toast.show(carContext, R.string.routine_no_data)
            }
        }
    }

    override fun onGetTemplate(): Template {
        var items = ItemList.Builder()
        dataLogger.getPidDefinitionRegistry().findBy(PIDsGroup.ROUTINE).sortedBy { it.description }.forEach {
            items = items.addItem(buildRoutineListItem(it))
        }

        return ListTemplate.Builder()
            .setLoading(false)
            .setTitle(carContext.getString(R.string.routine_page_title))
            .setSingleList(items.build())
            .setActionStrip(
                getHorizontalActionStrip(
                    preferencesEnabled = false,
                    exitEnabled = false,
                    screenId = screenNavigator.getCurrentScreenId(),
                    toggleBtnColor = screenNavigator.getCurrentScreenBtnColor()
                )
            ).build()
    }

    override fun onCreate(owner: LifecycleOwner) {
        carContext.registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(ROUTINE_REJECTED_EVENT)
            addAction(ROUTINE_WORKFLOW_NOT_RUNNING_EVENT)
            addAction(ROUTINE_EXECUTION_FAILED_EVENT)
            addAction(ROUTINE_EXECUTED_SUCCESSFULLY_EVENT)
            addAction(ROUTINE_EXECUTION_NO_DATA_RECEIVED_EVENT)
        })
    }

    override fun onDestroy(owner: LifecycleOwner) {
        carContext.unregisterReceiver(broadcastReceiver)
    }

    private fun buildRoutineListItem(data: PidDefinition): Row = Row.Builder()
        .setOnClickListener {
            Log.i(LOG_KEY, "Executing routine ${data.description}")
            dataLogger.executeRoutine(Query.instance(QueryStrategyType.ROUTINES_QUERY).update(setOf(data.id)))
        }
        .setBrowsable(false)
        .addText(data.longDescription?:data.description)
        .setTitle(data.description)
        .build()
}