package yangfentuozi.batteryrecorder.usecase.home

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import yangfentuozi.batteryrecorder.R
import yangfentuozi.batteryrecorder.appString
import yangfentuozi.batteryrecorder.data.history.HistoryRepository
import yangfentuozi.batteryrecorder.data.history.RecordCleanupRequest
import yangfentuozi.batteryrecorder.data.history.RecordCleanupResult
import yangfentuozi.batteryrecorder.shared.data.RecordsFile

/**
 * 首页记录清理结果。
 */
internal data class CleanupRecordsResult(
    val result: RecordCleanupResult,
    val userMessage: String
)

/**
 * 执行首页记录清理。
 */
internal object CleanupRecordsUseCase {

    /**
     * 按用户确认后的规则执行记录清理。
     *
     * @param context 应用上下文。
     * @param request 用户确认后的清理规则。
     * @param activeRecordsFile 当前活跃记录文件。
     * @return 返回清理结果和用户提示。
     */
    suspend fun execute(
        context: Context,
        request: RecordCleanupRequest,
        activeRecordsFile: RecordsFile?
    ): CleanupRecordsResult = withContext(Dispatchers.IO) {
        val result = HistoryRepository.cleanupRecords(
            context = context,
            request = request,
            activeRecordsFile = activeRecordsFile
        )
        CleanupRecordsResult(
            result = result,
            userMessage = buildRecordCleanupMessage(result)
        )
    }

    /**
     * 生成记录清理结果提示文案。
     *
     * @param result 仓库层返回的清理结果。
     * @return 返回用于首页 Toast 的简体中文提示。
     */
    private fun buildRecordCleanupMessage(result: RecordCleanupResult): String {
        val failedCount = result.failedFiles.size
        if (result.deletedCount == 0 && failedCount == 0) {
            return appString(R.string.record_cleanup_toast_no_match)
        }
        if (result.deletedCount == 0) {
            return appString(
                R.string.record_cleanup_toast_all_failed,
                failedCount
            )
        }
        if (failedCount == 0) {
            return appString(
                R.string.record_cleanup_toast_success,
                result.deletedCount,
                result.deletedChargingCount,
                result.deletedDischargingCount
            )
        }
        return appString(
            R.string.record_cleanup_toast_partial_failed,
            result.deletedCount,
            result.deletedChargingCount,
            result.deletedDischargingCount,
            failedCount
        )
    }
}
