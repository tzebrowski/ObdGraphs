/**
 * Copyright 2019-2023, Tomasz Żebrowski
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package org.obd.graphs.aa.screen.nav

import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.core.graphics.drawable.IconCompat
import org.obd.graphs.aa.CarSettings
import org.obd.graphs.aa.R
import org.obd.graphs.aa.screen.createAction
import org.obd.graphs.bl.datalogger.*

const val GIULIA_SCREEN_ID = 0
const val DRAG_RACING_SCREEN_ID = 1
const val ROUTINES_SCREEN_ID = 2

internal class AvailableFeaturesScreen(
    carContext: CarContext,
    private val carSettings: CarSettings
) : Screen(carContext) {

    override fun onGetTemplate(): Template  = try {
        if (dataLogger.status() == WorkflowStatus.Connecting) {
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
        Log.e(org.obd.graphs.aa.screen.LOG_KEY, "Failed to build template", e)
        PaneTemplate.Builder(Pane.Builder().setLoading(true).build())
            .setHeaderAction(Action.BACK)
            .setTitle(carContext.getString(R.string.pref_aa_car_error))
            .build()
    }
    private fun listTemplate(): ListTemplate {
        val items = ItemList.Builder().apply {


            addItem(
                row(
                    DRAG_RACING_SCREEN_ID, R.drawable.action_drag_race_screen,
                    carContext.getString(R.string.available_features_drag_race_screen_title)
                )
            )

            addItem(
                row(
                    GIULIA_SCREEN_ID, R.drawable.action_giulia,
                    carContext.getString(R.string.available_features_giulia_screen_title)
                )
            )

            if (carSettings.isRoutinesEnabled()){
                 addItem(
                    row(
                        ROUTINES_SCREEN_ID,
                        R.drawable.action_features,
                        carContext.getString(R.string.available_features_routine_screen_title)
                    )
                )
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

    private fun row(screenId: Int, iconId: Int, title: String) = Row.Builder()
        .setImage(
            CarIcon.Builder(
                IconCompat.createWithResource(
                    carContext, iconId)
            ).build()
        )
        .setOnClickListener {
            setResult(screenId)
            finish()
        }
        .setBrowsable(false)
        .setTitle(title)
        .build()

    private fun getHorizontalActionStrip(): ActionStrip {
        var builder = ActionStrip.Builder()
        builder = builder.addAction(createAction(carContext, R.drawable.action_exit, CarColor.RED) {
            Log.i(org.obd.graphs.aa.screen.LOG_KEY, "Exiting the app. Closing the context")
            carContext.finishCarApp()
        })
        return builder.build()
    }
}