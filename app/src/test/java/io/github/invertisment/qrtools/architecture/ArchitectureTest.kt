package io.github.invertisment.qrtools.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

/**
 * Mechanically enforces the core/infra/glue boundary described in the project's scaffolding
 * guideline. Layer is the bottom-level package cut: components are packaged normally (`qr`,
 * `camera`, `keyboard`, ...) and each one's innermost package is its layer — `qr.core`,
 * `camera.infra`, `keyboard.glue`. A file's or an import's layer is read from the layer-named
 * segment of its package path, so these rules hold across every component, including ones added
 * later, with no update needed here. Layer names are reserved, which is what makes that lookup
 * unambiguous — `every production file sits directly in exactly one layer package` below keeps
 * them that way.
 */
class ArchitectureTest {

    private val basePackage = "io.github.invertisment.qrtools"
    private val layers = listOf("core", "infra", "glue")

    @Test
    fun `core must not import android framework types`() {
        filesIn("core").assertTrue { file -> file.imports.none { it.name.startsWith("android") } }
    }

    @Test
    fun `core must not import infra or glue from any component`() {
        filesIn("core").assertTrue { file -> file.imports.none { layerOf(it.name) in setOf("infra", "glue") } }
    }

    @Test
    fun `infra must not import glue from any component`() {
        filesIn("infra").assertTrue { file -> file.imports.none { layerOf(it.name) == "glue" } }
    }

    @Test
    fun `every production file sits directly in exactly one layer package`() {
        // Production only (not test sources): a test helper like ArchitectureTest itself has no
        // reason to live in a layer package, and shouldn't be forced to. "Directly" means the
        // layer is the package's last segment — no subpackages under a layer, and no layer
        // nested inside another (`qr.core.infra`) or reused as a component name (`core.qr`).
        Konsist.scopeFromProduction().files.assertTrue { file ->
            val segments = segmentsUnderBase(file.packagee?.name)
            segments.count { it in layers } == 1 && segments.last() in layers
        }
    }

    private fun filesIn(layer: String) =
        Konsist.scopeFromProject().files.filter { layerOf(it.packagee?.name) == layer }

    /**
     * The layer named in [packageOrImportName]'s path under [basePackage], or null for names
     * outside it (third-party and framework imports) or under it with no layer segment (e.g.
     * the generated `R` class).
     */
    private fun layerOf(packageOrImportName: String?): String? =
        segmentsUnderBase(packageOrImportName).firstOrNull { it in layers }

    private fun segmentsUnderBase(name: String?): List<String> =
        if (name?.startsWith("$basePackage.") == true) name.removePrefix("$basePackage.").split('.') else emptyList()
}
