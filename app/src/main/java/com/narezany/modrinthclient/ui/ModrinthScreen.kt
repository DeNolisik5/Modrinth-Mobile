package com.narezany.modrinthclient.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.narezany.modrinthclient.R
import com.narezany.modrinthclient.data.ModHit
import com.narezany.modrinthclient.data.ModVersion
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModrinthScreen(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // На Android 8–9 запись в Downloads требует разрешения.
    var pendingVersion by remember { mutableStateOf<ModVersion?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val version = pendingVersion
        pendingVersion = null
        if (granted && version != null) viewModel.download(context, version)
    }

    val startDownload: (ModVersion) -> Unit = { version ->
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED

        if (needsPermission) {
            pendingVersion = version
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            viewModel.download(context, version)
        }
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Header()

            SearchField(
                query = state.query,
                onQueryChange = viewModel::updateQuery,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(12.dp))

            VersionChips(
                selected = state.gameVersion,
                onSelect = viewModel::setGameVersion
            )

            Spacer(Modifier.height(4.dp))

            Box(Modifier.weight(1f)) {
                Results(
                    state = state,
                    onOpen = viewModel::openMod,
                    onRetry = viewModel::retry
                )
            }
        }
    }

    val selected = state.selected
    if (selected != null) {
        ModDetailsSheet(
            mod = selected,
            state = state,
            onDismiss = viewModel::closeMod,
            onDownload = startDownload
        )
    }
}

// ------------------------------------------------------------------ шапка

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "Modrinth",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(ModrinthGreen)
            )
        }
        Text(
            text = "Моды для Minecraft",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ------------------------------------------------------------------ поиск

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Найти мод…") },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Очистить"
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
private fun VersionChips(
    selected: String?,
    onSelect: (String?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("Все версии") }
            )
        }
        items(AppViewModel.GAME_VERSIONS) { version ->
            FilterChip(
                selected = selected == version,
                onClick = { onSelect(if (selected == version) null else version) },
                label = { Text(version) }
            )
        }
    }
}

// ---------------------------------------------------------------- список

@Composable
private fun Results(
    state: ModrinthUiState,
    onOpen: (ModHit) -> Unit,
    onRetry: () -> Unit
) {
    when {
        state.isLoading && state.results.isEmpty() -> CenterBox {
            CircularProgressIndicator()
        }

        state.error != null && state.results.isEmpty() -> CenterBox {
            MessageBlock(
                title = "Ошибка сети",
                subtitle = state.error,
                actionLabel = "Повторить",
                onAction = onRetry
            )
        }

        state.results.isEmpty() -> CenterBox {
            MessageBlock(
                title = "Ничего не нашлось",
                subtitle = "Попробуй другой запрос или сними фильтр версии",
                actionLabel = null,
                onAction = null
            )
        }

        else -> LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(state.results) { _, mod ->
                ModCard(mod = mod, onClick = { onOpen(mod) })
            }
        }
    }
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun MessageBlock(
    title: String,
    subtitle: String?,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    Column(
        modifier = Modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun ModCard(mod: ModHit, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            ModIcon(url = mod.iconUrl, title = mod.title, size = 60.dp)

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = mod.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = mod.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "↓ ${formatCount(mod.downloads)}  ·  ${mod.author}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ModIcon(url: String?, title: String, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = title.take(1).uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// -------------------------------------------------------------- шторка

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModDetailsSheet(
    mod: ModHit,
    state: ModrinthUiState,
    onDismiss: () -> Unit,
    onDownload: (ModVersion) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp)
        ) {
            item { SheetHeader(mod) }

            item {
                Spacer(Modifier.height(20.dp))
                Text(text = "Версии", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
            }

            when {
                state.versionsLoading -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.versionsError != null -> item {
                    MessageBlock(
                        title = "Не удалось загрузить версии",
                        subtitle = state.versionsError,
                        actionLabel = null,
                        onAction = null
                    )
                }

                state.versions.isEmpty() -> item {
                    Text(
                        text = "У этого мода пока нет опубликованных версий.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> items(state.versions) { version ->
                    VersionRow(
                        version = version,
                        download = state.downloads[version.id],
                        onDownload = onDownload
                    )
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Файлы сохраняются в папку Downloads/ModrinthClient",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SheetHeader(mod: ModHit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ModIcon(url = mod.iconUrl, title = mod.title, size = 76.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = mod.title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "от ${mod.author}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(Modifier.height(14.dp))

    Text(
        text = mod.description,
        style = MaterialTheme.typography.bodyMedium
    )

    Spacer(Modifier.height(16.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatPill("Скачиваний", formatCount(mod.downloads))
        StatPill("Подписок", formatCount(mod.follows))
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun VersionRow(
    version: ModVersion,
    download: DownloadState?,
    onDownload: (ModVersion) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = version.versionNumber,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            val mc = version.gameVersions.firstOrNull()
                            if (mc != null) append(mc)
                            if (version.loaders.isNotEmpty()) {
                                if (isNotEmpty()) append(" · ")
                                append(version.loaders.joinToString(", "))
                            }
                            if (isNotEmpty()) append(" · ")
                            append(formatSize(version.fileSize))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(12.dp))

                DownloadButton(state = download, onClick = { onDownload(version) })
            }

            if (download is DownloadState.Running) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { download.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            if (download is DownloadState.Failed) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = download.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DownloadButton(state: DownloadState?, onClick: () -> Unit) {
    when (state) {
        is DownloadState.Done -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Готово",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        is DownloadState.Running -> {
            Text(
                text = "${(state.progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        is DownloadState.Failed -> {
            TextButton(onClick = onClick) { Text("Повторить") }
        }

        null -> {
            FilledTonalButton(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_download),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Скачать", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ------------------------------------------------------------- утилиты

private fun formatCount(value: Long): String = when {
    value >= 1_000_000 -> String.format(Locale.getDefault(), "%.1f млн", value / 1_000_000.0)
    value >= 1_000 -> String.format(Locale.getDefault(), "%.0f тыс.", value / 1_000.0)
    else -> value.toString()
}

private fun formatSize(bytes: Long): String = when {
    bytes <= 0 -> "—"
    bytes >= 1_048_576 -> String.format(Locale.getDefault(), "%.1f МБ", bytes / 1_048_576.0)
    bytes >= 1_024 -> String.format(Locale.getDefault(), "%.0f КБ", bytes / 1_024.0)
    else -> "$bytes Б"
}
