package com.lefu.mannequinchallenge

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.MotionEvent
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import org.pytorch.IValue
import org.pytorch.PyTorchAndroid
import org.pytorch.torchvision.TensorImageUtils
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import kotlin.math.*

@RuntimePermissions
class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    private val xyz = Array(480) { Array<Any>(640) { } }

    private val module by lazy {
        val pt = "pytorch_scripted.pt"
        val module = PyTorchAndroid.loadModuleFromAsset(assets, pt) // same as Module.load
        module
    }

    private val imageCapture by lazy {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
////            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetResolution(Size(640, 480))
            .setTargetRotation(Surface.ROTATION_90)
//            .setTargetRotation(windowManager.defaultDisplay.rotation)

//            .setDefaultResolution(Size(640,480))//640/480
            .build()
    }

    private val preview by lazy {
        Preview.Builder()
//                .setTargetAspectRatio(640 / 480)
//            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetResolution(Size(640, 480))
//            .setTargetRotation(Surface.ROTATION_90)
            .setTargetRotation(windowManager.defaultDisplay.rotation)
            .build()
            .also {
                it.setSurfaceProvider(module_view_finder.surfaceProvider)
            }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        module_cover.doOnPreDraw {
            resources.displayMetrics.widthPixels
                .also {
                    val w = (it / 2f).roundToInt()
                    val ratio = 640 / 480f
                    val h = w.div(ratio).roundToInt()
                    module_cover.setAspectRatio(w, h)
                    module_cover.layoutParams = module_cover.layoutParams.apply {
                        width = w
                        height = h
                    }
//                    module_depth.setAspectRatio(w, h)
                    module_view_finder.layoutParams = module_view_finder.layoutParams.apply {
                        width = w
                        height = h
                    }
                    module_view_finder.requestLayout()
                }
        }

        module_cover.setOnClickListener {
            module_cover.setImageBitmap(BitmapFactory.decodeStream(assets.open("deep_1622016818.png")))
        }

        module_cover.performClick()

        module_view_forward.setOnClickListener {
            GlobalScope.launch {
                val bitmap = withContext(Dispatchers.IO) {
//                    val b = BitmapFactory.decodeStream(assets.open("deep_1622016818.png"))
//                    withContext(Dispatchers.Main) {
//                        module_cover.setImageBitmap(b)
//                    }
                    val b = (module_cover.drawable as BitmapDrawable).bitmap

                    floatArrayToBitmap2(doForward(b), b.width, b.height)
                }

                runBlocking(Dispatchers.Main) {
                    module_depth.setImageBitmap(bitmap)
                }

            }
        }

        imageCapture.setCropAspectRatio(Rational(4, 3))

        module_view_capture.setOnClickListener {
            imageCapture.takePicture(ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val buffer = image.planes[0].buffer.also {
                            it.rewind()
                        }

                        val byteArray = ByteArray(buffer.remaining())
                        buffer.get(byteArray)

                        image.close()

                        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                        ContextCompat.getMainExecutor(this@MainActivity).execute(
                            Runnable {
                                module_cover.setImageBitmap(rotateBitmap(bitmap, 90))
                            }
                        )
                    }

                    override fun onError(exception: ImageCaptureException) {
                        super.onError(exception)
                    }
                })
        }

        module_depth.setOnTouchListener { v, event ->
            if (module_depth.drawable != null && event.action == MotionEvent.ACTION_MOVE) {
                val x_y = floatArrayOf(event.x, event.y)
                val imageMatrix = module_depth.imageMatrix
                val matrix = Matrix()
                imageMatrix.invert(matrix)
                val xy = FloatArray(2)
                matrix.mapPoints(xy, x_y)
                module_text.text = "down: ${event.x} & ${event.y}"
                module_text.append("\n")
                module_text.append("rel:  ${xy[0]} & ${xy[1]}")



                try {
                    val z = xyz[xy[1].roundToInt()][xy[0].roundToInt()] as IntArray
                    module_text.append("\n z = ${255 - z[2]}")
                } catch (e: Exception) {
                    e.printStackTrace()
                }


            }
            true
        }

        startCameraWithPermissionCheck()

    }


    @NeedsPermission(
        value = [Manifest.permission.CAMERA]
    )
    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    suspend fun doForward(bitmap: Bitmap): FloatArray {
        // same as
        // transforms.Compose([
        // ...
        // transforms.Normalize(menu,std)
        // ]}
        val tensor = TensorImageUtils.bitmapToFloat32Tensor(
            bitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
            TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )

        // to Variable
        val value = IValue.from(tensor)

        val output = module.forward(value).toTuple()

        val prediction_d = output[0].toTensor()
        val pred_confidence = output[1]

        //
        val scores = transpose(prediction_d.dataAsFloatArray)
        return scores
    }

    /**
     *
     */
    fun transpose(floatArray: FloatArray): FloatArray {
        val max = floatArray.max() ?: 1f
        return floatArray
            .map { 1f.div(exp(it)) }
//            .map { it.div(max) }
//            .flatMap { listOf(it, it, it) }
////            .map { it.times(255).roundToInt() }
            .toFloatArray()

    }

    private fun floatArrayToBitmap2(floatArray: FloatArray, width: Int, height: Int): Bitmap {

        // Create empty bitmap in RGBA format
        val bmp: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height * 4)

