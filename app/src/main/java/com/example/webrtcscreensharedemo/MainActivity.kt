package com.example.webrtcscreensharedemo

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.example.webrtcscreensharedemo.Util.Companion.getFullScreenFlag
import com.example.webrtcscreensharedemo.Util.Companion.getSystemUiVisibility
import com.example.webrtcscreensharedemo.webrtc.AppRTCClient
import com.example.webrtcscreensharedemo.webrtc.PeerConnectionClient
import com.example.webrtcscreensharedemo.webrtc.WebRtcClient
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.VideoCapturer

class MainActivity : AppCompatActivity(), WebRtcClient.RtcListener {
    val TAG = MainActivity::class.java.simpleName
    private val CAPTURE_PERMISSION_REQUEST_CODE = 1
    private val MANDATORY_PERMISSIONS = arrayOf(
        "android.permission.MODIFY_AUDIO_SETTINGS",
        "android.permission.RECORD_AUDIO", "android.permission.INTERNET"
    )
    var STREAM_NAME_PREFIX = "android_device_stream"
    private var mediaProjectionPermissionResultData: Intent? = null
    private var mediaProjectionPermissionResultCode = 0
    private var iceConnected = false
    private var signalingParameters: AppRTCClient.SignalingParameters? = null
    private val SCREEN_RESOLUTION_SCALE = 2
    private var sDeviceWidth = 0
    private var sDeviceHeight = 0
    private var mWebRtcClient: WebRtcClient? = null

    fun reportError(info: String?) {
        Log.e(TAG, info!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set window styles for fullscreen-window size. Needs to be done before adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(getFullScreenFlag())
        window.decorView.systemUiVisibility = getSystemUiVisibility()
        setContentView(R.layout.activity_main)

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        sDeviceWidth = metrics.widthPixels
        sDeviceHeight = metrics.heightPixels

        checkPermission()
        startScreenCapture()
    }

    private fun checkPermission() {
        for (permission in MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                setResult(Activity.RESULT_CANCELED)
//                finish()
                return
            }
        }
    }

    @TargetApi(21)
    private fun startScreenCapture() {
        val mediaProjectionManager =
            application.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(),
            CAPTURE_PERMISSION_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAPTURE_PERMISSION_REQUEST_CODE) {
            mediaProjectionPermissionResultCode = resultCode
            mediaProjectionPermissionResultData = data
            init()
        }
    }

    private fun init() {
        val peerConnectionParameters: PeerConnectionClient.PeerConnectionParameters =
            PeerConnectionClient.PeerConnectionParameters(
                true,
                false,
                true,
                sDeviceWidth / SCREEN_RESOLUTION_SCALE,
                sDeviceHeight / SCREEN_RESOLUTION_SCALE,
                0,
                0,
                "VP8",
                false,
                true,
                0,
                "OPUS",
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                null
            )

        mWebRtcClient = WebRtcClient(
            applicationContext,
            this,
            createScreenCapturer(),
            peerConnectionParameters
        )

    }

    override fun onCall(applicant: String?) {

    }

    override fun onStatusChanged(newStatus: String?) {

    }

    override fun onReady(remoteId: String?) {
        mWebRtcClient!!.start(STREAM_NAME_PREFIX)
    }

    override fun onHandup() {

    }

    private fun createScreenCapturer(): VideoCapturer? {
        if (mediaProjectionPermissionResultCode != Activity.RESULT_OK) {
            reportError("User didn't give permission to capture the screen.")
            return null
        }
        return ScreenCapturerAndroid(
            mediaProjectionPermissionResultData,
            object : MediaProjection.Callback() {
                override fun onStop() {
                    reportError("User revoked permission to capture the screen.")
                }
            })
    }

}