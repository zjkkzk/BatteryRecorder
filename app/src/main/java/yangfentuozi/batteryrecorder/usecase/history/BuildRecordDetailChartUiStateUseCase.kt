package yangfentuozi.batteryrecorder.usecase.history

import yangfentuozi.batteryrecorder.data.model.ChartPoint
import yangfentuozi.batteryrecorder.data.model.RecordDetailChartPoint
import yangfentuozi.batteryrecorder.data.model.normalizeRecordDetailChartPoints
import yangfentuozi.batteryrecorder.shared.data.BatteryStatus
import yangfentuozi.batteryrecorder.ui.mapper.PowerDisplayMapper
import yangfentuozi.batteryrecorder.ui.model.RecordDetailChartUiState
import yangfentuozi.batteryrecorder.utils.computePowerW
import kotlin.math.roundToLong

private const val TARGET_TREND_BUCKET_COUNT = 240L

/**
 * 构建记录详情页图表状态。
 */
internal object BuildRecordDetailChartUiStateUseCase {

    /**
     * 生成记录详情页图表状态。
     *
     * @param rawPoints 原始记录点。
     * @param batteryStatus 当前记录类型。
     * @param dischargeDisplayPositive 是否将放电视为正值。
     * @param dualCellEnabled 是否启用双电芯显示换算。
     * @param calibrationValue 当前校准值。
     * @param recordScreenOffEnabled 当前息屏显示开关。
     * @return 返回详情页图表状态。
     */
    fun execute(
        rawPoints: List<ChartPoint>,
        batteryStatus: BatteryStatus,
        dischargeDisplayPositive: Boolean,
        dualCellEnabled: Boolean,
        calibrationValue: Int,
        recordScreenOffEnabled: Boolean
    ): RecordDetailChartUiState {
        val displayRawPoints = PowerDisplayMapper.mapChartPoints(
            points = rawPoints,
            batteryStatus = batteryStatus,
            dischargeDisplayPositive = dischargeDisplayPositive
        )
        val displayPoints = mapDisplayPoints(
            rawPoints = displayRawPoints,
            dualCellEnabled = dualCellEnabled,
            calibrationValue = calibrationValue
        )
        val filteredDisplayPoints = normalizeRecordDetailChartPoints(
            points = displayPoints,
            recordScreenOffEnabled = recordScreenOffEnabled
        )
        val trendPoints = computeTrendPoints(filteredDisplayPoints)
        return computeViewportState(
            points = displayPoints,
            trendPoints = trendPoints
        )
    }

    /**
     * 将原始记录点换算为图表展示点。
     *
     * @param rawPoints 原始记录点。
     * @param dualCellEnabled 是否启用双电芯显示换算。
     * @param calibrationValue 当前校准值。
     * @return 返回图表展示点列表。
     */
    private fun mapDisplayPoints(
        rawPoints: List<ChartPoint>,
        dualCellEnabled: Boolean,
        calibrationValue: Int
    ): List<RecordDetailChartPoint> {
        return rawPoints.sortedBy { it.timestamp }.map { point ->
            val rawPowerW = computePowerW(
                rawPower = point.power,
                dualCellEnabled = dualCellEnabled,
                calibrationValue = calibrationValue
            )
            RecordDetailChartPoint(
                timestamp = point.timestamp,
                rawPowerW = rawPowerW,
                fittedPowerW = rawPowerW,
                packageName = point.packageName,
                capacity = point.capacity,
                isDisplayOn = point.isDisplayOn,
                temp = point.temp,
                voltage = point.voltage
            )
        }
    }

