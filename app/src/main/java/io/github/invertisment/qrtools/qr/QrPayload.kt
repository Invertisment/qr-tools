package io.github.invertisment.qrtools.qr

/** Text encoded into, or decoded from, a QR code. */
data class QrPayload(val text: String) {
    init {
        require(text.isNotEmpty()) { "QR payload text must not be empty" }
    }
}
