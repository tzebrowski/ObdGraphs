package org.obd.graphs.bl.datalogger.drag

data class DragRaceEntry(var _0_100val: Long = 0, var _0_160val: Long = 0,var _100_200val: Long = 0)

data class DragRaceResults(val current: DragRaceEntry = DragRaceEntry(),
                           val last: DragRaceEntry = DragRaceEntry(),
                           val best: DragRaceEntry = DragRaceEntry()
)