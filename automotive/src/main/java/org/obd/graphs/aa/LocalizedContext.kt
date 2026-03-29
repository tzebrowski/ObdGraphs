package org.obd.graphs.aa

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.car.app.CarContext
import org.obd.graphs.language.LanguageManager
import java.util.Locale
import java.util.WeakHashMap
import kotlin.collections.set

private val contextCache = WeakHashMap<CarContext, Pair<String, Context>>()

fun CarContext.getLocString(@StringRes resId: Int, vararg formatArgs: Any): String {
    val myLocaleCode = LanguageManager.getStoredLanguage(this)

    var cachedData = contextCache[this]

    if (cachedData == null || cachedData.first != myLocaleCode) {
        val config = Configuration(this.resources.configuration)
        config.setLocale(Locale(myLocaleCode))
        val newLocalizedContext = this.createConfigurationContext(config)

        cachedData = Pair(myLocaleCode, newLocalizedContext)
        contextCache[this] = cachedData
    }

    val localizedContext = cachedData.second

    return if (formatArgs.isEmpty()) {
        localizedContext.getString(resId)
    } else {
        localizedContext.getString(resId, *formatArgs)
    }
}
