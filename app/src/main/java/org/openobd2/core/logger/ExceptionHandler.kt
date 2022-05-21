package org.openobd2.core.logger

import android.content.Context
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class ExceptionHandler : Thread.UncaughtExceptionHandler {
    private val delegate: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    private val dateFormat: SimpleDateFormat =
        SimpleDateFormat("MM.dd HH:mm:ss", Locale.getDefault())

    override fun uncaughtException(t: Thread, e: Throwable) {
        val report = buildReport(e)
        writeReport(report)
        delegate?.uncaughtException(t, e)
    }

    private fun buildReport(e: Throwable): String {
        var arr = e.stackTrace
        var report = """
                $e
                """.trimIndent()
        report += "--------- Stack trace ---------\n\n"
        for (i in arr.indices) {
            report += """${arr[i]}"""
        }
        report += "-------------------------------\n\n"
        report += "--------- Cause ---------\n\n"
        val cause = e.cause
        if (cause != null) {
            report += """
                    $cause
                    """.trimIndent()
            arr = cause.stackTrace
            for (i in arr.indices) {
                report += """${arr[i]}"""
            }
        }
        report += "-------------------------------\n\n"
        return report
    }

    private fun writeReport(report: String) {
        ApplicationContext.get()?.let {
            try {
                val date = dateFormat.format(Date(System.currentTimeMillis()))
                val trace: FileOutputStream = it.openFileOutput(
                    "$date-stack_trace.txt",
                    Context.MODE_PRIVATE
                )
                trace.write(report.toByteArray())
                trace.close()
            } catch (ioe: IOException) {
                // ...
            }
        }
    }
}