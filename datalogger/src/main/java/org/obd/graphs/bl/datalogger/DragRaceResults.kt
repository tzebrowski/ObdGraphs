package org.obd.graphs.bl.datalogger

data class DragRaceEntry(val _0_100val: Int = 0, val _0_160val: Int = 0,val _100_200val: Int = 0)

data class DragRaceResults(val current: DragRaceEntry = DragRaceEntry(),
                           val last: DragRaceEntry = DragRaceEntry(),
                           val best: DragRaceEntry = DragRaceEntry())