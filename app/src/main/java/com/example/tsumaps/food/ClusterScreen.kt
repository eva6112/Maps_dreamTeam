package com.example.tsumaps.food

import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.roundToInt
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

import androidx.compose.material3.*

import kotlinx.coroutines.launch

import com.example.tsumaps.clusterization.Place
import com.example.tsumaps.clusterization.Centroid
import com.example.tsumaps.clusterization.KMeans
import com.example.tsumaps.clusterization.readPlacesFromCsv
import com.example.tsumaps.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration


@OptIn(ExperimentalMaterial3Api::class)                                 //получаем разрещение исползовать экспериметнальную фичу

@Composable                                                             //кусочек строит часть интерфейса
fun ClusterScreen() {
    val context =
        LocalContext.current                                                              //для информации из assets

    var kText by remember { mutableStateOf("3") }                                                   //наблюдаемые переменные
    var places by remember { mutableStateOf<List<Place>>(emptyList()) }
    var clusters by remember { mutableStateOf<Map<Centroid, List<Place>>>(emptyMap()) }

    val sheetState =
        rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)  //видно только верхушку шита
    val scaffoldState =
        rememberBottomSheetScaffoldState(bottomSheetState = sheetState)             //состояние для каркаса всего экрана

    val scope =
        rememberCoroutineScope()                                                            //для возможности ассинхронной работы
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    LaunchedEffect(Unit)                                                                            //ровно один раз, при запуске экрана
    {
        places = readPlacesFromCsv(context, "places_coordinates.csv")
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 50.dp,                                                //высота выглядующего шита
        sheetContent = { ClusterResultList(clusters = clusters) },                        //содержимое шита
        containerColor = MaterialTheme.colorScheme.primary,
        sheetContainerColor = MaterialTheme.colorScheme.primary
    )
    { innerPadding ->                                                         //отступы, которые генерирует система (не совпадало с системными кнопками)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
        {
            ClusterMapRenderer(
                places = places,
                clusters = clusters
            )            //отрисовка карты и точек

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)                                 //прижимаем наверх к центру
                    .padding(top = if (isLandscape) 8.dp else 28.dp)
                    .widthIn(max = if (isLandscape) 400.dp else 600.dp)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically                  //выравнивание внутренних элементо по центру
            )
            {
                OutlinedTextField(
                    value = kText,
                    onValueChange = {
                        kText = it
                    },                                             //действие при вводе
                    label = if (!isLandscape) {
                        { Text("Кластеры") }
                    } else null,
                    placeholder = { if (isLandscape) Text("Кластеры") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),      //показываем только цифры на клавиатуре
                    modifier = Modifier
                        .width(if (isLandscape) 120.dp else 150.dp)
                        .height(if (isLandscape) 50.dp else 65.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        cursorColor = MaterialTheme.colorScheme.onPrimary,
                        focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                        focusedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        val k = kText.toIntOrNull()
                        if (k != null && k > 0 && places.isNotEmpty()) {
                            val kmeans = KMeans(places, k)
                            clusters = kmeans.run()

                            scope.launch {
                                sheetState.expand()                  //шит полностью открывается
                            }
                        }
                    }
                )
                {
                    Text("Найти зоны")
                }
            }
        }
    }
}

@Composable
fun ClusterResultList(clusters: Map<Centroid, List<Place>>) {
    LazyColumn(                                                             //умный вертикальный список (тотолько из того, что видно)
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    )
    {


        item {
            Text(
                text = "Результаты кластеризации:",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        if (clusters.isEmpty()) {
            item {
                Text("Кластеры еще не рассчитаны.")
            }
        } else {
            val clusterEntries =
                clusters.entries.toList()                  //преобразование map в список (для LazyColumn)

            itemsIndexed(clusterEntries)
            { index, entry ->                                               //проходим циклом по каждому кластеру
                val clusterNumber = index + 1
                val clusterPlaces = entry.value

                Column(modifier = Modifier.padding(bottom = 16.dp))
                {
                    Text(
                        text = "Кластер №$clusterNumber",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    clusterPlaces.forEach { place ->
                        Text(
                            text = "- ${place.name}",
                            modifier = Modifier.padding(start = 8.dp)         //отступ для вложенности
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ClusterMapRenderer(places: List<Place>, clusters: Map<Centroid, List<Place>>) {
    var mapOffset by remember { mutableStateOf(Offset.Zero) }                         //запоминаем смещение карты, когда пользователь водит пальцем
    var zoom by remember { mutableFloatStateOf(0.6f) }

    val gridWidth =
        152                                                                        //размеры матрицы и карты
    val gridHeight = 150
    val mapWidthDp = 3040.dp
    val mapHeightDp = 3000.dp

    val scaledWidth = mapWidthDp * zoom
    val scaledHeight = mapHeightDp * zoom

    val clusterColors = listOf(
        Color(0xCC8C1212),
        Color(0xCC0C3069),
        Color(0xCC16501B),
        Color(0xCCBB5407),
        Color(0xCC2F224D),
        Color(0xCC015256),
        Color(0xFF444818)
    )

    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.LTGRAY
            textSize = 28f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            .pointerInput(Unit)
            {
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
        Box(                                                                                    //бокс, перехватывающий касание экрана
            modifier = Modifier
                .offset {
                    IntOffset(
                        mapOffset.x.roundToInt(),
                        mapOffset.y.roundToInt()
                    )
                }       //двигаем согласно mapOffset
                .requiredSize(scaledWidth, scaledHeight)
        ) {
            Image(
                painter = painterResource(id = R.drawable.map_color),
                contentDescription = "Карта для кластеров",
                alignment = Alignment.TopStart,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )

            Canvas(modifier = Modifier.fillMaxSize())                                           //накладываем прозрачный холст поверх карты
            {
                val cellWidthPx =
                    size.width / gridWidth                                        //размер ячейки в пикселях
                val cellHeightPx = size.height / gridHeight

                if (clusters.isEmpty())                                                         //алгоритм не запущен
                {
                    places.forEach { place ->
                        val center = Offset(
                            x = (place.x * cellWidthPx).toFloat(),
                            y = (place.y * cellHeightPx).toFloat()
                        )

                        drawCircle(
                            color = Color.LightGray.copy(alpha = 0.7f),
                            radius = 25f,
                            center = center
                        )

                        drawCircle(
                            color = Color.Black,
                            radius = 25f,
                            center = center,
                            style = Stroke(width = 5f)
                        )
                    }
                } else {
                    var colorIndex = 0
                    for ((centroid, clusterPlaces) in clusters) {
                        val clusterColor = clusterColors[colorIndex % clusterColors.size]
                        val clusterNumber = (colorIndex + 1).toString()

                        for (place in clusterPlaces) {
                            val center = Offset(
                                x = (place.x * cellWidthPx).toFloat(),
                                y = (place.y * cellHeightPx).toFloat()
                            )
                            val radius = 35f

                            drawCircle(
                                color = clusterColor,
                                radius = radius,
                                center = center
                            )

                            drawCircle(
                                color = Color.Black,
                                radius = radius,
                                center = center,
                                style = Stroke(width = 5f)
                            )

                            drawContext.canvas.nativeCanvas.drawText(
                                clusterNumber,
                                center.x,
                                center.y + (textPaint.textSize / 3),                    //немного опускаем по вертикали ниже
                                textPaint
                            )
                        }
                        colorIndex++
                    }
                }
            }
        }
    }
}