package org.openobd2.core.logger.bl

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.github.mikephil.charting.data.Entry
import org.obd.metrics.ObdMetric
import org.obd.metrics.Reply
import org.obd.metrics.ReplyObserver
import org.obd.metrics.command.obd.SupportedPidsCommand
import org.openobd2.core.logger.ui.common.Cache
import org.openobd2.core.logger.ui.graph.Scaler

const val CACHE_ENTRIES_PROPERTY_NAME = "cache.graph.entries"
const val CACHE_TS_PROPERTY_NAME = "cache.graph.ts"
const val CACHE_X_AXIS_MIN_PROPERTY_NAME = "cache.graph.x_axis.min"

internal class MetricsAggregator : ReplyObserver<Reply<*>>() {

    companion object {
        @JvmStatic
        val debugData: MutableLiveData<Reply<*>> = MutableLiveData<Reply<*>>().apply {
        }

        @JvmStatic
        val metrics: MutableLiveData<ObdMetric> = MutableLiveData<ObdMetric>().apply {
        }
    }

    private var firstTimeStamp: Long? = null
    private val scaler  = Scaler()

    fun reset() {
        debugData.postValue(null)
        metrics.postValue(null)
    }

    override fun onNext(reply: Reply<*>) {
        initCache()

        debugData.postValue(reply)
        if (reply is ObdMetric && reply.command !is SupportedPidsCommand) {
            reply.command.pid?.let {
                metrics.postValue(reply)
                addCacheEntry(reply)
            }
        }
    }

    private fun initCache() {
        if (Cache[CACHE_ENTRIES_PROPERTY_NAME] == null) {
            Cache[CACHE_ENTRIES_PROPERTY_NAME] = mutableMapOf<String, MutableList<Entry>>()
        }

        if (firstTimeStamp == null) {
            firstTimeStamp = System.currentTimeMillis().apply {
                Cache[CACHE_TS_PROPERTY_NAME] = this
            }
            Log.i("MetricsAggregator", "Init cache stamp: $firstTimeStamp")
        }
    }

    private fun addCacheEntry(reply: ObdMetric) {
        try {
            Cache[CACHE_ENTRIES_PROPERTY_NAME]?.let {
                val cache = it as MutableMap<String, MutableList<Entry>>
                val timestamp = (System.currentTimeMillis() - firstTimeStamp!!).toFloat()
                val entry = Entry(timestamp, scaler.scaleToNewRange(reply))
                cache.getOrPut(reply.command.pid.description) {
                    mutableListOf<Entry>()
                }.add(entry)
            }
        }catch (e: Exception){
            Log.i("MetricsAggregator","Failed to add cache entry",e)
        }
    }
}