package yangfentuozi.batteryrecorder.data.history

import yangfentuozi.batteryrecorder.shared.data.BatteryStatus
import yangfentuozi.batteryrecorder.shared.data.LineRecord
import yangfentuozi.batteryrecorder.shared.util.LoggerX

private const val TAG = "RecordDetailPowerStats"
private const val MICROAMPERE_HOUR_DIVISOR = 3_600_000_000.0

/**
 * 记录详情页电量变化拆分结果。
 *
 * @param totalPercent 当前记录的总电量变化百分比。
 * @param screenOffPercent 息屏区间累计的电量变化百分比。
 * @param screenOnPercent 亮屏区间累计的电量变化百分比。
 */
data class CapacityChange(
    val totalPercent: Int,
    val screenOffPercent: Int,
    val screenOnPercent: Int
)

/**
 * 详情页功耗统计结果。
 *
 * mAh 字段都表示“校准值为 1 时”的基准积分值，其中 `netMahBase` 保留原始正负号。
 */
data class RecordDetailPowerStats(
    val averagePowerRaw: Double,
    val screenOnAveragePowerRaw: Double?,
    val screenOffAveragePowerRaw: Double?,
    val netMahBase: Double,
    val screenOnMahBase: Double,
    val screenOffMahBase: Double,
    val capacityChange: CapacityChange
)

object RecordDetailPowerStatsComputer {

    /**
     * 按记录文件的真实采样区间计算详情页功耗统计。
     *
     * @param detailType 当前详情页记录类型，只接受充电和放电。
     * @param records 已通过解析得到的有效记录点列表，要求时间戳按文件原始顺序传入
     * @return 返回总平均、亮屏平均、息屏平均三项原始功率，以及“校准值为 1 时”的净 mAh 变化量、亮屏/息屏耗电量和电量变化拆分；若有效区间不足则返回 null
     */
    fun compute(
        detailType: BatteryStatus,
        records: List<LineRecord>
    ): RecordDetailPowerStats? {
        if (records.size < 2) return null

        var totalDurationMs = 0L
        var totalEnergyRawMs = 0.0
        var netMahBase = 0.0
        var screenOnDurationMs = 0L
        var screenOnEnergyRawMs = 0.0
        var screenOnMahBase = 0.0
        var screenOnCapacityDropPercent = 0
        var screenOffDurationMs = 0L
        var screenOffEnergyRawMs = 0.0
        var screenOffMahBase = 0.0
        var screenOffCapacityDropPercent = 0

        var previous: LineRecord? = null
        records.forEach { current ->
            val previousRecord = previous
            previous = current
            if (previousRecord == null) return@forEach

            val durationMs = current.timestamp - previousRecord.timestamp
            if (durationMs <= 0L) return@forEach

            val energyRawMs =
                (previousRecord.power.toDouble() + current.power.toDouble()) * 0.5 * durationMs
            val consumedMahBase = computeConsumedMahBase(
                previousCurrent = previousRecord.current,
                currentCurrent = current.current,
                durationMs = durationMs
            )
            val transferredMahBaseSigned = computeTransferredMahBaseSigned(
                previousCurrent = previousRecord.current,
                currentCurrent = current.current,
                durationMs = durationMs
            )
            val capacityDelta = computeCapacityDelta(
                detailType = detailType,
                previousCapacity = previousRecord.capacity,
                currentCapacity = current.capacity
            )
            totalDurationMs += durationMs
            totalEnergyRawMs += energyRawMs
            netMahBase += transferredMahBaseSigned

            if (previousRecord.isDisplayOn == 1) {
                screenOnDurationMs += durationMs
                screenOnEnergyRawMs += energyRawMs
                screenOnMahBase += consumedMahBase
                screenOnCapacityDropPercent += capacityDelta
                return@forEach
            }

            screenOffDurationMs += durationMs
            screenOffEnergyRawMs += energyRawMs
            screenOffMahBase += consumedMahBase
            screenOffCapacityDropPercent += capacityDelta
        }

        if (totalDurationMs <= 0L) return null

        val capacityChange = CapacityChange(
            totalPercent = screenOffCapacityDropPercent + screenOnCapacityDropPercent,
            screenOffPercent = screenOffCapacityDropPercent,
            screenOnPercent = screenOnCapacityDropPercent
        )
        val stats = RecordDetailPowerStats(
            averagePowerRaw = totalEnergyRawMs / totalDurationMs.toDouble(),
            screenOnAveragePowerRaw = screenOnDurationMs.takeIf { it > 0L }?.let {
                screenOnEnergyRawMs / it.toDouble()
            },
            screenOffAveragePowerRaw = screenOffDurationMs.takeIf { it > 0L }?.let {
                screenOffEnergyRawMs / it.toDouble()
            },
            netMahBase = netMahBase,
            screenOnMahBase = screenOnMahBase,
            screenOffMahBase = screenOffMahBase,
            capacityChange = capacityChange
        )
        LoggerX.d(
            TAG,
            "[记录详情] 统计完成: netMahBase=${stats.netMahBase} screenOnMahBase=${stats.screenOnMahBase} screenOffMahBase=${stats.screenOffMahBase} totalCapacity=${stats.capacityChange.totalPercent} screenOnCapacity=${stats.capacityChange.screenOnPercent} screenOffCapacity=${stats.capacityChange.screenOffPercent}"
        )
        return stats
    }

