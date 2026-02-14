package com.example.deviceinfo

import android.app.Activity
import android.os.Looper
import android.util.Log
import android.widget.Toast

class CrashLogger(private val currentActivity: Activity?) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val message = buildString {
            appendLine("⚠️ App crashed in thread: ${thread.name}")
            currentActivity?.let {
                appendLine("📍 Activity: ${it::class.java.simpleName}")
            }
            appendLine("🧩 Exception: ${throwable.javaClass.simpleName}")
            appendLine("💥 Message: ${throwable.message}")
            appendLine("📜 Stack trace:")
            appendLine(throwable.stackTraceToString())
        }

        Log.e("CrashLogger", message)

        // Show a quick toast so you can see the activity instantly before crash dialog
        try {
            Looper.prepare()
            Toast.makeText(currentActivity, "Crash in ${currentActivity?.localClassName}", Toast.LENGTH_LONG).show()
            Looper.loop()
        } catch (_: Exception) { }

        // Pass it to system handler
        defaultHandler?.uncaughtException(thread, throwable)
    }

    companion object {
        fun init(activity: Activity) {
            Thread.setDefaultUncaughtExceptionHandler(CrashLogger(activity))
        }
    }
}
