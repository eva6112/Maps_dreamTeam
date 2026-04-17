package com.example.tsumaps.food

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsumaps.neural_network.AndroidNeuralNetwork
import com.example.tsumaps.neural_network.ImageProcessor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingScreen()
{
    val context = LocalContext.current
    var neuralNetwork by remember { mutableStateOf<AndroidNeuralNetwork?>(null) }
    val appBlueColor = Color(0xFF1976D2)
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(Unit)
    {
        try
        {
            neuralNetwork = AndroidNeuralNetwork(context)                               //загрузка нейросети из assets
        }
        catch (e: Exception)
        { }
    }

    val restaurants = listOf("Кафе «Минутка»", "Кафе «Сибирские блины»", "Кофейня «Starbooks»",
        "Магазин «Абрикос»", "Столовая «Столовая №1»", "Кафе «Rostic's»", "Кофейня «Точка»",
        "Кофейня «Белка»", "Ресторан «Гербарий»", "Кофейня «Baba Roma»", "Ресторан «Вечный зов»",
        "Столовая «Укромное местечко»", "Магазин «Ярче!»", "Ресторан «Ближе»", "Кофейня «Тесто»",
        "Ресторан «Poly bistro»", "Ресторан «Пешком постою»", "Кофейня «Xo bakery»", "Кафе «Кафе 2 корпуса»")

    var expanded by remember { mutableStateOf(false) }
    var selectedRestaurant by rememberSaveable { mutableStateOf(restaurants[0]) }

    var paths by remember { mutableStateOf(listOf<Path>()) }         //готовые отрезки (линии)
    var currentPath by remember { mutableStateOf<Path?>(null) }      //текущий рисующийся отрезок
    var drawTrigger by remember { mutableIntStateOf(0) }             //триггер перерисовки Canvas
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }      //размеры холста в пикселях

    var recognizedDigit by rememberSaveable { mutableStateOf<Int?>(null) }

    //сохранение списка оценок при повороте экрана
    val savedRatings = rememberSaveable(
        saver = mapSaver(
            save = { list -> list.mapIndexed { index, pair -> index.toString() to listOf(pair.first, pair.second) }.toMap() },
            restore = { map ->
                val list = mutableStateListOf<Pair<String, Int>>()
                map.values.forEach {
                    val data = it as List<*>
                    list.add((data[0] as String) to (data[1] as Int))
                }
                list
            }
        )
    ) { mutableStateListOf<Pair<String, Int>>() }                   //список заведений - оценка

    // Левая панель (Управление)
    val controlsContent = @Composable
    {
        if (!isLandscape)
        {
            Text("Выберите заведение для оценки", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (recognizedDigit == null) expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedRestaurant,
                onValueChange = {},
                readOnly = true,
                label = { if (isLandscape) Text("Заведение") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(focusedBorderColor = appBlueColor,
                    unfocusedBorderColor = appBlueColor),
                modifier = Modifier.menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = true
                ).fillMaxWidth(),
                enabled = recognizedDigit == null
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false })
            {
                restaurants.forEach { DropdownMenuItem(text = { Text(it) },
                    onClick = { selectedRestaurant = it; expanded = false; paths = emptyList() }) }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Ряд с Холстом и Кнопками справа (для горизонтального режима)
        val canvasAndButtons = @Composable
        {
            Box(
                modifier = Modifier
                    .size(if (isLandscape) 140.dp else 220.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(2.dp, Color.LightGray, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(recognizedDigit) {
                        if (recognizedDigit == null)
                        {
                            detectDragGestures(
                                onDragStart = { offset -> currentPath = Path().apply { moveTo(offset.x, offset.y) } },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentPath?.lineTo(change.position.x, change.position.y)
                                    drawTrigger++
                                },
                                onDragEnd = { currentPath?.let { paths = paths + it }; currentPath = null }
                            )
                        }
                    }
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawTrigger
                    val stroke = Stroke(width = if (isLandscape) 25f else 35f,
                        cap = StrokeCap.Round, join = StrokeJoin.Round)
                    paths.forEach { drawPath(it, Color.Black, style = stroke) }
                    currentPath?.let { drawPath(it, Color.Black, style = stroke) }
                }
            }

            if (isLandscape)
                Spacer(modifier = Modifier.width(16.dp))
            else
                Spacer(modifier = Modifier.height(12.dp))

            // Кнопки / Подтверждение
            Column(
                modifier = if (isLandscape) Modifier.width(150.dp) else Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                if (recognizedDigit == null)
                {
                    Button(
                        onClick = { paths = emptyList(); currentPath = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = appBlueColor)
                    ) { Text("Стереть", fontSize = 12.sp) }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (neuralNetwork != null && paths.isNotEmpty()) {
                                val bitmap = createBitmapFromPathsLocal(canvasSize, paths)
                                val floatVector = ImageProcessor.processBitmapToVector(bitmap)
                                recognizedDigit = neuralNetwork?.predict(floatVector)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = appBlueColor)
                    )
                    { Text("Распознать", fontSize = 12.sp) }
                }
                else
                {
                    Text("Это $recognizedDigit?", fontWeight = FontWeight.Bold, color = appBlueColor)
                    Row {
                        IconButton(onClick = { recognizedDigit = null; paths = emptyList() }) {
                            Text("Нет", color = Color(0xFF001383))
                        }
                        IconButton(onClick = {
                            savedRatings.removeAll { it.first == selectedRestaurant }
                            savedRatings.add(0, selectedRestaurant to recognizedDigit!!)
                            recognizedDigit = null
                            paths = emptyList()
                        }) {
                            Text("Да", color = Color(0xFF001383))
                        }
                    }
                }
            }
        }

        if (isLandscape)
        {
            Row(verticalAlignment = Alignment.CenterVertically) { canvasAndButtons() }
        } else
        {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { canvasAndButtons() }
        }
    }

    // История оценок
    val historyContent = @Composable
    {
        Text("История оценок", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (!savedRatings.isEmpty())
            {
                items(savedRatings)
                { rating ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant))
                    {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically)
                        {
                            Text(rating.first, modifier = Modifier.weight(1f), fontSize = 14.sp)

                            Box(modifier = Modifier.background(
                                appBlueColor,
                                RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp))
                            {
                                Text("${rating.second} из 9", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Финальный Layout
    if (isLandscape)
    {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(modifier = Modifier
                .weight(1.2f)
                .verticalScroll(rememberScrollState())) { controlsContent() }

            VerticalDivider(modifier = Modifier.padding(horizontal = 12.dp))
            Column(modifier = Modifier.weight(1f)) { historyContent() }
        }
    }
    else
    {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp))
        {
            controlsContent()
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Box(modifier = Modifier.weight(1f)) { historyContent() }
        }
    }
}

private fun createBitmapFromPathsLocal(canvasSize: IntSize, paths: List<Path>): Bitmap
{
    val bitmap = Bitmap.createBitmap(canvasSize.width.coerceAtLeast(1), canvasSize.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = if (canvasSize.width < 500) 25f else 35f
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        isAntiAlias = true
    }
    paths.forEach { canvas.drawPath(it.asAndroidPath(), paint) }
    return bitmap
}