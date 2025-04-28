package com.rekoj134.tensorflowlitedemo

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.rekoj134.tensorflowlitedemo.model.TFLiteModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var imageViewSegmented: ImageView
    private lateinit var progressBar: ProgressBar

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        imageViewSegmented = findViewById(R.id.imageViewSegmented)
        progressBar = findViewById(R.id.progressBar)

        runSegmentation()
    }

    private fun runSegmentation() {
        coroutineScope.launch {
            progressBar.visibility = ProgressBar.VISIBLE

            val tfliteModel = TFLiteModel(this@MainActivity)

            val inputStream = assets.open("person.jpg")
            val bitmap = BitmapFactory.decodeStream(inputStream)

            val output = withContext(Dispatchers.IO) { tfliteModel.runModel(bitmap) }
            val segmentationMap = withContext(Dispatchers.IO) { tfliteModel.getSegmentationMap(output) }
            val segmentedBitmap = withContext(Dispatchers.IO) { tfliteModel.applySegmentationOverlay(bitmap, segmentationMap) }

            imageViewSegmented.setImageBitmap(segmentedBitmap)
            progressBar.visibility = ProgressBar.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}