package org.obd.graphs.bl.datalogger.drag

val dragRaceRegistry: DragRaceRegistry by lazy { DragRaceRegistryImpl() }

interface DragRaceRegistry {
    fun getResult(): DragRaceResults
    fun update0100(value: Long)
    fun update0160(value: Long)
    fun update100200(value: Long)
}