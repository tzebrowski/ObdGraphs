package org.obd.graphs.ui.giulia

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import org.obd.graphs.R
import org.obd.graphs.RenderingThread
import org.obd.graphs.aa.SURFACE_BROKEN_EVENT
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
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.COLOR_TRANSPARENT
import org.obd.graphs.ui.gauge.gaugeVirtualScreen

private const val LOG_KEY = "GiuliaDashboardFragment"

open class GiuliaDashboardFragment : Fragment(), SurfaceHolder.Callback {
    private lateinit var root: View

    private val metricsCollector = CarMetricsCollector()

    private lateinit var surfaceHolder: SurfaceHolder
    private var surface: Surface? = null
    private var visibleArea: Rect? = null
    private var surfaceLocked = false

    private val fps = Fps()
    private lateinit var renderer: ScreenRenderer

    private val settings = GiuliaDashboardSettings()

    private val renderingThread: RenderingThread = RenderingThread(
        renderAction = {
            renderFrame()
        },
        perfFrameRate = {
            Prefs.getS("pref.giulia.dashboard.fps", "10").toInt()
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

        root  = inflater.inflate(R.layout.fragment_giulia_dashboard, container, false);
        val surfaceView = root.findViewById<SurfaceView>(R.id.surface_view)
        surfaceView.holder.addCallback(this)
        setupVirtualViewPanel()

        renderer = ScreenRenderer.of(requireContext(), settings, metricsCollector, fps)
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

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceHolder = holder
        surfaceHolder.addCallback(this);
        visibleArea = Rect()
        visibleArea?.set(holder.surfaceFrame.left+10, holder.surfaceFrame.top + 10, holder.surfaceFrame.right+10, holder.surfaceFrame.bottom);
        surface = surfaceHolder.surface
        renderFrame()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

        surface = surfaceHolder.surface
        visibleArea?.set(holder.surfaceFrame.left+10, holder.surfaceFrame.top + 10, width, height);

        renderFrame()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
       surface?.release()
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
                renderFrame()
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
        setVirtualViewBtn(R.id.virtual_view_8, currentVirtualScreen, "8")
    }

    private fun getVisiblePIDsList(metricsIdsPref: String): Set<Long> {
        val query = dataLoggerPreferences.getPIDsToQuery()
        return Prefs.getLongSet(metricsIdsPref).filter { query.contains(it) }.toSet()
    }

    @MainThread
    fun renderFrame() {

        surface?.let {
            var canvas: Canvas? = null
            if (it.isValid && !surfaceLocked) {
                try {
                    Log.e("Render frame", "Render frame")
                    canvas = it.lockHardwareCanvas()
                    surfaceLocked = true
                    renderer.onDraw(
                        canvas = canvas,
                        visibleArea = visibleArea
                    )

                } catch (e: Throwable) {
                    Log.e(LOG_KEY, "Exception was thrown during surface locking.", e)
                    surface = null
                    sendBroadcastEvent(SURFACE_BROKEN_EVENT)
                } finally {
                    try {
                        canvas?.let { c ->
                            it.unlockCanvasAndPost(c)
                        }
                    } catch (e: Throwable) {
                        Log.e(LOG_KEY, "Exception was thrown during surface un-locking.", e)
                        sendBroadcastEvent(SURFACE_BROKEN_EVENT)
                    }

                    surfaceLocked = false
                }
            }
        }
    }
}