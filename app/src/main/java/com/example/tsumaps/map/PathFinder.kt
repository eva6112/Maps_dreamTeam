package com.example.tsumaps.map

import kotlin.math.abs

data class Node(
    val x: Int, val y: Int, var g: Int = Int.MAX_VALUE,
    var h: Int = 0, var parent: Node? = null
) {
    val f: Int get() = g + h
}

object PathFinder {
    fun findPath(matrix: Array<IntArray>, start: Pair<Int, Int>, end: Pair<Int, Int>):
            List<Pair<Int, Int>>? {
        val gridHeight = matrix.size
        val gridWidth = matrix[0].size
        val openList = mutableListOf<Node>()
        val closedSet = mutableSetOf<Pair<Int, Int>>()
        val startNode = Node(start.first, start.second, g = 0, h = manhattan(start, end))
        openList.add(startNode)
        while (openList.isNotEmpty()) {
            val current = openList.minByOrNull { it.f }!!
            if (current.x == end.first && current.y == end.second) {
                return reconstructPath(current)
            }
            openList.remove(current)
            closedSet.add(current.x to current.y)
            val neighbors = listOf(0 to -1, 1 to 0, 0 to 1, -1 to 0)
            for (offset in neighbors) {
                val nx = current.x + offset.first
                val ny = current.y + offset.second
                if (nx !in 0 until gridWidth || ny !in 0 until gridHeight ||
                    matrix[ny][nx] == 1 || (nx to ny) in closedSet
                )
                    continue
                val tentativeG = current.g + 1
                var neighborNode = openList.find { it.x == nx && it.y == ny }
                if (neighborNode == null) {
                    neighborNode = Node(nx, ny)
                    if (tentativeG < neighborNode.g) {
                        neighborNode.parent = current
                        neighborNode.g = tentativeG
                        neighborNode.h = manhattan(nx to ny, end)
                        openList.add(neighborNode)
                    }
                } else if (tentativeG < neighborNode.g) {
                    neighborNode.parent = current
                    neighborNode.g = tentativeG
                }
            }
        }
        return null
    }

    private fun manhattan(a: Pair<Int, Int>, b: Pair<Int, Int>): Int {
        return abs(a.first - b.first) + abs(a.second - b.second)
    }

    private fun reconstructPath(node: Node?): List<Pair<Int, Int>> {
        val path = mutableListOf<Pair<Int, Int>>()
        var curr = node
        while (curr != null) {
            path.add(curr.x to curr.y)
            curr = curr.parent
        }
        return path.reversed()
    }
}