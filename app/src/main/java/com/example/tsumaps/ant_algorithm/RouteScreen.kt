package com.example.tsumaps.ant_algorithm

import android.Manifest
import android.content.res.Configuration
import android.location.Location
import android.widget.Toast
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Цветовая схема ТГУ
val TsuBlue = Color(0xFF0072BC)
val TsuDarkBlue = Color(0xFF023B61)
val TsuLightBlue = Color(0xFF1397F1)
val TsuWhite = Color(0xFFFFFFFF)
val TsuDark = Color(0xFF021521)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RouteScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var selectedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var currentGps by remember { mutableStateOf<GpsLocation?>(null) }
    var isLocating by remember { mutableStateOf(false) }
    var isCalculating by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<AntResult?>(null) }

    var listHeightPercent by remember { mutableFloatStateOf(0.5f) }

    val listState = rememberLazyListState()
    val resultScrollState = rememberScrollState()

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    val attractions = Attractions.allAttractions

    fun getCurrentLocation() {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
            return
        }

        isLocating = true
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    currentGps = GpsLocation(it.latitude, it.longitude)
                    Toast.makeText(context, "Местоположение определено", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(context, "GPS не определён, используем демо-точку", Toast.LENGTH_SHORT).show()
                    currentGps = GpsLocation(56.466000, 84.949000)
                }
                isLocating = false
            }.addOnFailureListener {
                Toast.makeText(context, "Ошибка GPS, используем демо-точку", Toast.LENGTH_SHORT).show()
                currentGps = GpsLocation(56.466000, 84.949000)
                isLocating = false
            }
        } else {
            isLocating = false
        }
    }

    fun findRoute() {
        if (selectedIds.isEmpty()) {
            Toast.makeText(context, "Выберите хотя бы одну достопримечательность", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentGps == null) {
            Toast.makeText(context, "GPS не определён, используем демо-точку", Toast.LENGTH_SHORT).show()
            currentGps = GpsLocation(56.466000, 84.949000)
        }

        val selectedPoints = Attractions.getPointsByIds(selectedIds.toList())

        isCalculating = true
        result = null

        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val antColony = AntColony(
                        selectedPoints = selectedPoints,
                        userGps = currentGps!!,
                        numAnts = 15,
                        iterations = 80
                    )
                    val routeResult = antColony.findOptimalRoute()

                    withContext(Dispatchers.Main) {
                        result = routeResult
                        isCalculating = false
                        Toast.makeText(context, "Маршрут найден на итерации ${routeResult.foundAtIteration}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isCalculating = false
                        Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    if (isLandscape) {
        LandscapeRouteScreen(
            attractions = attractions,
            selectedIds = selectedIds,
            onSelectedIdsChange = { selectedIds = it },
            currentGps = currentGps,
            isLocating = isLocating,
            isCalculating = isCalculating,
            result = result,
            onGetLocation = { getCurrentLocation() },
            onFindRoute = { findRoute() },
            onClear = { selectedIds = emptySet() }
        )
    } else {
        PortraitRouteScreen(
            attractions = attractions,
            selectedIds = selectedIds,
            onSelectedIdsChange = { selectedIds = it },
            currentGps = currentGps,
            isLocating = isLocating,
            isCalculating = isCalculating,
            result = result,
            onGetLocation = { getCurrentLocation() },
            onFindRoute = { findRoute() },
            onClear = { selectedIds = emptySet() },
            listHeightPercent = listHeightPercent,
            onListHeightChange = { listHeightPercent = it }
        )
    }
}

// Портретная разметка с оптимизированным растягиванием
@Composable
fun PortraitRouteScreen(
    attractions: List<RoutePoint>,
    selectedIds: Set<Int>,
    onSelectedIdsChange: (Set<Int>) -> Unit,
    currentGps: GpsLocation?,
    isLocating: Boolean,
    isCalculating: Boolean,
    result: AntResult?,
    onGetLocation: () -> Unit,
    onFindRoute: () -> Unit,
    onClear: () -> Unit,
    listHeightPercent: Float,
    onListHeightChange: (Float) -> Unit
) {
    val listState = rememberLazyListState()
    val resultScrollState = rememberScrollState()

    var isDragging by remember { mutableStateOf(false) }
    var localHeight by remember { mutableFloatStateOf(listHeightPercent) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = TsuWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            AppHeader()

            Text(
                text = "Выберите достопримечательности:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TsuDarkBlue,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Блок со списком
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(localHeight)
                    .padding(bottom = 4.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = TsuWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(attractions) { attraction ->
                            AttractionListItem(
                                attraction = attraction,
                                isSelected = selectedIds.contains(attraction.id),
                                onToggle = {
                                    onSelectedIdsChange(
                                        if (selectedIds.contains(attraction.id)) {
                                            selectedIds.minus(attraction.id)
                                        } else {
                                            selectedIds.plus(attraction.id)
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Разделитель с ползунком для перетаскивания
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(vertical = 4.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                            },
                            onDragEnd = {
                                isDragging = false
                                onListHeightChange(localHeight)
                            },
                            onDragCancel = {
                                isDragging = false
                                onListHeightChange(localHeight)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val delta = dragAmount.y / 800f
                                val newHeight = (localHeight + delta).coerceIn(0.15f, 0.75f)
                                localHeight = newHeight
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(6.dp)
                            .background(
                                color = if (isDragging) TsuBlue else TsuLightBlue,
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                }
            }

            Text(
                text = "📊 РЕЗУЛЬТАТ:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TsuDarkBlue,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Блок с результатом
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f - 0.2f - localHeight)
                    .padding(bottom = 4.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = TsuWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    ResultContent(
                        isCalculating = isCalculating,
                        result = result,
                        scrollState = resultScrollState
                    )
                }
            }

            SelectionInfoCard(selectedIds.size, attractions.size)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onClear,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = TsuDark)
                ) {
                    Text("🗑️ Очистить", color = TsuWhite)
                }

                Button(
                    onClick = onGetLocation,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = TsuBlue),
                    enabled = !isLocating
                ) {
                    Text(if (isLocating) "⏳ Поиск..." else "GPS", color = TsuWhite)
                }
            }

            GpsStatusCard(currentGps)

            Button(
                onClick = onFindRoute,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TsuDarkBlue),
                enabled = !isCalculating && selectedIds.isNotEmpty()
            ) {
                Text(
                    if (isCalculating) "⏳ Поиск маршрута..." else "🚀 НАЙТИ МАРШРУТ",
                    color = TsuDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Ландшафтная разметка - три колонки с уменьшенными кнопками
@Composable
fun LandscapeRouteScreen(
    attractions: List<RoutePoint>,
    selectedIds: Set<Int>,
    onSelectedIdsChange: (Set<Int>) -> Unit,
    currentGps: GpsLocation?,
    isLocating: Boolean,
    isCalculating: Boolean,
    result: AntResult?,
    onGetLocation: () -> Unit,
    onFindRoute: () -> Unit,
    onClear: () -> Unit
) {
    val listState = rememberLazyListState()
    val resultScrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = TsuWhite
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(25.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ЛЕВАЯ КОЛОНКА - выбор достопримечательностей
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Достопримечательности",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TsuDarkBlue
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = TsuWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(attractions) { attraction ->
                            AttractionListItem(
                                attraction = attraction,
                                isSelected = selectedIds.contains(attraction.id),
                                onToggle = {
                                    onSelectedIdsChange(
                                        if (selectedIds.contains(attraction.id)) {
                                            selectedIds.minus(attraction.id)
                                        } else {
                                            selectedIds.plus(attraction.id)
                                        }
                                    )
                                },
                                compact = true
                            )
                        }
                    }
                }

                SelectionInfoCard(selectedIds.size, attractions.size, compact = true)
            }

            // СРЕДНЯЯ КОЛОНКА - кнопки управления
            Column(
                modifier = Modifier
                    .weight(0.2f)
                    .fillMaxSize()
                    .padding(start=8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ТГУ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TsuDarkBlue,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.weight(0.2f))

                Button(
                    onClick = onGetLocation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TsuBlue),
                    enabled = !isLocating,
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📍", fontSize = 20.sp)
                        Text(
                            if (isLocating) "Поиск" else "GPS",
                            fontSize = 10.sp,
                            color = TsuWhite
                        )
                    }
                }

                Button(
                    onClick = onClear,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TsuDark),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🗑️", fontSize = 20.sp)
                        Text("Сброс", fontSize = 10.sp, color = TsuWhite)
                    }
                }

                Button(
                    onClick = onFindRoute,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TsuDarkBlue),
                    enabled = !isCalculating && selectedIds.isNotEmpty(),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (isCalculating) "⏳" else "🚀",
                            fontSize = 24.sp
                        )
                        Text(
                            if (isCalculating) "Поиск" else "Пуск",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TsuWhite
                        )
                    }
                }

                GpsStatusCard(currentGps, compact = true)

                Spacer(modifier = Modifier.weight(0.3f))
            }

            // ПРАВАЯ КОЛОНКА - результат
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "📊 РЕЗУЛЬТАТ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TsuDarkBlue
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = TsuWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    ResultContent(
                        isCalculating = isCalculating,
                        result = result,
                        scrollState = resultScrollState,
                        compact = true
                    )
                }
            }
        }
    }
}