    /**
     * 按记录类型计算当前区间的电量变化百分比。
     *
     * @param detailType 当前详情页记录类型，只接受充电和放电。
     * @param previousCapacity 区间起点电量百分比。
     * @param currentCapacity 区间终点电量百分比。
     * @return 返回当前区间在正确语义下的正向电量变化值；方向不一致时返回 0。
     */
    private fun computeCapacityDelta(
        detailType: BatteryStatus,
        previousCapacity: Int,
        currentCapacity: Int
    ): Int {
        val rawDelta = when (detailType) {
            BatteryStatus.Discharging -> previousCapacity - currentCapacity
            BatteryStatus.Charging -> currentCapacity - previousCapacity
            else -> throw IllegalArgumentException("Unsupported detail type: $detailType")
        }
        return rawDelta.coerceAtLeast(0)
    }

    /**
     * 按相邻两点的真实采样区间计算“校准值为 1 时”的基础 mAh 消耗量。
     *
     * 这里不应用双电芯倍率：
     * - 统计器只负责产出单路基础值
     * - 展示层再根据 `dualCellEnabled` 统一决定是否乘 2
     * - 展示层再根据 `calibrationValue` 统一决定是否放大 / 缩小 / 反转方向
     *
     * @param previousCurrent 区间起点电流
     * @param currentCurrent 区间终点电流
     * @param durationMs 区间时长，单位毫秒
     * @return 返回该区间对应的基础 mAh 消耗量
     */
    private fun computeConsumedMahBase(
        previousCurrent: Long,
        currentCurrent: Long,
        durationMs: Long
    ): Double {
        val averageAbsCurrent = (absCurrent(previousCurrent) + absCurrent(currentCurrent)) * 0.5
        return averageAbsCurrent * durationMs / MICROAMPERE_HOUR_DIVISOR
    }

    /**
     * 按相邻两点的真实采样区间计算“校准值为 1 时”的带符号基础 mAh 变化量。
     *
     * 这里保留电流原始符号：
     * - 正值表示该区间净流入电池
     * - 负值表示该区间净流出电池
     *
     * 真实展示值仍由外层统一乘上 `calibrationValue`；
     * 这样切换校准配置时无需重新解析整条记录文件。
     *
     * @param previousCurrent 区间起点电流
     * @param currentCurrent 区间终点电流
     * @param durationMs 区间时长，单位毫秒
     * @return 返回该区间对应的带符号基础 mAh 变化量
     */
    private fun computeTransferredMahBaseSigned(
        previousCurrent: Long,
        currentCurrent: Long,
        durationMs: Long
    ): Double {
        val averageCurrent = (previousCurrent.toDouble() + currentCurrent.toDouble()) * 0.5
        return averageCurrent * durationMs / MICROAMPERE_HOUR_DIVISOR
    }

    /**
     * 返回 Long 电流值的安全绝对值。
     *
     * @param current 原始电流值
     * @return 返回绝对值；遇到 Long.MIN_VALUE 时退回 Long.MAX_VALUE，避免溢出
     */
    private fun absCurrent(current: Long): Long {
        if (current == Long.MIN_VALUE) return Long.MAX_VALUE
        return kotlin.math.abs(current)
    }
}
