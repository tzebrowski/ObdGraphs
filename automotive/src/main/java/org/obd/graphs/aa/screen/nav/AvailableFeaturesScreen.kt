package org.obd.graphs.aa.screen.nav

import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.core.graphics.drawable.IconCompat
import org.obd.graphs.aa.R
import org.obd.graphs.aa.createAction
import org.obd.graphs.bl.datalogger.*


open class AvailableFeaturesScreen(
    carContext: CarContext
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
            addItem(row(ROUTINES_SCREEN_ID,
                R.drawable.action_features,
                carContext.getString(R.string.available_features_routine_screen_title))
            )

            addItem(row(DRAG_RACING_SCREEN_ID,  R.drawable.action_drag_race_screen,
                carContext.getString(R.string.available_features_drag_race_screen_title))
            )
            addItem(row(GIULIA_SCREEN_ID,  R.drawable.action_giulia,
                carContext.getString(R.string.available_features_giulia_screen_title))
            )
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