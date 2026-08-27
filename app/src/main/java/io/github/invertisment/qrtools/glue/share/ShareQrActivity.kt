package io.github.invertisment.qrtools.glue.share

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        val matrix = QrCodec.encode(QrPayload(text))
        if (matrix == null) {
            Toast.makeText(this, R.string.share_qr_too_long, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContentView(R.layout.activity_share_qr)
        val imageView = findViewById<ImageView>(R.id.qr_image)
        // Filtering would blur the crisp module edges when this small bitmap is scaled up,
        // which risks making the code harder for a camera to scan.
        val drawable = BitmapDrawable(resources, matrix.toBitmap()).apply { isFilterBitmap = false }
        imageView.setImageDrawable(drawable)

        val dismiss = View.OnClickListener { finish() }
        findViewById<View>(R.id.qr_root).setOnClickListener(dismiss)
        imageView.setOnClickListener(dismiss)
    }

    private fun QrMatrix.toBitmap(): Bitmap {
        val pixels = IntArray(moduleCount * moduleCount) { i ->
            val x = i % moduleCount
            val y = i / moduleCount
            if (this[x, y]) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(pixels, moduleCount, moduleCount, Bitmap.Config.ARGB_8888)
    }
}
