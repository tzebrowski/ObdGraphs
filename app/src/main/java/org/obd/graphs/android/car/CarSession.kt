package org.obd.graphs.android.car

import android.content.Context
import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver


class CarSession : Session(), DefaultLifecycleObserver {

    companion object {
        const val SHARED_PREF_KEY = "ShowcasePrefs"
        const val PRE_SEED_KEY = "PreSeed"
    }

    override fun onCreateScreen(intent: Intent): Screen {
        val lifecycle = lifecycle
        lifecycle.addObserver(this)
        if (carContext.callingComponent != null) {
            carContext
                .getCarService(ScreenManager::class.java)
                .push(CarScreen(carContext))
            return ResultScreen(carContext)
        }
        val shouldPreSeedBackStack = carContext
            .getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE)
            .getBoolean(PRE_SEED_KEY, false)
        if (shouldPreSeedBackStack) {
            carContext
                .getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(PRE_SEED_KEY, false)
                .apply()
            carContext
                .getCarService(ScreenManager::class.java)
                .push(CarScreen(carContext))
            return RequestPermissionScreen(carContext, true)
        }
        return CarScreen(carContext)
    }

    override fun onNewIntent(intent: Intent) {
        val screenManager = carContext.getCarService(
            ScreenManager::class.java
        )
        if (carContext.callingComponent != null) {
            screenManager.popToRoot()
            screenManager.push(
                ResultScreen(
                    carContext
                )
            )
            return
        }
    }
}
