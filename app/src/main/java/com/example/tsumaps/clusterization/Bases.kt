package com.example.tsumaps.clusterization

import kotlin.math.pow
import kotlin.math.sqrt

// 1. Класс для информации о заведении
data class Place(
    val name: String,
    val x: Double,
    val y: Double
)

// 2. Класс, для координат центроида
data class Centroid(
    var x: Double,
    var y: Double
)

// 3. Функция для расчета расстояния от точки до центроида
fun getDistance(place: Place, centroid: Centroid): Double {
    return sqrt((place.x - centroid.x).pow(2) + (place.y - centroid.y).pow(2))
}

