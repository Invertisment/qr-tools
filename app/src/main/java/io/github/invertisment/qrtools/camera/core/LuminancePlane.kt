package io.github.invertisment.qrtools.camera.core

/**
 * Pure layout conversion for a camera luminance (Y) plane. Camera frames store pixel (x, y) at
 * `y * rowStride + x * pixelStride`, and some devices pad each row past its last pixel, so the
 * raw plane can't be handed to a decoder that expects one byte per pixel, rows back to back.
 */
object LuminancePlane {

    /**
     * Returns [width] x [height] luminance bytes, tightly packed row after row, read out of
     * [plane]. [plane] may omit the padding after the last pixel of the last row, which is how
     * camera buffers commonly end.
     */
    fun unpad(plane: ByteArray, width: Int, height: Int, rowStride: Int, pixelStride: Int): ByteArray {
        require(width > 0 && height > 0) { "frame must not be empty, got ${width}x$height" }
        require(pixelStride >= 1) { "pixelStride must be at least 1, got $pixelStride" }
        val rowSpan = (width - 1) * pixelStride + 1
        require(rowStride >= rowSpan) { "rowStride $rowStride is shorter than a row of $width pixels ($rowSpan bytes)" }
        val minSize = (height - 1) * rowStride + rowSpan
        require(plane.size >= minSize) { "plane has ${plane.size} bytes, needs at least $minSize" }

        val packed = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                packed[y * width + x] = plane[y * rowStride + x * pixelStride]
            }
        }
        return packed
    }
}
