package yangfentuozi.batteryrecorder.server.notification

import java.io.Closeable

interface NotificationUtil: Closeable {
    fun updateNotification(info: NotificationInfo)
    override fun close()
}
