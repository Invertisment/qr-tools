package io.github.invertisment.qrtools.core.qr

/** The module grid of a rendered QR code, independent of any bitmap/rendering library. */
data class QrMatrix(val moduleCount: Int, private val modules: List<List<Boolean>>) {
    init {
        require(modules.size == moduleCount) { "expected $moduleCount rows, got ${modules.size}" }
        require(modules.all { it.size == moduleCount }) { "every row must have $moduleCount columns" }
    }

    /** True if the module at (x, y) is dark. */
    operator fun get(x: Int, y: Int): Boolean = modules[y][x]
}
