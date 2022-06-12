package org.openobd2.core.logger.bl.datalogger

import android.content.Context
import android.util.Log
import org.openobd2.core.logger.ui.preferences.Prefs
import java.io.File

private const val STORAGE_FILE_CODING_KEY = "storage:"
private const val LOG_TAG = "PidResourceListPreferences"
const val ACCESS_EXTERNAL_STORAGE_ENABLED = "pref.pids.registry.access_external_storage"

internal fun externalResourceToURL(it: String) =
    File(it.substring(STORAGE_FILE_CODING_KEY.length, it.length)).toURI().toURL()

internal fun isExternalStorageResource(it: String) = it.startsWith(STORAGE_FILE_CODING_KEY)

fun getExternalPidResources(context: Context?): MutableMap<String, String>? {
    return getExternalPidResources(context) {
        Prefs.getBoolean(
            ACCESS_EXTERNAL_STORAGE_ENABLED,
            false
        )
    }
}

fun getExternalPidResources(
    context: Context?,
    isFeatureEnabled: () -> Boolean
): MutableMap<String, String>? {
    if (isFeatureEnabled()) {
        getExternalPidDirectory(context)?.let { directory ->
            val files = File(directory).listFiles()
            Log.d(
                LOG_TAG,
                "Reading directory $directory for available extra PID resource files. " +
                        "\nFound number of files: ${files?.size}"
            )

            return files?.associate {
                Log.d(
                    LOG_TAG, "Found file: ${it.absolutePath}." +
                            "\n Adding to the path."
                )
                "${STORAGE_FILE_CODING_KEY}${it.absolutePath}" to it.absolutePath
            }?.toMutableMap()

        }
    }
    return null
}

private fun getExternalPidDirectory(context: Context?): String? =
    context?.getExternalFilesDir("pid")?.absolutePath