package org.obd.graphs.ui.preferences

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.CollectionType
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.obd.graphs.ApplicationContext
import org.obd.metrics.ObdMetric

class ItemPreference(var id: Long, var position: Int)

class RecycleViewPreferences constructor(private val prefName: String) {
    private var mapper = ObjectMapper()

    fun map(m: ObdMetric, index: Int): ItemPreference {
        return ItemPreference(m.command.pid.id, index)
    }

    init {
        mapper.registerModule(KotlinModule())
    }

    fun store(
        data: MutableList<ObdMetric>
    ) {

        val pref = sharedPreferences()
        val mapIndexed = data.mapIndexed { index, metric ->
            map(metric, index)
        }

        val writeValueAsString = mapper.writeValueAsString(mapIndexed)
        val edit = pref.edit()
        edit.putString(prefName, writeValueAsString)
        edit.apply()
    }

    fun load(): List<ItemPreference>? {
        val pref = sharedPreferences()
        val it = pref.getString(prefName, "")
        val listType: CollectionType =
            mapper.typeFactory.constructCollectionType(
                ArrayList::class.java,
                ItemPreference::class.java
            )

        return if (it!!.isEmpty()) listOf() else mapper.readValue(
            it, listType
        )
    }

    private fun sharedPreferences(): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(ApplicationContext.get()!!)
    }
}