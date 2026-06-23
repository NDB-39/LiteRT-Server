package com.example.metrics

import android.app.ActivityManager
import android.content.Context

object MetricsManager {
    var tokensPerSecond: Float = 0f
    var backend: String = "CPU" // NPU, GPU, CPU
    var currentModelName: String = ""

    fun getRamUsedMb(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        // getProcessMemoryInfo or returning available RAM.
        // For simplicity, returning the process PSS if possible, or overall used
        val memInfo = am.getProcessMemoryInfo(intArrayOf(android.os.Process.myPid()))
        return if (memInfo.isNotEmpty()) {
            (memInfo[0].totalPss / 1024L).toLong()
        } else {
            0L
        }
    }
}
