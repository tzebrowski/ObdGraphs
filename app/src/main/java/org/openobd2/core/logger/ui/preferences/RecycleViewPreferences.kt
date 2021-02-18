package org.openobd2.core.logger.ui.preferences

import android.content.Context
import androidx.preference.PreferenceManager
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.CollectionType
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.obd.metrics.ObdMetric


abstract class RecycleViewPreferences<T> constructor(prefName: String) {
    private val prefName = prefName
    private var mapper = ObjectMapper()

    interface MetricsMapper<T> {
        fun map(m: ObdMetric, index: Int): T
    }

    abstract fun metricsMapper(): MetricsMapper<T>
    abstract fun genericType(): Class<T>

    init {
        mapper.registerModule(KotlinModule())
    }

    fun store(
        context: Context,
        data: MutableList<ObdMetric>
    ) {

        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val mapIndexed = data.mapIndexed { index, metric ->
            metricsMapper().map(metric, index)
        }

        val writeValueAsString = mapper.writeValueAsString(mapIndexed)
        val edit = pref.edit()
        edit.putString(prefName, writeValueAsString)
        edit.commit()
    }

    fun load(context: Context): List<T>? {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val it = pref.getString(prefName, "")
        val listType: CollectionType =
            mapper.typeFactory.constructCollectionType(
                ArrayList::class.java,
                genericType()
            )

        return if (it!!.isEmpty()) listOf() else mapper.readValue(
            it, listType
        )
    }
}