    /**
     * 计算趋势点序列。
     *
     * @param points 过滤后的展示点。
     * @return 返回趋势点序列。
     */
    private fun computeTrendPoints(points: List<RecordDetailChartPoint>): List<RecordDetailChartPoint> {
        if (points.isEmpty()) return emptyList()

        val firstTimestamp = points.first().timestamp
        val lastTimestamp = points.last().timestamp
        val bucketDurationMs = computeTrendBucketDurationMs(lastTimestamp - firstTimestamp)
        val bucketPoints = LinkedHashMap<Long, MutableList<RecordDetailChartPoint>>()
        for (point in points) {
            val bucketIndex = (point.timestamp - firstTimestamp) / bucketDurationMs
            bucketPoints.getOrPut(bucketIndex) { ArrayList() } += point
        }

        return bucketPoints.map { (bucketIndex, pointsInBucket) ->
            val bucketStart = firstTimestamp + bucketIndex * bucketDurationMs
            val bucketEnd = (bucketStart + bucketDurationMs).coerceAtMost(lastTimestamp)
            val bucketCenterTimestamp = bucketStart + (bucketEnd - bucketStart) / 2L
            val representativePoint = pointsInBucket.minBy { point ->
                kotlin.math.abs(point.timestamp - bucketCenterTimestamp)
            }
            val powerValues = pointsInBucket.map { it.rawPowerW }.sorted()
            representativePoint.copy(
                timestamp = bucketCenterTimestamp,
                fittedPowerW = medianOfSorted(powerValues)
            )
        }
    }

    /**
     * 计算详情图表的时间窗口状态。
     *
     * @param points 原始展示点。
     * @param trendPoints 趋势展示点。
     * @return 返回图表窗口状态。
     */
    private fun computeViewportState(
        points: List<RecordDetailChartPoint>,
        trendPoints: List<RecordDetailChartPoint>
    ): RecordDetailChartUiState {
        val minChartTime = points.minOfOrNull { it.timestamp }
        val maxChartTime = points.maxOfOrNull { it.timestamp }
        val totalDurationMs = if (minChartTime != null && maxChartTime != null) {
            (maxChartTime - minChartTime).coerceAtLeast(1L)
        } else {
            0L
        }
        val viewportDurationMs = if (totalDurationMs > 0L) {
            (totalDurationMs * 0.25).roundToLong().coerceAtLeast(1L)
        } else {
            0L
        }
        val maxViewportStart = if (minChartTime != null && maxChartTime != null) {
            (maxChartTime - viewportDurationMs).coerceAtLeast(minChartTime)
        } else {
            null
        }

        return RecordDetailChartUiState(
            points = points,
            trendPoints = trendPoints,
            minChartTime = minChartTime,
            maxChartTime = maxChartTime,
            maxViewportStartTime = maxViewportStart,
            viewportDurationMs = viewportDurationMs
        )
    }

    private fun computeTrendBucketDurationMs(totalDurationMs: Long): Long {
        val rawBucketDurationMs =
            (totalDurationMs / TARGET_TREND_BUCKET_COUNT).coerceAtLeast(1_000L)
        return normalizeTrendBucketDurationMs(rawBucketDurationMs)
    }

    private fun normalizeTrendBucketDurationMs(rawBucketDurationMs: Long): Long {
        val readableDurationsMs = buildReadableTrendDurationsMs(rawBucketDurationMs)
        return readableDurationsMs.minBy { duration -> kotlin.math.abs(duration - rawBucketDurationMs) }
    }

    private fun buildReadableTrendDurationsMs(rawBucketDurationMs: Long): List<Long> {
        val baseDurationsSeconds = listOf(1L, 2L, 3L, 5L, 10L, 15L, 20L, 30L, 60L)
        val maxDurationMs = rawBucketDurationMs * 10
        val candidates = LinkedHashSet<Long>()
        var scaleSeconds = 1L
        while (scaleSeconds * 1_000L <= maxDurationMs) {
            for (baseDurationSeconds in baseDurationsSeconds) {
                candidates += baseDurationSeconds * scaleSeconds * 1_000L
            }
            scaleSeconds *= 10L
        }
        return candidates.sorted()
    }

    private fun medianOfSorted(values: List<Double>): Double {
        val middleIndex = values.size / 2
        return if (values.size % 2 == 0) {
            (values[middleIndex - 1] + values[middleIndex]) / 2.0
        } else {
            values[middleIndex]
        }
    }
}
