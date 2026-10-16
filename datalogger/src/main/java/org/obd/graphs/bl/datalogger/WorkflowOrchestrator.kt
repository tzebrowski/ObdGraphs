/*
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
package org.obd.graphs.bl.datalogger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.*
import org.obd.graphs.bl.datalogger.connectors.ConnectionManager
import org.obd.graphs.bl.datalogger.dtc.DtcActionQueue
import org.obd.graphs.bl.datalogger.dtc.DtcRequest
import org.obd.graphs.bl.datalogger.dtc.DtcScanKind
import org.obd.graphs.bl.datalogger.dtc.EngineStateTracker
import org.obd.graphs.bl.datalogger.dtc.dtcClearOutcome
import org.obd.graphs.bl.datalogger.dtc.dtcConnectScanModules
import org.obd.graphs.bl.datalogger.dtc.dtcScanTargets
import org.obd.graphs.bl.datalogger.dtc.toScanProgress
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.readiness.PREF_READINESS_LAST
import org.obd.graphs.bl.readiness.resetsReadiness
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.updateString
import org.obd.graphs.bl.trip.tripManager
import org.obd.graphs.profile.PROFILE_CHANGED_EVENT
import org.obd.metrics.api.CANNetworkRegistry
import org.obd.metrics.api.DefaultCanNetworkRegistry
import org.obd.metrics.api.Workflow
import org.obd.metrics.api.WorkflowExecutionStatus
import org.obd.metrics.api.model.*
import org.obd.metrics.codec.formula.FormulaEvaluatorConfig
import org.obd.metrics.command.Command
import org.obd.metrics.command.dtc.DiagnosticTroubleCodeClearStatus
import org.obd.metrics.command.dtc.DtcModuleStatus
import org.obd.metrics.command.dtc.DtcProgress
import org.obd.metrics.command.group.CommandGroup
import org.obd.metrics.command.group.DefaultCommandGroup
import org.obd.metrics.command.routine.RoutineCommand
import org.obd.metrics.command.routine.RoutineExecutionStatus
import org.obd.metrics.diagnostic.Diagnostics
import org.obd.metrics.diagnostic.Histogram
import org.obd.metrics.diagnostic.Rate
import org.obd.metrics.diagnostic.RateType
import org.obd.metrics.pid.PIDsGroup
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.PidDefinitionRegistry
import org.obd.metrics.pid.Urls
import org.obd.metrics.pid.ValueType
import org.obd.metrics.translation.TranslationProvider
import java.util.*

const val JS_ENGINE_NAME = "rhino"

private const val CONNECT_SCAN_START_TIMEOUT_MS = 10_000L

/**
 * That's the wrapper interface on Workflow API.
 */
