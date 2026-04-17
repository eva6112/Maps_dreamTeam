package com.example.tsumaps.food

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable


@Composable
fun FoodScreen(modifier: Modifier = Modifier) {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Кластеры", "Генетический", "Оценка", "Дерево")
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.weight(1f)) {
            when (tabIndex) {
                0 -> ClusterScreen()
                1 -> GeneticScreen()
                2 -> RatingScreen()
                3 -> TreeScreen()
            }
        }
        SecondaryTabRow(
            selectedTabIndex = tabIndex,
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = tabIndex == index,
                    onClick = { tabIndex = index },
                    text = { Text(title) }
                )
            }
        }
    }
}