package yangfentuozi.batteryrecorder.usecase.history

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import yangfentuozi.batteryrecorder.R
import yangfentuozi.batteryrecorder.appString
import yangfentuozi.batteryrecorder.data.history.HistoryRepository
import yangfentuozi.batteryrecorder.data.history.RecordCleanupRequest
import yangfentuozi.batteryrecorder.data.history.RecordCleanupResult
import yangfentuozi.batteryrecorder.ipc.Service
import yangfentuozi.batteryrecorder.shared.data.BatteryStatus
import yangfentuozi.batteryrecorder.shared.data.RecordsFile
import yangfentuozi.batteryrecorder.shared.util.LoggerX

private const val TAG = "CleanupHistoryRecordsUseCase"

internal data class CleanupHistoryRecordsResult(
    val historyListUiResult: HistoryListUiResult,
    val userMessage: String
)

internal object CleanupHistoryRecordsUseCase {

    suspend fun execute(
        context: Context,
        type: BatteryStatus,
        request: RecordCleanupRequest,
        previousSession: HistoryListSession,
        chargeCapacityChangeFilter: Int?
    ): CleanupHistoryRecordsResult {
        require(type == BatteryStatus.Charging || type == BatteryStatus.Discharging) {
            "cleanup type must be charging or discharging"
        }

        val appContext = context.applicationContext
        val activeRecordsFile = currentRecordsFile(type)
        LoggerX.i(
            TAG,
            "[记录清理] 开始执行: type=${type.dataDirName} keep=${request.keepCountPerType} duration=${request.maxDurationMinutes} capacity=${request.maxCapacityChangePercent} active=${activeRecordsFile?.name}"
        )
        val cleanupResult = withContext(Dispatchers.IO) {
            HistoryRepository.cleanupRecords(
                context = appContext,
                type = type,
                request = request,
                activeRecordsFile = activeRecordsFile
            )
        }
        if (cleanupResult.failedFiles.isNotEmpty()) {
            LoggerX.w(
                TAG,
                "[记录清理] 存在删除失败文件: ${cleanupResult.failedFiles.joinToString()}"
            )
        }
        val historyListUiResult = LoadHistoryListUseCase.reload(
            context = appContext,
            type = type,
            previousSession = previousSession,
            chargeCapacityChangeFilter = if (type == BatteryStatus.Charging) {
                chargeCapacityChangeFilter
            } else {
                null
            }
        )
        return CleanupHistoryRecordsResult(
            historyListUiResult = historyListUiResult,
            userMessage = buildCleanupMessage(type, cleanupResult)
        )
    }

    private suspend fun currentRecordsFile(type: BatteryStatus): RecordsFile? {
        return withContext(Dispatchers.IO) {
            runCatching { Service.service?.currRecordsFile }
                .onFailure { error ->
                    LoggerX.w(TAG, "[记录清理] 读取当前记录文件失败", tr = error)
                }
                .getOrNull()
                ?.takeIf { it.type == type }
        }
    }

    private fun buildCleanupMessage(
        type: BatteryStatus,
        result: RecordCleanupResult
    ): String {
        val failedCount = result.failedFiles.size
        val deletedTypeCount = if (type == BatteryStatus.Charging) {
            result.deletedChargingCount
        } else {
            result.deletedDischargingCount
        }
        val typeLabel = if (type == BatteryStatus.Charging) {
            appString(R.string.history_record_type_charging)
        } else {
            appString(R.string.history_record_type_discharging)
        }
        if (deletedTypeCount == 0 && failedCount == 0) {
            return appString(R.string.record_cleanup_toast_no_match)
        }
        if (deletedTypeCount == 0) {
            return appString(
                R.string.record_cleanup_toast_all_failed,
                failedCount
            )
        }
        if (failedCount == 0) {
            return appString(
                R.string.record_cleanup_toast_success_single_type,
                deletedTypeCount,
                typeLabel
            )
        }
        return appString(
            R.string.record_cleanup_toast_partial_failed_single_type,
            deletedTypeCount,
            typeLabel,
            failedCount
        )
    }
}
