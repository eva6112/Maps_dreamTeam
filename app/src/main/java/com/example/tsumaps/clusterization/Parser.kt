package com.example.tsumaps.clusterization

import android.content.Context
import java.io.BufferedReader                                                                       //ускоряет чтение больших файлов (readline)
import java.io.InputStreamReader                                                                    //для перевода байтов в символы

// 1. Парсер данных о заведених
fun readPlacesFromCsv(context: Context, fileName: String): List<Place>
{
    val places = mutableListOf<Place>()

    try
    {
        val inputStream = context.assets.open(fileName)
        val reader = BufferedReader(InputStreamReader(inputStream))

        reader.use { r ->
            r.readLine()

            var line: String?

            while (r.readLine().also { line = it } != null)
            {
                val tokens = line?.split(';')

                if (tokens != null && tokens.size == 3)
                {
                    try
                    {
                        val name = tokens[0]
                        val x = tokens[1].toDouble()
                        val y = tokens[2].toDouble()
                        places.add(Place(name, x, y))
                    }
                    catch (e: NumberFormatException)
                    {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    catch (e: java.io.IOException)
    {
        e.printStackTrace()
    }

    return places
}