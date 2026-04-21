package yangfentuozi.batteryrecorder.usecase.home

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import yangfentuozi.batteryrecorder.R
import yangfentuozi.batteryrecorder.appString
import yangfentuozi.batteryrecorder.data.log.LogRepository

/**
 * 首页日志导出结果。
 */
internal data class ExportLogsResult(
    val userMessage: String
)

/**
 * 导出首页日志 ZIP。
 */
internal object ExportLogsUseCase {

    /**
     * 执行日志导出并整理用户提示。
     *
     * @param context 应用上下文。
     * @param destinationUri SAF 目标 Uri。
     * @return 返回导出后的用户提示。
     */
    suspend fun execute(
        context: Context,
        destinationUri: Uri
    ): ExportLogsResult = withContext(Dispatchers.IO) {
        val exportResult = LogRepository.exportLogsZip(
            context = context,
            destinationUri = destinationUri
        )
        ExportLogsResult(
            userMessage = if (exportResult.serverExportFailed) {
                appString(R.string.toast_export_partial_success)
            } else {
                appString(R.string.toast_export_success)
            }
        )
    }
}
