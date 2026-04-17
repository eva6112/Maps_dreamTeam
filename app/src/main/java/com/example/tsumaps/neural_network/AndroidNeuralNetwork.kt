package com.example.tsumaps.neural_network

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.exp

class AndroidNeuralNetwork(context: Context)
{

    private val inputSize = 2500
    private val hiddenSize = 128
    private val outputSize = 10

    private val hiddenWeights: Array<FloatArray>
    private val hiddenBiases: FloatArray
    private val outputWeights: Array<FloatArray>
    private val outputBiases: FloatArray

    init {
        val jsonString = context.assets.open("weights.json").bufferedReader().use { it.readText() }
        val json = JSONObject(jsonString)           //чтобы легко доставать массивы по имени

        hiddenWeights = parseJson2DArray(json.getJSONArray("hidden_weights"), hiddenSize, inputSize)
        hiddenBiases = parseJson1DArray(json.getJSONArray("hidden_biases"))
        outputWeights = parseJson2DArray(json.getJSONArray("output_weights"), outputSize, hiddenSize)
        outputBiases = parseJson1DArray(json.getJSONArray("output_biases"))
    }

    private fun sigmoid(x: Float):
            Float = 1.0f / (1.0f + exp(-x))

    fun predict(inputs: FloatArray): Int
    {
        // 1. Скрытый слой
        val a1 = FloatArray(hiddenSize)
        for (i in 0 until hiddenSize)
        {
            var dotProduct = hiddenBiases[i]                    //начало со смещения
            for (j in 0 until inputSize)
            {
                dotProduct += inputs[j] * hiddenWeights[i][j]   //пиксель * вес
            }
            a1[i] = sigmoid(dotProduct)                         //активация нейрона
        }

        // 2. Выходной слой
        val a2 = FloatArray(outputSize)
        var maxIndex = 0
        var maxProb = -1f

        for (i in 0 until outputSize)
        {
            var dotProduct = outputBiases[i]                    //начало со смещения
            for (j in 0 until hiddenSize)
            {
                dotProduct += a1[j] * outputWeights[i][j]
            }
            a2[i] = sigmoid(dotProduct)                         //получение 10 вероятностей

            // 3. Ответ с максимальной вероятностью
            if (a2[i] > maxProb)
            {
                maxProb = a2[i]
                maxIndex = i
            }
        }
        return maxIndex
    }

    private fun parseJson1DArray(jsonArray: JSONArray): FloatArray
    {
        val list = FloatArray(jsonArray.length())
        for (i in 0 until jsonArray.length())
        {
            list[i] = jsonArray.getDouble(i).toFloat()
        }
        return list
    }

    private fun parseJson2DArray(jsonArray: JSONArray, dim1: Int, dim2: Int): Array<FloatArray>
    {
        val array = Array(dim1)
        {
            FloatArray(dim2)
        }
        for (i in 0 until dim1)
        {
            val innerArray = jsonArray.getJSONArray(i)
            for (j in 0 until dim2)
            {
                array[i][j] = innerArray.getDouble(j).toFloat()
            }
        }
        return array
    }
}