package yangfentuozi.batteryrecorder.usecase.common

import android.content.Context
import yangfentuozi.batteryrecorder.shared.config.SharedSettings

/**
 * 当前展示配置快照。
 */
internal data class DisplaySettingsSnapshot(
    val dischargeDisplayPositive: Boolean
)

/**
 * 统一读取展示侧配置，避免多个 ViewModel 直接访问 SharedPreferences。
 */
internal object ReadDisplaySettingsUseCase {

    /**
     * 读取当前展示配置。
     *
     * @param context 应用上下文。
     * @return 返回当前展示配置快照。
     */
    fun execute(context: Context): DisplaySettingsSnapshot {
        val appSettings = SharedSettings.readAppSettings(context)
        return DisplaySettingsSnapshot(
            dischargeDisplayPositive = appSettings.dischargeDisplayPositive
        )
    }
}
