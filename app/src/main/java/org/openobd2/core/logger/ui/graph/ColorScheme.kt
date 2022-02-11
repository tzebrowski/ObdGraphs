package org.openobd2.core.logger.ui.graph

import android.graphics.Color

fun colorScheme(): IntIterator {

    val colorScheme = mutableListOf<Int>()
    colorScheme.add(Color.RED)
    colorScheme.add(Color.BLUE)
    colorScheme.add(Color.YELLOW)
    colorScheme.add(Color.GREEN)
    colorScheme.add(Color.CYAN)
    colorScheme.add(Color.MAGENTA)
    colorScheme.add(Color.LTGRAY)
    colorScheme.add(Color.WHITE)

    return colorScheme.toIntArray().iterator()
}