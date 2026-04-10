package com.example.tsumaps.ant_algorithm

import android.Manifest
import android.location.Location
import android.widget.Toast
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RouteScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Состояния
    var selectedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var currentGps by remember { mutableStateOf<GpsLocation?>(null) }
    var isLocating by remember { mutableStateOf(false) }
    var isCalculating by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<AntResult?>(null) }

    // Состояния для прокрутки с видимой полосой
    val listState = rememberLazyListState()
    val resultScrollState = rememberScrollState()

    // Разрешение на геолокацию
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    val attractions = Attractions.allAttractions

    // Получение GPS с демо-режимом
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
                    Toast.makeText(context, "📍 Местоположение определено", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(context, "⚠️ GPS не определён, используем демо-точку", Toast.LENGTH_LONG).show()
                    currentGps = GpsLocation(56.466000, 84.949000)
                }
                isLocating = false
            }.addOnFailureListener {
                Toast.makeText(context, "❌ Ошибка GPS, используем демо-точку", Toast.LENGTH_SHORT).show()
                currentGps = GpsLocation(56.466000, 84.949000)
                isLocating = false
            }
        } else {
            isLocating = false
        }
    }

    // Поиск маршрута
    fun findRoute() {
        if (selectedIds.isEmpty()) {
            Toast.makeText(context, "Выберите хотя бы одну достопримечательность", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentGps == null) {
            Toast.makeText(context, "⚠️ GPS не определён, используем демо-точку", Toast.LENGTH_SHORT).show()
            currentGps = GpsLocation(56.466000, 84.949000)
        }

        val selectedPoints = Attractions.getPointsByIds(selectedIds.toList())
        if (selectedPoints.isEmpty()) {
            Toast.makeText(context, "Не удалось загрузить точки", Toast.LENGTH_SHORT).show()
            return
        }

        isCalculating = true
        result = null

        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val antColony = AntColony(
                        selectedPoints = selectedPoints,
                        userGps = currentGps!!,
                        numAnts = 25,
                        iterations = 150
                    )
                    val routeResult = antColony.findOptimalRoute()

                    withContext(Dispatchers.Main) {
                        result = routeResult
                        isCalculating = false
                        Toast.makeText(context, "✅ Маршрут найден на итерации ${routeResult.foundAtIteration}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isCalculating = false
                        Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        // Заголовок
        Text(
            text = "🐜 Муравьиный алгоритм",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Text(
            text = "Оптимизация маршрута по достопримечательностям ТГУ",
            fontSize = 14.sp,
            color = Color(0xFF7F8C8D),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textAlign = TextAlign.Center
        )

        // Список достопримечательностей с прокруткой
        Text(
            text = "📋 Выберите достопримечательности:",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // 🔧 LazyColumn автоматически показывает полосу прокрутки при необходимости
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(attractions) { attraction ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedIds = if (selectedIds.contains(attraction.id)) {
                                    selectedIds.minus(attraction.id)
                                } else {
                                    selectedIds.plus(attraction.id)
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedIds.contains(attraction.id),
                            onCheckedChange = null
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = attraction.name,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Text(
                                text = attraction.description,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                maxLines = 1
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }

        // Информация о выборе
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFECF0F1))
        ) {
            Text(
                text = "✅ Выбрано: ${selectedIds.size} / ${attractions.size}",
                modifier = Modifier.padding(12.dp),
                fontSize = 14.sp
            )
        }

        // Кнопки в ряд
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { selectedIds = emptySet() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C))
            ) {
                Text("🗑️ Очистить")
            }

            Button(
                onClick = { getCurrentLocation() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3498DB)),
                enabled = !isLocating
            ) {
                Text(if (isLocating) "⏳ Поиск..." else "📍 GPS")
            }
        }

        // Статус местоположения
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (currentGps != null) Color(0xFFC8E6C9) else Color(0xFFFFF3E0)
            )
        ) {
            Text(
                text = currentGps?.let { "📍 Широта: ${String.format("%.4f", it.latitude)}, Долгота: ${String.format("%.4f", it.longitude)}" }
                    ?: "⏳ Нажмите GPS для определения местоположения",
                modifier = Modifier.padding(12.dp),
                fontSize = 12.sp
            )
        }

        // Кнопка поиска маршрута
        Button(
            onClick = { findRoute() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
            enabled = !isCalculating && selectedIds.isNotEmpty()
        ) {
            Text(if (isCalculating) "⏳ Поиск маршрута..." else "🚀 НАЙТИ МАРШРУТ")
        }

        // Результат с прокруткой и видимой полосой
        Text(
            text = "📊 РЕЗУЛЬТАТ:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // 🔧 Используем Column + verticalScroll для видимой полосы прокрутки
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            if (isCalculating) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Поиск оптимального маршрута...", fontSize = 12.sp)
                        Text("Муравьиный алгоритм работает", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            } else if (result != null) {
                // Используем Column с verticalScroll для видимой полосы прокрутки
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(resultScrollState)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🐜 ОПТИМАЛЬНЫЙ МАРШРУТ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Найден на итерации: ${result!!.foundAtIteration}",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Длина маршрута: ${String.format("%.2f", result!!.totalDistance)}",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Стартовая точка: ${result!!.startPoint.name}",
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "📋 ПОРЯДОК ОБХОДА:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Список маршрута
                    result!!.route.forEachIndexed { index, point ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F8FF))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF27AE60),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Column {
                                    Text(
                                        text = point.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (point.description.isNotEmpty()) {
                                        Text(
                                            text = point.description,
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "✨ Маршрут оптимизирован с помощью муравьиного алгоритма!",
                        fontSize = 10.sp,
                        color = Color(0xFF27AE60),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Выберите точки, определите GPS и нажмите «Найти маршрут»",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}