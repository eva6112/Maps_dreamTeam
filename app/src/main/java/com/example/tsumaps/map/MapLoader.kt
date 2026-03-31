package com.example.tsumaps.map

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.core.graphics.get
import com.example.tsumaps.R

object MapLoader {
    const val GRID_WIDTH = 152
    const val GRID_HEIGHT = 150
    fun loadMatrix(context: Context): Array<IntArray> {
        val options = BitmapFactory.Options().apply { inScaled = false }
        val original = BitmapFactory.decodeResource(context.resources, R.drawable.map_bw, options)
        return Array(GRID_HEIGHT) { y ->
            IntArray(GRID_WIDTH) { x ->
                val pixel = original[x, y]
                val brightness = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                if (brightness < 128) 1 else 0
            }
        }
    }
    fun getColorMapResId(): Int = R.drawable.map_color
}