internal class WorkflowOrchestrator internal constructor() {
    inner class EventsReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action === PROFILE_CHANGED_EVENT) {
                Log.i(LOG_TAG, "Received profile changed event")
                updateModulesRegistry()
            }

            if (intent.action === MODULES_LIST_CHANGED_EVENT) {
                updateModulesRegistry()
                sendBroadcastEvent(WORKFLOW_RELOAD_EVENT)
            }
        }
    }

    internal val eventsReceiver = EventsReceiver()
    private val metricsObserver = MetricsObserver().apply { observe(EngineStateTracker) }

    private var lifecycle = object : Lifecycle {

        override fun onConnecting() {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "Received onConnecting event.Start collecting process")
            }

            status = WorkflowStatus.Connecting

            sendBroadcastEvent(DATA_LOGGER_CONNECTING_EVENT)
        }

        // Called on ObdMetrics' command thread as each module's requests start: only a broadcast here. The phone
        // overlay and the AA loading title show it.
        override fun onDTCProgress(progress: DtcProgress) {
            sendBroadcastEvent(DATA_LOGGER_DTC_PROGRESS, progress.toScanProgress())
        }

        // moduleStatus: how each scanned module answered (ObdMetrics); a module that did not answer is shown as such
        // instead of as clean, and is left out of the history comparison.
        override fun onDTCCompleted(
            dtc: Set<DiagnosticTroubleCode>,
            status: DiagnosticTroubleCodeClearStatus?,
            moduleStatus: Map<String, DtcModuleStatus>?
        ) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "Received onDTCCompleted event.")
            }

            // The completion belongs to the request in flight (one at a time, see DtcActionQueue); null
            // for one nobody asked for, stored as a plain read like before.
            val (finished, next) = dtcActions.complete()
            val kind = finished?.kind ?: DtcScanKind.READ
            Log.i(LOG_TAG, "DTC action completed: $kind, codes=${dtc.size}, next=${next?.kind}, modules=$moduleStatus")
            VehicleCapabilitiesManager.updateDTC(dtc, finished?.moduleLabels.orEmpty(), kind = kind, moduleStatus = moduleStatus.orEmpty())

            if (kind == DtcScanKind.CONNECT && dtc.isNotEmpty()) sendBroadcastEvent(DATA_LOGGER_DTC_AVAILABLE)

            when {
                // A request waited for this one: the screen that asked for it keeps waiting for its result.
                // Sent off ObdMetrics' callback thread, which is the DTC handler finishing this action.
                next != null -> runAsync(wait = false) { send(next) }
                // A clear is always followed by a read, so dtc are the codes left after it.
                kind == DtcScanKind.CLEAR -> {
                    val outcome = dtcClearOutcome(status, dtc)
                    Log.i(LOG_TAG, "DTC clear completed. Status=$status, codes left=${dtc.size}, outcome=$outcome")
                    if (outcome.resetsReadiness()) Prefs.updateString(PREF_READINESS_LAST, null)
                    sendBroadcastEvent(DATA_LOGGER_DTC_ACTION_COMPLETED, outcome)
                }
                else -> sendBroadcastEvent(DATA_LOGGER_DTC_ACTION_COMPLETED)
            }
        }

        override fun onRoutineCompleted(
            routineCommand: RoutineCommand,
            status: RoutineExecutionStatus
        ) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "Received onRoutineCompleted event. Routine: ${routineCommand.pid.description}  execution status: $status")
            }

            val event = when (status) {
                RoutineExecutionStatus.ERROR -> ROUTINE_EXECUTION_FAILED_EVENT
                RoutineExecutionStatus.NO_DATA -> ROUTINE_EXECUTION_NO_DATA_RECEIVED_EVENT
                RoutineExecutionStatus.SUCCESS -> ROUTINE_EXECUTED_SUCCESSFULLY_EVENT
            }

            sendBroadcastEvent(event)
        }

        override fun onModuleDiscovered(
            header: String,
            status: ModuleDiscoveryStatus
        ) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "Received onModuleDiscovered event. Header: $header, status: $status")
            }

            sendBroadcastEvent(
                MODULE_DISCOVERY_RESULT_EVENT,
                ModuleDiscoveryResult(header = header, found = status == ModuleDiscoveryStatus.FOUND)
            )
        }

        override fun onRunning(vehicleCapabilities: VehicleCapabilities) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "Received onRunning event. We are connected to the vehicle: $vehicleCapabilities")
            }

            sendBroadcastEvent(SCREEN_UNLOCK_PROGRESS_EVENT)
            status = WorkflowStatus.Connected
            dtcActions.reset()
            val connectScanModules =
                if (dataLoggerSettings.instance().adapter.dtcEnabled) {
                    dtcConnectScanModules(DiagnosticRequestIDManager.getMappings())
                } else {
                    emptySet()
                }
            VehicleCapabilitiesManager.updateCapabilities(vehicleCapabilities, moduleScanOnConnect = connectScanModules.isNotEmpty())
            sendBroadcastEvent(DATA_LOGGER_CONNECTED_EVENT)
            tripManager.startNewTrip(System.currentTimeMillis())

            if (connectScanModules.isNotEmpty()) {
                scheduleConnectScan(connectScanModules)
            }
        }

        override fun onError(msg: String, tr: Throwable?) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "Received onConnecting event. An error occurred during interaction with the device. Msg: $msg")
            }

            sendBroadcastEvent(SCREEN_UNLOCK_PROGRESS_EVENT)
            sendBroadcastEvent(DATA_LOGGER_ERROR_EVENT)
        }

        override fun onStopped() {
            sendBroadcastEvent(SCREEN_UNLOCK_PROGRESS_EVENT)
            dtcActions.reset()

            status = WorkflowStatus.Disconnected

            Log.i(
                LOG_TAG,
                "Collecting process is completed."
            )
            sendBroadcastEvent(DATA_LOGGER_STOPPED_EVENT)
            tripManager.saveCurrentTrip()
        }
    }

    private val workflow: Workflow = workflow().apply {
        applySavedOverrides(this.pidRegistry)
        registerGPSPids(this.pidRegistry)
    }

    private var status = WorkflowStatus.Disconnected
    private val metricsProcessorsRegistry = mutableSetOf<MetricsProcessor>()
    private val adjustmentsStrategy = AdjustmentsStrategy()

    fun observe(metricsProcessor: MetricsProcessor) {
        if (metricsProcessorsRegistry.contains(metricsProcessor)) {
            Log.i(LOG_TAG, "Metrics processor is already registered: $metricsProcessor")
        } else {
            Log.i(LOG_TAG, "Registering: $metricsProcessor metrics processor")
            metricsProcessorsRegistry.add(metricsProcessor)
            metricsObserver.observe(metricsProcessor)
        }
    }

    fun observe(lifecycleOwner: LifecycleOwner, observer: (metric: ObdMetric) -> Unit) =
        metricsObserver.observe(lifecycleOwner) {
            it.let {
                observer(it)
            }
        }

    fun observeEach(lifecycleOwner: LifecycleOwner, observer: (metric: ObdMetric) -> Unit) =
        metricsObserver.observeEach(lifecycleOwner, observer)

    fun status(): WorkflowStatus = status

    fun isRunning(): Boolean = workflow.isRunning

    fun diagnostics(): Diagnostics = workflow.diagnostics

    fun findHistogramFor(metric: ObdMetric): Histogram =
        workflow.diagnostics.histogram().findBy(metric.command.pid)

    fun findRateFor(metric: ObdMetric): Optional<Rate> =
        workflow.diagnostics.rate().findBy(RateType.MEAN, metric.command.pid)

    fun pidDefinitionRegistry(): PidDefinitionRegistry = workflow.pidRegistry

    fun stop() {

        Log.i(
            LOG_TAG, "Sending STOP to the workflow with 'graceful.stop' parameter set to " +
                    "${dataLoggerSettings.instance().adapter.gracefulStop}"
        )
        try {
            workflow.stop(dataLoggerSettings.instance().adapter.gracefulStop)
            Log.i(LOG_TAG, "After send the STOP. Workflow is running ${workflow.isRunning}")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to stop the workflow", e)
        }
    }
    fun start(query: Query) {
        runAsync (wait=false) {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
            currentQuery = query

            val dataLoggerQuery =
                org.obd.metrics.api.model.Query.builder().pids(query.getIDs()).build()
            Log.i(
                LOG_TAG,
                "Stating collecting process. Strategy: ${query.getStrategy()}. Selected PIDs: ${dataLoggerQuery.pids}"
            )
            val adjustments = adjustmentsStrategy.findAdjustmentFor(query.getStrategy())
            val init = init()

            ConnectionManager.obtain(pidDefinitionRegistry(), dataLoggerQuery, adjustments, init)
                ?.run {
                    val status = workflow.start(this, dataLoggerQuery, init, adjustments)
                    Log.i(
                        LOG_TAG,
                        "Collecting process started. Strategy: ${query.getStrategy()}. Status=$status"
                    )
                }
        }
    }

    // selectedModules restricts the scan to the given Diagnostic Request ID entry names (their
    // requestKey, eg. "ABS") - empty means the plain default single-ECU scan, no module targeting.
    // DTC actions run one at a time: ObdMetrics cancels a running one when another is scheduled.
    private val dtcActions = DtcActionQueue()

    // ObdMetrics counts the workflow as running only once all its threads are up, which is after
    // onRunning returns - scheduling from inside the callback would be refused as NOT_RUNNING.
    private fun scheduleConnectScan(modules: Set<String>) {
        runAsync(wait = false) {
            val deadline = System.currentTimeMillis() + CONNECT_SCAN_START_TIMEOUT_MS
            while (!workflow.isRunning && System.currentTimeMillis() < deadline) {
                Thread.sleep(50)
            }
            if (workflow.isRunning) {
                Log.i(LOG_TAG, "Scheduling DTC read on connect for all modules: $modules")
                scheduleDTCRead(modules, connectScan = true)
            } else {
                Log.w(LOG_TAG, "Workflow did not start in time, skipping DTC read on connect")
            }
        }
    }

    fun scheduleDTCCleanup(selectedModules: Set<String> = emptySet()) =
        submit(DtcRequest(DtcScanKind.CLEAR, getDtcModules(selectedModules)))

    fun scheduleDTCRead(
        selectedModules: Set<String> = emptySet(),
        connectScan: Boolean = false
    ) = submit(DtcRequest(if (connectScan) DtcScanKind.CONNECT else DtcScanKind.READ, getDtcModules(selectedModules)))

    private fun submit(request: DtcRequest) {
        if (dtcActions.submit(request)) {
            send(request)
        } else {
            Log.i(LOG_TAG, "DTC action ${request.kind} waits for the one in flight")
        }
    }

    private fun send(request: DtcRequest) {
        val readAction = getReadDtcAction()
        val actions = if (request.kind == DtcScanKind.CLEAR) setOf(DtcAction.CLEAR, readAction) else setOf(readAction)
        Log.i(LOG_TAG, "Schedule DTC ${request.kind}. Read action=$readAction, modules=${request.modules}")
        val result = workflow.scheduleDTCAction(actions, request.modules)
        Log.i(LOG_TAG, "DTC ${request.kind} is scheduled: $result")
        // Refused (e.g. not connected): no completion will come, so nothing is in flight.
        if (result != WorkflowExecutionStatus.DTC_QUEUED) dtcActions.reset()
    }

    // Reuses the Diagnostic Request ID table as the DTC scan target list, restricted to whichever
    // entries the user picked for this scan (selection is by requestKey, the internal ID). The
    // module's displayName (description, falling back to requestKey) is what ends up on
    // Init.Header.mode - not a protocol mode, but the label shown in the DTC list/dialogs.
    private fun getDtcModules(selectedModules: Set<String>): List<Init.Header> =
        dtcScanTargets(DiagnosticRequestIDManager.getMappings(), selectedModules)
            .map { Init.Header.builder().mode(it.displayName).header(it.headerValue).build() }

    fun discoverModule(header: String) {
        val result = workflow.discoverModule(header)
        Log.i(LOG_TAG, "Module discovery for header=$header scheduled: $result")
    }

    // Runs off the caller's thread for the same reason start() does: DataLoggerService dispatches
    // this from onStartCommand, i.e. on the MAIN thread, and executeRoutine opens the transport -
    // AdapterConnection.connect() blocks for as long as the adapter takes to answer.
    fun executeRoutine(query: Query) {
        runAsync (wait=false) {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
            currentQuery = query
            val init = init()
            val dataLoggerQuery = org.obd.metrics.api.model.Query.builder().pids(query.getIDs()).build()
            val adjustments = adjustmentsStrategy.findAdjustmentFor(query.getStrategy())

            ConnectionManager.obtain(pidDefinitionRegistry(), dataLoggerQuery, adjustments, init)?.run {
                Log.i(
                    LOG_TAG,
                    "Executing routine. Strategy: ${query.getStrategy()}. Selected PIDs: ${dataLoggerQuery.pids}"
                )

                val status = workflow.executeRoutine(dataLoggerQuery.pids.first(), init)
                Log.i(
                    LOG_TAG,
                    "Routines has been completed. Strategy: ${query.getStrategy()}. Status=$status"
                )

                when (status) {
                    WorkflowExecutionStatus.REJECTED -> sendBroadcastEvent(ROUTINE_REJECTED_EVENT)
                    WorkflowExecutionStatus.NOT_RUNNING -> sendBroadcastEvent(
                        ROUTINE_WORKFLOW_NOT_RUNNING_EVENT
                    )

                    else -> sendBroadcastEvent(ROUTINE_UNKNOWN_STATUS_EVENT)
                }
            }
        }
    }

    private lateinit var currentQuery: Query

    fun updateQuery(query: Query) {
        if (isSameQuery(query)) {
            Log.w(LOG_TAG, "Received same query=${query.getIDs()}. Do not update.")
        } else {

            val dataLoggerQuery =
                org.obd.metrics.api.model.Query.builder().pids(query.getIDs()).build()
            val adjustments = adjustmentsStrategy.findAdjustmentFor(query.getStrategy())

            val status = workflow.updateQuery(
                dataLoggerQuery,
                init(), adjustments
            )

            Log.i(
                LOG_TAG,
                "Query update finished, strategy: ${query.getStrategy()}. Status=$status"
            )
        }

        currentQuery = query
    }

    // The query running now, null before the first start; lets a screen put back what it replaced.
    fun runningQuery(): Query? = if (::currentQuery.isInitialized) currentQuery else null

    fun isSameQuery(query: Query) =
        ::currentQuery.isInitialized && query.getIDs() == currentQuery.getIDs()

    fun isDTCEnabled(): Boolean = workflow.pidRegistry.findBy(PIDsGroup.DTC_READ).isNotEmpty()

    private fun init(preferences: DataLoggerSettings = dataLoggerSettings.instance()) : Init {
        val init  = Init.builder()
            .delayAfterInit(preferences.adapter.initDelay)
            .delayAfterReset(preferences.adapter.delayAfterReset)
            .headers(DiagnosticRequestIDManager.getMapping().map { entry ->
                Init.Header.builder().mode(entry.key).header(entry.value).build()
            }.toMutableList())
            .protocol(Init.Protocol.valueOf(preferences.adapter.initProtocol))
            .networkRegistry(networkRegistry(preferences.adapter.type))
            .sequence(sequence(preferences.adapter.initSequence)).build()

        return init
    }

    private fun networkRegistry(adapterType : String): CANNetworkRegistry {
        Log.i(LOG_TAG,"Selected adapter type $adapterType")

        return when (adapterType) {
            "DEFAULT" -> {
                DefaultCanNetworkRegistry()
            }

            "VGATE_VLINKER_MS" -> {
                runCatching {
                    val clazz = Class.forName("org.obd.metrics.init.VgateVlinkerMsCANNetworkRegistry")
                    val defaultField = clazz.getDeclaredField("DEFAULT")
                    defaultField.get(null) as CANNetworkRegistry
                }.getOrElse { exception ->
                    Log.e(LOG_TAG, "Reflection failed: ${exception.message}. Falling back to DefaultCanNetworkRegistry.")
                    DefaultCanNetworkRegistry()
                }
            }

            else -> {
                DefaultCanNetworkRegistry()
            }
        }
    }


    private fun sequence(seqName : String): CommandGroup<out Command?> =
        if (seqName == "DEFAULT") {
            DefaultCommandGroup.INIT
        } else {
            try {
                Log.i(LOG_TAG, "Loading init-sequence for: $seqName")
                val className = "org.obd.metrics.init.VgateVlinkerMsSequence"
                val field = Class.forName(className).getField(seqName)
                field.get(null) as? CommandGroup<*> ?: DefaultCommandGroup.INIT
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed loading init-sequence for: $seqName", e)
                DefaultCommandGroup.INIT
            }
        }

    private fun workflow() = Workflow.instance()
        .formulaEvaluatorConfig(
            FormulaEvaluatorConfig.builder().scriptEngine(JS_ENGINE_NAME).build()
        )
        .pids(pids())
        .translationProvider(createTranslationProvider())
        .observer(metricsObserver)
        .lifecycle(lifecycle)
        .lifecycle(metricsObserver)
        .initialize()

    private fun createTranslationProvider(): TranslationProvider {
        val locale = Locale.getDefault().language
        Log.i(LOG_TAG, "Creating TranslationProvider for locale: $locale")
        return TranslationProvider.instance(locale)
    }

    fun updateTranslations(locale: String) {
        Log.i(LOG_TAG, "Updating PID translations for locale: $locale")
        val provider = if (locale.isNotEmpty()) TranslationProvider.instance(locale) else TranslationProvider.instance("en")
        workflow.updatePidRegistry(
            pids(),
            provider
        )

        applySavedOverrides(workflow.pidRegistry)
        registerGPSPids(workflow.pidRegistry)
    }

    private fun updateModulesRegistry() = runAsync {
        workflow.updatePidRegistry(
            pids()
        )

        applySavedOverrides(workflow.pidRegistry)
        registerGPSPids(workflow.pidRegistry)
    }

    private fun registerGPSPids(pidRegistry: PidDefinitionRegistry) {
        Log.d(LOG_TAG, "Registering GPS PIDs")
        pidRegistry.register(
            PidDefinition(
                Pid.GPS_LOCATION_PID_ID.id,
                2,
                "",
                "22",
                "Alt",
                "m",
                "GPS",
                -180,
                10000,
                ValueType.DOUBLE
            )
        )
    }

    private fun applySavedOverrides(pidRegistry: PidDefinitionRegistry) {
        pidRegistry.findAll().forEach { livePid ->
            if (!livePid.isUserCustom) {
                livePid.deserialize()?.let { savedPid ->
                    livePid.description = savedPid.description ?: livePid.description
                    livePid.longDescription = savedPid.longDescription ?: livePid.longDescription
                    livePid.mode = savedPid.mode ?: livePid.mode
                    livePid.pid = savedPid.pid ?: livePid.pid
                    livePid.formula = savedPid.formula ?: livePid.formula
                    livePid.length = savedPid.length ?: livePid.length
                    livePid.min = savedPid.min ?: livePid.min
                    livePid.max = savedPid.max ?: livePid.max
                    livePid.units = savedPid.units ?: livePid.units
                    livePid.type = savedPid.type ?: livePid.type
                    livePid.overrides = savedPid.overrides ?: livePid.overrides
                    livePid.stable = savedPid.stable
                    livePid.cacheable = savedPid.cacheable

                    livePid.alert.lowerThreshold = savedPid.alert.lowerThreshold
                    livePid.alert.upperThreshold = savedPid.alert.upperThreshold
                }
            }
        }
    }

    private fun getReadDtcAction(): DtcAction  = if (dataLoggerSettings.instance().adapter.dtcReadSnapshots) DtcAction.READ_SNAPSHPOTS else DtcAction.READ

    private fun pids(): Pids? {
        val resourceUrls = dataLoggerSettings.instance().resources.map {
            if (Modules.isExternalStorageModule(it)) {
                Modules.externalModuleToURL(it)
            } else {
                Urls.resourceToUrl(it)
            }
        }.toMutableList()

        Modules.getCustomPidsFile()?.let {
           if (it.exists()) {
               resourceUrls.add(it.toURI().toURL())
           }
       }

       return Pids.builder().resources(resourceUrls).build()
    }
}
