package com.lykke.matching.engine.utils.monitoring

import com.lykke.matching.engine.daos.monitoring.MonitoringResult
import com.sun.management.OperatingSystemMXBean
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.lang.management.ManagementFactory

@Component
class MonitoringStatsCollector {
    private val MB = 1024 * 1024

    @Autowired
    @Qualifier("appStarterLogger")
    private lateinit var LOGGER: Logger

    fun collectMonitoringResult(): MonitoringResult? {
        try {
            val osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
            val vmCpuLoad = osBean.processCpuLoad
            val totalCpuLoad = osBean.systemCpuLoad
            val totalMemory = osBean.totalPhysicalMemorySize / MB
            val freeMemory = osBean.freePhysicalMemorySize / MB
            val maxHeap = Runtime.getRuntime().maxMemory() / MB
            val freeHeap = Runtime.getRuntime().freeMemory() / MB
            val totalHeap = Runtime.getRuntime().totalMemory() / MB
            val totalSwap = osBean.totalSwapSpaceSize / MB
            val freeSwap = osBean.freeSwapSpaceSize / MB

            val threadsCount = Thread.getAllStackTraces().keys.size

            return MonitoringResult(
                    vmCpuLoad = vmCpuLoad,
                    totalCpuLoad = totalCpuLoad,
                    totalMemory = totalMemory,
                    freeMemory = freeMemory,
                    maxHeap = maxHeap,
                    freeHeap = freeHeap,
                    totalHeap = totalHeap,
                    totalSwap = totalSwap,
                    freeSwap = freeSwap,
                    threadsCount = threadsCount
            )
        } catch (e: Exception) {
            LOGGER.error("Unable to gather monitoring stats: ${e.message}")
        }
        return null
    }
}