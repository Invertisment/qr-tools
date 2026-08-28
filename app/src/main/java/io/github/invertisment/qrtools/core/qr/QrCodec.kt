package io.github.invertisment.qrtools.core.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.WriterException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Pure QR encode/decode over plain data. ZXing's own types never cross this
 * boundary — callers only ever see [QrPayload] and [QrMatrix].
 */
object QrCodec {

    /**
     * ZXing's default segmentation encoder has a confirmed bit-alignment bug that can
     * produce a symbol its own reader cannot decode, for some short strings that mix
     * characters eligible for ALPHANUMERIC mode with characters that force BYTE mode (e.g.
     * "#  f          !-5\"5!%,"). QR_COMPACT selects ZXing's minimal-encoding algorithm,
     * which avoids that specific case, at no known cost in the cases it handles — but does
     * not make round-tripping universal: see QrCodecPropertyTest for a separate,
     * lower-frequency decode-side limitation that persists regardless of this hint.
     */
    private val encodeHints = mapOf(EncodeHintType.QR_COMPACT to true)

    /**
     * QR_COMPACT's minimal encoder has its own failure mode the default encoder doesn't:
     * it throws "Internal error: failed to encode" for some Unicode content outside the
     * Basic Multilingual Plane (e.g. many emoji), even for a handful of characters nowhere
     * near QR's actual capacity. The default encoder handles that content fine, so this is
     * the fallback [encode] retries with rather than surfacing the QR_COMPACT failure — the
     * two encoders have different failure triggers, so a string only needs one of them to
     * succeed.
     */
    private val fallbackEncodeHints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")

    /**
     * Encodes [payload] into a module grid. Fails if the text can't be encoded by either
     * encoder tried — usually because it's too long for any QR version, but ZXing also fails
     * this way for other reasons (e.g. missing Shift_JIS charset support for Kanji-mode
     * content, or an internal encoder inconsistency) — the failure carries ZXing's own
     * message so a caller can show the real reason instead of guessing "too long" for every
     * case.
     */
    fun encode(payload: QrPayload): Result<QrMatrix> {
        val bitMatrix = try {
            QRCodeWriter().encode(payload.text, BarcodeFormat.QR_CODE, 0, 0, encodeHints)
        } catch (e: WriterException) {
            try {
                QRCodeWriter().encode(payload.text, BarcodeFormat.QR_CODE, 0, 0, fallbackEncodeHints)
            } catch (fallbackError: WriterException) {
                return Result.failure(
                    IllegalArgumentException(fallbackError.message ?: "could not encode QR code", fallbackError),
                )
            }
        }
        val rows = (0 until bitMatrix.height).map { y ->
            (0 until bitMatrix.width).map { x -> bitMatrix.get(x, y) }
        }
        return Result.success(QrMatrix(bitMatrix.width, rows))
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
