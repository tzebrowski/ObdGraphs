package org.openobd2.core.logger.ui.preferences

import android.content.Context
import androidx.preference.PreferenceManager
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.obd.metrics.Metric

class DashItemPreferences(query: String, position: Int) {
    var query: String = query
    var position: Int = position

    companion object {

        private const val PREF_NAME = "prefs.dash.pids.settings"
        private var mapper = ObjectMapper()

        init {
            mapper.registerModule(KotlinModule())
        }


        @JvmStatic
        fun store(context: Context, data: MutableList<Metric<*>>) {

            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            val mapIndexed = data.mapIndexed { index, metric ->
                DashItemPreferences(metric.command.query, index)
            }

            val writeValueAsString = mapper.writeValueAsString(mapIndexed)
            val edit = pref.edit()
            edit.putString(PREF_NAME, writeValueAsString)
            edit.commit()
        }


        @JvmStatic
        fun load(context: Context): List<DashItemPreferences>? {

            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            val it = pref.getString(PREF_NAME, "")
            return if (it!!.isEmpty()) listOf() else mapper.readValue(
                it,
                object : TypeReference<List<DashItemPreferences>>() {})
        }
    }
}