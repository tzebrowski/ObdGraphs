package org.obd.graphs.bl.query

import org.obd.graphs.bl.drag.dragRacingResultRegistry

internal class DragRacingQueryStrategy : QueryStrategy(
    mutableSetOf(
        dragRacingResultRegistry.getEngineRpmPID(),
        dragRacingResultRegistry.getVehicleSpeedPID()
    )
)