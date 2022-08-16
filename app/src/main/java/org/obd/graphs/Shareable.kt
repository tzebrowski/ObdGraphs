package org.obd.graphs

import android.app.Activity
import androidx.car.app.CarContext
import java.lang.ref.WeakReference

lateinit var Cache: MutableMap<String, Any>
lateinit var ApplicationContext: WeakReference<Activity>
lateinit var CarApplicationContext: WeakReference<CarContext>


