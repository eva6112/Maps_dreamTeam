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
    var neuralNetwork by remember { mutableStateOf<AndroidNeuralNetwork?>(null) }                 // инициализация нейросети
    val appBlueColor = Color(0xFF1976D2)
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE          // проверка ориентации экрана (горизонталь/вертикаль)

    LaunchedEffect(Unit)
    {
        try
        {
            neuralNetwork = AndroidNeuralNetwork(context)                                        // загрузка весов из assets/weights.json
        }
        catch (e: Exception)
        { }
    }

    // список заведений для оценки
    val restaurants = listOf("Кафе «Минутка»", "Кафе «Сибирские блины»", "Кофейня «Starbooks»",
        "Магазин «Абрикос»", "Столовая «Столовая №1»", "Кафе «Rostic's»", "Кофейня «Точка»",
        "Кофейня «Белка»", "Ресторан «Гербарий»", "Кофейня «Baba Roma»", "Ресторан «Вечный зов»",
        "Столовая «Укромное местечко»", "Магазин «Ярче!»", "Ресторан «Ближе»", "Кофейня «Тесто»",
        "Ресторан «Poly bistro»", "Ресторан «Пешком постою»", "Кофейня «Xo bakery»", "Кафе «Кафе 2 корпуса»")

    var expanded by remember { mutableStateOf(false) }                                           // состояние выпадающего списка
    var selectedRestaurant by rememberSaveable { mutableStateOf(restaurants[0]) }                // выбранное заведение (сохраняется при повороте)

    var paths by remember { mutableStateOf(listOf<Path>()) }                                     // готовые нарисованные линии
    var currentPath by remember { mutableStateOf<Path?>(null) }                                  // линия, которую сейчас рисуют
    var drawTrigger by remember { mutableIntStateOf(0) }                                         // триггер для перерисовки Canvas
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }                                  // размер холста в пикселях

    var recognizedDigit by rememberSaveable { mutableStateOf<Int?>(null) }                       // распознанная цифра (сохраняется при повороте)

    // сохранение истории оценок при повороте экрана через mapSaver
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
    ) { mutableStateListOf<Pair<String, Int>>() }                                                // список (заведение → оценка)

    // Левая панель (выбор заведения + рисование цифры)
    val controlsContent = @Composable
    {
        if (!isLandscape)
        {
            Text("Выберите заведение для оценки", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        }

        // выпадающий список заведений (блокируется, если цифра уже распознана)
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

        // холст для рисования + кнопки управления
        val canvasAndButtons = @Composable
        {
            Box(
                modifier = Modifier
                    .size(if (isLandscape) 140.dp else 220.dp)                                    // размер холста (меньше в горизонтальном режиме)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(2.dp, Color.LightGray, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .onSizeChanged { canvasSize = it }                                           // запоминаем размер для конвертации в Bitmap
                    .pointerInput(recognizedDigit) {
                        if (recognizedDigit == null)                                             // рисование разрешено только до распознавания
                        {
                            detectDragGestures(
                                onDragStart = { offset -> currentPath = Path().apply { moveTo(offset.x, offset.y) } },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentPath?.lineTo(change.position.x, change.position.y)
                                    drawTrigger++                                                 // принудительная перерисовка
                                },
                                onDragEnd = { currentPath?.let { paths = paths + it }; currentPath = null }
                            )
                        }
                    }
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawTrigger                                                                  // следим за изменением для перерисовки
                    val stroke = Stroke(width = if (isLandscape) 25f else 35f,                   // толщина линии (тоньше в горизонтальном режиме)
                        cap = StrokeCap.Round, join = StrokeJoin.Round)
                    paths.forEach { drawPath(it, Color.Black, style = stroke) }                  // рисуем готовые линии
                    currentPath?.let { drawPath(it, Color.Black, style = stroke) }               // рисуем текущую линию
                }
            }

            if (isLandscape)
                Spacer(modifier = Modifier.width(16.dp))
            else
                Spacer(modifier = Modifier.height(12.dp))

            // кнопки: Стереть / Распознать / Подтверждение цифры
            Column(
                modifier = if (isLandscape) Modifier.width(150.dp) else Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                if (recognizedDigit == null)                                                     // режим "ожидание распознавания"
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
                                val bitmap = createBitmapFromPathsLocal(canvasSize, paths)      // холст → Bitmap
                                val floatVector = ImageProcessor.processBitmapToVector(bitmap)  // 2500 чисел (0 или 1)
                                recognizedDigit = neuralNetwork?.predict(floatVector)           // нейросеть → цифра
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = appBlueColor)
                    )
                    { Text("Распознать", fontSize = 12.sp) }
                }
                else                                                                             // режим "подтверждение цифры"
                {
                    Text("Это $recognizedDigit?", fontWeight = FontWeight.Bold, color = appBlueColor)
                    Row {
                        IconButton(onClick = { recognizedDigit = null; paths = emptyList() }) {  // нет → рисуем заново
                            Text("Нет", color = Color(0xFF001383))
                        }
                        IconButton(onClick = {                                                    // сохраняем оценку
                            savedRatings.removeAll { it.first == selectedRestaurant }           // удаляем старую оценку
                            savedRatings.add(0, selectedRestaurant to recognizedDigit!!)        // добавляем новую в начало
                            recognizedDigit = null                                               // сбрасываем режим
                            paths = emptyList()                                                  // очищаем холст
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

    // Правая панель (история оценок)
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
                            Text(rating.first, modifier = Modifier.weight(1f), fontSize = 14.sp)     // название заведения

                            Box(modifier = Modifier.background(
                                appBlueColor,
                                RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp))
                            {
                                Text("${rating.second} из 9", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)  // оценка (0-9)
                            }
                        }
                    }
                }
            }
        }
    }

    // финальная верстка с учетом ориентации экрана
    if (isLandscape)                                                                             // горизонтальный режим: Row (2 колонки)
    {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(modifier = Modifier
                .weight(1.2f)
                .verticalScroll(rememberScrollState())) { controlsContent() }

            VerticalDivider(modifier = Modifier.padding(horizontal = 12.dp))
            Column(modifier = Modifier.weight(1f)) { historyContent() }
        }
    }
    else                                                                                         // вертикальный режим: Column
    {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp))
        {
            controlsContent()
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Box(modifier = Modifier.weight(1f)) { historyContent() }
        }
    }
}

// конвертация нарисованных линий в Bitmap для передачи в нейросеть
private fun createBitmapFromPathsLocal(canvasSize: IntSize, paths: List<Path>): Bitmap
{
    val bitmap = Bitmap.createBitmap(canvasSize.width.coerceAtLeast(1), canvasSize.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)                                               // белый фон (важно для нейросети)
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK                                                     // черные линии
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = if (canvasSize.width < 500) 25f else 35f                                   // адаптивная толщина линии
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        isAntiAlias = true
    }
    paths.forEach { canvas.drawPath(it.asAndroidPath(), paint) }
    return bitmap
}