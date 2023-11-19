package org.obd.graphs.bl.query

internal open class QueryStrategy(protected val pids: MutableSet<Long> = mutableSetOf()) : java.io.Serializable {
    open fun update(newPIDs: Set<Long>) {
        pids.clear()
        pids.addAll(newPIDs)
    }

    open fun getPIDs(): MutableSet<Long> = pids
}