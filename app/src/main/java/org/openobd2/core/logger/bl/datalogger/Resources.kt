package org.openobd2.core.logger.bl.datalogger

import android.content.Context
import android.util.Log
import org.openobd2.core.logger.ui.preferences.Prefs
import java.io.File

private const val STORAGE_FILE_CODING_KEY = "storage:"

internal fun externalResourceToURL(it: String) =
    File(it.substring(STORAGE_FILE_CODING_KEY.length, it.length)).toURI().toURL()

internal fun isExternalStorageResource(it: String) = it.startsWith(STORAGE_FILE_CODING_KEY)

fun getExternalPidResources(context: Context?): MutableMap<String, String>? {

    if (Prefs.getBoolean("pref.pids.registry.access_external_storage", false)) {
        val directory = "${context?.getExternalFilesDir("pid")?.absolutePath}"
        Log.d(
            "PidResourceListPreferences",
            "Reading directory $directory for available extra PID resource files. " +
                    "\nFound number of files: ${File(directory).listFiles()?.size}"
        )
        val files = mutableMapOf<String,String>()
        File(directory).listFiles()?.forEach {

            Log.d(
                "PidResourceListPreferences", "Found file: ${it.absolutePath}." +
                        "\n Adding to the path."
            )
            files["${STORAGE_FILE_CODING_KEY}${it.absolutePath}"] = it.absolutePath
        }
        return files
    }
    return null
}