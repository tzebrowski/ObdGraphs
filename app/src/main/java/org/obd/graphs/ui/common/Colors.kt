package org.obd.graphs.ui.common

import android.graphics.Color
import androidx.core.content.ContextCompat
import org.obd.graphs.ApplicationContext
import java.util.*

class Colors {

    private val recycle: Stack<Int> = Stack()
    private val colors: Stack<Int> = Stack()
    private val base: List<Int> =  mutableListOf<Int>().apply {
        add(Color.parseColor("#C22636"))
        add(Color.parseColor("#01804F"))
        add(Color.parseColor("#1C3D72"))
        add(Color.parseColor("#BBBBBB"))

        add(Color.parseColor("#F44336"))
        add(Color.parseColor("#4A148C"))
        add(Color.parseColor("#FF9800"))
        add(Color.parseColor("#FFFF00"))
        add(Color.parseColor("#42A5F5"))
        add(Color.parseColor("#4DB6AC"))
        add(Color.parseColor("#3F51B5"))
        add(Color.parseColor("#C0CA33"))
        add(Color.parseColor("#FF6F00"))
        add(Color.parseColor("#E8F5E9"))
        add(Color.parseColor("#757575"))
        add(Color.parseColor("#FFCCBC"))
        add(Color.parseColor("#00C853"))
        add(Color.parseColor("#66BB6A"))
    }

    val color: Int
        get() {
            if (colors.size == 0) {
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
        val randomColors = Colors()
        colorScheme.addAll(base)
        repeat((0 until 30).count()) { colorScheme.add(randomColors.color) }
        return colorScheme.toIntArray().iterator()
    }
}


fun color(id: Int) = ContextCompat.getColor(ApplicationContext.get()!!, id)
