package com.example.tsumaps.food

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.*


@Composable
fun FoodScreen(modifier: Modifier = Modifier) {
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Кластеры", "Генетический", "Оценка")
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.weight(1f)) {
            when (tabIndex) {
                0 -> ClusterScreen()
                1 -> GeneticScreen()
                2 -> RatingScreen()
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