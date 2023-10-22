package org.obd.graphs.bl.datalogger.drag

val dragRaceRegistry: DragRaceRegistry by lazy { DragRaceRegistryImpl() }

interface DragRaceRegistry {
    fun getResult(): DragRaceResults
    fun update0100(time: Long, speed: Int)
    fun update0160(time: Long, speed: Int)
    fun update100200(time: Long, speed: Int)
    fun readyToRace(value: Boolean)
}