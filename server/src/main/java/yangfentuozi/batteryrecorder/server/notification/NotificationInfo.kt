package yangfentuozi.batteryrecorder.server.notification

import java.io.DataInputStream
import java.io.DataOutputStream

data class NotificationInfo(val power: Double, val temp: Int) {
    fun writeToDos(dos: DataOutputStream) {
        dos.writeDouble(power)
        dos.writeInt(temp)
    }
    companion object {
        fun readFromDis(dis: DataInputStream): NotificationInfo {
            val power = dis.readDouble()
            val temp = dis.readInt()
            return NotificationInfo(power, temp)
        }
    }
}