@Composable
fun AppHeader(compact: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp)
    ) {
        Text(
            text = "Муравьиный алгоритм",
            fontSize = if (compact) 16.sp else 22.sp,
            fontWeight = FontWeight.Bold,
            color = TsuDarkBlue
        )
        if (!compact) {
            Text(
                text = "Оптимизация маршрута по достопримечательностям ТГУ",
                fontSize = 11.sp,
                color = TsuDark,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(if (compact) 4.dp else 8.dp))
    }
}

@Composable
fun AttractionListItem(
    attraction: RoutePoint,
    isSelected: Boolean,
    onToggle: () -> Unit,
    compact: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(if (compact) 8.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = null,
            colors = androidx.compose.material3.CheckboxDefaults.colors(
                checkedColor = TsuBlue
            )
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = attraction.name,
                fontWeight = FontWeight.Medium,
                fontSize = if (compact) 11.sp else 13.sp,
                color = TsuDark
            )
            if (!compact) {
                Text(
                    text = attraction.description,
                    fontSize = 10.sp,
                    color = TsuDark,
                    maxLines = 1
                )
            }
        }
    }
    HorizontalDivider(color = TsuLightBlue.copy(alpha = 0.3f))
}

@Composable
fun SelectionInfoCard(selectedCount: Int, totalCount: Int, compact: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = TsuLightBlue.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Text(
            text = "✅ Выбрано: $selectedCount / $totalCount",
            modifier = Modifier.padding(if (compact) 6.dp else 10.dp),
            fontSize = if (compact) 10.sp else 12.sp,
            color = TsuDarkBlue,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun GpsStatusCard(currentGps: GpsLocation?, compact: Boolean = false) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (currentGps != null) TsuBlue.copy(alpha = 0.1f)
            else TsuDark.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Text(
            text = if (currentGps != null) {
                if (compact) "📍 ${String.format("%.2f", currentGps.latitude)}, ${String.format("%.2f", currentGps.longitude)}"
                else "📍 Широта: ${String.format("%.4f", currentGps.latitude)}, Долгота: ${String.format("%.4f", currentGps.longitude)}"
            } else "⏳ Нажмите GPS",
            modifier = Modifier.padding(if (compact) 6.dp else 10.dp),
            fontSize = if (compact) 9.sp else 11.sp,
            color = if (currentGps != null) TsuBlue else TsuDark
        )
    }
}

