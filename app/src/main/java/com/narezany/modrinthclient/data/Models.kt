package com.narezany.modrinthclient.data

/** Краткая карточка мода из результатов поиска. */
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

/** Конкретная сборка мода: файл, который можно скачать. */
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
