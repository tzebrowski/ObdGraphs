 /**
 * Copyright 2019-2026, Tomasz Żebrowski
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.obd.graphs.ui.graph

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.view.isVisible
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
import org.obd.graphs.R
import org.obd.graphs.activity.TOOLBAR_TOGGLE_ACTION
import org.obd.graphs.bl.datalogger.DATA_LOGGER_CONNECTED_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_CONNECTING_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_SCHEDULED_START_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_STOPPED_EVENT
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.datalogger.scaleToRange
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.bl.trip.SensorData
import org.obd.graphs.bl.trip.tripManager
import org.obd.graphs.bl.trip.tripVirtualScreenManager
import org.obd.graphs.getPowerPreferences
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.registerReceiver
import org.obd.graphs.ui.BaseFragment
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.COLOR_TRANSPARENT
import org.obd.graphs.ui.common.Colors
import org.obd.graphs.ui.common.onDoubleClickListener
import org.obd.graphs.ui.withDataLogger
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.PidDefinitionRegistry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val LOG_TAG = "Graph"

class GraphFragment : BaseFragment() {
    private var broadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                when (intent?.action) {
                    DATA_LOGGER_SCHEDULED_START_EVENT -> {
                        if (isAdded && isVisible) {
                            Log.i(org.obd.graphs.activity.LOG_TAG, "Scheduling data logger for=${query().getIDs()}")
                            withDataLogger { dataLogger ->
                                dataLogger.scheduleStart(getPowerPreferences().startDataLoggingAfter, query())
                            }
                        }
                    }

                    DATA_LOGGER_CONNECTING_EVENT -> {
                        initializeChart(root)
                    }

                    DATA_LOGGER_STOPPED_EVENT -> {
                        virtualScreensPanel {
                            it.isVisible = true
                        }

                        attachToFloatingButton(activity, query())
                    }

                    DATA_LOGGER_CONNECTED_EVENT -> {
                        virtualScreensPanel {
                            it.isVisible = false
                        }
                    }

                    TOOLBAR_TOGGLE_ACTION -> {
                        virtualScreensPanel {
                            it.isVisible = !it.isVisible
                        }
                    }
                }
            }
        }

    private class ReverseValueFormatter(
        val pid: PidDefinition,
    ) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String = pid.scaleToRange(value).toString()
    }

    private val xAxisFormatter =
        object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = simpleDateFormat.format(Date(tripStartTs + value.toLong()))
        }

    private val onGestureListener =
        object : OnChartGestureListener {
            override fun onChartGestureStart(
                me: MotionEvent,
                lastPerformedGesture: ChartGesture,
            ) {
            }

            override fun onChartGestureEnd(
                me: MotionEvent,
                lastPerformedGesture: ChartGesture,
            ) {
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
                velocityY: Float,
            ) {
            }

            override fun onChartScale(
                me: MotionEvent,
                scaleX: Float,
                scaleY: Float,
            ) {
            }

            override fun onChartTranslate(
                me: MotionEvent,
                dX: Float,
                dY: Float,
            ) {
            }
        }

    private val simpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private lateinit var chart: LineChart
    private var tripStartTs: Long = System.currentTimeMillis()
    private lateinit var preferences: GraphPreferences
    private lateinit var root: View

    override fun onDestroyView() {
        super.onDestroyView()
        requireContext().unregisterReceiver(broadcastReceiver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        root = inflater.inflate(R.layout.fragment_graph, container, false)
        preferences = graphPreferencesReader.read()

        initializeChart(root)
        registerMetricsObserver()
        initializeTripDetails()
        loadCurrentTrip()
        registerReceivers()
        configureRecyclerView()
        setupVirtualViewPanel()
        attachToFloatingButton(activity, query())
        return root
    }

    private fun configureRecyclerView() {
        val displayInfoPanel = Prefs.getBoolean("pref.trips.recordings.display_info", true)
        configureRecyclerView(R.id.graph_view_chart, true, 5f)
        configureRecyclerView(R.id.graph_view_table_layout, displayInfoPanel, 0.2f)
        configureRecyclerView(R.id.recycler_view, displayInfoPanel, 1.3f)
    }

    private fun configureRecyclerView(
        id: Int,
        visible: Boolean,
        weight: Float,
    ) {
        val view: View = root.findViewById(id)
        view.visibility = if (visible) View.VISIBLE else View.GONE
        (view.layoutParams as LinearLayout.LayoutParams).run {
            this.weight = weight
            this.width = LinearLayout.LayoutParams.MATCH_PARENT
        }
    }

    private fun registerMetricsObserver() {
        DataLoggerRepository.observe(viewLifecycleOwner) {
            if (preferences.metrics.contains(it.command.pid.id)) {
                addEntry(it)
            }
        }
    }

    private fun initializeTripDetails() {
        val data: MutableList<SensorData> = arrayListOf()
        val adapter = TripDetailsViewAdapter(root.context, data)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)

        recyclerView.layoutManager = GridLayoutManager(root.context, 1)
        recyclerView.adapter = adapter

        DataLoggerRepository.observe(viewLifecycleOwner) {
            DataLoggerRepository.findHistogramFor(it).let { hist ->
                val sensorData =
                    SensorData(
                        id = it.command.pid.id,
                        metrics = mutableListOf(),
                        min = hist.min,
                        max = hist.max,
                        mean = hist.mean,
                    )
                val indexOf = data.indexOf(sensorData)
                if (indexOf == -1) {
                    data.add(sensorData)
                    adapter.notifyItemInserted(data.indexOf(sensorData))
                } else {
                    data[indexOf] = sensorData
                    adapter.notifyItemChanged(indexOf, sensorData)
                }
            }
        }
    }

    private fun initializeChart(root: View) {
        val colors = Colors().get()
        chart =
            buildChart(root).apply {
                val pidRegistry: PidDefinitionRegistry = DataLoggerRepository.getPidDefinitionRegistry()
                val metrics =
                    preferences.metrics
                        .mapNotNull {
                            pidRegistry.findBy(it)
                        }.toMutableList()

                Log.i(LOG_TAG, "Initializing chart with following PIDs: ${preferences.metrics}")

                val dataSets =
                    LineData(
                        metrics
                            .mapNotNull {
                                try {
                                    val dataSet = createDataSetFor(it, colors.nextInt())
                                    Log.d(LOG_TAG, "Created chart data-set for PID: ${it.id}")
                                    dataSet
                                } catch (e: Throwable) {
                                    Log.v(LOG_TAG, "Failed to create chart  data-set ${e.message} for PID: ${it.id}", e)
                                    null
                                }
                            }.toList(),
                    )

                Log.i(LOG_TAG, "Created data-set size: ${dataSets.dataSetCount}")
                data = dataSets
                setOnTouchListener(onDoubleClickListener(requireContext()))
                invalidate()
                onChartGestureListener = onGestureListener
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadCurrentTrip() {
        if (preferences.cacheEnabled) {
            val trip = tripManager.getCurrentTrip()
            val registry = DataLoggerRepository.getPidDefinitionRegistry()
            tripStartTs = trip.startTs

            val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
            val adapter = recyclerView.adapter as TripDetailsViewAdapter
            adapter.mData.addAll(trip.entries.values)
            adapter.notifyDataSetChanged()

            trip.entries.let { cache ->
                chart.run {
                    cache.forEach { (id, entry) ->
                        registry.findBy(id)?.let { pid ->
                            data.getDataSetByLabel(pid.description, true)?.let { lineData ->
                                lineData.clear()

                                getEntries(entry).forEach {
                                    lineData.addEntry(it)
                                }

                                data.notifyDataChanged()
                            }
                        }
                    }

                    Log.i(LOG_TAG, "Set scale minima of XAxis to 7f")
                    notifyDataSetChanged()
                    setScaleMinima(7f, 0.1f)
                    moveViewToX(xAxis.axisMaximum - 5000f)

                    debug("Reset view port")
                }
            }
        }
    }

    private fun getEntries(entry: SensorData): MutableList<Entry> =
        mutableListOf<Entry>().apply {
            entry.metrics.forEach {
                if (it.entry.y is Number) {
                    add(Entry(it.entry.x, (it.entry.y as Number).toFloat(), it.entry.data))
                }
            }
            sortBy { entry -> entry.x }
        }

    private fun LineChart.debug(label: String) {
        Log.i(
            LOG_TAG,
            "$label: axisMinimum=${xAxis.axisMinimum},axisMaximum=${xAxis.axisMaximum}, visibleXRange=$visibleXRange",
        )
    }

    private fun registerReceivers() {
        registerReceiver(requireContext(), broadcastReceiver) {
            it.addAction(DATA_LOGGER_CONNECTED_EVENT)
            it.addAction(DATA_LOGGER_STOPPED_EVENT)
            it.addAction(DATA_LOGGER_CONNECTING_EVENT)
            it.addAction(TOOLBAR_TOGGLE_ACTION)
            it.addAction(DATA_LOGGER_SCHEDULED_START_EVENT)
        }
    }

    private fun addEntry(obdMetric: ObdMetric) {
        chart.run {
            data.getDataSetByLabel(obdMetric.command.pid.description, true)?.let {
                val ts = (System.currentTimeMillis() - tripStartTs).toFloat()
                val entry =
                    Entry(ts, obdMetric.scaleToRange(), obdMetric.command.pid.id)

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

    private fun buildChart(root: View): LineChart =
        (root.findViewById<LineChart>(R.id.graph_view_chart)!!).apply {
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

    private fun createDataSetFor(
        pid: PidDefinition,
        col: Int,
    ): LineDataSet {
        val values = mutableListOf<Entry>()
        val lineDataSet = LineDataSet(values, pid.description)
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
            valueFormatter = ReverseValueFormatter(pid)
            fillAlpha = 35
            fillColor = col
            highLightColor = Color.rgb(244, 117, 117)
            setDrawCircleHole(false)
            valueTextSize = 14f
            isHighlightEnabled = true
        }
        return lineDataSet
    }

    private fun setVirtualViewBtn(
        btnId: Int,
        selection: String,
        viewId: String,
    ) {
        (root.findViewById<Button>(btnId)).let {
            if (selection == viewId) {
                it.setBackgroundColor(COLOR_PHILIPPINE_GREEN)
            } else {
                it.setBackgroundColor(COLOR_TRANSPARENT)
            }

            it.setOnClickListener {
                tripVirtualScreenManager.updateScreenId(viewId)
                setupVirtualViewPanel()
                preferences = graphPreferencesReader.read()

                initializeChart(root)
                initializeTripDetails()
                loadCurrentTrip()
            }
        }
    }

    private fun setupVirtualViewPanel() {
        val currentVirtualScreen = tripVirtualScreenManager.getCurrentScreenId()
        setVirtualViewBtn(R.id.virtual_view_1, currentVirtualScreen, "1")
        setVirtualViewBtn(R.id.virtual_view_2, currentVirtualScreen, "2")
        setVirtualViewBtn(R.id.virtual_view_3, currentVirtualScreen, "3")
        setVirtualViewBtn(R.id.virtual_view_4, currentVirtualScreen, "4")
        setVirtualViewBtn(R.id.virtual_view_5, currentVirtualScreen, "5")
        setVirtualViewBtn(R.id.virtual_view_6, currentVirtualScreen, "6")
    }

    private fun virtualScreensPanel(func: (p: LinearLayout) -> Unit) {
        if (graphPreferencesReader.read().toggleVirtualPanel) {
            func(root.findViewById(R.id.virtual_view_panel))
        }
    }

    private fun query() = Query.instance(QueryStrategyType.SHARED_QUERY).apply(preferences.metrics)
}
