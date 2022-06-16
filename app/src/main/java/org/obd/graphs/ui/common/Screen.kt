package org.obd.graphs.ui.common

import android.content.Context
import android.content.res.Configuration

fun isTablet(context: Context): Boolean {
    val xlarge =
        context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == 4
    val large =
        context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK ==
                Configuration.SCREENLAYOUT_SIZE_LARGE
    return xlarge || large
}