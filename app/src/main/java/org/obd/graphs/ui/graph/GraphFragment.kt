package org.obd.graphs.ui.graph

import android.annotation.SuppressLint
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.utils.ColorTemplate
import org.obd.graphs.Cache
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.*
import org.obd.graphs.bl.trip.TripRecorder
import org.obd.graphs.ui.common.Colors
import org.obd.graphs.ui.common.onDoubleClickListener
import org.obd.graphs.ui.preferences.Prefs
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.PidDefinitionRegistry
import java.text.SimpleDateFormat
import java.util.*


private const val METRIC_COLLECTING_PROCESS_IS_RUNNING = "cache.graph.collecting_process_is_running"

const val LOADED_TRIP_PREFERENCE_ID = "pref.graph.trips.selected"

private const val LOGGER_TAG = "GraphFragment"

class GraphFragment : Fragment() {

    private var prefsChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == LOADED_TRIP_PREFERENCE_ID) {
                sharedPreferences!!.getString(key, null)?.let {
                    if (!isDataCollectingProcessWorking()) {
                        context?.run {
                            tripRecorder.loadTrip(it)
                        }
                    }
                }
            }
        }

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                DATA_LOGGER_CONNECTING_EVENT -> {
                    Cache[METRIC_COLLECTING_PROCESS_IS_RUNNING] = true
                    initializeChart(root)
                }
                DATA_LOGGER_STOPPED_EVENT -> {
                    Cache[METRIC_COLLECTING_PROCESS_IS_RUNNING] = false
                }
            }
        }
    }

    private class ReverseValueFormatter(val pid: PidDefinition, val valueScaler: ValueScaler) :
        ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return valueScaler.scaleToPidRange(pid, value).toString()
        }
    }

    private val xAxisFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return simpleDateFormat.format(Date(tripStartTs + value.toLong()))
        }
    }

    private val onGestureListener = object : OnChartGestureListener {
        override fun onChartGestureStart(me: MotionEvent, lastPerformedGesture: ChartGesture) {}
        override fun onChartGestureEnd(me: MotionEvent, lastPerformedGesture: ChartGesture) {
            if (lastPerformedGesture != ChartGesture.SINGLE_TAP) {
                chart.highlightValues(null)
            }
        }

        override fun onChartLongPressed(me: MotionEvent) {}
        override fun onChartDoubleTapped(me: MotionEvent) {}
        override fun onChartSingleTapped(me: MotionEvent) {}
        override fun onChartFling(
            me1: MotionEvent,
            me2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ) {
        }

        override fun onChartScale(me: MotionEvent, scaleX: Float, scaleY: Float) {}
        override fun onChartTranslate(me: MotionEvent, dX: Float, dY: Float) {}
    }

    private val simpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private lateinit var chart: LineChart
    private var colors: IntIterator = Colors().generate()
    private val valueScaler = ValueScaler()
    private var tripStartTs: Long = System.currentTimeMillis()
    private lateinit var preferences: GraphPreferences
    private val tripRecorder: TripRecorder by lazy { TripRecorder.instance }
    private lateinit var root: View
    private lateinit var tripViewAdapter: TripViewAdapter

    override fun onDestroyView() {
        super.onDestroyView()
        requireContext().unregisterReceiver(broadcastReceiver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        root = inflater.inflate(R.layout.fragment_graph, container, false)

        Prefs.registerOnSharedPreferenceChangeListener(prefsChangeListener)

        colors = Colors().generate()
        preferences = getGraphPreferences()

        initializeChart(root)

        registerMetricsObserver()

        initializeTripDetails()
        loadCurrentTrip()
        registerReceivers()

        val displayInfoPanel = Prefs.getBoolean("pref.trips.recordings.display_info", true)
        configureRecyclerView(R.id.graph_view_chart, true, 5f)
        configureRecyclerView(R.id.graph_view_table_layout, displayInfoPanel, 0.2f)
        configureRecyclerView(R.id.recycler_view, displayInfoPanel, 1.3f)
        return root
    }

    private fun configureRecyclerView(id: Int, visible: Boolean, weight: Float) {
        val view: View = root.findViewById(id)
        view.visibility = if (visible) View.VISIBLE else View.GONE
        (view.layoutParams as LinearLayout.LayoutParams).run {
            this.weight = weight
            this.width = LinearLayout.LayoutParams.MATCH_PARENT
        }
    }

    private fun registerMetricsObserver() {
        MetricsAggregator.metrics.observe(viewLifecycleOwner) {
            it?.let {
                if (preferences.selectedPids.contains(it.command.pid.id)) {
                    addEntry(it)
                }
            }
        }
    }

    private fun initializeTripDetails() {
        tripViewAdapter = TripViewAdapter(root.context, mutableListOf())
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(root.context, 1)
        recyclerView.adapter = tripViewAdapter
    }


    private fun initializeChart(root: View) {

        chart = buildChart(root).apply {

            val pidRegistry: PidDefinitionRegistry = DataLogger.instance.pidDefinitionRegistry()
            val metrics = preferences.selectedPids.mapNotNull {
                pidRegistry.findBy(it)
            }.toMutableList()

            data = LineData(metrics.map { createDataSet(it) }.toList())
            setOnTouchListener(onDoubleClickListener(requireContext()))
            invalidate()
            onChartGestureListener = onGestureListener
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadCurrentTrip() {
        if (preferences.cacheEnabled) {
            val trip = tripRecorder.getCurrentTrip()
            val registry = DataLogger.instance.pidDefinitionRegistry()
            tripStartTs = trip.startTs


            tripViewAdapter.mData.addAll(trip.entries.values)
            tripViewAdapter.notifyDataSetChanged()

            trip.entries.let { cache ->
                chart.run {
                    cache.forEach { (id, entry) ->
                        registry.findBy(id)?.let { pid ->
                            data.getDataSetByLabel(pid.description, true)?.let { lineData ->
                                lineData.clear()
                                entry.entries.sortBy { entry -> entry.x }
                                entry.entries.forEach {
                                    Log.d(LOGGER_TAG, " ${pid.description} =  ${it.x} =  ${it.y}")
                                    lineData.addEntry(it)
                                }

                                data.notifyDataChanged()
                            }
                        }
                    }


                    Log.i(LOGGER_TAG, "Set scale minima of XAxis to 7f")
                    notifyDataSetChanged()
                    setScaleMinima(7f, 0.1f)
                    moveViewToX(xAxis.axisMaximum - 5000f)

                    debug("Reset view port")
                }
            }
        }
    }

    private fun LineChart.debug(label: String) {
        Log.i(
            LOGGER_TAG,
            "$label: axisMinimum=${xAxis.axisMinimum},axisMaximum=${xAxis.axisMaximum}, visibleXRange=${visibleXRange}"
        )
    }

    private fun isDataCollectingProcessWorking() =
        (Cache[METRIC_COLLECTING_PROCESS_IS_RUNNING] as Boolean?) ?: false

    private fun registerReceivers() {
        requireContext().registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(DATA_LOGGER_CONNECTED_EVENT)
            addAction(DATA_LOGGER_STOPPED_EVENT)
            addAction(DATA_LOGGER_CONNECTING_EVENT)
        })
    }

    private fun addEntry(obdMetric: ObdMetric) {
        chart.run {
            data.getDataSetByLabel(obdMetric.command.pid.description, true)?.let {
                val ts = (System.currentTimeMillis() - tripStartTs).toFloat()
                val entry =
                    Entry(ts, valueScaler.scaleToNewRange(obdMetric), obdMetric.command.pid.id)
                it.addEntry(entry)
                data.notifyDataChanged()
                notifyDataSetChanged()

                if (!xAxis.axisMaximum.isNaN() && !xAxis.axisMaximum.isInfinite()) {
                    xAxis.axisMinimum = xAxis.axisMinimum + preferences.xAxisMinimumShift
                }
                invalidate()
            }
        }
    }


    private fun buildChart(root: View): LineChart {
        return (root.findViewById(R.id.graph_view_chart) as LineChart).apply {
            description.isEnabled = false
            setTouchEnabled(true)
            dragDecelerationFrictionCoef = 0.9f
            isDoubleTapToZoomEnabled = false

            isDragEnabled = true
            setScaleEnabled(true)
            setDrawGridBackground(false)
            isHighlightPerDragEnabled = true
            setBackgroundColor(Color.BLACK)
            setViewPortOffsets(10f, 10f, 10f, 10f)
            marker = MarkerWindow(context, R.layout.graph_marker_view, this)

            legend.run {
                isEnabled = true
                form = Legend.LegendForm.LINE
                textColor = Color.WHITE
                textSize = 16f
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                orientation = Legend.LegendOrientation.HORIZONTAL
                isWordWrapEnabled = true
                maxSizePercent = 0.20f
                xOffset = 40f
            }

            xAxis.run {
                position = XAxis.XAxisPosition.TOP_INSIDE
                textSize = 10f
                textColor = Color.GREEN
                setDrawAxisLine(true)
                setDrawGridLines(true)
                textColor = Color.rgb(255, 192, 56)
                setCenterAxisLabels(true)
                valueFormatter = xAxisFormatter
                isGranularityEnabled = true
            }

            axisLeft.run {
                setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
                textColor = ColorTemplate.getHoloBlue()
                setDrawGridLines(true)
                setDrawMarkers(true)
                isGranularityEnabled = true

                axisMinimum = 0f
                axisMaximum = 5000f
                textColor = Color.rgb(255, 192, 56)
            }

            axisRight.run {
                isEnabled = false
            }
        }
    }

    private fun createDataSet(pid: PidDefinition): LineDataSet {
        val values = mutableListOf<Entry>()
        val lineDataSet = LineDataSet(values, pid.description)
        val col = colors.nextInt()
        lineDataSet.run {
            mode = LineDataSet.Mode.CUBIC_BEZIER
            label = pid.description
            lineDataSet.form = Legend.LegendForm.SQUARE
            axisDependency = AxisDependency.LEFT
            color = col
            valueTextColor = col
            lineWidth = 4f
            setDrawCircles(false)
            setDrawValues(true)
            setDrawFilled(true)
            fillColor = col
            valueFormatter = ReverseValueFormatter(pid, valueScaler)
            fillAlpha = 35
            fillColor = col
            highLightColor = Color.rgb(244, 117, 117)
            setDrawCircleHole(false)
            valueTextSize = 14f
            isHighlightEnabled = true
        }
        return lineDataSet
    }
}