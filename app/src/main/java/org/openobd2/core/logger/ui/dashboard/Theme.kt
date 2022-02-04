package org.openobd2.core.logger.ui.dashboard

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
            return arrayOf(std1(), std2(), std3(), std4(), std5())[pref.getString("pref.dash.theme", "0")!!.toInt()]
        }

        @JvmStatic
        internal fun colorTheme(c1: GradientColor, c2: GradientColor): ColorTheme {
            return ColorTheme(ArrayList<GradientColor>().apply{
                add(c1)
            }, ArrayList<GradientColor>().apply{
                add(c2)
            })
        }

        @JvmStatic
        internal fun std5(): ColorTheme {
            return colorTheme(GradientColor(
                Color.rgb(236, 233, 230),
                Color.rgb(255, 255, 255)
            ),GradientColor(
                Color.rgb(255, 251, 213),
                Color.rgb(178, 10, 44)
            ))
        }

        @JvmStatic
        internal fun std3(): ColorTheme {
            return colorTheme(GradientColor(
                Color.rgb(255, 252, 0),
                Color.rgb(255, 255, 255)
            ),GradientColor(
                Color.rgb(255, 251, 213),
                Color.rgb(178, 10, 44)
            ))
        }

        @JvmStatic
        internal fun std2(): ColorTheme {
            return colorTheme(GradientColor(
                Color.rgb(234, 234, 234),
                Color.rgb(219, 219, 219)
            ),GradientColor(
                Color.rgb(237, 33, 58),
                Color.rgb(147, 41, 30)
            ))
        }

        @JvmStatic
        internal fun std1(): ColorTheme {
            return colorTheme(GradientColor(
                Color.rgb(243, 249, 167),
                Color.rgb(113, 178, 128)

            ),GradientColor(
                Color.rgb(255, 251, 213),
                Color.rgb(178, 10, 44)
            ))
        }

        @JvmStatic
        internal fun std4(): ColorTheme {
            return colorTheme(GradientColor(
                Color.rgb(33, 147, 176),
                Color.rgb(109, 213, 237)
            ),GradientColor(
                Color.rgb(237, 33, 58),
                Color.rgb(147, 41, 30)
            ))
        }
    }
}
