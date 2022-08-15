package org.obd.graphs.android.car

import android.app.Activity
import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import org.obd.graphs.R


class ResultScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val callingComponent = carContext.callingComponent
            ?: return MessageTemplate.Builder(
                carContext.getString(R.string.not_started_for_result_msg)
            )
                .setTitle(carContext.getString(R.string.result_demo_title))
                .setHeaderAction(Action.BACK)
                .build()
        return MessageTemplate.Builder(
            carContext.getString(
                R.string.started_for_result_msg,
                callingComponent.packageName
            )
        )
            .setTitle(carContext.getString(R.string.result_demo_title))
            .setHeaderAction(Action.BACK)
            .addAction(
                Action.Builder()
                    .setTitle("Okay (action = 'foo')")
                    .setOnClickListener {
                        carContext.setCarAppResult(
                            Activity.RESULT_OK,
                            Intent("foo")
                        )
                        carContext.finishCarApp()
                    }
                    .build())
            .addAction(
                Action.Builder()
                    .setTitle(carContext.getString(R.string.cancel_action_title))
                    .setOnClickListener {
                        carContext.setCarAppResult(Activity.RESULT_CANCELED, null)
                        carContext.finishCarApp()
                    }
                    .build())
            .build()
    }
}
