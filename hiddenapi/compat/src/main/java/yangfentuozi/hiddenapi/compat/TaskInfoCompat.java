package yangfentuozi.hiddenapi.compat;

import android.app.ActivityTaskManager;
import android.app.IActivityTaskManager;
import android.app.TaskInfo;
import android.graphics.Rect;
import android.os.RemoteException;

import androidx.annotation.Nullable;

public final class TaskInfoCompat {
    private TaskInfoCompat() {
    }

    /**
     * 读取任务当前窗口边界；任务为空时返回空。
     *
     * @param taskInfo 任务信息。
     * @return 成功返回窗口当前边界，任务为空时返回 `null`。
     */
    @Nullable
    public static Rect getBoundsOrNull(
            IActivityTaskManager activityTaskManager,
            @Nullable TaskInfo taskInfo,
            @Nullable ActivityTaskManager.RootTaskInfo focusedRootTaskInfo
    ) throws RemoteException {
        if (taskInfo == null) {
            return null;
        }
        if (taskInfo instanceof ActivityTaskManager.RootTaskInfo) {
            return getTopChildBoundsOrRootBounds(focusedRootTaskInfo);
        }
        return activityTaskManager.getTaskBounds(taskInfo.taskId);
    }

    /**
     * 读取任务可达到的最大窗口边界；任务为空时返回空。
     *
     * @param taskInfo 任务信息。
     * @return 成功返回窗口最大边界，任务为空时返回 `null`。
     */
    @Nullable
    public static Rect getMaxBoundsOrNull(@Nullable ActivityTaskManager.RootTaskInfo focusedRootTaskInfo) {
        if (focusedRootTaskInfo == null) {
            return null;
        }
        return focusedRootTaskInfo.bounds;
    }

    /**
     * 从 RootTaskInfo 的顶部子任务读取当前任务边界。
     *
     * @param focusedRootTaskInfo 当前聚焦 RootTask 信息。
     * @return 优先返回顶部子任务边界；缺失时返回 RootTask 自身边界。
     */
    @Nullable
    private static Rect getTopChildBoundsOrRootBounds(
            @Nullable ActivityTaskManager.RootTaskInfo focusedRootTaskInfo
    ) {
        if (focusedRootTaskInfo == null) {
            return null;
        }
        final Rect[] childTaskBounds = focusedRootTaskInfo.childTaskBounds;
        if (childTaskBounds != null && childTaskBounds.length > 0) {
            return childTaskBounds[childTaskBounds.length - 1];
        }
        return focusedRootTaskInfo.bounds;
    }
}
