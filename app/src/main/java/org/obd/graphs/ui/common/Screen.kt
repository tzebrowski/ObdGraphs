package org.obd.graphs.ui.common

import android.content.Context
import android.content.res.Configuration
import org.obd.graphs.ApplicationContext

fun isTablet(): Boolean {
    val context: Context = ApplicationContext.get()!!
    val xlarge =
        context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == 4
    val large =
        context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK ==
                Configuration.SCREENLAYOUT_SIZE_LARGE
    return xlarge || large
}