package io.github.invertisment.qrtools.camera.core

import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.IntRange
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import kotlin.random.Random

class LuminancePlanePropertyTest {

    /**
     * For every frame shape, stride and padding, unpadding a plane laid out the way camera
     * buffers are returns exactly the pixels that were laid out. Padding bytes are all
     * [PADDING], a value the generated pixels never take, so reading a single byte from the
     * wrong offset fails the comparison.
     */
    @Property
    fun `unpad returns exactly the pixels laid out in a padded plane`(
        @ForAll @IntRange(min = 1, max = 48) width: Int,
        @ForAll @IntRange(min = 1, max = 48) height: Int,
        @ForAll @IntRange(min = 1, max = 4) pixelStride: Int,
        @ForAll @IntRange(min = 0, max = 16) rowPadding: Int,
        @ForAll lastRowPadded: Boolean,
        @ForAll seed: Long,
    ) {
        val random = Random(seed)
        val pixels = ByteArray(width * height) { random.nextInt(0, PADDING.toInt()).toByte() }
        val rowStride = (width - 1) * pixelStride + 1 + rowPadding
        val plane = layOut(pixels, width, height, rowStride, pixelStride, lastRowPadded)

        assertArrayEquals(pixels, LuminancePlane.unpad(plane, width, height, rowStride, pixelStride))
    }

    @Test
    fun `unpad rejects a plane too short for the frame it describes`() {
        assertThrows(IllegalArgumentException::class.java) {
            LuminancePlane.unpad(ByteArray(15), width = 4, height = 4, rowStride = 4, pixelStride = 1)
        }
    }

    @Test
    fun `unpad rejects a row stride shorter than one row of pixels`() {
        assertThrows(IllegalArgumentException::class.java) {
            LuminancePlane.unpad(ByteArray(64), width = 4, height = 4, rowStride = 3, pixelStride = 1)
        }
    }

    /**
     * Writes [pixels] at `y * rowStride + x * pixelStride`, the camera plane layout, with every
     * other byte set to [PADDING]. Without [lastRowPadded] the plane ends right after the last
     * pixel, as camera buffers commonly do.
     */
    private fun layOut(
        pixels: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int,
        lastRowPadded: Boolean,
    ): ByteArray {
        val size = if (lastRowPadded) height * rowStride else (height - 1) * rowStride + (width - 1) * pixelStride + 1
        val plane = ByteArray(size) { PADDING }
        for (y in 0 until height) {
            for (x in 0 until width) {
                plane[y * rowStride + x * pixelStride] = pixels[y * width + x]
            }
        }
        return plane
    }

    private companion object {
        const val PADDING: Byte = 0x7F
    }
}
