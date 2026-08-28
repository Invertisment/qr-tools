package io.github.invertisment.qrtools.glue.share

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import io.github.invertisment.qrtools.R
import io.github.invertisment.qrtools.core.qr.QrCodec
import io.github.invertisment.qrtools.core.qr.QrMatrix
import io.github.invertisment.qrtools.core.qr.QrPayload

/**
 * Glue: the share-target entry point for "share text, get a QR code for it right there".
 * Registered for ACTION_SEND on text/plain, so any app's share sheet can launch it directly.
 * Themed as a dialog (see the manifest) so it pops up over whatever app the text was shared
 * from instead of taking over the screen; tapping anywhere dismisses it.
 */
class ShareQrActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = intent.takeIf { it.action == Intent.ACTION_SEND }
            ?.getStringExtra(Intent.EXTRA_TEXT)
            ?.trim()

        if (text.isNullOrEmpty()) {
            finish()
            return
        }

        val matrix = QrCodec.encode(QrPayload(text)).getOrElse { error ->
            Toast.makeText(this, getString(R.string.share_qr_encode_failed, error.message), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContentView(R.layout.activity_share_qr)
        title = truncatedTitle(text)

        // Window width is fixed; height is left to wrap the title bar + our content. If we
        // fixed both to the same value, the title bar would eat into it, leaving the white
        // square shorter than it is wide. Forcing the white square's height to match its own
        // *actual rendered* width (below) is what keeps it square.
        val displayMetrics = resources.displayMetrics
        val requestedSize = (minOf(displayMetrics.widthPixels, displayMetrics.heightPixels) * DIALOG_SIZE_FRACTION).toInt()
        window?.setLayout(requestedSize, WindowManager.LayoutParams.WRAP_CONTENT)

        val whiteSquare = findViewById<View>(R.id.qr_white_square)
        val imageView = findViewById<ImageView>(R.id.qr_image)
        whiteSquare.doOnLayout { view ->
            // The dialog theme insets the window's content by some margin (elevation/shadow/
            // rounded corners), so the rendered width ends up smaller than requestedSize —
            // read the real value back rather than assume it matches what we asked for.
            val actualWidth = view.width
            if (view.layoutParams.height != actualWidth) {
                view.layoutParams = view.layoutParams.apply { height = actualWidth }
                val qrPadding = (actualWidth * (1 - QR_SIZE_FRACTION) / 2).toInt()
                imageView.setPadding(qrPadding, qrPadding, qrPadding, qrPadding)
                view.requestLayout()
            }
        }

        // Filtering would blur the crisp module edges when this small bitmap is scaled up,
        // which risks making the code harder for a camera to scan.
        val drawable = BitmapDrawable(resources, matrix.toBitmap()).apply { isFilterBitmap = false }
        imageView.setImageDrawable(drawable)

        val dismiss = View.OnClickListener { finish() }
        findViewById<View>(R.id.qr_root).setOnClickListener(dismiss)
        imageView.setOnClickListener(dismiss)
    }

    private fun truncatedTitle(text: String): String =
        if (text.length <= TITLE_PREVIEW_LENGTH) text else text.take(TITLE_PREVIEW_LENGTH) + "..."

    private fun QrMatrix.toBitmap(): Bitmap {
        val pixels = IntArray(moduleCount * moduleCount) { i ->
            val x = i % moduleCount
            val y = i / moduleCount
            if (this[x, y]) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(pixels, moduleCount, moduleCount, Bitmap.Config.ARGB_8888)
    }

    private companion object {
        const val DIALOG_SIZE_FRACTION = 0.8
        const val QR_SIZE_FRACTION = 0.9
        const val TITLE_PREVIEW_LENGTH = 24
    }
}
