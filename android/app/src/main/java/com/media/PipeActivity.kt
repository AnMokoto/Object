package com.media

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.mediapipe.components.CameraHelper
import com.google.mediapipe.components.CameraXPreviewHelper
import com.google.mediapipe.components.ExternalTextureConverter
import com.google.mediapipe.components.FrameProcessor
import com.google.mediapipe.framework.AndroidAssetUtil
import com.google.mediapipe.framework.AndroidPacketCreator
import com.google.mediapipe.framework.Packet
import com.google.mediapipe.framework.PacketGetter
import com.google.mediapipe.glutil.EglManager
import com.google.mediapipe.modules.objectron.calculators.protos.AnnotationProto
import com.lefu.mannequinchallenge.R
import com.mokoto.pipe.P3
import com.mokoto.pipe.Pipe
import kotlinx.android.synthetic.main.activity_pipe.*
import java.util.*

class PipeActivity : AppCompatActivity() {
    private val TAG = "PipeActivity"

    private var previewFrameTexture: SurfaceTexture? = null
    private val TFLITE_NAME = "object_detection_3d.tflite"
    private val BINARY_GRAPH_NAME = "mobile_gpu_binary_graph.binarypb"
    private val INPUT_VIDEO_STREAM_NAME = "input_video"
    private val OUTPUT_VIDEO_STREAM_NAME = "output_video"
    private val FLIP_FRAME_VERTICALLY = true // flipFramesVertically
    private val CONVERTER_NUM_BUFFERS = 2 // converterNumBuffers
    private val OBJ_TEXTURE = "texture.jpg"
    private val OBJ_FILE = "model.obj.uuu"
    private val BOX_TEXTURE = "classic_colors.png"
    private val BOX_FILE = "box.obj.uuu"

    private var eglManager: EglManager? = null
    private var processor: FrameProcessor? = null

    private var cameraHelper: CameraXPreviewHelper? = null
    private var converter: ExternalTextureConverter? = null
    private val size = Size(1280, 960)

    init {
        System.loadLibrary("mediapipe_jni");
        System.loadLibrary("opencv_java3");
        System.loadLibrary("pipe")
    }

    private val lunchCamera by lazy {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                startCamera()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pipe)

        AndroidAssetUtil.initializeNativeAssetManager(this)

        initializationPipe()

