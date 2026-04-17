package com.example.tsumaps.geneticAlgorithm

import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

object MapData {
    const val TOP_LEFT_LAT = 56.471848
    const val TOP_LEFT_LON = 84.941164
    const val BOTTOM_RIGHT_LAT = 56.464983
    const val BOTTOM_RIGHT_LON = 84.953859
    const val ROWS = 150
    const val COLS = 152

    val LAT_STEP = (TOP_LEFT_LAT - BOTTOM_RIGHT_LAT) / (ROWS - 1)
    val LON_STEP = (BOTTOM_RIGHT_LON - TOP_LEFT_LON) / (COLS - 1)
    val CELL_SIZE_KM = 0.0051
}

fun gpsToPoint(lat: Double, lon: Double): Point {
    val x = ((lon - MapData.TOP_LEFT_LON) / MapData.LON_STEP).toInt()
        .coerceIn(0, MapData.COLS - 1)
    val y = ((MapData.TOP_LEFT_LAT - lat) / MapData.LAT_STEP).toInt()
        .coerceIn(0, MapData.ROWS - 1)
    return Point(x.toDouble(), y.toDouble())
}

data class Point(val x: Double, val y: Double)

data class RouteNode(val shop: Shop, val itemsBought: List<String>)

data class OptimizationResult(
    val route: List<RouteNode>,
    val totalDistance: Double,
    val timePenalty: Double,
    val fitness: Double
)

class Individual(
    val route: List<RouteNode>,
    private val startPoint: Point,
    private val startTimeMins: Int
) {
    var totalDistance: Double = 0.0
    var timePenalty: Double = 0.0

    init {
        var currentTime = startTimeMins.toDouble()
        val speedKmph = 5.0
        val speedKmpm = speedKmph / 60.0
        var currentPoint = startPoint

        for (i in route.indices) {
            val shop = route[i].shop
            val shopPoint = Point(shop.x.toDouble(), shop.y.toDouble())

            val dCells = sqrt(
                (shopPoint.x - currentPoint.x).pow(2) +
                        (shopPoint.y - currentPoint.y).pow(2)
            )

            val dKm = dCells * MapData.CELL_SIZE_KM
            totalDistance += dKm

            val travelTimeMinutes = dKm / speedKmpm
            currentTime += travelTimeMinutes

            if (currentTime < shop.openTime || currentTime > shop.closeTime) {
                timePenalty += 2000.0
            }

            currentTime += 5.0
            currentPoint = shopPoint
        }

        // ВОЗВРАТ В СТАРТОВУЮ ТОЧКУ
        val returnDistanceCells = sqrt(
            (startPoint.x - currentPoint.x).pow(2) +
                    (startPoint.y - currentPoint.y).pow(2)
        )
        val returnDistanceKm = returnDistanceCells * MapData.CELL_SIZE_KM
        totalDistance += returnDistanceKm
    }

    val fitness: Double get() = 1.0 / (totalDistance + timePenalty + 1.0)

    fun toResult(): OptimizationResult = OptimizationResult(
        route = route,
        totalDistance = totalDistance,
        timePenalty = timePenalty,
        fitness = fitness
    )
}

class GeneticAlgorithm(
    private val targetItems: Set<String>,
    private val userLocation: Point,
    private val userStartTime: Int,
    private val populationSize: Int = 80,
    private val mutationRate: Double = 0.2,
    private val eliteCount: Int = 1
) {
    private var population = mutableListOf<Individual>()

    init {
        repeat(populationSize) { population.add(generateRandomIndividual()) }
    }

    fun getBest(): OptimizationResult = population.maxByOrNull { it.fitness }?.toResult()
        ?: population[0].toResult()

    fun evolve(): OptimizationResult {
        val nextPop = mutableListOf<Individual>()

        val elites = population.sortedByDescending { it.fitness }.take(eliteCount)
        nextPop.addAll(elites)

        while (nextPop.size < populationSize) {
            val parent1 = tournament()
            val parent2 = tournament()

            var childRoute = crossover(parent1, parent2)

            if (Random.nextDouble() < mutationRate) {
                childRoute = mutate(childRoute)
            }

            nextPop.add(repair(childRoute))
        }

        population = nextPop
        return getBest()
    }

    fun optimize(
        generations: Int,
        onProgress: ((generation: Int, best: OptimizationResult) -> Unit)? = null
    ): OptimizationResult {
        var best = getBest()
        onProgress?.invoke(0, best)

        for (generation in 1..generations) {
            best = evolve()
            onProgress?.invoke(generation, best)
        }

        return best
    }

    private fun tournament(): Individual {
        return List(5) { population.random() }.maxBy { it.fitness }
    }

    private fun crossover(parent1: Individual, parent2: Individual): List<RouteNode> {
        if (parent1.route.isEmpty()) return parent2.route

        val cut = Random.nextInt(parent1.route.size)
        val newRoute = parent1.route.take(cut).toMutableList()

        parent2.route.forEach { node ->
            if (newRoute.none { it.shop.id == node.shop.id }) {
                newRoute.add(node)
            }
        }

        return newRoute
    }

    private fun mutate(route: List<RouteNode>): List<RouteNode> {
        val result = route.toMutableList()
        if (result.size >= 2) {
            val i = result.indices.random()
            val j = result.indices.random()
            val temp = result[i]
            result[i] = result[j]
            result[j] = temp
        }
        return result
    }

    private fun repair(route: List<RouteNode>): Individual {
        val finalNodes = mutableListOf<RouteNode>()
        val remaining = targetItems.toMutableSet()

        val processedRoute = if (Random.nextDouble() < 0.3) {
            val startIndex = Random.nextInt(route.size)
            route.subList(startIndex, route.size) + route.subList(0, startIndex)
        } else {
            route
        }

        for (node in processedRoute) {
            if (remaining.isEmpty()) break
            val canBuy = node.shop.items.intersect(remaining)
            if (canBuy.isNotEmpty()) {
                finalNodes.add(RouteNode(node.shop, canBuy.toList()))
                remaining.removeAll(canBuy)
            }
        }

        while (remaining.isNotEmpty()) {
            val needed = remaining.first()
            val shop = ShopRegistry.shops.firstOrNull { it.items.contains(needed) } ?: break
            val canBuy = shop.items.intersect(remaining)
            finalNodes.add(RouteNode(shop, canBuy.toList()))
            remaining.removeAll(canBuy)
        }

        return Individual(finalNodes, userLocation, userStartTime)
    }

    private fun generateRandomIndividual(): Individual {
        val shuffledShops = ShopRegistry.shops.shuffled()
        return repair(shuffledShops.map { RouteNode(it, emptyList()) })
    }
}