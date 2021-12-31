package org.openobd2.core.logger.ui.dash

import android.content.Context
import android.graphics.Color
import androidx.preference.PreferenceManager
import com.github.mikephil.charting.model.GradientColor


data class ColorTheme(val col1: MutableList<GradientColor>, val col2: MutableList<GradientColor>)

class Theme {

    companion object {

        @JvmStatic
        fun getSelectedTheme(context: Context): ColorTheme {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return list()[pref.getString("pref.dash.theme", "0")!!.toInt()]
        }

        private fun list(): Array<ColorTheme> {
            return arrayOf(std1(), std2(), std3(), std4(), std5())
        }


        @JvmStatic
        fun std5(): ColorTheme {
            val col1: MutableList<GradientColor> = ArrayList()
            col1.add(
                GradientColor(
                    Color.rgb(236, 233, 230),
                    Color.rgb(255, 255, 255)
                )
            )

            val col2: MutableList<GradientColor> = ArrayList()
            col2.add(
                GradientColor(
                    Color.rgb(255, 251, 213),
                    Color.rgb(178, 10, 44)
                )
            )
            return ColorTheme(col1, col2)
        }

        @JvmStatic
        fun std3(): ColorTheme {
            val col1: MutableList<GradientColor> = ArrayList()
            col1.add(
                GradientColor(
                    Color.rgb(255, 252, 0),
                    Color.rgb(255, 255, 255)
                )
            )

            val col2: MutableList<GradientColor> = ArrayList()
            col2.add(
                GradientColor(
                    Color.rgb(255, 251, 213),
                    Color.rgb(178, 10, 44)
                )
            )
            return ColorTheme(col1, col2)
        }

        @JvmStatic
        fun std2(): ColorTheme {
            val col1: MutableList<GradientColor> = ArrayList()
            col1.add(
                GradientColor(
                    Color.rgb(234, 234, 234),
                    Color.rgb(219, 219, 219)
                )
            )

            val col2: MutableList<GradientColor> = ArrayList()
            col2.add(
                GradientColor(
                    Color.rgb(237, 33, 58),
                    Color.rgb(147, 41, 30)
                )
            )
            return ColorTheme(col1, col2)
        }


        @JvmStatic
        fun std1(): ColorTheme {
            val col1: MutableList<GradientColor> = ArrayList()
            col1.add(
                GradientColor(
                    Color.rgb(243, 249, 167),
                    Color.rgb(113, 178, 128)

                )
            )

            val col2: MutableList<GradientColor> = ArrayList()
            col2.add(
                GradientColor(
                    Color.rgb(255, 251, 213),
                    Color.rgb(178, 10, 44)
                )
            )
            return ColorTheme(col1, col2)
        }

        @JvmStatic
        fun std4(): ColorTheme {
            val col1: MutableList<GradientColor> = ArrayList()
            col1.add(
                GradientColor(
                    Color.rgb(33, 147, 176),
                    Color.rgb(109, 213, 237)
                )
            )

            val col2: MutableList<GradientColor> = ArrayList()
            col2.add(
                GradientColor(
                    Color.rgb(237, 33, 58),
                    Color.rgb(147, 41, 30)
                )
            )
            return ColorTheme(col1, col2)
        }

    }

}
