package yangfentuozi.batteryrecorder.usecase.common

import android.content.pm.PackageManager

/**
 * 已解析的应用标签结果。
 */
internal data class ResolvedInstalledAppLabel(
    val packageName: String,
    val label: String,
    val isInstalled: Boolean
)

/**
 * 统一处理应用标签解析与安装态判断。
 */
internal object ResolveInstalledAppLabelUseCase {

    /**
     * 解析包名对应的展示标签。
     *
     * @param packageManager 当前上下文可用的 PackageManager。
     * @param packageName 需要解析的包名。
     * @return 返回安装态与展示标签；未安装时 label 回退为包名本身。
     */
    fun execute(
        packageManager: PackageManager,
        packageName: String
    ): ResolvedInstalledAppLabel {
        val appInfo = runCatching {
            packageManager.getApplicationInfo(packageName, 0)
        }.getOrNull()
        if (appInfo == null) {
            return ResolvedInstalledAppLabel(
                packageName = packageName,
                label = packageName,
                isInstalled = false
            )
        }
        val label = runCatching {
            appInfo.loadLabel(packageManager).toString()
        }.getOrDefault(packageName)
        return ResolvedInstalledAppLabel(
            packageName = packageName,
            label = label,
            isInstalled = true
        )
    }
}
