package celestemeown.app.android

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogSystem {
    private var sessionFile: File? = null
    private var writer: PrintWriter? = null
    private const val TAG = "LogSystem"

    fun init(context: Context) {
        try {
            val base = File(context.getExternalFilesDir(null), "Celeste/Logs")
            if (!base.exists()) base.mkdirs()

            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val dir = File(base, date)
            if (!dir.exists()) dir.mkdirs()

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            sessionFile = File(dir, "session_$timestamp.txt")
            writer = PrintWriter(FileWriter(sessionFile, true))

            log("=== LogSystem started: $timestamp ===")

            // Install default uncaught exception handler to capture crashes
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                log("UNCAUGHT EXCEPTION in thread ${thread.name}: ${throwable.message}")
                logException(throwable)
                writer?.flush()
                defaultHandler?.uncaughtException(thread, throwable)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init LogSystem: ${e.message}")
        }
    }

    @Synchronized
    fun log(message: String) {
        try {
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            writer?.println("[$ts] $message")
            writer?.flush()
            Log.i(TAG, message)
        } catch (e: Exception) {
            Log.e(TAG, "Log write error: ${e.message}")
        }
    }

    @Synchronized
    fun logException(t: Throwable) {
        try {
            writer?.println("--- EXCEPTION START ---")
            t.printStackTrace(writer)
            writer?.println("--- EXCEPTION END ---")
            writer?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Log exception error: ${e.message}")
        }
    }

    fun getSessionFilePath(): String? = sessionFile?.absolutePath
}
