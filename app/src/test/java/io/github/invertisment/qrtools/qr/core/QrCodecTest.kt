package io.github.invertisment.qrtools.qr.core

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QrCodecTest {

    @Test
    fun `encode produces the same module grid ZXing itself would produce`() {
        val text = "hello world"
        val reference = QRCodeWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            0,
            0,
            mapOf(EncodeHintType.QR_COMPACT to true),
        )

        val matrix = QrCodec.encode(QrPayload(text)).getOrThrow()

        assertEquals(reference.width, matrix.moduleCount)
        for (y in 0 until reference.height) {
            for (x in 0 until reference.width) {
                assertEquals(reference.get(x, y), matrix[x, y], "mismatch at ($x, $y)")
            }
        }
    }

    @Test
    fun `decoding a frame rendered by ZXing's own image writer returns the original text`() {
        for (text in listOf("hello world", "https://example.com", "1234567890", "a")) {
            val payload = QrPayload(text)
            val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 200, 200)
            val image = MatrixToImageWriter.toBufferedImage(bitMatrix)
            val luminance = BufferedImageLuminanceSource(image).matrix

            assertEquals(payload, QrCodec.decode(luminance, image.width, image.height))
        }
    }

    @Test
    fun `decoding a blank frame finds nothing`() {
        val blank = ByteArray(100 * 100) { 0xFF.toByte() }
        assertNull(QrCodec.decode(blank, 100, 100))
    }

    @Test
    fun `encoding text past QR capacity fails with a message instead of throwing`() {
        val tooLong = "a".repeat(5000)
        val result = QrCodec.encode(QrPayload(tooLong))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.isNotBlank() == true)
    }

    @Test
    fun `encoding text with an emoji succeeds via the fallback encoder`() {
        // Regression guard: QR_COMPACT (added earlier to fix a different encoder bug) throws
        // for some Unicode content outside the Basic Multilingual Plane — confirmed for this
        // exact 14-character string, nowhere near QR's actual capacity, which used to be
        // reported to the user as "text is too long" via a hardcoded message. QrCodec now
        // falls back to the default encoder for exactly this case, which handles it fine.
        val text = "hello 😀 world"
        val reference = QRCodeWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            0,
            0,
            mapOf(EncodeHintType.CHARACTER_SET to "UTF-8"),
        )

        val matrix = QrCodec.encode(QrPayload(text)).getOrThrow()

        assertEquals(reference.width, matrix.moduleCount)
        for (y in 0 until reference.height) {
            for (x in 0 until reference.width) {
                assertEquals(reference.get(x, y), matrix[x, y], "mismatch at ($x, $y)")
            }
        }
    }
}
