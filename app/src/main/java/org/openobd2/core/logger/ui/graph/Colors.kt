package org.openobd2.core.logger.ui.graph

import java.util.*

class Colors {
    private val recycle: Stack<Int> = Stack()
    private val colors: Stack<Int> = Stack()
    val color: Int
        get() {
            if (colors.size === 0) {
                while (!recycle.isEmpty()) colors.push(recycle.pop())
                Collections.shuffle(colors)
            }
            val c: Int = colors.pop()
            recycle.push(c)
            return c
        }

    init {
        recycle.addAll(
            listOf(
                -0xbbcca, -0x16e19d, -0x63d850, -0x98c549,
                -0xc0ae4b, -0xde690d, -0xfc560c, -0xff432c,
                -0xff6978, -0xb350b0, -0x743cb6, -0x3223c7,
                -0x14c5, -0x3ef9, -0x6800, -0xa8de,
                -0x86aab8, -0x616162, -0x9f8275, -0xcccccd
            )
        )
    }

    fun generate(): IntIterator {

        val colorScheme = mutableListOf<Int>()
        val randomCollors = Colors()
        repeat((0 until 30).count()) { colorScheme.add(randomCollors.color) }

        return colorScheme.toIntArray().iterator()
    }
}

