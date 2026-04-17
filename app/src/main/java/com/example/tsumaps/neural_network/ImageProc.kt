package com.example.tsumaps.neural_network

import android.graphics.Bitmap
import android.graphics.Color

object ImageProcessor               //синглтон
{

    fun processBitmapToVector(originalBitmap: Bitmap): FloatArray
    {
        // 1. Сжимаем до 50x50
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 50, 50, true)      //сжимает рисунок до 50*50

        val pixelsArray = FloatArray(2500)
        var index = 0

        // 2. Проходим по всем пикселям
        for (y in 0 until 50)
        {
            for (x in 0 until 50)
            {
                val pixelColor = scaledBitmap.getPixel(x, y)                    //берем цвет пикселя

                val red = Color.red(pixelColor)
                val green = Color.green(pixelColor)
                val blue = Color.blue(pixelColor)

                val grayscale = (red + green + blue) / 3.0f                     //цветой пиксель - в серый

                val normalized = 1.0f - (grayscale / 255.0f)                    //нормализация для свпадения с МНИТСТом

                pixelsArray[index] =
                    if (normalized > 0.25f)
                        1.0f
                    else
                        0.0f
                index++
            }
        }
        return pixelsArray
    }
}