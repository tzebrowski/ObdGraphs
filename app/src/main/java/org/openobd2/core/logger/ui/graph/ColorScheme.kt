package org.openobd2.core.logger.ui.graph

import com.github.mikephil.charting.utils.ColorTemplate

fun colorScheme(): IntIterator {

    val colorScheme = mutableListOf<Int>()
    ColorTemplate.MATERIAL_COLORS.forEach {
        colorScheme.add(it)
    }

    ColorTemplate.COLORFUL_COLORS.forEach {
        colorScheme.add(it)
    }

    ColorTemplate.JOYFUL_COLORS.forEach {
        colorScheme.add(it)
    }
    return colorScheme.toIntArray().iterator()
}