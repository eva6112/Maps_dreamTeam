package com.example.tsumaps.food

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.runtime.*
import com.google.android.gms.location.LocationServices
import com.google.accompanist.permissions.rememberPermissionState
import java.time.LocalTime
import android.Manifest
import androidx.compose.ui.geometry.Offset
import com.example.tsumaps.geneticAlgorithm.*
import android.annotation.SuppressLint
import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.tsumaps.R
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import com.google.accompanist.permissions.isGranted
import java.util.Locale
import androidx.compose.ui.text.font.FontWeight

@OptIn( ExperimentalPermissionsApi::class)
@Composable
fun GeneticScreen() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var mapOffset by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableFloatStateOf(0.6f) }
    var userGridPoint by remember { mutableStateOf<Point?>(null) }
    var selectedItems by remember { mutableStateOf(setOf<String>()) }
    var bestResult by remember { mutableStateOf<OptimizationResult?>(null) }
    var currentGen by remember { mutableIntStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }
    val allPossibleItems = remember {
        ShopRegistry.shops.flatMap { it.items }.distinct().sorted()
    }

    @SuppressLint("MissingPermission")
    fun updateLocationAndRun() {
        bestResult = null
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            userGridPoint = location?.let { gpsToPoint(it.latitude, it.longitude) } ?: Point(75.0, 75.0)
            isRunning = true
        }
    }

    LaunchedEffect(isRunning) {
        if (isRunning && selectedItems.isNotEmpty()) {
            val currentTimeMins = LocalTime.now().let { it.hour * 60 + it.minute }
            val ga = GeneticAlgorithm(
                targetItems = selectedItems,
                userLocation = userGridPoint ?: Point(75.0, 75.0),
                userStartTime = currentTimeMins,
                populationSize = 100
            )
            withContext(Dispatchers.Default) {
                ga.optimize(200, onProgress = { gen, result ->
                    currentGen = gen
                    bestResult = result
                })
            }
            isRunning = false
            showResultDialog = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary)) {
        Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectTransformGestures { centroid, pan, zoomMultiplier, _ ->
                val oldZoom = zoom
                val newZoom = (oldZoom * zoomMultiplier).coerceIn(0.1f, 5f)
                val centroidOnMapX = (centroid.x - mapOffset.x) / oldZoom
                val centroidOnMapY = (centroid.y - mapOffset.y) / oldZoom
                mapOffset = Offset(centroid.x - centroidOnMapX * newZoom, centroid.y - centroidOnMapY * newZoom)
                zoom = newZoom
                mapOffset += pan
            }
        }) {
            Box(modifier = Modifier
                .offset { IntOffset(mapOffset.x.roundToInt(), mapOffset.y.roundToInt()) }
                .requiredSize(3040.dp * zoom, 3000.dp * zoom)
            ) {
                Image(painter = painterResource(id = R.drawable.map_color), contentDescription = "Карта", modifier = Modifier.fillMaxSize())
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cellW = size.width / 152
                    val cellH = size.height / 150
                    userGridPoint?.let { drawCircle(Color.Red, 15f * zoom, Offset(it.x.toFloat() * cellW, it.y.toFloat() * cellH)) }
                    bestResult?.let { result ->
                        val routePath = Path()
                        val start = userGridPoint ?: Point(75.0, 75.0)
                        routePath.moveTo(start.x.toFloat() * cellW, start.y.toFloat() * cellH)
                        result.route.forEach { node ->
                            val shopPt = Offset(node.shop.x.toFloat() * cellW, node.shop.y.toFloat() * cellH)
                            routePath.lineTo(shopPt.x, shopPt.y)
                            drawCircle(Color.Green, 15f * zoom, shopPt)
                        }
                        drawPath(routePath, Color.Black, style = Stroke(12f * zoom))
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
                .padding(top = if (isLandscape) 4.dp else 30.dp, bottom = 8.dp)
        ) {
            Text("Выберите продукты:", modifier = Modifier.padding(horizontal = 16.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                items(allPossibleItems) { item ->
                    FilterChip(
                        selected = selectedItems.contains(item),
                        onClick = { selectedItems = if (selectedItems.contains(item)) selectedItems - item
                        else selectedItems + item },
                        label = { Text(item, fontSize = 10.sp) },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isLandscape) 48.dp else 64.dp)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (locationPermissionState.status.isGranted) updateLocationAndRun()
                        else locationPermissionState.launchPermissionRequest()
                    },
                    enabled = selectedItems.isNotEmpty() && !isRunning,
                    modifier = Modifier.fillMaxHeight().weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (isRunning) "Оптимизация: $currentGen/200" else "Построить маршрут",
                        fontSize = if (isLandscape) 12.sp else 14.sp
                    )
                }

                if (bestResult != null && !isRunning) {
                    Button(
                        onClick = { showResultDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.fillMaxHeight().weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Путь",
                            fontSize = if (isLandscape) 12.sp else 14.sp
                        )
                    }
                }
            }
        }

        if (showResultDialog && bestResult != null) {
            AlertDialog(
                onDismissRequest = { showResultDialog = false },
                confirmButton = {
                    TextButton(onClick = { showResultDialog = false }) { Text("ПОНЯТНО") }
                },
                title = { Text("Оптимальный путь" )},
                text = {
                    Column {
                        Text(
                            "Маршрут: ${bestResult!!.route.joinToString(" -> ") { it.shop.name }}",
                            fontSize = 16.sp,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = String.format(Locale.US, "Общая дистанция: %.2f км", bestResult!!.totalDistance),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    }
}