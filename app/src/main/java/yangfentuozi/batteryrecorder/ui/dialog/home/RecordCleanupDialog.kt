package yangfentuozi.batteryrecorder.ui.dialog.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import yangfentuozi.batteryrecorder.R
import yangfentuozi.batteryrecorder.data.history.RecordCleanupRequest
import yangfentuozi.batteryrecorder.ui.theme.AppShape

/**
 * 渲染历史页“记录清理”配置弹窗。
 *
 * @param targetTypeLabel 当前历史页对应的记录类型文案，如“充电记录”。
 * @param initialRequest 已填写过的清理规则；用于“返回修改”时回填用户输入。
 * @param onDismiss 用户取消或关闭弹窗时回调。
 * @param onConfirm 用户确认规则后回调；仅在输入合法时触发。
 * @return 无，直接渲染弹窗。
 */
@Composable
fun RecordCleanupDialog(
    targetTypeLabel: String,
    initialRequest: RecordCleanupRequest? = null,
    onDismiss: () -> Unit,
    onConfirm: (RecordCleanupRequest) -> Unit
) {
    var keepEnabled by remember(initialRequest) {
        mutableStateOf(initialRequest?.keepCountPerType != null)
    }
    var keepCountText by remember(initialRequest) {
        mutableStateOf(initialRequest?.keepCountPerType?.toString().orEmpty())
    }
    var durationEnabled by remember(initialRequest) {
        mutableStateOf(initialRequest?.maxDurationMinutes != null)
    }
    var durationText by remember(initialRequest) {
        mutableStateOf(initialRequest?.maxDurationMinutes?.toString() ?: "5")
    }
    var capacityEnabled by remember(initialRequest) {
        mutableStateOf(initialRequest?.maxCapacityChangePercent != null)
    }
    var capacityText by remember(initialRequest) {
        mutableStateOf(initialRequest?.maxCapacityChangePercent?.toString() ?: "5")
    }

    val keepCount = keepCountText.toIntOrNull()
    val durationMinutes = durationText.toIntOrNull()
    val capacityPercent = capacityText.toIntOrNull()

    val keepError = keepEnabled && (keepCount == null || keepCount <= 0)
    val durationError = durationEnabled && (durationMinutes == null || durationMinutes <= 0)
    val capacityError = capacityEnabled && (capacityPercent == null || capacityPercent !in 1..100)
    val hasAnyRule = keepEnabled || durationEnabled || capacityEnabled
    val hasInputError = keepError || durationError || capacityError

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.record_cleanup_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.record_cleanup_intro_current_type, targetTypeLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CleanupRuleField(
                    checked = keepEnabled,
                    title = stringResource(R.string.record_cleanup_keep_count_title),
                    value = keepCountText,
                    onCheckedChange = { keepEnabled = it },
                    onValueChange = { keepCountText = it },
                    label = stringResource(R.string.record_cleanup_keep_count_label),
                    placeholder = stringResource(R.string.record_cleanup_keep_count_placeholder),
                    supportingText = stringResource(
                        R.string.record_cleanup_keep_count_hint_current_type,
                        targetTypeLabel
                    ),
                    errorText = stringResource(R.string.record_cleanup_keep_count_error),
                    isError = keepError
                )
                CleanupRuleField(
                    checked = durationEnabled,
                    title = stringResource(R.string.record_cleanup_duration_title),
                    value = durationText,
                    onCheckedChange = { durationEnabled = it },
                    onValueChange = { durationText = it },
                    label = stringResource(R.string.record_cleanup_duration_label),
                    placeholder = stringResource(R.string.record_cleanup_duration_placeholder),
                    supportingText = stringResource(R.string.record_cleanup_duration_hint),
                    errorText = stringResource(R.string.record_cleanup_duration_error),
                    isError = durationError
                )
                CleanupRuleField(
                    checked = capacityEnabled,
                    title = stringResource(R.string.record_cleanup_capacity_title),
                    value = capacityText,
                    onCheckedChange = { capacityEnabled = it },
                    onValueChange = { capacityText = it },
                    label = stringResource(R.string.record_cleanup_capacity_label),
                    placeholder = stringResource(R.string.record_cleanup_capacity_placeholder),
                    supportingText = stringResource(
                        R.string.record_cleanup_capacity_hint_current_type,
                        targetTypeLabel
                    ),
                    errorText = stringResource(R.string.record_cleanup_capacity_error),
                    isError = capacityError
                )
                Text(
                    text = if (!hasAnyRule) {
                        stringResource(R.string.record_cleanup_rule_required)
                    } else {
                        stringResource(R.string.record_cleanup_condition_logic_hint)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!hasAnyRule) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        RecordCleanupRequest(
                            keepCountPerType = keepCount.takeIf { keepEnabled },
                            maxDurationMinutes = durationMinutes.takeIf { durationEnabled },
                            maxCapacityChangePercent = capacityPercent.takeIf { capacityEnabled }
                        )
                    )
                },
                enabled = hasAnyRule && !hasInputError
            ) {
                Text(stringResource(R.string.record_cleanup_next_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.record_cleanup_cancel_action))
            }
        },
        shape = AppShape.extraLarge
    )
}

/**
 * 渲染记录清理执行前的二次确认弹窗。
 *
 * @param targetTypeLabel 当前历史页对应的记录类型文案，如“充电记录”。
 * @param request 用户已填写并校验通过的清理规则。
 * @param onDismiss 用户取消执行时回调。
 * @param onConfirm 用户确认执行时回调。
 * @return 无，直接渲染弹窗。
 */
@Composable
fun RecordCleanupConfirmDialog(
    targetTypeLabel: String,
    request: RecordCleanupRequest,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val summaryText = buildString {
        appendLine(stringResource(R.string.record_cleanup_confirm_intro_current_type, targetTypeLabel))
        request.keepCountPerType?.let { keepCount ->
            appendLine(
                stringResource(
                    R.string.record_cleanup_confirm_keep_count_current_type,
                    targetTypeLabel,
                    keepCount
                )
            )
        }
        request.maxDurationMinutes?.let { durationMinutes ->
            appendLine(
                stringResource(
                    R.string.record_cleanup_confirm_duration,
                    durationMinutes
                )
            )
        }
        request.maxCapacityChangePercent?.let { capacityPercent ->
            appendLine(
                stringResource(
                    R.string.record_cleanup_confirm_capacity,
                    capacityPercent
                )
            )
        }
        append(stringResource(R.string.record_cleanup_confirm_condition_logic))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.record_cleanup_confirm_title)) },
        text = { Text(summaryText) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.record_cleanup_execute_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.record_cleanup_back_action))
            }
        },
        shape = AppShape.extraLarge
    )
}

/**
 * 渲染单条可切换的清理规则输入区。
 *
 * @param checked 当前规则是否启用。
 * @param title 规则标题。
 * @param value 规则输入值。
 * @param onCheckedChange 规则开关变化回调。
 * @param onValueChange 输入值变化回调。
 * @param label 输入框标签。
 * @param placeholder 输入框占位文案。
 * @param supportingText 正常状态下的辅助提示。
 * @param errorText 非法输入时的错误提示。
 * @param isError 当前输入是否合法。
 * @return 无，直接渲染规则区域。
 */
@Composable
private fun CleanupRuleField(
    checked: Boolean,
    title: String,
    value: String,
    onCheckedChange: (Boolean) -> Unit,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    supportingText: String,
    errorText: String,
    isError: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = checked,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            isError = isError,
            supportingText = {
                Text(
                    text = if (isError) errorText else supportingText
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
