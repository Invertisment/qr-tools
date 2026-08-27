package io.github.invertisment.qrtools.glue.keyboard

import android.Manifest
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * Transparent trampoline: an [android.inputmethodservice.InputMethodService] cannot request
 * runtime permissions itself, so this Activity exists solely to request CAMERA and finish. Its
 * one unavoidable side effect is that showing any Activity hides the soft keyboard and takes
 * focus away from the field being edited — acceptable here because it only ever runs once, the
 * first time a user taps "Scan QR code" before camera permission has been granted.
 */
class CameraPermissionActivity : AppCompatActivity() {

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestCameraPermission.launch(Manifest.permission.CAMERA)
    }
}
