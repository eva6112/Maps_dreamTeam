package com.example.tsumaps.map

import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.tsumaps.R
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val matrix = remember { MapLoader.loadMatrix(context) }
    var mapOffset by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableFloatStateOf(0.6f) }
    var startPoint by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var endPoint by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var path by remember { mutableStateOf<List<Pair<Int, Int>>?>(null) }
    var debugInfo by remember { mutableStateOf("Выберите точку старта") }
    val gridWidth = 152
    val gridHeight = 150
    val mapWidthDp = 3040.dp
    val mapHeightDp = 3000.dp
    val scaledWidth = mapWidthDp * zoom
    val scaledHeight = mapHeightDp * zoom

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoomMultiplier, _ ->
                    val oldZoom = zoom
                    val newZoom = (oldZoom * zoomMultiplier).coerceIn(0.1f, 5f)
                    val centroidOnMapX = (centroid.x - mapOffset.x) / oldZoom
                    val centroidOnMapY = (centroid.y - mapOffset.y) / oldZoom
                    val newMapOffsetX = centroid.x - centroidOnMapX * newZoom
                    val newMapOffsetY = centroid.y - centroidOnMapY * newZoom
                    mapOffset = Offset(newMapOffsetX, newMapOffsetY)
                    zoom = newZoom
                    mapOffset += pan
                }
            }
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(mapOffset.x.roundToInt(), mapOffset.y.roundToInt()) }
                .requiredSize(scaledWidth, scaledHeight)
                .pointerInput(Unit) {
                    detectTapGestures { pressOffset ->
                        val gridX = ((pressOffset.x / size.width) * gridWidth).toInt()
                        val gridY = ((pressOffset.y / size.height) * gridHeight).toInt()
                        if (gridX in 0 until gridWidth && gridY in 0 until gridHeight) {
                            if (matrix[gridY][gridX] == 1) {
                                debugInfo = "Это препятствие!"
                            } else {
                                if (startPoint == null || (startPoint != null && endPoint != null)) {
                                    startPoint = gridX to gridY
                                    endPoint = null
                                    path = null
                                    debugInfo = "Старт: [$gridX, $gridY]. Выберите финиш."
                                } else {
                                    endPoint = gridX to gridY
                                    val result =
                                        PathFinder.findPath(matrix, startPoint!!, endPoint!!)
                                    if (result != null) {
                                        path = result
                                        debugInfo = "Маршрут построен!"
                                    } else {
                                        path = null
                                        debugInfo = "Маршрута не существует"
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            Image(
                painter = painterResource(id = R.drawable.map_color),
                contentDescription = "Карта",
                modifier = Modifier.fillMaxSize()
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellWidth = size.width / gridWidth
                val cellHeight = size.height / gridHeight
                path?.forEach { (x, y) ->
                    drawRect(
                        color = Color(0xFF0072BC).copy(alpha = 0.5f),
                        topLeft = Offset(x * cellWidth, y * cellHeight),
                        size = Size(cellWidth, cellHeight)
                    )
                }
                startPoint?.let { (x, y) ->
                    drawCircle(
                        Color.Green,
                        radius = 15f,
                        center = Offset(
                            x * cellWidth + cellWidth / 2,
                            y * cellHeight + cellHeight / 2
                        )
                    )
                }
                endPoint?.let { (x, y) ->
                    drawCircle(
                        Color.Red,
                        radius = 15f,
                        center = Offset(
                            x * cellWidth + cellWidth / 2,
                            y * cellHeight + cellHeight / 2
                        )
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (isLandscape) 8.dp else 40.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Text(text = debugInfo, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}