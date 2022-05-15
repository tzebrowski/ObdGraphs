package org.openobd2.core.logger

import android.app.Activity
import androidx.constraintlayout.solver.Cache
import java.lang.ref.WeakReference

lateinit var Cache: MutableMap<String, Any>
lateinit var ApplicationContext: WeakReference<Activity>

