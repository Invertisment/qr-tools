package io.github.invertisment.qrtools.camera.infra

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.github.invertisment.qrtools.camera.core.LuminancePlane
import io.github.invertisment.qrtools.qr.core.QrCodec
import io.github.invertisment.qrtools.qr.core.QrPayload
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Drives a CameraX preview + frame analysis pipeline, decoding frames with [QrCodec] until one
 * succeeds. Infra layer: the only place CameraX and its buffers are visible — callers
 * only ever see [QrPayload]. One instance is good for exactly one scan session; call [stop] when
 * done (or discard the instance) rather than reusing it for a second scan.
 */
class QrScanner(private val context: Context) : LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val delivered = AtomicBoolean(false)
    private var cameraProvider: ProcessCameraProvider? = null

    /** Starts the camera preview into [previewView], calling [onDecoded] on the main thread at most once. */
    fun start(previewView: PreviewView, onDecoded: (QrPayload) -> Unit) {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        val mainExecutor = ContextCompat.getMainExecutor(context)
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                val provider = providerFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { useCase ->
                        useCase.setAnalyzer(analysisExecutor) { frame ->
                            val payload = frame.use { decodeFrame(it) }
                            if (payload != null && delivered.compareAndSet(false, true)) {
                                mainExecutor.execute { onDecoded(payload) }
                            }
                        }
                    }

                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            },
            mainExecutor,
        )
    }

    /** Releases the camera. Safe to call more than once. */
    fun stop() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        analysisExecutor.shutdown()
    }

    private fun decodeFrame(frame: ImageProxy): QrPayload? =
        QrCodec.decode(frame.toLuminanceBytes(), frame.width, frame.height)

    /**
     * CameraX guarantees YUV_420_888 for [ImageAnalysis], whose plane 0 is the luminance (Y)
     * plane [QrCodec.decode] expects — once [LuminancePlane.unpad] strips any per-row padding.
     * This only copies the plane out of CameraX's buffer.
     */
    private fun ImageProxy.toLuminanceBytes(): ByteArray {
        val plane = planes[0]
        val buffer = plane.buffer.apply { rewind() }
        val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
        return LuminancePlane.unpad(bytes, width, height, plane.rowStride, plane.pixelStride)
    }
}
