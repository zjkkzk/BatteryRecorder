package yangfentuozi.batteryrecorder.ui.mapper

import yangfentuozi.batteryrecorder.data.history.HistoryRecord
import yangfentuozi.batteryrecorder.data.history.HistorySummary
import yangfentuozi.batteryrecorder.data.model.ChartPoint
import yangfentuozi.batteryrecorder.shared.data.BatteryStatus

/**
 * 统一处理放电记录的展示符号映射。
 *
 * 约束：
 * 1. 数据层继续保留原始功率符号。
 * 2. 只有展示侧允许做“放电视为正值”的映射。
 * 3. Home/History 统一复用同一套映射规则，避免页面各自散落处理。
 */
internal object PowerDisplayMapper {

    /**
     * 按当前展示口径映射单条历史记录。
     *
     * @param record 原始历史记录。
     * @param dischargeDisplayPositive 是否将放电视为正值。
     * @return 返回映射后的展示记录；非放电记录保持原样。
     */
    fun mapHistoryRecord(
        record: HistoryRecord,
        dischargeDisplayPositive: Boolean
    ): HistoryRecord {
        if (record.type != BatteryStatus.Discharging) return record
        val multiplier = if (dischargeDisplayPositive) -1.0 else 1.0
        return record.copy(
            stats = record.stats.copy(averagePower = record.stats.averagePower * multiplier)
        )
    }

    /**
     * 按当前展示口径映射首页汇总统计。
     *
     * @param summary 原始首页汇总统计。
     * @param dischargeDisplayPositive 是否将放电视为正值。
     * @return 返回映射后的展示统计；非放电统计保持原样。
     */
    fun mapHistorySummary(
        summary: HistorySummary,
        dischargeDisplayPositive: Boolean
    ): HistorySummary {
        if (summary.type != BatteryStatus.Discharging) return summary
        val multiplier = if (dischargeDisplayPositive) -1.0 else 1.0
        return summary.copy(averagePower = summary.averagePower * multiplier)
    }

    /**
     * 按当前展示口径映射记录详情图表原始点。
     *
     * @param points 原始图表点。
     * @param batteryStatus 当前记录类型。
     * @param dischargeDisplayPositive 是否将放电视为正值。
     * @return 返回映射后的图表点；非放电记录保持原样。
     */
    fun mapChartPoints(
        points: List<ChartPoint>,
        batteryStatus: BatteryStatus,
        dischargeDisplayPositive: Boolean
    ): List<ChartPoint> {
        if (batteryStatus != BatteryStatus.Discharging) return points
        val multiplier = if (dischargeDisplayPositive) -1.0 else 1.0
        return points.map { point -> point.copy(power = point.power * multiplier) }
    }
}
