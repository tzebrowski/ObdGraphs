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
package org.obd.graphs.aa.screen.nav

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
import androidx.car.app.model.Template
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.DiagnosticRequestIDManager
import org.obd.graphs.LocalizedStringProvider
import org.obd.graphs.aa.CarSettings
import org.obd.graphs.aa.R
import org.obd.graphs.aa.mapColor
import org.obd.graphs.aa.screen.CarScreen
import org.obd.graphs.aa.screen.createAction
import org.obd.graphs.aa.screen.withDataLogger
import org.obd.graphs.aa.toast
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.datalogger.DATA_LOGGER_CONNECTED_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_CONNECTING_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_DTC_ACTION_COMPLETED
import org.obd.graphs.bl.datalogger.DATA_LOGGER_ERROR_CONNECT_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_ERROR_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_STOPPED_EVENT
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.datalogger.VehicleCapabilitiesManager
import org.obd.graphs.bl.datalogger.WorkflowStatus
import org.obd.graphs.bl.datalogger.dtc.DtcListItem
import org.obd.graphs.bl.datalogger.dtc.PREF_DTC_DESELECTED_MODULES
import org.obd.graphs.bl.datalogger.dtc.displayCode
import org.obd.graphs.bl.datalogger.dtc.displayDescription
import org.obd.graphs.bl.datalogger.dtc.dtcScanModules
import org.obd.graphs.bl.datalogger.dtc.sortedForDisplay
import org.obd.graphs.bl.datalogger.dtc.toDtcListItems
import org.obd.graphs.bl.datalogger.dtc.toDtcSections
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getStringSet
import org.obd.graphs.registerReceiver
import org.obd.graphs.renderer.api.Fps
import org.obd.graphs.renderer.api.Identity
import org.obd.metrics.api.model.DiagnosticTroubleCode

internal enum class DtcScreenIdentity(
    private val code: Int
) : Identity {
    DTC(223)
    ;

    override fun id(): Int = this.code
}

private enum class DtcAction { READ, CLEAR }

private const val LOG_TAG = "DtcScreen"

