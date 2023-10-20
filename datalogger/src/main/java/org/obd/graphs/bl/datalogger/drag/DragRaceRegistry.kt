package org.obd.graphs.bl.datalogger.drag


val dragRaceRegistry = DragRaceRegistry()

class DragRaceRegistry {

    private val current =  DragRaceEntry()
    private val best =  DragRaceEntry()

    fun update0_100(value: Long){
        current._0_100val = value
        if (best._0_100val > value || best._0_100val == 0L){
            best._0_100val = value
        }
    }

    fun update_160(value: Long){
        current._0_160val = value

        if (best._0_160val > value || best._0_160val == 0L){
            best._0_160val = value
        }
    }

    fun update_100_200(value: Long){
        current._100_200val = value

        if (best._100_200val > value  || best._100_200val == 0L){
            best._100_200val = value
        }
    }

    fun getResult(): DragRaceResults = DragRaceResults(current = current, best = best)
}