package yangfentuozi.batteryrecorder.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import yangfentuozi.batteryrecorder.R
import yangfentuozi.batteryrecorder.appString
import yangfentuozi.batteryrecorder.shared.config.dataclass.StatisticsSettings
import yangfentuozi.batteryrecorder.shared.util.LoggerX
import yangfentuozi.batteryrecorder.ui.model.PredictionDetailUiState
import yangfentuozi.batteryrecorder.usecase.prediction.LoadPredictionDetailUseCase

private const val TAG = "PredictionDetailViewModel"

class PredictionDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PredictionDetailUiState())
    val uiState: StateFlow<PredictionDetailUiState> = _uiState.asStateFlow()

    // 详情页配置变化时允许新请求覆盖旧请求，避免 isLoading 把刷新吞掉。
    private var loadJob: Job? = null
    private var loadGeneration = 0L

    /**
     * 加载应用维度预测详情。
     *
     * 同步、文件读取与包管理器查询都在 IO 线程执行；仅最后一次请求允许落状态。
     */
    fun load(
        context: Context,
        request: StatisticsSettings,
        recordIntervalMs: Long
    ) {
        val generation = ++loadGeneration
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val entries = try {
                LoadPredictionDetailUseCase.execute(context, request, recordIntervalMs)
            } catch (error: Throwable) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                LoggerX.e(TAG, "加载应用预测失败", tr = error)
                if (generation == loadGeneration) {
                    _uiState.value = PredictionDetailUiState(
                        isLoading = false,
                        errorMessage = appString(R.string.prediction_detail_load_failed)
                    )
                }
                return@launch
            }

            if (generation == loadGeneration) {
                _uiState.value = PredictionDetailUiState(
                    isLoading = false,
                    entries = entries
                )
            }
        }
    }
}
