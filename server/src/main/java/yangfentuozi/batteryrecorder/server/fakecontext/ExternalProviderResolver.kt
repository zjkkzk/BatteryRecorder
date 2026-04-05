package yangfentuozi.batteryrecorder.server.fakecontext

import android.app.IActivityManager
import android.content.ContentResolver
import android.content.Context
import android.content.IContentProvider
import android.os.IBinder
import android.os.RemoteException
import androidx.annotation.Keep
import java.util.concurrent.ConcurrentHashMap

@Keep
@Suppress("unused")
class ExternalProviderResolver(
    context: Context?,
    private val activityManager: IActivityManager,
    private val providerToken: IBinder?
) : ContentResolver(context) {
    private val lock = Any()
    private val providerRefs: ConcurrentHashMap<IBinder, ProviderRef> =
        ConcurrentHashMap<IBinder, ProviderRef>()

    fun acquireProvider(context: Context?, auth: String?): IContentProvider? {
        return acquireExternalProvider(auth)
    }

    fun acquireExistingProvider(context: Context?, auth: String?): IContentProvider? {
        return acquireExternalProvider(auth)
    }

    fun releaseProvider(provider: IContentProvider): Boolean {
        return releaseExternalProvider(provider)
    }

    fun acquireUnstableProvider(context: Context?, auth: String?): IContentProvider? {
        return acquireExternalProvider(auth)
    }

    fun releaseUnstableProvider(provider: IContentProvider): Boolean {
        return releaseExternalProvider(provider)
    }

    fun unstableProviderDied(provider: IContentProvider?) {
    }

    fun appNotRespondingViaProvider(provider: IContentProvider?) {
    }

    private fun acquireExternalProvider(auth: String?): IContentProvider? {
        synchronized(lock) {
            try {
                val holder =
                    activityManager.getContentProviderExternal(auth, 0, providerToken, auth)
                if (holder == null || holder.provider == null) {
                    return null
                }
                val provider = holder.provider
                providerRefs.compute(provider.asBinder()) { binder: IBinder?, ref: ProviderRef? ->
                    if (ref != null) {
                        ref.count++
                        return@compute ref
                    }
                    ProviderRef(auth)
                }
                return provider
            } catch (e: RemoteException) {
                throw e.rethrowFromSystemServer()
            }
        }
    }

    private fun releaseExternalProvider(provider: IContentProvider): Boolean {
        synchronized(lock) {
            val ref = providerRefs[provider.asBinder()] ?: return false
            if ((--ref.count) > 0) {
                return true
            }
            providerRefs.remove(provider.asBinder(), ref)
            try {
                activityManager.removeContentProviderExternal(ref.authority, providerToken)
                return true
            } catch (e: RemoteException) {
                throw e.rethrowFromSystemServer()
            }
        }
    }

    private class ProviderRef(val authority: String?) {
        @Volatile
        var count = 1
    }
}
