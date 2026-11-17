package ru.mtuci.drivenext.presentation.catalog

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.mtuci.drivenext.R
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Кэш живёт столько же, сколько список; повторно использованные карточки отменяют загрузку. */
internal class CarImageLoader(private val scope: CoroutineScope) {
    private val cache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun load(url: String, target: ImageView): Job {
        target.setImageResource(R.drawable.catalog_loader_car)
        return scope.launch {
            val cached = cache.get(url)
            if (cached != null) {
                target.setImageBitmap(cached)
                return@launch
            }
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    try {
                        connection.connectTimeout = 15_000
                        connection.readTimeout = 15_000
                        if (connection.responseCode !in 200..299) throw IOException("Image unavailable")
                        connection.inputStream.use { input ->
                            BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inSampleSize = 4 })
                                ?: throw IOException("Invalid image")
                        }
                    } finally { connection.disconnect() }
                }
                ensureActive()
                cache.put(url, bitmap)
                target.setImageBitmap(bitmap)
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (_: IOException) {
                // Карточка остаётся читаемой; нажатие на фото повторяет запрос.
                target.setImageResource(R.drawable.catalog_loader_car)
            }
        }
    }
}
