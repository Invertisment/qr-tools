package io.github.invertisment.qrtools.keyboard.glue

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.PopupWindow
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import io.github.invertisment.qrtools.R
import io.github.invertisment.qrtools.camera.infra.QrScanner

/**
 * Glue: the framework-mandated IME entry point. Wires the keyboard's scan button to [QrScanner]
 * (infra layer) and commits a successful decode straight into the field currently being
 * edited via [getCurrentInputConnection]. The same button toggles the popup open and closed —
 * it reads "Scan QR code" normally and "Hide QR scanner" while the popup is showing. Makes no
 * decisions of its own beyond that wiring — everything it calls already decided what to do.
 *
 * "Switch keyboard" calls [InputMethodManager.showInputMethodPicker] rather than disabling this
 * IME outright — there is no API for an IME to disable or uninstall itself (that would be a
 * privilege-escalation hole: an IME could otherwise lock itself in as the only option). The
 * picker is the standard mechanism every keyboard app uses to let the user leave it; fully
 * disabling it still requires the user to go through system settings.
 *
 * The scan UI is a [PopupWindow] anchored to the keyboard's own input view rather than a
 * separate Activity: a `PopupWindow` is attached to the IME's own window, so the host app's
 * focused field is never touched and the soft keyboard is never hidden. The one place that
 * isn't true is the first-ever scan, when [CameraPermissionActivity] has to run to obtain the
 * CAMERA permission — an Activity unavoidably takes focus and hides the keyboard, but only
 * that once.
 */
class QrKeyboardService : InputMethodService() {

    private var activeScanner: QrScanner? = null
    private var activePopup: PopupWindow? = null
    private var scanButton: Button? = null

    override fun onCreateInputView(): View {
        val view = LayoutInflater.from(this).inflate(R.layout.keyboard_view, null)
        val scanButton = view.findViewById<Button>(R.id.scan_qr_button)
        this.scanButton = scanButton
        scanButton.setOnClickListener {
            if (activePopup != null) dismissScanPopup() else onScanRequested(view)
        }
        view.findViewById<View>(R.id.switch_keyboard_button).setOnClickListener {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }
        return view
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        dismissScanPopup()
    }

    private fun onScanRequested(anchor: View) {
        val hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

        if (!hasCameraPermission) {
            startActivity(
                Intent(this, CameraPermissionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }

        showScanPopup(anchor)
    }

    private fun showScanPopup(anchor: View) {
        dismissScanPopup()

        val popupView = LayoutInflater.from(this).inflate(R.layout.qr_scan_popup, null)
        val previewView = popupView.findViewById<PreviewView>(R.id.qr_preview)

        val popup = PopupWindow(popupView, anchor.width, anchor.height * POPUP_HEIGHT_IN_ANCHOR_HEIGHTS, true)
        popup.setOnDismissListener {
            // Runs for every dismissal path: a successful scan, the "Hide QR scanner" button
            // (dismissScanPopup()), or tapping outside the popup — so this is the one place
            // that needs to undo everything showScanPopup() set up.
            activeScanner?.stop()
            activeScanner = null
            activePopup = null
            scanButton?.setText(R.string.scan_qr_button)
        }
        activePopup = popup

        val scanner = QrScanner(this)
        activeScanner = scanner
        scanner.start(previewView) { payload ->
            currentInputConnection?.commitText(payload.text, 1)
            activePopup?.dismiss()
        }

        popup.showAsDropDown(anchor)
        scanButton?.setText(R.string.hide_qr_scanner_button)
    }

    private fun dismissScanPopup() {
        activePopup?.dismiss()
    }

    private companion object {
        const val POPUP_HEIGHT_IN_ANCHOR_HEIGHTS = 6
    }
}
