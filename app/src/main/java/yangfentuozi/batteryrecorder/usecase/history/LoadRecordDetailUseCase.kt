package yangfentuozi.batteryrecorder.usecase.history

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import yangfentuozi.batteryrecorder.data.history.HistoryRecord
import yangfentuozi.batteryrecorder.data.history.HistoryRepository
import yangfentuozi.batteryrecorder.data.history.HistoryRepository.toFile
import yangfentuozi.batteryrecorder.data.history.RecordAppStatsComputer
import yangfentuozi.batteryrecorder.data.history.RecordDetailPowerStats
import yangfentuozi.batteryrecorder.data.history.RecordDetailPowerStatsComputer
import yangfentuozi.batteryrecorder.data.model.ChartPoint
import yangfentuozi.batteryrecorder.shared.data.BatteryStatus
import yangfentuozi.batteryrecorder.shared.data.LineRecord
import yangfentuozi.batteryrecorder.shared.data.RecordsFile
import yangfentuozi.batteryrecorder.ui.model.RecordAppDetailUiEntry
import yangfentuozi.batteryrecorder.usecase.common.ResolveInstalledAppLabelUseCase

/**
 * 记录详情页原始加载结果。
 */
internal data class RecordDetailLoadResult(
    val detail: HistoryRecord,
    val lineRecords: List<LineRecord>,
    val rawChartPoints: List<ChartPoint>,
    val powerStats: RecordDetailPowerStats?,
    val appEntries: List<RecordAppDetailUiEntry>,
    val appSwitchCount: Int,
    val referenceVoltageV: Double?
)

/**
 * 加载记录详情页原始数据。
 */
internal object LoadRecordDetailUseCase {

    /**
     * 读取并组装记录详情页所需的原始数据。
     *
     * @param context 应用上下文。
     * @param recordsFile 当前记录文件。
     * @param recordIntervalMs 当前采样间隔配置。
     * @return 成功时返回详情原始数据；记录文件缺失时返回空值。
     */
    suspend fun execute(
        context: Context,
        recordsFile: RecordsFile,
        recordIntervalMs: Long
    ): RecordDetailLoadResult? = withContext(Dispatchers.IO) {
        val recordFile = recordsFile.toFile(context) ?: return@withContext null
        val lineRecords = HistoryRepository.loadLineRecords(recordFile)
        val detail = HistoryRepository.loadRecord(context, recordFile)
        val powerStats = buildRecordDetailPowerStats(
            detailType = recordsFile.type,
            lineRecords = lineRecords,
            recordIntervalMs = recordIntervalMs
        )
        RecordDetailLoadResult(
            detail = detail,
            lineRecords = lineRecords,
            rawChartPoints = lineRecords.toChartPoints(),
            powerStats = powerStats,
            appEntries = buildRecordAppDetailEntries(
                context = context.applicationContext,
                detailType = recordsFile.type,
                lineRecords = lineRecords,
                recordIntervalMs = recordIntervalMs
            ),
            appSwitchCount = buildRecordDetailAppSwitchCount(lineRecords),
            referenceVoltageV = lineRecords
                .lastOrNull { lineRecord -> lineRecord.voltage > 0L }
                ?.voltage
                ?.toDouble()
                ?.div(1_000_000.0)
        )
    }

    /**
     * 根据详情类型计算顶部功耗统计。
     *
     * @param detailType 当前详情类型。
     * @param lineRecords 当前详情记录点列表。
     * @param recordIntervalMs 当前采样间隔配置。
     * @return 返回详情页功耗统计；非充放电记录返回空值。
     */
    fun buildRecordDetailPowerStats(
        detailType: BatteryStatus,
        lineRecords: List<LineRecord>,
        recordIntervalMs: Long
    ): RecordDetailPowerStats? {
        if (
            detailType != BatteryStatus.Discharging &&
            detailType != BatteryStatus.Charging
        ) {
            return null
        }
        return RecordDetailPowerStatsComputer.compute(
            detailType = detailType,
            recordIntervalMs = recordIntervalMs,
            records = lineRecords
        )
    }

    /**
     * 构建详情页应用明细。
     *
     * @param context 应用上下文。
     * @param detailType 当前详情类型。
     * @param lineRecords 当前详情记录点列表。
     * @param recordIntervalMs 当前采样间隔配置。
     * @return 返回详情页应用明细；非放电记录返回空列表。
     */
    fun buildRecordAppDetailEntries(
        context: Context,
        detailType: BatteryStatus,
        lineRecords: List<LineRecord>,
        recordIntervalMs: Long
    ): List<RecordAppDetailUiEntry> {
        if (detailType != BatteryStatus.Discharging) return emptyList()
        val statsEntries = RecordAppStatsComputer.compute(
            lineRecords,
            recordIntervalMs
        ).filterNot { it.isScreenOff }
        if (statsEntries.isEmpty()) return emptyList()

        val packageManager = context.packageManager
        return statsEntries.map { entry ->
            val packageName = entry.packageName!!
            val resolved = ResolveInstalledAppLabelUseCase.execute(
                packageManager = packageManager,
                packageName = packageName
            )
            RecordAppDetailUiEntry(
                packageName = resolved.packageName,
                appLabel = resolved.label,
                averagePowerRaw = entry.averagePowerRaw,
                averageTempCelsius = entry.averageTempCelsius,
                maxTempCelsius = entry.maxTempCelsius,
                durationMs = entry.totalDurationMs
            )
        }
    }

    /**
     * 统计有效前后台应用切换次数。
     *
     * @param lineRecords 当前详情记录点列表。
     * @return 返回应用切换次数。
     */
    fun buildRecordDetailAppSwitchCount(
        lineRecords: List<LineRecord>
    ): Int {
        if (lineRecords.size < 2) return 0
        var appSwitchCount = 0
        var previousRecord: LineRecord? = null
        lineRecords.forEach { currentRecord ->
            val previous = previousRecord
            previousRecord = currentRecord
            if (previous == null) return@forEach
            if (
                previous.packageName.isMeaningfulPackageName() &&
                currentRecord.packageName.isMeaningfulPackageName() &&
                previous.packageName != currentRecord.packageName
            ) {
                appSwitchCount++
            }
        }
        return appSwitchCount
    }

    private fun String?.isMeaningfulPackageName(): Boolean =
        !this.isNullOrBlank()

    private fun List<LineRecord>.toChartPoints(): List<ChartPoint> {
        return map { record ->
            ChartPoint(
                timestamp = record.timestamp,
                power = record.power.toDouble(),
                packageName = record.packageName,
                capacity = record.capacity,
                isDisplayOn = record.isDisplayOn == 1,
                temp = record.temp,
                voltage = record.voltage
            )
        }
    }
}