@Composable
fun ResultContent(
    isCalculating: Boolean,
    result: AntResult?,
    scrollState: ScrollState,
    compact: Boolean = false
) {
    if (isCalculating) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = TsuBlue)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Поиск маршрута...", fontSize = if (compact) 11.sp else 12.sp, color = TsuDark)
            }
        }
    } else if (result != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(if (compact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)
        ) {
            Text(
                text = "ОПТИМАЛЬНЫЙ МАРШРУТ",
                fontSize = if (compact) 11.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                color = TsuDarkBlue
            )
            Text(
                text = "Итерация: ${result.foundAtIteration}",
                fontSize = if (compact) 9.sp else 11.sp,
                color = TsuDark
            )
            Text(
                text = "Длина: ${String.format("%.2f", result.totalDistance)} м",
                fontSize = if (compact) 9.sp else 11.sp,
                color = TsuDark
            )
            Text(
                text = "Старт: ${result.startPoint.name}",
                fontSize = if (compact) 9.sp else 11.sp,
                color = TsuDark
            )

            Spacer(modifier = Modifier.height(if (compact) 4.dp else 8.dp))

            Text(
                text = "📋 ПОРЯДОК ОБХОДА:",
                fontSize = if (compact) 10.sp else 13.sp,
                fontWeight = FontWeight.Bold,
                color = TsuBlue
            )

            result.route.forEachIndexed { index, point ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = TsuLightBlue.copy(alpha = 0.1f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(if (compact) 8.dp else 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}.",
                            fontSize = if (compact) 10.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TsuBlue,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = point.name,
                            fontSize = if (compact) 10.sp else 12.sp,
                            color = TsuDark
                        )
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Выберите точки и нажмите «Найти»",
                fontSize = if (compact) 10.sp else 12.sp,
                color = TsuDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}