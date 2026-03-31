package com.example.tsumaps.map

import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.tsumaps.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Text

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val matrix = remember { MapLoader.loadMatrix(context) }
    var mapOffset by remember { mutableStateOf(Offset.Zero) }
    var debugInfo by remember { mutableStateOf("Нажми на карту") }
    val gridWidth = 152
    val gridHeight = 150
    val mapWidthDp = 3040.dp
    val mapHeightDp = 3000.dp
    Box(modifier = Modifier.fillMaxSize().background(Color.Gray)) {
        Image(
            painter = painterResource(id = R.drawable.map_color),
            contentDescription = "Карта",
            alignment = Alignment.TopStart,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .offset { IntOffset(mapOffset.x.roundToInt(), mapOffset.y.roundToInt()) }
                .requiredSize(mapWidthDp, mapHeightDp)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, _, _ ->
                        mapOffset += pan
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { pressOffset ->
                        val mapWidthPx = size.width.toFloat()
                        val mapHeightPx = size.height.toFloat()
                        val relativeX = pressOffset.x / mapWidthPx
                        val relativeY = pressOffset.y / mapHeightPx
                        val gridX = (relativeX * gridWidth).toInt()
                        val gridY = (relativeY * gridHeight).toInt()
                        if (gridX in 0 until gridWidth && gridY in 0 until gridHeight) {
                            val type = if (matrix[gridY][gridX] == 1) "СТЕНА" else "ПУТЬ"
                            debugInfo = "Сетка: [$gridX, $gridY] | $type"
                        } else {
                            debugInfo = "Вне сетки: $gridX, $gridY"
                        }
                    }
                }
        )
        Box(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp)
        ) {
            Text(text = debugInfo, color = Color.White, modifier = Modifier.padding(8.dp))
        }
    }
}