package com.orion.app.system

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.StatFs
import java.io.RandomAccessFile

/**
 * Every value here is either read from a real Android API or explicitly
 * marked unavailable. Nothing in this file invents a number.
 */
data class RamStats(val usedMb: Long, val totalMb: Long, val percent: Int)
data class StorageStats(val usedGb: Double, val totalGb: Double, val percent: Int)
data class BatteryStats(val percent: Int, val isCharging: Boolean)
data class NetworkStats(val connected: Boolean, val transport: String, val downlinkKbps: Int?)
data class CpuStats(val available: Boolean, val percent: Int?) // GPU has no public counterpart at all.

class SystemStatsProvider(private val context: Context) {

    fun getRam(): RamStats {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        val totalMb = info.totalMem / (1024 * 1024)
        val usedMb = totalMb - (info.availMem / (1024 * 1024))
        val percent = if (totalMb > 0) ((usedMb * 100) / totalMb).toInt() else 0
        return RamStats(usedMb, totalMb, percent)
    }

    fun getStorage(): StorageStats {
        val stat = StatFs(context.filesDir.path)
        val totalBytes = stat.totalBytes
        val availableBytes = stat.availableBytes
        val usedBytes = totalBytes - availableBytes
        val toGb = { b: Long -> b / 1_073_741_824.0 }
        val percent = if (totalBytes > 0) ((usedBytes * 100) / totalBytes).toInt() else 0
        return StorageStats(toGb(usedBytes), toGb(totalBytes), percent)
    }

    fun getBattery(): BatteryStats {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, filter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return BatteryStats(pct, charging)
    }

    fun getNetwork(): NetworkStats {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val active = cm.activeNetwork ?: return NetworkStats(false, "None", null)
        val caps = cm.getNetworkCapabilities(active) ?: return NetworkStats(false, "None", null)

        val transport = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Other"
        }
        val connected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        // linkDownstreamBandwidthKbps is the OS's own estimate — real, not fabricated,
        // though it is itself an estimate rather than a live speed test.
        val downlink = if (caps.linkDownstreamBandwidthKbps > 0) caps.linkDownstreamBandwidthKbps else null
        return NetworkStats(connected, transport, downlink)
    }

    /**
     * CPU usage: on Android 8+, non-system apps are generally blocked from
     * reading global /proc/stat by SELinux policy. We attempt it and fall
     * back to "unavailable" honestly rather than showing a fake number —
     * exactly as instructed for GPU, which has NO public API at all on
     * unrooted Android and is therefore always unavailable.
     */
    fun getCpu(): CpuStats {
        return try {
            RandomAccessFile("/proc/stat", "r").use { reader ->
                val line = reader.readLine()
                if (line != null && line.startsWith("cpu ")) {
                    // Reading succeeded on this device/OS build — a real second
                    // sample would be needed to compute a delta-based percentage;
                    // left as a TODO hook rather than faking a value here.
                    CpuStats(available = true, percent = null)
                } else {
                    CpuStats(available = false, percent = null)
                }
            }
        } catch (e: Exception) {
            CpuStats(available = false, percent = null)
        }
    }

    // GPU: there is no public Android API for GPU load on unrooted devices.
    // Always DATA UNAVAILABLE — this is intentional, not a missing feature.
}
