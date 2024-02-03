package org.obd.graphs.bl.query

import android.util.Log
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getLongSet
import org.obd.graphs.runAsync

private const val LOG_KEY = "query"

internal class QueryStrategyOrchestrator : java.io.Serializable, Query {

    private val strategies: Map<QueryStrategyType, QueryStrategy> = mutableMapOf<QueryStrategyType, QueryStrategy>().apply {
        runAsync {
            this[QueryStrategyType.SHARED_QUERY] = SharedQueryStrategy()
            this[QueryStrategyType.DRAG_RACING_QUERY] = DragRacingQueryStrategy()
            this[QueryStrategyType.INDIVIDUAL_QUERY_FOR_EACH_VIEW] = IndividualQueryStrategy()
            this[QueryStrategyType.ROUTINES_QUERY] = RoutinesQueryStrategy()
        }
    }

    private var strategy: QueryStrategyType = QueryStrategyType.SHARED_QUERY

    override fun getIDs(): MutableSet<Long> = strategies[strategy]?.getPIDs() ?: mutableSetOf()

    override fun getStrategy(): QueryStrategyType = strategy

    override fun setStrategy(queryStrategyType: QueryStrategyType): Query {
        this.strategy = queryStrategyType
        return this
    }

    override fun update(newPIDs: Set<Long>): Query {
        strategies[strategy]?.update(newPIDs)
        return this
    }

    override fun filterBy(filter: String): Set<Long> {
        val query = getIDs()
        val selection = Prefs.getLongSet(filter)
        val intersection =  selection.filter { query.contains(it) }.toSet()

        Log.i(LOG_KEY,"Individual query enabled:${isIndividualQuerySelected()}, " +
                " key:$filter, query=$query,selection=$selection, intersection=$intersection")

        return if (isIndividualQuerySelected()) {
            Log.i(LOG_KEY,"Returning selection=$selection")
            selection
        } else {
            Log.i(LOG_KEY,"Returning intersection=$intersection")
            intersection
        }
    }
    override fun apply(filter: String): Query =
        if (isIndividualQuerySelected()) {
            setStrategy(QueryStrategyType.INDIVIDUAL_QUERY_FOR_EACH_VIEW)
                .update(filterBy(filter))
        } else {
            setStrategy(QueryStrategyType.SHARED_QUERY)
        }

    override fun apply(filter: Set<Long>): Query =
        if (isIndividualQuerySelected()) {
            setStrategy(QueryStrategyType.INDIVIDUAL_QUERY_FOR_EACH_VIEW)
                .update(filter)
        } else {
            setStrategy(QueryStrategyType.SHARED_QUERY)
        }

    private fun isIndividualQuerySelected() = dataLoggerPreferences.instance.queryForEachViewStrategyEnabled
}