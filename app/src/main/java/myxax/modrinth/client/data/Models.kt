package myxax.modrinth.client.data

/** Тип контента — соответствует вкладкам приложения. */
enum class ContentType(val apiValue: String, val label: String) {
    MOD("mod", "Моды"),
    RESOURCE_PACK("resourcepack", "Ресурспаки"),
    SHADER("shader", "Шейдеры")
}

/** Варианты сортировки, которые понимает API Modrinth. */
enum class SortOption(val apiValue: String, val label: String) {
    RELEVANCE("relevance", "По релевантности"),
    DOWNLOADS("downloads", "По скачиваниям"),
    NEWEST("newest", "Сначала новые"),
    UPDATED("updated", "Недавно обновлённые")
}

/** Загрузчик мода. В API Modrinth это тоже категория. */
enum class ModLoader(val apiValue: String, val label: String) {
    FABRIC("fabric", "Fabric"),
    FORGE("forge", "Forge"),
    NEOFORGE("neoforge", "NeoForge"),
    QUILT("quilt", "Quilt")
}

/** Категория контента с русской подписью. */
enum class ModCategory(val apiValue: String, val label: String) {
    OPTIMIZATION("optimization", "Оптимизация"),
    SHADERS("shaders", "Шейдеры"),
    ADVENTURE("adventure", "Приключения"),
    UTILITY("utility", "Утилиты"),
    DECORATION("decoration", "Декор"),
    WORLDGEN("worldgen", "Генерация мира"),
    MOBS("mobs", "Мобы"),
    MAGIC("magic", "Магия"),
    TECHNOLOGY("technology", "Технологии"),
    STORAGE("storage", "Хранилища"),
    EQUIPMENT("equipment", "Снаряжение"),
    LIBRARY("library", "Библиотеки")
}

data class SearchFilters(
    val loaders: Set<ModLoader> = emptySet(),
    val categories: Set<ModCategory> = emptySet(),
    val sort: SortOption = SortOption.RELEVANCE
) {
    val isActive: Boolean
        get() = loaders.isNotEmpty() ||
            categories.isNotEmpty() ||
            sort != SortOption.RELEVANCE

    val activeCount: Int
        get() = loaders.size + categories.size + if (sort != SortOption.RELEVANCE) 1 else 0
}

/** Краткая карточка контента из результатов поиска. */
data class ModHit(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    val iconUrl: String?,
    val downloads: Long,
    val follows: Long,
    val author: String,
    val categories: List<String>,
    val versions: List<String>
)

/** Конкретная сборка: файл, который можно скачать. */
data class ModVersion(
    val id: String,
    val name: String,
    val versionNumber: String,
    val versionType: String,
    val gameVersions: List<String>,
    val loaders: List<String>,
    val fileName: String,
    val fileUrl: String,
    val fileSize: Long
)

/** Одна страница результатов поиска. */
data class SearchPage(
    val items: List<ModHit>,
    val total: Int,
    val offset: Int
) {
    val hasMore: Boolean
        get() = items.isNotEmpty() && offset + items.size < total
}
