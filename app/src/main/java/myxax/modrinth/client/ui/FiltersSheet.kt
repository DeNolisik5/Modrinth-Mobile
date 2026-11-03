package myxax.modrinth.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import myxax.modrinth.client.data.ModCategory
import myxax.modrinth.client.data.ModLoader
import myxax.modrinth.client.data.SearchFilters
import myxax.modrinth.client.data.SortOption

/**
 * Шторка с расширенными фильтрами.
 *
 * Правки копятся локально и применяются только по кнопке «Применить» —
 * иначе каждый тап по чипу перезапускал бы поиск.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersSheet(
    current: SearchFilters,
    versions: List<String>,
    onDismiss: () -> Unit,
    onApply: (SearchFilters) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var loaders by remember { mutableStateOf(current.loaders) }
    var categories by remember { mutableStateOf(current.categories) }
    var sort by remember { mutableStateOf(current.sort) }
    var gameVersion by remember { mutableStateOf(current.gameVersion) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Фильтры",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(20.dp))
            SectionTitle("Версия Minecraft")
            ChipFlow {
                FilterChip(
                    selected = gameVersion == null,
                    onClick = { gameVersion = null },
                    label = { Text("Любая") }
                )
                versions.take(60).forEach { version ->
                    FilterChip(
                        selected = gameVersion == version,
                        onClick = {
                            gameVersion = if (gameVersion == version) null else version
                        },
                        label = { Text(version) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("Загрузчик")
            ChipFlow {
                ModLoader.entries.forEach { loader ->
                    FilterChip(
                        selected = loader in loaders,
                        onClick = {
                            loaders = if (loader in loaders) loaders - loader else loaders + loader
                        },
                        label = { Text(loader.label) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("Категории")
            ChipFlow {
                ModCategory.entries.forEach { category ->
                    FilterChip(
                        selected = category in categories,
                        onClick = {
                            categories = if (category in categories) {
                                categories - category
                            } else {
                                categories + category
                            }
                        },
                        label = { Text(category.label) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("Сортировка")
            ChipFlow {
                SortOption.entries.forEach { option ->
                    FilterChip(
                        selected = sort == option,
                        onClick = { sort = option },
                        label = { Text(option.label) }
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        loaders = emptySet()
                        categories = emptySet()
                        sort = SortOption.RELEVANCE
                        gameVersion = null
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Сбросить")
                }

                Button(
                    onClick = {
                        onApply(
                            SearchFilters(
                                loaders = loaders,
                                categories = categories,
                                sort = sort,
                                gameVersion = gameVersion
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Применить")
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(10.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipFlow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}
