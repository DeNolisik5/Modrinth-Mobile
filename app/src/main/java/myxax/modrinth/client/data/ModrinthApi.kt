package myxax.modrinth.client.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Обёртка над публичным API Modrinth (https://api.modrinth.com/v2).
 *
 * Разбор JSON идёт через org.json — он входит в Android, поэтому лишних
 * зависимостей не требуется.
 */
object ModrinthApi {

    const val PAGE_SIZE = 25

    private const val BASE = "https://api.modrinth.com"

    private const val USER_AGENT =
        "ModrinthClient/1.1.1 (github.com/DeNolisik5/Modrinth-Api-Mobile)"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS)
        .build()

    // ---------------------------------------------------------------- поиск

    suspend fun search(
        query: String,
        type: ContentType,
        gameVersion: String?,
        filters: SearchFilters,
        offset: Int,
        limit: Int = PAGE_SIZE
    ): SearchPage = withContext(Dispatchers.IO) {
        val url = "$BASE/v2/search".toHttpUrl().newBuilder()
            .addQueryParameter("query", query.trim())
            .addQueryParameter("limit", limit.toString())
            .addQueryParameter("offset", offset.toString())
            .addQueryParameter("index", pickIndex(query, filters.sort))
            .addQueryParameter("facets", buildFacets(type, gameVersion, filters))
            .build()

        val root = JSONObject(fetch(url.toString()))
        val hits = root.optJSONArray("hits") ?: JSONArray()

        val items = buildList(hits.length()) {
            for (i in 0 until hits.length()) {
                hits.optJSONObject(i)?.let { add(parseHit(it)) }
            }
        }

        SearchPage(
            items = items,
            total = root.optInt("total_hits", items.size),
            offset = root.optInt("offset", offset)
        )
    }

    /**
     * У пустого поискового запроса «релевантность» бессмысленна — Modrinth
     * в этом случае отдаёт случайную выдачу, поэтому показываем популярное.
     */
    private fun pickIndex(query: String, sort: SortOption): String =
        if (query.isBlank() && sort == SortOption.RELEVANCE) {
            SortOption.DOWNLOADS.apiValue
        } else {
            sort.apiValue
        }

    /**
     * Фасеты Modrinth — это массив групп. Внутри группы значения объединяются
     * через ИЛИ, между группами — через И. Отсюда три отдельные группы:
     * тип контента, загрузчики и категории.
     */
    private fun buildFacets(
        type: ContentType,
        gameVersion: String?,
        filters: SearchFilters
    ): String {
        val groups = mutableListOf<String>()

        groups += "[\"project_type:${type.apiValue}\"]"

        if (filters.loaders.isNotEmpty()) {
            groups += filters.loaders.joinToString(",", "[", "]") { "\"categories:${it.apiValue}\"" }
        }

        if (filters.categories.isNotEmpty()) {
            groups += filters.categories.joinToString(",", "[", "]") { "\"categories:${it.apiValue}\"" }
        }

        if (!gameVersion.isNullOrBlank()) {
            groups += "[\"versions:$gameVersion\"]"
        }

        return groups.joinToString(",", "[", "]")
    }

    private fun parseHit(o: JSONObject) = ModHit(
        id = o.optString("project_id"),
        slug = o.optString("slug"),
        title = o.optString("title"),
        description = o.optString("description"),
        iconUrl = o.stringOrNull("icon_url"),
        downloads = o.optLong("downloads"),
        follows = o.optLong("follows"),
        author = o.optString("author"),
        categories = o.optJSONArray("categories").toStringList(),
        versions = o.optJSONArray("versions").toStringList()
    )

    // --------------------------------------------------------------- версии

    suspend fun versions(projectId: String): List<ModVersion> = withContext(Dispatchers.IO) {
        val arr = JSONArray(fetch("$BASE/v2/project/$projectId/version"))
        buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { add(parseVersion(it)) }
            }
        }
    }

    private fun parseVersion(o: JSONObject): ModVersion {
        val files = o.optJSONArray("files")
        var fileName = ""
        var fileUrl = ""
        var fileSize = 0L

        if (files != null && files.length() > 0) {
            var chosen: JSONObject? = null
            for (i in 0 until files.length()) {
                val f = files.optJSONObject(i) ?: continue
                if (f.optBoolean("primary")) {
                    chosen = f
                    break
                }
            }
            if (chosen == null) chosen = files.optJSONObject(0)
            if (chosen != null) {
                fileName = chosen.optString("filename")
                fileUrl = chosen.optString("url")
                fileSize = chosen.optLong("size")
            }
        }

        return ModVersion(
            id = o.optString("id"),
            name = o.optString("name"),
            versionNumber = o.optString("version_number"),
            versionType = o.optString("version_type"),
            gameVersions = o.optJSONArray("game_versions").toStringList(),
            loaders = o.optJSONArray("loaders").toStringList(),
            fileName = fileName,
            fileUrl = fileUrl,
            fileSize = fileSize
        )
    }

    // -------------------------------------------------------------- служебное

    private fun fetch(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Сервер ответил ${response.code}")
            }
            return response.body?.string() ?: throw IOException("Пустой ответ сервера")
        }
    }
}

private fun JSONObject.stringOrNull(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    val out = ArrayList<String>(length())
    for (i in 0 until length()) {
        val v = optString(i, "")
        if (v.isNotBlank()) out.add(v)
    }
    return out
}
