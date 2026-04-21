package yangfentuozi.batteryrecorder.ui.stateholder

private const val MAX_LIVE_POINTS = 20

/**
 * 首页当前分段的实时点缓存。
 *
 * 该状态只服务当前 HomeScreen，不承载跨页面或持久化语义。
 */
internal class LiveRecordSessionStateHolder {
    private var activeRecordsFileName: String? = null
    private val livePoints = ArrayList<Long>(MAX_LIVE_POINTS + 1)

    /**
     * 返回当前活动分段名称。
     *
     * @return 当前活动分段名称；若尚未确定则返回空值。
     */
    fun activeRecordsFileName(): String? = activeRecordsFileName

    /**
     * 切换当前实时点所属分段。
     *
     * @param nextRecordsFileName 新的分段名称。
     * @return 若分段发生变化则返回 true，同时清空已有实时点。
     */
    fun switchActiveSegment(nextRecordsFileName: String?): Boolean {
        if (activeRecordsFileName == nextRecordsFileName) return false
        activeRecordsFileName = nextRecordsFileName
        livePoints.clear()
        return true
    }

    /**
     * 追加一条实时采样并返回快照。
     *
     * @param power 原始功率采样值。
     * @return 返回最新的实时点快照。
     */
    fun appendLivePoint(power: Long): List<Long> {
        livePoints.add(power)
        while (livePoints.size > MAX_LIVE_POINTS) {
            livePoints.removeAt(0)
        }
        return livePoints.toList()
    }

    /**
     * 返回当前实时点快照。
     *
     * @return 当前实时点列表快照。
     */
    fun snapshotLivePoints(): List<Long> = livePoints.toList()

    /**
     * 清空会话状态。
     *
     * @return 无返回值。
     */
    fun clear() {
        activeRecordsFileName = null
        livePoints.clear()
    }
}
