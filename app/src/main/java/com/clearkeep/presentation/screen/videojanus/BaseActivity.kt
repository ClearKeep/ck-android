package com.clearkeep.presentation.screen.videojanus

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.clearkeep.common.utilities.printlnCK

abstract class BaseActivity : AppCompatActivity() {
    private var isOpenSettingScreen = false

    private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _: ActivityResult ->
        printlnCK("onActivityResult, open setting")
        isOpenSettingScreen = false
        requestCallPermissions()
    }

    fun requestCallPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED)
                || (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED)
                || (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.MODIFY_AUDIO_SETTINGS
                )
                        != PackageManager.PERMISSION_GRANTED)
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA,
                        Manifest.permission.MODIFY_AUDIO_SETTINGS
                    ), REQUEST_PERMISSIONS
                )
                return
            }
        }
        onPermissionsAvailable()
    }

    fun openSettingScreen() {
        isOpenSettingScreen = true
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        startForResult.launch(intent)
    }

    override fun onUserLeaveHint() {
        printlnCK("onUserLeaveHint: $isOpenSettingScreen")
        if (hasSupportPIP() && !isOpenSettingScreen) {
            enterPIPMode()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS && grantResults.isNotEmpty()) {
            var allPermissionsGranted = true
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }
            if (!allPermissionsGranted) {
                var somePermissionsForeverDenied = false
                var somePermissionsDenied = false
                for (permission in permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                        //denied
                        somePermissionsDenied = true
                    } else {
                        if (ActivityCompat.checkSelfPermission(
                                this,
                                permission
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            //allowed
                        } else {
                            //set to never ask again
                            somePermissionsForeverDenied = true
                        }
                    }
                }

                if (somePermissionsDenied) {
                    onPermissionsDenied()
                    return
                }
                if (somePermissionsForeverDenied) {
                    onPermissionsForeverDenied()
                }
            } else {
                onPermissionsAvailable()
            }
        } else {
            onPermissionsDenied()
        }
    }

    fun hasSupportPIP(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    fun enterPIPMode() {
        if (hasSupportPIP()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val aspectRatio = Rational(3, 4)
                val params =
                    PictureInPictureParams.Builder().setAspectRatio(aspectRatio).build()
                enterPictureInPictureMode(params)
            } else {
                enterPictureInPictureMode()
            }
        }
    }

    abstract fun onPermissionsAvailable()

    abstract fun onPermissionsDenied()

    abstract fun onPermissionsForeverDenied()

    companion object {
        private const val REQUEST_PERMISSIONS = 1
    }
}