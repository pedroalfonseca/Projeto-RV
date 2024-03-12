package com.ai.detect_object

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.ai.detect_object.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap

class MainActivity : AppCompatActivity() {

    lateinit var labels:List<String>
    var colors = listOf<Int>(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
        Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED)
    val paint = Paint()
    lateinit var imageProcessor: ImageProcessor
    lateinit var bitmap:Bitmap
    lateinit var imageView: ImageView
    lateinit var cameraDevice: CameraDevice
    lateinit var handler: Handler
    lateinit var cameraManager: CameraManager
    lateinit var textureView: TextureView
    lateinit var model:SsdMobilenetV11Metadata1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        get_permission()

        lateinit var scores: FloatArray
        lateinit var locations: FloatArray
        lateinit var classes: FloatArray

        labels = FileUtil.loadLabels(this, "labels.txt")
        imageProcessor = ImageProcessor.Builder().build()
        model = SsdMobilenetV11Metadata1.newInstance(this)

        var isThreadRunning = true
        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()

        handler = Handler(handlerThread.looper)
        imageView = findViewById(R.id.imageView)
        textureView = findViewById(R.id.textureView)

        textureView.surfaceTextureListener = object:TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                open_camera()
            }
            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
            }
            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }
            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                if (isThreadRunning) {

                    bitmap = textureView.bitmap!!
                    var image = TensorImage.fromBitmap(bitmap)
                    image = imageProcessor.process(image)

                    val outputs = model.process(image)
                    locations = outputs.locationsAsTensorBuffer.floatArray
                    classes = outputs.classesAsTensorBuffer.floatArray
                    scores = outputs.scoresAsTensorBuffer.floatArray
                    val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray

                    var mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                    val canvas = Canvas(mutable)

                    val h = mutable.height
                    val w = mutable.width
                    paint.textSize = h / 15f
                    paint.strokeWidth = h / 85f

                    var x = 0
                    scores.forEachIndexed { index, fl ->
                        x = index
                        x *= 4
                        if (fl > 0.5) {
                            paint.setColor(colors.get(index))
                            paint.style = Paint.Style.STROKE

                            val rect = RectF(
                                locations.get(x + 1) * w,
                                locations.get(x) * h,
                                locations.get(x + 3) * w,
                                locations.get(x + 2) * h
                            )
                            canvas.drawRect(rect, paint)

                            paint.style = Paint.Style.FILL
                            canvas.drawText(
                                labels.get(
                                    classes.get(index).toInt()
                                ) + " " + fl.toString(),
                                locations.get(x + 1) * w,
                                locations.get(x) * h,
                                paint
                            )
                        }
                    }

                    imageView.setImageBitmap(mutable)
                }
            }
        }

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val newAttemptButton = findViewById<Button>(R.id.newAttemptButton)
        val captureButton = findViewById<Button>(R.id.captureButton)

        captureButton.setOnClickListener {
            isThreadRunning = false
            captureButton.visibility = View.GONE
            newAttemptButton.visibility = View.VISIBLE
        }

        newAttemptButton.setOnClickListener {
            isThreadRunning = true
            newAttemptButton.visibility = View.GONE
            captureButton.visibility = View.VISIBLE
        }

        imageView.setOnClickListener {
            val clickX = it.x
            val clickY = it.y

            var x = 0

            scores.forEachIndexed { index, fl ->
                x = index
                x *= 4

                if (fl > 0.5) {

                    val rect = RectF(
                        locations.get(x + 1) * imageView.width,
                        locations.get(x) * imageView.height,
                        locations.get(x + 3) * imageView.width,
                        locations.get(x + 2) * imageView.height
                    )

                    if (rect.contains(clickX, clickY)) {
                        return@forEachIndexed
                    }
                }
            }
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }

    @SuppressLint("MissingPermission")
    fun open_camera(){
        cameraManager.openCamera(cameraManager.cameraIdList[0], object:CameraDevice.StateCallback(){
            override fun onOpened(p0: CameraDevice) {
                cameraDevice = p0

                var surfaceTexture = textureView.surfaceTexture
                var surface = Surface(surfaceTexture)

                var captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequest.addTarget(surface)

                cameraDevice.createCaptureSession(listOf(surface), object: CameraCaptureSession.StateCallback(){
                    override fun onConfigured(p0: CameraCaptureSession) {
                        p0.setRepeatingRequest(captureRequest.build(), null, null)
                    }
                    override fun onConfigureFailed(p0: CameraCaptureSession) {
                    }
                }, handler)
            }

            override fun onDisconnected(p0: CameraDevice) {

            }

            override fun onError(p0: CameraDevice, p1: Int) {

            }
        }, handler)
    }

    fun get_permission(){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
            get_permission()
        }
    }
}