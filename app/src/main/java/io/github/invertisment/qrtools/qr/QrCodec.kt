package io.github.invertisment.qrtools.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Pure QR encode/decode over plain data. ZXing's own types never cross this
 * boundary — callers only ever see [QrPayload] and [QrMatrix].
 */
object QrCodec {

    fun encode(payload: QrPayload): QrMatrix {
        val bitMatrix = QRCodeWriter().encode(payload.text, BarcodeFormat.QR_CODE, 0, 0)
        val rows = (0 until bitMatrix.height).map { y ->
            (0 until bitMatrix.width).map { x -> bitMatrix.get(x, y) }
        }
        return QrMatrix(bitMatrix.width, rows)
    }

    private val decodeHints = mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))

    /** Decodes one frame of grayscale luminance data. Returns null if no QR code is found. */
    fun decode(luminance: ByteArray, width: Int, height: Int): QrPayload? {
        val source = PlanarYUVLuminanceSource(luminance, width, height, 0, 0, width, height, false)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader().apply { setHints(decodeHints) }
        return try {
            QrPayload(reader.decode(bitmap).text)
        } catch (e: ReaderException) {
            null
        }
    }
}
