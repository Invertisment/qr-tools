package io.github.invertisment.qrtools.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

/** Mechanically enforces the functional-core boundary: no Android framework types leak in. */
class ArchitectureTest {

    @Test
    fun `qr package must not import android framework types`() {
        Konsist.scopeFromProject()
            .files
            .filter { it.packagee?.name == "io.github.invertisment.qrtools.qr" }
            .assertTrue { file -> file.imports.none { it.name.startsWith("android") } }
    }
}
