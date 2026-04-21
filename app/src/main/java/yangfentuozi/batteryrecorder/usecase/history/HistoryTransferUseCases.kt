package yangfentuozi.batteryrecorder.usecase.history

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import yangfentuozi.batteryrecorder.data.history.HistoryRepository
import yangfentuozi.batteryrecorder.data.history.ImportRecordsZipResult
import yangfentuozi.batteryrecorder.shared.data.BatteryStatus
import yangfentuozi.batteryrecorder.shared.data.RecordsFile

/**
 * 删除单条历史记录。
 */
internal object DeleteRecordUseCase {

    /**
     * 删除指定记录。
     *
     * @param context 应用上下文。
     * @param recordsFile 目标记录。
     * @return 返回是否删除成功。
     */
    suspend fun execute(
        context: Context,
        recordsFile: RecordsFile
    ): Boolean = withContext(Dispatchers.IO) {
        HistoryRepository.deleteRecord(context, recordsFile)
    }
}

/**
 * 导出单条历史记录。
 */
internal object ExportRecordUseCase {

    /**
     * 导出单条记录到目标 Uri。
     *
     * @param context 应用上下文。
     * @param recordsFile 目标记录。
     * @param destinationUri 导出目标 Uri。
     * @return 无返回值。
     */
    suspend fun execute(
        context: Context,
        recordsFile: RecordsFile,
        destinationUri: Uri
    ) = withContext(Dispatchers.IO) {
        HistoryRepository.exportRecord(context, recordsFile, destinationUri)
    }
}

/**
 * 批量导出历史记录。
 */
internal object ExportRecordsUseCase {

    /**
     * 导出指定类型的多条记录。
     *
     * @param context 应用上下文。
     * @param type 记录类型。
     * @param destinationUri 导出目标 Uri。
     * @param records 优先使用的记录列表；为空时回退磁盘扫描。
     * @return 返回最终参与导出的记录数量。
     */
    suspend fun execute(
        context: Context,
        type: BatteryStatus,
        destinationUri: Uri,
        records: List<RecordsFile>?
    ): Int = withContext(Dispatchers.IO) {
        val exportRecords = records ?: HistoryRepository
            .listRecordFiles(context, type)
            .map { file -> RecordsFile.fromFile(file) }
        HistoryRepository.exportRecordsZip(context, exportRecords, destinationUri)
        exportRecords.size
    }
}

/**
 * 批量导入历史记录。
 */
internal object ImportRecordsUseCase {

    /**
     * 从 ZIP 导入一批记录。
     *
     * @param context 应用上下文。
     * @param type 当前导入类型。
     * @param sourceUri 用户选择的 ZIP Uri。
     * @return 返回导入结果。
     */
    suspend fun execute(
        context: Context,
        type: BatteryStatus,
        sourceUri: Uri
    ): ImportRecordsZipResult = withContext(Dispatchers.IO) {
        HistoryRepository.importRecordsZip(context, type, sourceUri)
    }
}
