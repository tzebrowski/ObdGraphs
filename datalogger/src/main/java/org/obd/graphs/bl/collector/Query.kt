package org.obd.graphs.bl.collector

import org.obd.graphs.bl.datalogger.QueryType
import org.obd.graphs.bl.drag.dragRacingResultRegistry
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getStringSet


private const val PREFERENCE_PID_FAST = "pref.pids.generic.high"
private const val PREFERENCE_PID_SLOW = "pref.pids.generic.low"

class Query: java.io.Serializable {
    private val directMetrics = mutableSetOf<Long>()
    private var queryType: QueryType  =QueryType.METRICS


    fun getPIDs(): MutableSet<Long> {
        return  when(queryType){
            QueryType.DIRECT_METRICS -> {
                directMetrics
            }
            QueryType.METRICS -> {
                (fastPIDs() + slowPIDs()).toMutableSet()
            }
            QueryType.DRAG_RACING -> {
                mutableSetOf(dragRacingResultRegistry.getEngineRpmPID(),
                        dragRacingResultRegistry.getVehicleSpeedPID())
            }
        }
    }

    fun getQueryType(): QueryType = queryType

    fun setQueryType (queryType: QueryType){
        this.queryType = queryType
    }

    fun updateDirectMetricsPIDs(newPIDs: Set<Long>){
        directMetrics.clear()
        directMetrics.addAll(newPIDs)
    }

    private fun fastPIDs() = Prefs.getStringSet(PREFERENCE_PID_FAST).map { s -> s.toLong() }
    private fun slowPIDs() = Prefs.getStringSet(PREFERENCE_PID_SLOW).mapNotNull {
        try {
            it.toLong()
        }catch (e: Exception){
            null
        }
    }
}