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
package org.obd.graphs.aa.screen.nav

import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.core.graphics.drawable.IconCompat
import org.obd.graphs.aa.R
import org.obd.graphs.aa.screen.createAction
import org.obd.graphs.bl.datalogger.*
import org.obd.graphs.renderer.api.Identity

private const val LOG_TAG = "AvailableFeaturesScreen"

data class FeatureDescription(val identity: Identity, val iconId: Int, val title: String)

internal class AvailableFeaturesScreen(
    carContext: CarContext,
    private val screenMapping:  List<FeatureDescription>
) : Screen(carContext) {

    override fun onGetTemplate(): Template  = try {
        if (DataLoggerRepository.status() == WorkflowStatus.Connecting) {
             ListTemplate.Builder()
                .setHeaderAction(Action.BACK)
                .setActionStrip(getHorizontalActionStrip())
                .setLoading(true)
                .setTitle(carContext.getString(R.string.available_features_page_title))
                .build()
        } else {
            listTemplate()
        }
    } catch (e: Exception) {
        Log.e(LOG_TAG, "Failed to build template", e)
        PaneTemplate.Builder(Pane.Builder().setLoading(true).build())
            .setHeaderAction(Action.BACK)
            .setTitle(carContext.getString(R.string.pref_aa_car_error))
            .build()
    }
    private fun listTemplate(): ListTemplate {
        val items = ItemList.Builder().apply {
            screenMapping.forEach {
                addItem(row(it.identity, it.iconId, it.title))
            }
        }.build()

        return ListTemplate.Builder()
            .setHeaderAction(Action.BACK)
            .setActionStrip(getHorizontalActionStrip())
            .setLoading(false)
            .setTitle(carContext.getString(R.string.available_features_page_title))
            .setSingleList(items)
            .build()
    }

    private fun row(identity: Identity, iconId: Int, title: String) = Row.Builder()
        .setImage(
            CarIcon.Builder(
                IconCompat.createWithResource(
                    carContext, iconId)
            ).build()
        )
        .setOnClickListener {
            setResult(identity)
            finish()
        }
        .setBrowsable(false)
        .setTitle(title)
        .build()

    private fun getHorizontalActionStrip(): ActionStrip {
        var builder = ActionStrip.Builder()
        builder = builder.addAction(createAction(carContext, R.drawable.action_exit, CarColor.RED) {
            Log.i(LOG_TAG, "Exiting the app. Closing the context")
            carContext.finishCarApp()
        })
        return builder.build()
    }
}
