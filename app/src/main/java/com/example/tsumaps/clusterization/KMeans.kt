package com.example.tsumaps.clusterization

import kotlin.math.ceil
import kotlin.math.sqrt

// 1. Класс кминс (заведения, кол-во кластеров, итерации)
class KMeans(
    private val places: List<Place>, private val k: Int,
    private val maxIterations: Int = 100
) {
    private lateinit var centroids: List<Centroid>

    // 1.1. Создание центроидов
    private fun initializeCentroidsByZone() {
        if (places.isEmpty())
            return

        val minX = places.minOf { it.x }
        val maxX = places.maxOf { it.x }
        val minY = places.minOf { it.y }
        val maxY = places.maxOf { it.y }

        val newCentroids = mutableListOf<Centroid>()
        val tempPlaces =
            places.toMutableList()                                                     //копия для удаления использованных заведений

        val cols = ceil(sqrt(k.toDouble())).toInt()
        val rows = ceil(k.toDouble() / cols).toInt()

        val zoneWidth =
            (maxX - minX) / cols                                                        //ширина зоны
        val zoneHeight =
            (maxY - minY) / rows                                                       //высота зоны

        for (i in 0 until rows) {
            for (j in 0 until cols) {
                if (newCentroids.size >= k)
                    break

                val zoneCenterX =
                    minX + j * zoneWidth + zoneWidth / 2                              //х центра зоны (j * zoneWidth = смещение до левого края ячейки)
                val zoneCenterY =
                    minY + i * zoneHeight + zoneHeight / 2                            //y центра зоны

                val zoneCenterPoint = Place("temp", zoneCenterX, zoneCenterY)
                val nearestPlace = tempPlaces.minByOrNull {
                    getDistance(
                        it,
                        Centroid(zoneCenterPoint.x, zoneCenterPoint.y)
                    )
                }

                if (nearestPlace != null) {
                    newCentroids.add(Centroid(nearestPlace.x, nearestPlace.y))
                    tempPlaces.remove(nearestPlace)                                                 //удаляем точку из будущего выбора
                }
            }
        }

        if (newCentroids.size < k && tempPlaces.isNotEmpty()) {
            val remainingNeeded = k - newCentroids.size
            newCentroids.addAll(
                tempPlaces.shuffled().take(remainingNeeded).map { Centroid(it.x, it.y) })
        }

        this.centroids = newCentroids
    }

    // 1.2. Распределние центроидов
    private fun assignToClusters(): Map<Centroid, MutableList<Place>> {
        val clusters = mutableMapOf<Centroid, MutableList<Place>>()
        centroids.forEach {
            clusters[it] = mutableListOf()
        }                                        //для каждого центроида - пустой список мест

        for (place in places) {
            val nearestCentroid = centroids.minByOrNull { getDistance(place, it) }!!
            clusters[nearestCentroid]?.add(place)
        }
        return clusters
    }

    // 1.3. Обновление центроидов
    private fun updateCentroids(clusters: Map<Centroid, List<Place>>): Boolean {
        var centroidsMoved = false

        for ((centroid, clusterPlaces) in clusters) {
            if (clusterPlaces.isNotEmpty()) {
                val newX = clusterPlaces.map { it.x }
                    .average()                                     //среднее значение
                val newY = clusterPlaces.map { it.y }.average()

                if (centroid.x != newX || centroid.y != newY) {
                    centroidsMoved = true
                    centroid.x = newX
                    centroid.y = newY
                }
            }
        }
        return centroidsMoved
    }

    // 1.4. Запуск кластеризации
    fun run(): Map<Centroid, List<Place>> {
        if (places.isEmpty() || k <= 0)
            return emptyMap()

        initializeCentroidsByZone()

        var clusters: Map<Centroid, List<Place>> = emptyMap()

        for (i in 0 until maxIterations) {
            clusters = assignToClusters()
            val centroidsMoved = updateCentroids(clusters)
            if (!centroidsMoved)
                break
        }

        return clusters
    }
}
