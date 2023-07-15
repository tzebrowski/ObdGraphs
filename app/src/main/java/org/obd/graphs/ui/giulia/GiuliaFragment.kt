package org.obd.graphs.ui.giulia

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import android.widget.Button
import androidx.fragment.app.Fragment
import org.obd.graphs.R
import org.obd.graphs.RenderingThread
import org.obd.graphs.bl.collector.CarMetricsCollector
import org.obd.graphs.bl.datalogger.DATA_LOGGER_CONNECTED_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_STOPPED_EVENT
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getLongSet
import org.obd.graphs.preferences.getS
import org.obd.graphs.renderer.Fps
import org.obd.graphs.renderer.ScreenRenderer
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.COLOR_TRANSPARENT


open class GiuliaFragment : Fragment() {
    private lateinit var root: View

    private val metricsCollector = CarMetricsCollector()
    private val fps = Fps()
    private val settings = GiuliaSettings()
    private lateinit var surfaceController: SurfaceController

    private val renderingThread: RenderingThread = RenderingThread(
        renderAction = {
            surfaceController.renderFrame()
        },
        perfFrameRate = {
            settings.getSurfaceFrameRate()
        }
    )

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {

                DATA_LOGGER_CONNECTED_EVENT -> {
                    renderingThread.start()
                }

                DATA_LOGGER_STOPPED_EVENT -> {
                    renderingThread.stop()
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        surfaceController.renderFrame()
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(DATA_LOGGER_CONNECTED_EVENT)
            addAction(DATA_LOGGER_STOPPED_EVENT)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        renderingThread.stop()
    }

    override fun onDetach() {
        super.onDetach()
        activity?.unregisterReceiver(broadcastReceiver)
        renderingThread.stop()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        root  = inflater.inflate(R.layout.fragment_giulia, container, false)
        val surfaceView = root.findViewById<SurfaceView>(R.id.surface_view)
        setupVirtualViewPanel()
        surfaceController = SurfaceController(ScreenRenderer.of(requireContext(), settings, metricsCollector, fps))
        surfaceView.holder.addCallback(surfaceController)

        metricsCollector.applyFilter(getVisiblePIDsList(giuliaVirtualScreen.getVirtualScreenPrefKey()))

        dataLogger.observe(viewLifecycleOwner) {
            it.run {
                metricsCollector.append(it)
            }
        }

        if (dataLogger.isRunning()) {
            renderingThread.start()
        }

        return root
    }

    private fun setVirtualViewBtn(btnId: Int, selection: String, viewId: String) {
        (root.findViewById<Button>(btnId)).let {
            if (selection == viewId) {
                it.setBackgroundColor(COLOR_PHILIPPINE_GREEN)
            } else {
                it.setBackgroundColor(COLOR_TRANSPARENT)
            }

            it.setOnClickListener {
                giuliaVirtualScreen.updateVirtualScreen(viewId)
                metricsCollector.applyFilter(getVisiblePIDsList(giuliaVirtualScreen.getVirtualScreenPrefKey()))
                setupVirtualViewPanel()
                surfaceController.renderFrame()
            }
        }
    }

    private fun setupVirtualViewPanel() {
        val currentVirtualScreen = giuliaVirtualScreen.getCurrentVirtualScreen()
        setVirtualViewBtn(R.id.virtual_view_1, currentVirtualScreen, "1")
        setVirtualViewBtn(R.id.virtual_view_2, currentVirtualScreen, "2")
        setVirtualViewBtn(R.id.virtual_view_3, currentVirtualScreen, "3")
        setVirtualViewBtn(R.id.virtual_view_4, currentVirtualScreen, "4")
        setVirtualViewBtn(R.id.virtual_view_5, currentVirtualScreen, "5")
        setVirtualViewBtn(R.id.virtual_view_6, currentVirtualScreen, "6")
        setVirtualViewBtn(R.id.virtual_view_7, currentVirtualScreen, "7")
    }

    private fun getVisiblePIDsList(metricsIdsPref: String): Set<Long> {
        val query = dataLoggerPreferences.getPIDsToQuery()
        return Prefs.getLongSet(metricsIdsPref).filter { query.contains(it) }.toSet()
    }
}