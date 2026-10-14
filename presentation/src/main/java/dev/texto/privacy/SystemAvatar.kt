package dev.texto.privacy

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.WorkerThread
import dev.octoshrimpy.quik.util.GlideApp
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** Small, independently owned images for notifications, shortcuts and RemoteViews. */
object SystemAvatar {
    @WorkerThread
    fun load(context: Context, source: Any?, width: Int, height: Int, circular: Boolean = true): Bitmap? {
        if (source == null) return null
        val manager = GlideApp.with(context.applicationContext)
        val request = manager.asBitmap().load(source).disallowHardwareConfig()
        if (circular) request.circleCrop() else request.centerInside()
        val target = request.submit(width.coerceIn(1, 512), height.coerceIn(1, 512))
        return try {
            // System UI retains this bitmap after the request is cleared. Never hand it a pooled bitmap.
            target.get(3, TimeUnit.SECONDS).copy(Bitmap.Config.ARGB_8888, false)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } catch (_: ExecutionException) {
            null
        } catch (_: TimeoutException) {
            null
        } finally {
            manager.clear(target)
        }
    }
}
