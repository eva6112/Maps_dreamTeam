package com.example.tsumaps.navigation

import com.example.tsumaps.R

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    MAP("Маршруты", R.drawable.ic_map),
    FOOD("Еда", R.drawable.ic_food),
    TOUR("Экскурсии", R.drawable.ic_tour),
}