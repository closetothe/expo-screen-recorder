package expo.modules.screenrecorder

import android.Manifest
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.Promise
import android.app.Activity
import android.content.Context
import android.media.MediaCodecList
import android.app.Application
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Environment
import android.util.DisplayMetrics
import androidx.core.content.ContextCompat
import expo.modules.kotlin.exception.CodedException
import java.io.File
import java.util.UUID

import com.hbisoft.hbrecorder.HBRecorder
import com.hbisoft.hbrecorder.HBRecorderListener
import expo.modules.kotlin.types.Enumerable

class ExpoScreenRecorderModule : Module(), HBRecorderListener {
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var startRecordingPromise: Promise? = null
    private var stopRecordingPromise: Promise? = null
    private var hbRecorder: HBRecorder? = null
    private var outputUri: File? = null
    private var isRecording = false
    private var isPaused = false
    private var hdVideoEnabled = false

    override fun HBRecorderOnStart() {
        println("HBRecorderOnStart")
        isRecording = true
        isPaused = false
    }

    override fun HBRecorderOnComplete() {
        println("HBRecorderOnComplete")
        isRecording = false
        isPaused = false

        if (stopRecordingPromise != null) {
            val uri = hbRecorder!!.filePath
            println("uri!!! $uri")
            stopRecordingPromise!!.resolve(uri)
            stopRecordingPromise = null
        }
    }

    override fun HBRecorderOnError(errorCode: Int, reason: String?) {
        val errorMessage = "ScreenRecorderError: $errorCode ${reason ?: ""}"
        isRecording = false
        isPaused = false

        startRecordingPromise?.reject(CodedException(errorMessage))
        startRecordingPromise = null
        stopRecordingPromise?.reject(CodedException(errorMessage))
        stopRecordingPromise = null

        println("HBRecorderOnError")
        println("errorCode: $errorCode")
        println("reason: $reason")
    }

    override fun HBRecorderOnPause() {
        println("HBRecorderOnPause")
        isPaused = true
    }

    override fun HBRecorderOnResume() {
        println("HBRecorderOnResume")
        isPaused = false
    }

    private fun getScreenDimensions(): Pair<Int, Int> {
        val activity = appContext.activityProvider?.currentActivity as? Activity
            ?: throw Exception("Activity is null")

        val width: Int
        val height: Int

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // For Android 11 (API 30) and above
            val windowMetrics = activity.windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            width = bounds.width()
            height = bounds.height()
        } else {
            // For older Android versions
            @Suppress("DEPRECATION")
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            width = displayMetrics.widthPixels
            height = displayMetrics.heightPixels
        }

