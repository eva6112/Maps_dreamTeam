package com.example.tsumaps.ant_algorithm

import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

data class RoutePoint(
    val id: Int,
    val name: String,
    val x: Int,
    val y: Int,
    val description: String = ""
)

data class GpsLocation(
    val latitude: Double,
    val longitude: Double
)


data class AntResult(
    val route: List<RoutePoint>,
    val totalDistance: Double,
    val foundAtIteration: Int,
    val startPoint: RoutePoint
)

class AntColony(
    private val selectedPoints: List<RoutePoint>,
    private val userGps: GpsLocation,
    private val numAnts: Int = 10,
    private val alpha: Double = 1.0,
    private val beta: Double = 1.0,
    private val evaporation: Double = 0.7,
    private val iterations: Int = 200
) {

    private val points = selectedPoints.toList()
    private val n = points.size
    private val distances = Array(n) { DoubleArray(n) }
    private val pheromones = Array(n) { DoubleArray(n) { 1.0 } }
    private val startId: Int

    companion object {
        private const val MIN_LAT = 56.464983   // юг
        private const val MAX_LAT = 56.471848   // север
        private const val MIN_LON = 84.941164   // запад
        private const val MAX_LON = 84.953859   // восток
        private const val MAP_SIZE_X= 152
        private const val MAP_SIZE_Y= 150 // 152x150 пикселей
    }

    init {
        // Вычисление матрицы расстояний между точками
        for (i in 0 until n) {
            for (j in 0 until n) {
                if (i == j) {
                    distances[i][j] = 0.0
                } else {
                    val dx = (points[i].x - points[j].x).toDouble()
                    val dy = (points[i].y - points[j].y).toDouble()
                    distances[i][j] = sqrt(dx * dx + dy * dy)
                }
            }
        }

        // Определяем стартовую точку (ближайшую к GPS пользователя)
        startId = findNearestPointToGps()
    }

    private fun gpsToMapCoords(gps: GpsLocation): Pair<Int, Int> {
        val x = ((gps.longitude - MIN_LON) / (MAX_LON - MIN_LON) * MAP_SIZE_X).toInt()
            .coerceIn(0, MAP_SIZE_X-1)

        val y = MAP_SIZE_Y - ((gps.latitude - MIN_LAT) / (MAX_LAT - MIN_LAT) * MAP_SIZE_Y).toInt()
            .coerceIn(0, MAP_SIZE_Y-1)

        return Pair(x, y)
    }
    private fun findNearestPointToGps(): Int {
        val (userX, userY) = gpsToMapCoords(userGps)

        var minDistance = Double.MAX_VALUE
        var closestIndex = 0

        for (i in 0 until n) {
            val dx = (points[i].x - userX).toDouble()
            val dy = (points[i].y - userY).toDouble()
            val distance = sqrt(dx * dx + dy * dy)

            if (distance < minDistance) {
                minDistance = distance
                closestIndex = i
            }
        }

        return closestIndex
    }

    fun findOptimalRoute(): AntResult {
        var bestDistance = Double.MAX_VALUE
        var bestRouteIndices = emptyList<Int>()
        var WhichIteration = 0
        var Iteration = 0
        var ChangeOrNot: Int = 0

        repeat(iterations) { iteration ->
            val allRoutes = mutableListOf<Pair<List<Int>, Double>>()
            Iteration++
            repeat(numAnts) {
                val route = buildRoute()
                val distance = calculateFullCycleDistance(route)
                allRoutes.add(route to distance)

                if (distance < bestDistance) {
                    bestDistance = distance
                    bestRouteIndices = route
                    ChangeOrNot = 1
                }
            }
            updatePheromones(allRoutes)
            if (ChangeOrNot == 1) {
                WhichIteration = Iteration
                ChangeOrNot = 0

            }
        }

            val bestRoutePoints = bestRouteIndices.map { points[it] }
            return AntResult(
                route = bestRoutePoints,
                totalDistance = bestDistance,
                foundAtIteration = WhichIteration,
                startPoint = points[startId]
            )

    }
    private fun buildRoute(): List<Int> {
        val visited = BooleanArray(n)
        val route = mutableListOf<Int>()

        var current = startId
        route.add(current)
        visited[current] = true

        while (route.size < n) {
            val next = selectNextPoint(current, visited)
            route.add(next)
            visited[next] = true
            current = next
        }

        return route
    }

    private fun selectNextPoint(current: Int, visited: BooleanArray): Int {
        val candidates = mutableListOf<Int>()
        val probabilities = mutableListOf<Double>()
        var sum = 0.0

        for (next in 0 until n) {
            if (!visited[next]) {
                candidates.add(next)

                val pheromone = pheromones[current][next].pow(alpha)
                val visibility = (1.0 / distances[current][next]).pow(beta)
                val randomFactor = 0.5 + Random.nextDouble()
                val probability = pheromone * visibility * randomFactor

                probabilities.add(probability)
                sum += probability
            }
        }

        if (candidates.isEmpty()) return -1
        if (sum < 1e-10) return candidates.random()

        var rand = Random.nextDouble() * sum
        for (i in candidates.indices) {
            rand -= probabilities[i]
            if (rand <= 0) return candidates[i]
        }

        return candidates.last()
    }

    private fun calculateFullCycleDistance(route: List<Int>): Double {
        var distance = 0.0

        for (i in 0 until route.size - 1) {
            distance += distances[route[i]][route[i + 1]]
        }

        distance += distances[route.last()][route[0]]

        return distance
    }

    private fun updatePheromones(routes: List<Pair<List<Int>, Double>>) {
        // Испарение
        for (i in 0 until n) {
            for (j in 0 until n) {
                pheromones[i][j] *= (1 - evaporation)
            }
        }

        // Откладывание нового феромона
        for ((route, distance) in routes) {
            val delta = 100.0 / distance

            for (i in 0 until route.size - 1) {
                val from = route[i]
                val to = route[i + 1]
                pheromones[from][to] += delta
                pheromones[to][from] += delta
            }

            val last = route.last()
            val first = route[0]
            pheromones[last][first] += delta
            pheromones[first][last] += delta
        }
    }
}