        lunchCamera.contract
    }

    private fun initializationPipe() {
        eglManager = EglManager(null)
        processor = FrameProcessor(
            this,
            eglManager!!.nativeContext,
            BINARY_GRAPH_NAME,
            INPUT_VIDEO_STREAM_NAME,
            OUTPUT_VIDEO_STREAM_NAME
        ).also {
            it.videoSurfaceOutput.setFlipY(FLIP_FRAME_VERTICALLY)
//            objectron(it)
            objectron3d(it)
//
        }
        processor?.addPacketCallback("lifted_objects") { //detected_objects mask_model_matrices
            if (it.isEmpty.not()) {
                val packet = it.copy()
                val protoBytes = PacketGetter.getProtoBytes(packet)
                val frameAnnotation = AnnotationProto.FrameAnnotation.parseFrom(protoBytes)
                frameAnnotation.annotationsList.forEach {
                    val p = it.keypointsList.map { p -> p.point3D }
                        .map { p3 -> P3(p3.x, p3.y, p3.z) }
                        .toTypedArray()

                    val float = Pipe.calculateDepth(
                        size.width,
                        size.height,
                        arrayOf(
                            p[4]
                            , p[1], p[5]
                            , p[6], p[8]
//                            , p[1], p[2]
                        )
                    )
                    Log.d(TAG, "float = $float")
                    runOnUiThread {
                        module_view_distance.setText("".plus(float))
                    }
                }
            }
            it.release()
        }

        cameraHelper = CameraXPreviewHelper()
    }

    private fun objectron(it: FrameProcessor) {
        val packetCreator: AndroidPacketCreator = it.packetCreator
        val inputSidePackets: MutableMap<String, Packet> = HashMap()
        inputSidePackets["box_landmark_model_path"] = packetCreator.createString(TFLITE_NAME)
        inputSidePackets["allowed_labels"] = packetCreator.createString("Coffee cup,Mug")
        inputSidePackets["max_num_objects"] = packetCreator.createInt32(5)
        //calculator_params
//        inputSidePackets["ConstantSidePacketCalculator.packet"] = packetCreator.createBool(true)
//        inputSidePackets["objectdetectionoidv4subgraph.__TensorsToDetectionsCalculator.min_score_thresh"] = packetCreator.createFloat32(0.5f)
//        inputSidePackets["boxlandmarksubgraph__ThresholdingCalculator.threshold"] = packetCreator.createFloat32(0.99f)
//        inputSidePackets["Lift2DFrameAnnotationTo3DCalculator.normalized_focal_x"] = packetCreator.createFloat32(1.0f)
//        inputSidePackets["Lift2DFrameAnnotationTo3DCalculator.normalized_focal_y"] = packetCreator.createFloat32(1.0f)
//        inputSidePackets["Lift2DFrameAnnotationTo3DCalculator.normalized_principal_point_x"] = packetCreator.createFloat32(0f)
//        inputSidePackets["Lift2DFrameAnnotationTo3DCalculator.normalized_principal_point_y"] = packetCreator.createFloat32(0f)

        it.setInputSidePackets(inputSidePackets)


    }

    private fun objectron3d(it: FrameProcessor) {
        val packetCreator: AndroidPacketCreator = it.packetCreator
        val inputSidePackets: MutableMap<String, Packet> = HashMap()
        inputSidePackets["box_landmark_model_path"] = packetCreator.createString(TFLITE_NAME)
        inputSidePackets["obj_asset_name"] = packetCreator.createString(OBJ_FILE)//
        inputSidePackets["box_asset_name"] = packetCreator.createString(BOX_FILE)//
        inputSidePackets["obj_texture"] = packetCreator.createRgbaImageFrame(
            BitmapFactory.decodeStream(
                assets.open(OBJ_TEXTURE)
            )
        )//
        inputSidePackets["box_texture"] = packetCreator.createRgbaImageFrame(
            BitmapFactory.decodeStream(
                assets.open(BOX_TEXTURE)
            )
        )//
        inputSidePackets["allowed_labels"] = packetCreator.createString("Coffee cup,Mug")//
        inputSidePackets["max_num_objects"] = packetCreator.createInt32(2)//
        inputSidePackets["model_scale"] =
            packetCreator.createFloat32Array(floatArrayOf(500f, 500f, 500f))//
//                packetCreator.createFloat32Array(floatArrayOf(0.25f, 0.25f, 0.12f))//
        inputSidePackets["model_transformation"] =
            packetCreator.createFloat32Array(
                doubleArrayOf(
                    1.0, 0.0, 0.0, 0.0,
                    0.0, 0.0, 1.0, -0.001,
                    0.0, -1.0, 0.0, 0.0,
                    0.0, 0.0, 0.0, 1.0
                ).map { it.toFloat() }.toFloatArray()
//                    doubleArrayOf(
//                        1.0, 0.0, 0.0, 0.0,
//                        0.0, 0.0, 1.0, 0.0,
//                        0.0, -1.0, 0.0, 0.0,
//                        0.0, 0.0, 0.0, 1.0
//                    ).map { it.toFloat() }.toFloatArray()
            )//
        it.setInputSidePackets(inputSidePackets)
    }

    override fun onResume() {
        super.onResume()
        converter = ExternalTextureConverter(
            eglManager?.context,
            CONVERTER_NUM_BUFFERS
        ).apply {
            setFlipY(FLIP_FRAME_VERTICALLY)
            setConsumer(processor)
        }.also {
            lunchCamera.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onPause() {
        super.onPause()
        converter?.close()
    }

    override fun onDestroy() {
        super.onDestroy()
    }


    private fun initializationView() {

        val surfaceView = SurfaceView(this)
        module_view_finder.addView(surfaceView)
        surfaceView.visibility = View.VISIBLE
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                onPreviewDisplaySurfaceChanged(holder, format, width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                processor?.videoSurfaceOutput?.setSurface(null)
            }

            override fun surfaceCreated(holder: SurfaceHolder) {
                processor?.videoSurfaceOutput?.setSurface(holder.surface)
            }

        })

    }

    private fun getCameraCharacteristics(
        context: Context,
        lensFacing: Int
    ): CameraCharacteristics? {
        val cameraManager = context.getSystemService("camera") as CameraManager
        try {
            val cameraList = cameraManager.cameraIdList.toList()
            val var4 = cameraList.iterator()
            while (var4.hasNext()) {
                val availableCameraId = var4.next()
                val availableCameraCharacteristics =
                    cameraManager.getCameraCharacteristics(availableCameraId)
                val availableLensFacing =
                    availableCameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                if (availableLensFacing != null && availableLensFacing == lensFacing) {
                    return availableCameraCharacteristics
                }
            }
        } catch (var8: CameraAccessException) {
            Log.e(
                "CameraXPreviewHelper",
                "Accessing camera ID info got error: $var8"
            )
        }
        return null
    }

    private fun startCamera() {
        cameraHelper?.setOnCameraStartedListener {
            previewFrameTexture = it
            initializationView()

            val focalLengthPixels = cameraHelper?.focalLengthPixels ?: -1f
            val c = getCameraCharacteristics(this, 1)
            val focalLength =
                c?.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.get(0) ?: -1f
            val sensor = c?.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
            Log.d(TAG, "focalLength=$focalLength")
            Log.d(TAG, "sensor=${sensor?.width} + ${sensor?.height}")
            if (focalLengthPixels <= 1) {
                Pipe.computeFocalLengthInPixels(size.width, size.height,focal_length_mm = focalLength)

                Log.d(TAG, "focalLengthPixels-math = $focalLengthPixels")
            } else {
                Pipe.initialization(focalLengthPixels, resources.displayMetrics.density/0.75f)
            }
            Log.d(TAG, "focalLengthPixels=$focalLengthPixels")

        }

        cameraHelper?.startCamera(
            this,
//            this,
            CameraHelper.CameraFacing.BACK,
            null,
            size
        )
    }


    private fun computeViewSize(width: Int, height: Int): Size {
        return Size(width, height) // Prefer 3:4 aspect ratio.
    }

    private fun onPreviewDisplaySurfaceChanged(
        holder: SurfaceHolder, format: Int, width: Int, height: Int
    ) {
        // (Re-)Compute the ideal size of the camera-preview display (the area that the
        // camera-preview frames get rendered onto, potentially with scaling and rotation)
        // based on the size of the SurfaceView that contains the display.
        val viewSize = computeViewSize(width, height)
        val displaySize = cameraHelper!!.computeDisplaySizeFromViewSize(viewSize)
        val isCameraRotated = cameraHelper!!.isCameraRotated

        // Connect the converter to the camera-preview frames as its input (via
        // previewFrameTexture), and configure the output width and height as the computed
        // display size.
        converter?.setSurfaceTextureAndAttachToGLContext(
            previewFrameTexture,
            if (isCameraRotated) displaySize.height else displaySize.width,
            if (isCameraRotated) displaySize.width else displaySize.height
        )


        val cameraImageSize = cameraHelper!!.frameSize
        processor?.setOnWillAddFrameListener { timestamp ->
            try {
                var cameraTextureWidth =
                    if (isCameraRotated) cameraImageSize.height else cameraImageSize.width
                var cameraTextureHeight =
                    if (isCameraRotated) cameraImageSize.width else cameraImageSize.height

                // Find limiting side and scale to 3:4 aspect ratio
                val aspectRatio =
                    cameraTextureWidth.toFloat() / cameraTextureHeight.toFloat()
                if (aspectRatio > 3.0 / 4.0) {
                    // width too big
                    cameraTextureWidth =
                        (cameraTextureHeight.toFloat() * 3.0 / 4.0).toInt()
                } else {
                    // height too big
                    cameraTextureHeight =
                        (cameraTextureWidth.toFloat() * 4.0 / 3.0).toInt()
                }

                Log.d(TAG, "aspectRatio=$aspectRatio")
                Log.d(TAG, "cameraTextureWidth=$cameraTextureWidth")
                Log.d(TAG, "cameraTextureHeight=$cameraTextureHeight")
                val widthPacket: Packet? = processor?.packetCreator?.createInt32(cameraTextureWidth)
                val heightPacket: Packet? =
                    processor?.packetCreator?.createInt32(cameraTextureHeight)
                try {
                    processor?.graph?.addPacketToInputStream("input_width", widthPacket, timestamp)
                    processor?.graph?.addPacketToInputStream(
                        "input_height",
                        heightPacket,
                        timestamp
                    )
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "MediaPipeException encountered adding packets to input_width and input_height input streams.",
                        e
                    )
                }
                widthPacket?.release()
                heightPacket?.release()
            } catch (ise: Exception) {
                Log.e(TAG, "Exception while adding packets to width and height input streams.")
            }
        }
    }
}