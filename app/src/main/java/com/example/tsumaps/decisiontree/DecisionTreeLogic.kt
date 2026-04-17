package com.example.tsumaps.decisiontree

import kotlin.math.log2

// 1. Структура, состоящая из признака и решения (отдельная строчка CSV)
data class DataRow(
    val features: Map<String, String>,
    val label: String
)

// 2. Класс узла дерева (признак, значение + поддерево, решение)
class TreeNode(
    val featureName: String? = null,
    val children: Map<String, TreeNode> = emptyMap(),
    val predictedLabel: String? = null
)

// 3. Класс узла (шага): признак, значение, лист или нет
data class NodeStep(
    val featureName: String,
    val chosenValue: String,
    val isLeaf: Boolean = false
)

// 4. Класс ответа (решение + путь)
data class PredictionResult(
    val recommendedPlace: String,
    val path: List<NodeStep>
)

// 5. Парсер CSV
fun parseCsv(csvText: String): List<DataRow> {
    val lines = csvText.trim().lines().filter { it.isNotEmpty() }
    if (lines.size < 2)
        return emptyList()

    val headers = lines[0].split(";")
    val targetIndex = headers.lastIndex
    val dataset =
        mutableListOf<DataRow>()                                                          //изменяемый список для результата

    for (i in 1 until lines.size) {
        val values = lines[i].split(";")

        if (values.size <= targetIndex)
            continue

        val features = mutableMapOf<String, String>()
        for (j in 0 until targetIndex) {
            features[headers[j]] = values[j]
        }
        dataset.add(DataRow(features, values[targetIndex]))
    }
    return dataset
}

// 6. Расчёт энтропии
fun calculateEntropy(data: List<DataRow>): Double {
    if (data.isEmpty())
        return 0.0
    val labelCounts =
        mutableMapOf<String, Int>()                                                   //подсчёт частоты ответов
    for (row in data) {
        labelCounts[row.label] = labelCounts.getOrDefault(row.label, 0) + 1
    }
    var entropy = 0.0
    val totalSize = data.size.toDouble()
    for (count in labelCounts.values) {
        val probability = count / totalSize
        entropy -= probability * log2(probability)                                                  //H = -Σ p * log₂(p)
    }
    return entropy
}

// 7. Разбивает данные по значениям указанного признака
fun splitData(data: List<DataRow>, featureName: String): Map<String, List<DataRow>> {
    val splits =
        mutableMapOf<String, MutableList<DataRow>>()                                       //значение, список строк с таким значением
    for (row in data) {
        val featureValue = row.features[featureName] ?: continue
        if (!splits.containsKey(featureValue))
            splits[featureValue] = mutableListOf()
        splits[featureValue]!!.add(row)                                                             //утверждаем, что ключ точно есть
    }
    return splits
}

// 8. Расчет, насколько уменьшается энтропия, после разделения по признаку
fun calculateInformationGain(data: List<DataRow>, featureName: String): Double {
    val baseEntropy = calculateEntropy(data)
    val splits = splitData(data, featureName)

    var newEntropy = 0.0
    val totalSize = data.size.toDouble()

    for (subset in splits.values) {
        val weight = subset.size / totalSize
        newEntropy += weight * calculateEntropy(subset)                                             //Σ (|Sᵥ|/|S|) * H(Sᵥ)
    }
    return baseEntropy - newEntropy                                                                 //чем больше, тем лучше признак разделяет данные
}

// 9. Поиск лучшего вопроса
fun findBestFeatureToSplit(data: List<DataRow>, availableFeatures: List<String>): String? {
    var bestFeature: String? = null
    var maxGain = -1.0

    for (feature in availableFeatures) {
        val gain = calculateInformationGain(data, feature)
        if (gain > maxGain) {
            maxGain = gain
            bestFeature = feature
        }
    }
    if (maxGain <= 0.0)
        return null
    return bestFeature
}

// 10. Рекурсивная функция постройки всего дерева
fun buildTree(
    data: List<DataRow>, availableFeatures: List<String>,
    currentDepth: Int = 0, maxDepth: Int = 6
): TreeNode {
    // Базовый случай 1: Если все строки ведут к одному результату
    val distinctLabels = data.map { it.label }
        .distinct()                                           //получаем уникальный ответ
    if (distinctLabels.size == 1) {
        return TreeNode(predictedLabel = distinctLabels.first())
    }

    // Базовый случай 2: Если использованы все случаи, возвращается самый частый ответ
    if (availableFeatures.isEmpty() || currentDepth >= maxDepth) {
        val majorityLabel = data.groupBy { it.label }.maxByOrNull { it.value.size }?.key
        return TreeNode(predictedLabel = majorityLabel)
    }

    // Ищем лучший вопрос
    val bestFeature = findBestFeatureToSplit(data, availableFeatures)
    if (bestFeature == null)                                                                        //разделение не улучшает результат
    {
        val majorityLabel = data.groupBy { it.label }.maxByOrNull { it.value.size }?.key
        return TreeNode(predictedLabel = majorityLabel)
    }

    // Разбиваем данные на кучки по ответам и строим ветки для каждой кучки
    val splits = splitData(
        data,
        bestFeature
    )                                                       //получаем списки строк с признаком
    val remainingFeatures = availableFeatures - bestFeature
    val children = mutableMapOf<String, TreeNode>()

    for ((featureValue, subset) in splits)                                                          //рекурсивный спуск по дереву
    {
        children[featureValue] = buildTree(
            subset, remainingFeatures,
            currentDepth + 1, maxDepth
        )
    }

    return TreeNode(featureName = bestFeature, children = children)
}

// 11. Решение + путь к нему
fun predictWithPath(
    node: TreeNode, userInput: Map<String, String>,
    path: MutableList<NodeStep> = mutableListOf()
): PredictionResult {
    if (node.predictedLabel != null)                                                                //если дошли до листа - уже есть решение
    {
        path.add(NodeStep("", "", true))
        return PredictionResult(node.predictedLabel, path)
    }

    val featureToAsk = node.featureName
        ?:                                                          //если признака нет (вряд ли)
        return PredictionResult("Ошибка", path)

    val userAnswer = userInput[featureToAsk]

    if (userAnswer == null) {
        path.add(NodeStep(featureToAsk, "НЕТ ДАННЫХ", false))
        return PredictionResult("Не указан признак: $featureToAsk", path)
    }

    path.add(NodeStep(featureToAsk, userAnswer, false))

    val childNode = node.children[userAnswer]

    return if (childNode != null) {
        predictWithPath(childNode, userInput, path)
    } else {
        PredictionResult("Нет данных для комбинации: $featureToAsk = $userAnswer", path)
    }
}

// 12. Трансформация дерева в текст
fun treeToString(node: TreeNode, indent: String = ""): String {
    if (node.predictedLabel != null) {
        return "$indent-> Решение: ${node.predictedLabel}\n"
    }

    val builder = StringBuilder()
    builder.append("$indent? Зависит от: ${node.featureName}\n")

    for ((value, childNode) in node.children) {
        builder.append("$indent  |-- [$value]\n")
        builder.append(treeToString(childNode, "$indent | "))
    }
    return builder.toString()
}