//        val floatArray = feature_normalize(floatArray)

        val maxValue = floatArray.max() ?: 1.0f
        val minValue = floatArray.min() ?: -1.0f
        val delta = maxValue - minValue

        // Define if float min..max will be mapped to 0..255 or 255..0
        val conversion = { v: Float ->
            {
                val value = v.div(maxValue).times(255f).roundToInt()
//                val value = ((v - minValue) / delta * 255.0f).roundToInt()
                intArrayOf(value, value, value)
            }
        }

        val arrays = floatArray.map {
            conversion(it).invoke()
        }

        GlobalScope.launch {
            ont2Two(arrays.toTypedArray())
        }

        // copy each value from float array to RGB channels and set alpha channel
        for (i in 0 until width * height) {
            val a = arrays[i]
//            val r = conversion(floatArray[i])
//            val g = conversion(floatArray[i + 1])
//            val b = conversion(floatArray[i + 2])
            pixels[i] = Color.rgb(a[0], a[1], a[2])
        }
        bmp.setPixels(pixels, 0, width, 0, 0, width, height)

        return bmp
    }

    /**
     * for rgb image
     */
    private fun floatArrayToBitmap(floatArray: FloatArray, width: Int, height: Int): Bitmap {

        // Create empty bitmap in RGBA format
        val bmp: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height * 4)

        // mapping smallest value to 0 and largest value to 255
        val maxValue = floatArray.max() ?: 1.0f
        val minValue = floatArray.min() ?: -1.0f
        val delta = maxValue - minValue

        // Define if float min..max will be mapped to 0..255 or 255..0
        val conversion = { v: Float -> ((v - minValue) / delta * 255.0f).roundToInt() }

        // copy each value from float array to RGB channels and set alpha channel
        for (i in 0 until width * height) {
            val r = conversion(floatArray[i])
            val g = conversion(floatArray[i + width * height])
            val b = conversion(floatArray[i + 2 * width * height])
            pixels[i] = Color.rgb(r, g, b)
        }
        bmp.setPixels(pixels, 0, width, 0, 0, width, height)

        return bmp
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    fun rotateBitmap(bitmap: Bitmap, angle: Int): Bitmap? {
//        val matrix = Matrix()
//        matrix.postRotate(angle.toFloat())
//        return Bitmap.createBitmap(
//            bitmap,
//            0,
//            0,
//            bitmap.width,
//            bitmap.height,
//            matrix,
//            true
//        )
        return bitmap;
    }

    suspend fun ont2Two(array: Array<IntArray>, width: Int = 640, height: Int = 480) {

        assert(array.size >= width * height) {
            "OutOfIndex!!"
        }

//        # 相机内参
        val cam_fx = 454.18441644
        val cam_fy = 454.4042991
        val cam_cx = 319.5
        val cam_cy = 239.5
        val factor = 1

//        depth = depth_img[v, u, z - 1]
//        x_over_z = (cam_cx - u) / cam_fx
//        y_over_z = (cam_cy - v) / cam_fy
//        zd = depth / np.sqrt(1. + x_over_z ** 2 + y_over_z ** 2)
//        x = x_over_z * zd
//        y = y_over_z * zd

        var index = 0
        for (k in 0 until height) {
            for (j in 0 until width) {
                val depth = array[index][2]
                val x_over_z = (cam_cx - j) / cam_fx
                val y_over_z = (cam_cy - k) / cam_fy
                val z = depth / sqrt(1f + x_over_z.pow(2) + y_over_z.pow(2))
                val x = x_over_z * z
                val y = y_over_z * z
                xyz[k][j] = arrayOf(x, y, z).map { it.roundToInt() }.toIntArray()
//                xyz[k][j] = array[index]
                index += 1
            }
        }
    }

    /**
     *
     *
     *
     * <p>
     *
     * def feature_normalize(data):
     *  mu = np.mean(data, axis=0)  # 均值
     *  std = np.std(data, axis=0)  # 标准值
     *  return (data - mu) / std
     *
     *
     */
    fun feature_normalize(floatArray: FloatArray): FloatArray {
        val mean = floatArray.average().toFloat()
        // std = sqrt(mean(abs(x - x.mean())**2))
        val std = sqrt(floatArray.map { abs(it - mean).pow(2) }.average().toFloat())


        return floatArray.map { (it - mean) / std }.toFloatArray()
    }
}