// Lists the stored DTC (the last read, same store as the phone's DTC dialog) and lets the driver
// re-read or clear them. Modules scanned follow the phone's module picker selection.
internal class DiagnosticTroubleCodesScreen(
    carContext: CarContext,
    settings: CarSettings,
    metricsCollector: MetricsCollector,
    fps: Fps
) : CarScreen(carContext, settings, metricsCollector, fps) {
    // Idle query: the screen only needs a connection for the DTC actions, not live PIDs.
    private val query: Query = Query.instance(QueryStrategyType.ROUTINES_QUERY)
    private val stringProvider = LocalizedStringProvider(carContext)
    private var actionInProgress: DtcAction? = null

    private val broadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                Log.i(LOG_TAG, "Received event=${intent?.action}")
                try {
                    when (intent?.action) {
                        DATA_LOGGER_DTC_ACTION_COMPLETED -> {
                            if (actionInProgress != null) {
                                toast.show(carContext, R.string.dtc_action_completed)
                            }
                            actionInProgress = null
                            invalidate()
                        }

                        // The DTC action never reports back once the connection is gone.
                        DATA_LOGGER_ERROR_EVENT,
                        DATA_LOGGER_STOPPED_EVENT,
                        DATA_LOGGER_ERROR_CONNECT_EVENT -> {
                            actionInProgress = null
                            invalidate()
                        }

                        DATA_LOGGER_CONNECTING_EVENT,
                        DATA_LOGGER_CONNECTED_EVENT -> invalidate()
                    }
                } catch (e: Exception) {
                    Log.w(LOG_TAG, "Failed event ${intent?.action} processing", e)
                }
            }
        }

    override fun getFeatureDescription(): List<FeatureDescription> =
        if (settings.getDtcScreenSettings().viewEnabled) {
            listOf(
                FeatureDescription(
                    DtcScreenIdentity.DTC,
                    R.drawable.action_features,
                    stringProvider.getString(R.string.available_features_dtc_screen_title)
                )
            )
        } else {
            emptyList()
        }

    // Registered for the screen's whole life, not only while resumed: a result arriving while the
    // clear confirmation is on top must still end the loading state.
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        registerReceiver(carContext, broadcastReceiver) {
            it.addAction(DATA_LOGGER_DTC_ACTION_COMPLETED)
            it.addAction(DATA_LOGGER_ERROR_EVENT)
            it.addAction(DATA_LOGGER_STOPPED_EVENT)
            it.addAction(DATA_LOGGER_ERROR_CONNECT_EVENT)
            it.addAction(DATA_LOGGER_CONNECTING_EVENT)
            it.addAction(DATA_LOGGER_CONNECTED_EVENT)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        carContext.unregisterReceiver(broadcastReceiver)
    }

    override fun dataLoggerStart() {
        withDataLogger {
            start(query)
        }
    }

    override fun onGetTemplate(): Template =
        try {
            when {
                actionInProgress != null ->
                    loadingTemplate(
                        if (actionInProgress == DtcAction.CLEAR) R.string.dtc_clearing else R.string.dtc_reading
                    )

                DataLoggerRepository.status() == WorkflowStatus.Connecting -> loadingTemplate(R.string.routine_page_connecting)

                else -> listTemplate()
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to build template", e)
            PaneTemplate
                .Builder(Pane.Builder().setLoading(true).build())
                .setHeaderAction(Action.BACK)
                .setTitle(stringProvider.getString(R.string.pref_aa_car_error))
                .build()
        }

    private fun loadingTemplate(titleId: Int): Template =
        ListTemplate
            .Builder()
            .setLoading(true)
            .setHeaderAction(Action.BACK)
            .setTitle(stringProvider.getString(titleId))
            .build()

    private fun listTemplate(): Template {
        val codes = diagnosticTroubleCodes()
        val sections =
            codes
                .toDtcListItems(
                    scannedModules = VehicleCapabilitiesManager.getDtcScannedModules(),
                    noCodesMessage = stringProvider.getString(R.string.dtc_no_codes),
                    noCodesForModuleMessage = stringProvider.getString(R.string.dtc_no_codes_for_module)
                ).toDtcSections()

        val builder =
            ListTemplate
                .Builder()
                .setLoading(false)
                .setHeaderAction(Action.BACK)
                .setTitle(title(codes))
                .setActionStrip(actionStrip(codes.isNotEmpty()))

        // A ListTemplate takes either one list or sections, never both.
        val headerless = sections.singleOrNull()?.takeIf { it.header == null }
        if (headerless != null) {
            builder.setSingleList(itemList(headerless.items))
        } else {
            sections.forEach { section ->
                builder.addSectionedList(SectionedItemList.create(itemList(section.items), section.header ?: ""))
            }
        }
        return builder.build()
    }

    private fun title(codes: List<DiagnosticTroubleCode>): String {
        val title = stringProvider.getString(R.string.dtc_page_title)
        val count = codes.count { it.standardCode.isNotEmpty() }
        return if (count > 0) "$title ($count)" else title
    }

    private fun itemList(items: List<DtcListItem>): ItemList =
        ItemList
            .Builder()
            .apply {
                items.forEach { item ->
                    when (item) {
                        is DtcListItem.DtcRow -> addItem(row(item.dtc))
                        is DtcListItem.Message -> addItem(Row.Builder().setTitle(item.text).build())
                        is DtcListItem.ModuleHeader -> Unit
                    }
                }
            }.build()

    private fun row(dtc: DiagnosticTroubleCode): Row {
        val description = dtc.displayDescription()
        if (dtc.standardCode.isEmpty()) {
            return Row.Builder().setTitle(description).build()
        }

        val builder =
            Row
                .Builder()
                .setTitle(dtc.displayCode())
                .addText(description)

        dtc.activeStatuses
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?.let { builder.addText(it.joinToString(", ")) }

        return builder.build()
    }

    // ListTemplate allows two strip actions: connect while disconnected; read + clear once connected
    // (disconnect is on the main screen, one Back away).
    private fun actionStrip(hasCodes: Boolean): ActionStrip {
        val builder = ActionStrip.Builder()

        if (DataLoggerRepository.isRunning()) {
            builder.addAction(
                createAction(carContext, android.R.drawable.ic_popup_sync, CarColor.BLUE) {
                    runAction(DtcAction.READ)
                }
            )
            if (hasCodes) {
                builder.addAction(
                    createAction(carContext, android.R.drawable.ic_menu_delete, CarColor.RED) {
                        confirmClear()
                    }
                )
            }
        } else {
            builder.addAction(
                createAction(
                    carContext,
                    R.drawable.actions_connect,
                    mapColor(settings.getColorTheme().actionsBtnConnectColor)
                ) {
                    dataLoggerStart()
                }
            )
        }
        return builder.build()
    }

    private fun confirmClear() {
        screenManager.pushForResult(DtcClearConfirmationScreen(carContext, stringProvider)) { confirmed ->
            if (confirmed == true) {
                runAction(DtcAction.CLEAR)
            }
        }
    }

    private fun runAction(action: DtcAction) {
        if (!DataLoggerRepository.isRunning()) {
            toast.show(carContext, R.string.routine_workflow_is_not_running)
            invalidate()
            return
        }

        val modules = dtcScanModules(DiagnosticRequestIDManager.getMappings(), Prefs.getStringSet(PREF_DTC_DESELECTED_MODULES))
        Log.i(LOG_TAG, "Scheduling DTC $action, modules=$modules")
        actionInProgress = action
        invalidate()

        withDataLogger {
            when (action) {
                DtcAction.READ -> scheduleDTCRead(modules)
                DtcAction.CLEAR -> scheduleDTCCleanup(modules)
            }
        }
    }

    private fun diagnosticTroubleCodes(): List<DiagnosticTroubleCode> =
        VehicleCapabilitiesManager.getDiagnosticTroubleCodes().sortedForDisplay()

    init {
        lifecycle.addObserver(this)
    }
}

// Clearing erases the codes and their freeze frames for good, so it is never one tap away.
private class DtcClearConfirmationScreen(
    carContext: CarContext,
    private val stringProvider: LocalizedStringProvider
) : Screen(carContext) {
    override fun onGetTemplate(): Template =
        MessageTemplate
            .Builder(stringProvider.getString(R.string.dtc_clear_confirm_message))
            .setTitle(stringProvider.getString(R.string.dtc_clear_confirm_title))
            .setHeaderAction(Action.BACK)
            .addAction(
                Action
                    .Builder()
                    .setTitle(stringProvider.getString(R.string.dtc_clear_confirm))
                    .setBackgroundColor(CarColor.RED)
                    .setOnClickListener {
                        setResult(true)
                        finish()
                    }.build()
            ).addAction(
                Action
                    .Builder()
                    .setTitle(stringProvider.getString(R.string.dtc_clear_cancel))
                    .setOnClickListener { finish() }
                    .build()
            ).build()
}
