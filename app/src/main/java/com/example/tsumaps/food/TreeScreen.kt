package com.example.tsumaps.food

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.tsumaps.decisiontree.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CornerSize
import android.content.Context

data class ChatMessage(
    val isUser: Boolean,
    val text: String,
    val options: List<String> = emptyList(),
    val isResetButton: Boolean = false
)

@Composable
fun TreeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var tree by remember { mutableStateOf<TreeNode?>(null) }
    var currentNode by remember { mutableStateOf<TreeNode?>(null) }
    var userInput by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    fun askNextQuestion(node: TreeNode?, chatList: MutableList<ChatMessage>) {
        if (node?.predictedLabel != null) {
            chatList.add(ChatMessage(isUser = false, text = "Рекомендую: ${node.predictedLabel}"))
            chatList.add(ChatMessage(isUser = false, text = "Хотите подобрать что-то другое?", isResetButton = true))
            return
        }
        val featureName = node?.featureName ?: return
        val possibleValues = node.children.keys.toList()
        chatList.add(ChatMessage(isUser = false, text = getQuestionText(featureName), options = possibleValues))
    }

    LaunchedEffect(Unit) {
        val csvText = getCsvFromAssets(context)
        val dataset = parseCsv(csvText)
        if (dataset.isNotEmpty()) {
            val features = dataset.firstOrNull()?.features?.keys?.toList() ?: emptyList()
            val builtTree = buildTree(dataset, features)
            tree = builtTree
            currentNode = builtTree
            messages.add(ChatMessage(isUser = false, text = "Привет! Я помогу выбрать место для обеда."))
            askNextQuestion(currentNode, messages)
        }
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(
            modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
            Box(modifier = Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
                Text(text = "Советник по обеду", fontWeight = FontWeight.Bold)
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message)
            }
        }

        val lastMessage = messages.lastOrNull()
        if (lastMessage != null && !lastMessage.isUser) {
            if (lastMessage.options.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(lastMessage.options) { option ->
                        Button(onClick = {
                            messages.add(ChatMessage(isUser = true, text = option))
                            val featureName = currentNode?.featureName ?: return@Button
                            val newUserInput = userInput.toMutableMap()
                            newUserInput[featureName] = option
                            userInput = newUserInput
                            currentNode = currentNode?.children?.get(option)
                            askNextQuestion(currentNode, messages)
                            scope.launch { listState.animateScrollToItem(messages.size - 1) }
                        }) {
                            Text(text = option)
                        }
                    }
                }
            }
            else if (lastMessage.isResetButton) {
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = {
                            messages.clear()
                            userInput = emptyMap()
                            currentNode = tree
                            messages.add(ChatMessage(isUser = false, text = "Начинаем новый поиск!"))
                            askNextQuestion(currentNode, messages)
                            scope.launch { listState.scrollToItem(0) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("Начать сначала")
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.isUser
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium.copy(
                bottomEnd = if (isUser) CornerSize(0.dp) else CornerSize(16.dp),
                bottomStart = if (isUser) CornerSize(16.dp) else CornerSize(0.dp)
            ),
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun getCsvFromAssets(context: Context): String {
    return context.assets.open("advisor_dataset.csv").bufferedReader().use { it.readText() }
}

fun getQuestionText(featureName: String): String {
    return when (featureName) {
        "Какая подходит людность" -> "Какая подходит людность?"
        "Насколько сильный голод" -> "Насколько сильный голод?"
        "Важны ли часы работы после 20.00" -> "Важны ли часы работы после 20:00?"
        "Формат места" -> "Какой формат места предпочитаете?"
        "Важно ли наличие комфортных мест" -> "Важно ли наличие комфортных мест?"
        "Важно ли наличие фоновой музыки" -> "Важно ли наличие фоновой музыки?"
        "Важно ли наличие Wi-Fi" -> "Важно ли наличие Wi-Fi?"
        "У вас: перемена/большой перерыв/уже освободился" -> "Ваш статус?"
        "Ваш бюджет" -> "Ваш бюджет?"
        else -> featureName
    }
}