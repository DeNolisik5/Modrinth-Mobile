package myxax.modrinth.client.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

/**
 * Скачивает файл проекта в общую папку «Downloads» телефона.
 *
 * Имя файла берётся из ответа API Modrinth (`files[].filename`), поэтому
 * расширение всегда родное: `.jar` у модов, `.zip` у ресурспаков и шейдеров.
 * MIME-тип подбирается по расширению — иначе MediaStore дописывает своё
 * расширение и получается `my_pack.zip.jar`.
 *
 * На Android 10+ используется MediaStore, поэтому никаких разрешений
 * запрашивать не нужно. На более старых системах пишем напрямую в
 * /storage/emulated/0/Download (нужно разрешение WRITE_EXTERNAL_STORAGE).
 */
object ModDownloader {

    private const val USER_AGENT =
        "ModrinthClient/1.1.7 (github.com/DeNolisik5/Modrinth-Mobile)"

    private const val SUBFOLDER = "ModrinthClient"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /** Возвращает путь вида «Downloads/ModrinthClient/файл.zip». */
    suspend fun download(
        context: Context,
        version: ModVersion,
        onProgress: (Float) -> Unit
    ): String = withContext(Dispatchers.IO) {
        if (version.fileUrl.isBlank()) {
            throw IOException("У этой версии нет файла для скачивания")
        }

        val fileName = resolveFileName(version)
        val request = Request.Builder()
            .url(version.fileUrl)
            .header("User-Agent", USER_AGENT)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Не удалось скачать: HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("Пустой ответ сервера")
            val total = body.contentLength()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithMediaStore(context, fileName, body.byteStream(), total, onProgress)
            } else {
                saveWithFile(fileName, body.byteStream(), total, onProgress)
            }

            "Downloads/$SUBFOLDER/$fileName"
        }
    }

    // ------------------------------------------------------- имя и тип файла

    /**
     * Точное имя файла: сначала `filename` из API, иначе последний сегмент
     * URL для скачивания. Символы, недопустимые в именах файлов, заменяются.
     */
    private fun resolveFileName(version: ModVersion): String {
        val raw = version.fileName.ifBlank { fileNameFromUrl(version.fileUrl) }
        val cleaned = raw
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")
            .trim()
        return cleaned.ifBlank { "modrinth-file" }
    }

    private fun fileNameFromUrl(url: String): String =
        runCatching {
            URLDecoder.decode(
                url.substringBefore('?').substringBefore('#').substringAfterLast('/'),
                "UTF-8"
            )
        }.getOrDefault("")

    /** MIME по расширению: MediaStore не должен додумывать своё. */
    private fun mimeTypeFor(fileName: String): String =
        when (fileName.substringAfterLast('.', "").lowercase()) {
            "jar", "litemod" -> "application/java-archive"
            "zip", "mrpack" -> "application/zip"
            "mcb" -> "application/octet-stream"
            "json", "txt", "toml", "yaml", "yml" -> "text/plain"
            else -> "application/octet-stream"
        }

    // ------------------------------------------------------------- Android 10+

    private fun saveWithMediaStore(
        context: Context,
        fileName: String,
        input: InputStream,
        total: Long,
        onProgress: (Float) -> Unit
    ): Unit {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            // MIME строго по расширению: иначе MediaStore дописывает своё
            // расширение и файл получает имя вида «pack.zip.jar».
            put(MediaStore.Downloads.MIME_TYPE, mimeTypeFor(fileName))
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/" + SUBFOLDER
            )
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Не удалось создать файл в папке Downloads")

        try {
            resolver.openOutputStream(uri)?.use { output ->
                copyWithProgress(input, output, total, onProgress)
            } ?: throw IOException("Нет доступа к созданному файлу")

            val done = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            resolver.update(uri, done, null, null)
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
    }

    // ------------------------------------------------------------ Android 8–9

    @Suppress("DEPRECATION")
    private fun saveWithFile(
        fileName: String,
        input: InputStream,
        total: Long,
        onProgress: (Float) -> Unit
    ) {
        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dir = File(root, SUBFOLDER)
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("Не удалось создать папку Downloads/$SUBFOLDER")
        }
        val file = File(dir, fileName)
        file.outputStream().use { output ->
            copyWithProgress(input, output, total, onProgress)
        }
    }

    // -------------------------------------------------------------- копирование

    private fun copyWithProgress(
        input: InputStream,
        output: OutputStream,
        total: Long,
        onProgress: (Float) -> Unit
    ) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var copied = 0L
        var lastReported = -1

        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            output.write(buffer, 0, read)
            copied += read

            if (total > 0) {
                val percent = ((copied * 100) / total).toInt()
                if (percent != lastReported) {
                    lastReported = percent
                    onProgress((copied.toFloat() / total).coerceIn(0f, 1f))
                }
            }
        }
        output.flush()
        onProgress(1f)
    }
}
