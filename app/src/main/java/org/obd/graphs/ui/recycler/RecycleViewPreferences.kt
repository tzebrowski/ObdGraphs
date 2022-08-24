package org.obd.graphs.ui.recycler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.CollectionType
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.obd.graphs.preferences.Prefs
import org.obd.metrics.api.model.ObdMetric

internal class ItemPreference(var id: Long, var position: Int)

internal class RecycleViewPreferences constructor(private val prefName: String) {
    private var mapper = ObjectMapper()

    init {
        mapper.registerModule(KotlinModule())
    }

    internal fun getItemsSortOrder(): Map<Long, Int>? =
        load()?.associate {
            it.id to it.position
        }

    internal fun store(
        data: MutableList<ObdMetric>
    ) {

        val mapIndexed = data.mapIndexed { index, metric ->
            map(metric, index)
        }

        val writeValueAsString = mapper.writeValueAsString(mapIndexed)

        Prefs.edit().run {
            putString(prefName, writeValueAsString)
            apply()
        }
    }

    private fun load(): List<ItemPreference>? {
        val it = Prefs.getString(prefName, "")
        val listType: CollectionType =
            mapper.typeFactory.constructCollectionType(
                ArrayList::class.java,
                ItemPreference::class.java
            )

        return if (it!!.isEmpty()) listOf() else mapper.readValue(
            it, listType
        )
    }

    private fun map(m: ObdMetric, index: Int): ItemPreference {
        return ItemPreference(m.command.pid.id, index)
    }
}