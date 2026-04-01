package com.example.tsumaps.decisiontree

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DecisionTreeScreen(modifier: Modifier = Modifier)
{
    var resultText by remember { mutableStateOf("Нажми, чтобы запустить алгоритм") }

    Column(modifier = modifier.padding(16.dp))
    {
        Text(text = "Умный советник (Дерево решений)", modifier = Modifier.padding(bottom = 16.dp))

        Button(onClick = {
            val rawCsv = """
                Погода,Время,Бюджет,Голод,Решение
                Ясно,Много,Высокий,Да,Кафе_Блины
                Ясно,Много,Низкий,Нет,Прогулка_Роща
                Дождь,Мало,Высокий,Да,Кофе_с_собой
                Дождь,Много,Низкий,Нет,Библиотека
            """.trimIndent()

            val dataset = parseCsv(rawCsv)
            val features = dataset.firstOrNull()?.features?.keys?.toList() ?: emptyList()
            val best = findBestFeatureToSplit(dataset, features)

            resultText = "Алгоритм считает, что самый важный вопрос: $best"
        })
        {
            Text("Запустить анализ")
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(text = resultText)
    }
}