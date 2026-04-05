package yangfentuozi.batteryrecorder.server.fakecontext

import android.app.ActivityThread
import android.app.IActivityManager
import android.content.AttributionSource
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.os.Binder
import android.os.ServiceManager
import android.system.Os
import androidx.annotation.Keep

@Keep
class FakeContext : ContextWrapper(systemContext) {

    override fun getPackageName(): String {
        return if (Os.getuid() == 0) "root" else "com.android.shell"
    }

    override fun getOpPackageName(): String {
        return packageName
    }

    override fun getAttributionSource(): AttributionSource {
        val builder = AttributionSource.Builder(Os.getuid())
        builder.setPackageName(packageName)
        return builder.build()
    }

    override fun getDeviceId(): Int {
        return 0
    }

    override fun getApplicationContext(): Context {
        return this
    }

    @Keep
    @Suppress("unused")
    fun createApplicationContext(
        application: ApplicationInfo,
        flags: Int
    ): Context {
        return this
    }

    override fun createPackageContext(
        packageName: String,
        flags: Int
    ): Context {
        return this
    }

    private val providerToken = Binder()

    private val activityManager =
        IActivityManager.Stub.asInterface(ServiceManager.getService("activity"))

    private val externalContentResolver =
        ExternalProviderResolver(this, activityManager, providerToken)

    override fun getContentResolver(): ContentResolver {
        return externalContentResolver
    }

    companion object {
        val systemContext: Context
            get() {
                val activityThread = ActivityThread.currentActivityThread()
                    ?: ActivityThread.systemMain()
                    ?: throw IllegalStateException("获取 system ActivityThread 失败")
                return activityThread.systemContext
                    ?: throw IllegalStateException("获取 systemContext 失败")
            }
    }
}
