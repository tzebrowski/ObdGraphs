package org.openobd2.core.logger.ui.preferences

import android.content.Context
import androidx.preference.PreferenceManager
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.CollectionType
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.obd.metrics.Metric


abstract class RecycleViewPreferences<T> constructor(prefName: String) {
    private val prefName = prefName
    private var mapper = ObjectMapper()

    interface MetricsMapper<T> {
        fun map(m: Metric<*>, index: Int): T
    }

    abstract fun metricsMapper():  MetricsMapper<T>
    abstract fun genericType (): Class<T>

    init {
        mapper.registerModule(KotlinModule())
    }

    fun store(
        context: Context,
        data: MutableList<Metric<*>>
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

    fun load(context: Context ) : List<T>? {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val it = pref.getString(prefName, "")
        val listType: CollectionType =
            mapper.getTypeFactory().constructCollectionType(
                ArrayList::class.java,
                genericType()
            )

        return if (it!!.isEmpty()) listOf() else mapper.readValue(
            it,listType)
    }
}