        println("Screen dimensions: ${width}x${height}")
        return Pair(width, height)
    }
    private fun setup(micEnabled: Boolean) {
        println("setup...")
        outputUri = appContext.reactContext!!.getExternalFilesDir("ExpoScreenRecorder")
        println("outputUri ${outputUri.toString()}")
        hbRecorder = HBRecorder(appContext.reactContext!!, this)

        hbRecorder!!.enableCustomSettings()
        hbRecorder!!.isAudioEnabled(micEnabled)
        hbRecorder!!.setOutputPath(outputUri!!.toString())

        // Set screen dimensions based on device screen
        val (width, height) = getScreenDimensions()
        hbRecorder!!.setScreenDimensions(height, width)

        // Apply HD video setting if it was set before recording started
        if (hdVideoEnabled) {
            hbRecorder!!.recordHDVideo(true)
        }

        if (doesSupportEncoder("h264")) {
            println("doesSupportEncoder: h264")
            hbRecorder!!.setVideoEncoder("H264")
        } else {
            println("doesSupportEncoder: DEFAULT")
            hbRecorder!!.setVideoEncoder("DEFAULT")
        }
    }

    override fun definition() = ModuleDefinition {
        Name("ExpoScreenRecorder")

        AsyncFunction("startRecording") { micEnabled: Boolean, promise: Promise ->
            startRecordingPromise = promise
            println("React startRecording")
            val activity = appContext.activityProvider?.currentActivity as? Activity
            if (activity == null) {
                promise.reject(CodedException("Activity is null"))
                return@AsyncFunction
            }

            val permissionsGranted = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (micEnabled && !permissionsGranted){
                promise.reject(CodedException("Audio Recording Permission for the app not granted. Handle on JS side (use expo-audio)."));
                return@AsyncFunction
            }

            if (isRecording) {
                promise.reject(CodedException("Recording is already in progress"))
                return@AsyncFunction
            }

            println("setup...")
            if (hbRecorder == null) {
                try {
                    setup(micEnabled)
                } catch (e: Exception) {
                    promise.reject(CodedException("Screen recorder initialization failed: ${e.message}"))
                    return@AsyncFunction
                }
            }
            println("done setup!")
            mediaProjectionManager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val captureIntent = mediaProjectionManager?.createScreenCaptureIntent()
            (appContext.currentActivity)?.startActivityForResult(captureIntent, SCREEN_RECORD_REQUEST_CODE)
        }

        AsyncFunction("stopRecording") { fileName: String?, promise: Promise ->
            println("React stopRecording")
            stopRecordingPromise = promise

            if (!isRecording) {
                promise.reject(CodedException("No recording in progress to stop"))
                stopRecordingPromise = null
                return@AsyncFunction
            }

            if (hbRecorder == null) {
                stopRecordingPromise?.reject(CodedException("Screen recorder not initialized"))
                stopRecordingPromise = null
            } else {
                hbRecorder!!.stopScreenRecording()
            }
        }

        AsyncFunction("pauseRecording") { promise: Promise ->
            println("React pauseRecording")

            if (!isRecording) {
                promise.reject(CodedException("No recording in progress. Please start recording before attempting to pause."))
                return@AsyncFunction
            }

            if (isPaused) {
                promise.reject(CodedException("Recording is already paused"))
                return@AsyncFunction
            }

            if (hbRecorder == null) {
                promise.reject(CodedException("Screen recorder not initialized"))
                return@AsyncFunction
            }

            try {
                hbRecorder!!.pauseScreenRecording()
                promise.resolve(null)
            } catch (e: Exception) {
                promise.reject(CodedException("Failed to pause recording: ${e.message}"))
            }
        }

        AsyncFunction("resumeRecording") { promise: Promise ->
            println("React resumeRecording")

            if (!isRecording) {
                promise.reject(CodedException("No recording in progress. Please start recording before attempting to resume."))
                return@AsyncFunction
            }

            if (!isPaused) {
                promise.reject(CodedException("Recording is not paused. Nothing to resume."))
                return@AsyncFunction
            }

            if (hbRecorder == null) {
                promise.reject(CodedException("Screen recorder not initialized"))
                return@AsyncFunction
            }

            try {
                hbRecorder!!.resumeScreenRecording()
                promise.resolve(null)
            } catch (e: Exception) {
                promise.reject(CodedException("Failed to resume recording: ${e.message}"))
            }
        }

        Function("isBusyRecording") {
            if (hbRecorder == null) {
                return@Function false
            }
            return@Function hbRecorder!!.isBusyRecording
        }

        Function("getState") {
            return@Function getRecordingState()
        }


        AsyncFunction("recordHDVideo") { enable: Boolean, promise: Promise ->
            println("React recordHDVideo: $enable")

            if (isRecording) {
                promise.reject(CodedException("Cannot change HD video setting while recording is in progress. Please call recordHDVideo() before startRecording()."))
                return@AsyncFunction
            }

            // Store the setting to be applied when recording starts
            hdVideoEnabled = enable

            // If hbRecorder is already initialized but not recording, apply the setting
            if (hbRecorder != null) {
                try {
                    hbRecorder!!.recordHDVideo(enable)
                    promise.resolve(null)
                } catch (e: Exception) {
                    promise.reject(CodedException("Failed to set HD video: ${e.message}"))
                }
            } else {
                // Just store the setting for later
                promise.resolve(null)
            }
        }

        OnActivityResult { activity, onActivityResultPayload ->
            val requestCode = onActivityResultPayload.requestCode
            val resultCode = onActivityResultPayload.resultCode
            val data = onActivityResultPayload.data
            println("OnActivityResult...")

            if (hbRecorder == null) {
                startRecordingPromise?.reject(CodedException("Screen recorder not initialized"))
                startRecordingPromise = null
            } else if (requestCode == SCREEN_RECORD_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                hbRecorder!!.startScreenRecording(data, resultCode)
                startRecordingPromise?.resolve(null)
                startRecordingPromise = null
            } else {
                startRecordingPromise?.reject(CodedException("Screen recording permission denied"))
                startRecordingPromise = null
            }
        }
    }

    private fun getOutputFile(fileName: String? = null): File {
        val name = (fileName ?: UUID.randomUUID().toString()) + ".mp4"
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), name)
    }

    companion object {
        private const val SCREEN_RECORD_REQUEST_CODE = 1001
    }

    private fun doesSupportEncoder(encoder: String): Boolean {
        val list = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
        val size = list.size
        for (i in 0 until size) {
            val codecInfo = list[i]
            if (codecInfo.isEncoder) {
                if (codecInfo.name.contains(encoder)) {
                    return true
                }
            }
        }
        return false
    }

    private fun getRecordingState(): State {
        if (isRecording) {
            if (isPaused) {
                return State.Paused
            }
            return State.Recording
        }
        return State.Idle
    }

    enum class State(val value: String) : Enumerable{
        Idle("idle"),
        Recording("recording"),
        Paused("paused")
    }

}
