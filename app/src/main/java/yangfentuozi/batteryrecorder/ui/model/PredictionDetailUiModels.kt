package yangfentuozi.batteryrecorder.ui.model

/**
 * 预测详情页单行展示数据。
 *
 * averagePowerRaw 保持原始口径，正负值映射交给 Screen 层根据设置处理。
 */
data class PredictionDetailUiEntry(
    val packageName: String,
    val appLabel: String,
    val averagePowerRaw: Double,
    val currentHours: Double?
)

/**
 * 预测详情页整体 UI 状态。
 */
data class PredictionDetailUiState(
    val isLoading: Boolean = false,
    val entries: List<PredictionDetailUiEntry> = emptyList(),
    val errorMessage: String? = null
)
