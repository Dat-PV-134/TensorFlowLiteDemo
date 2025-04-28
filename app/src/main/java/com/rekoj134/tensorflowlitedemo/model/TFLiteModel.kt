package com.rekoj134.tensorflowlitedemo.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteModel(context: Context) {

    private var interpreter: Interpreter

    init {
        val modelFile = loadModelFile(context, "selfie_multiclass_256x256.tflite")
        interpreter = Interpreter(modelFile)
    }

    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    fun runModel(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val inputImage = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
        val inputBuffer = Array(1) { Array(256) { Array(256) { FloatArray(3) } } }

        for (y in 0 until 256) {
            for (x in 0 until 256) {
                val pixel = inputImage.getPixel(x, y)
                inputBuffer[0][y][x][0] = Color.red(pixel) / 255.0f
                inputBuffer[0][y][x][1] = Color.green(pixel) / 255.0f
                inputBuffer[0][y][x][2] = Color.blue(pixel) / 255.0f
            }
        }

        val outputBuffer = Array(1) { Array(256) { Array(256) { FloatArray(6) } } }
        interpreter.run(inputBuffer, outputBuffer)
        return outputBuffer
    }

    fun getSegmentationMap(output: Array<Array<Array<FloatArray>>>): Array<IntArray> {
        val segmentationMap = Array(256) { IntArray(256) }
        for (y in 0 until 256) {
            for (x in 0 until 256) {
                var maxIdx = 0
                var maxProb = output[0][y][x][0]
                for (i in 1 until 6) {
                    if (output[0][y][x][i] > maxProb) {
                        maxProb = output[0][y][x][i]
                        maxIdx = i
                    }
                }
                segmentationMap[y][x] = maxIdx
            }
        }
        return segmentationMap
    }

    fun applySegmentationOverlay(original: Bitmap, segmentationMap: Array<IntArray>): Bitmap {
        val overlay = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(overlay)
        val paint = Paint()

        val colorMap = arrayOf(
            Color.TRANSPARENT,   // Background
            Color.RED,           // Hair
            Color.BLUE,          // Clothes
            Color.GREEN,         // Face
            Color.YELLOW,        // Other
            Color.MAGENTA        // Extra class
        )

        for (y in 0 until 256) {
            for (x in 0 until 256) {
                paint.color = colorMap[segmentationMap[y][x]]
                canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
            }
        }

        val finalBitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val finalCanvas = Canvas(finalBitmap)
        finalCanvas.drawBitmap(Bitmap.createScaledBitmap(original, 256, 256, true), 0f, 0f, null)
        finalCanvas.drawBitmap(overlay, 0f, 0f, Paint().apply { alpha = 120 })
        return finalBitmap
    }
}
