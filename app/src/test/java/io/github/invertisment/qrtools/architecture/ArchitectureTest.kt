package io.github.invertisment.qrtools.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

/**
 * Mechanically enforces the core/infra/glue boundary described in the project's scaffolding
 * guideline: layer is the top-level package cut (`core`, `infra`, `glue`), so these rules are
 * generic over whatever feature packages live inside each layer, rather than naming today's
 * `qr`/`camera`/`keyboard` specifically — a new package under an existing layer is covered
 * automatically, with no matching update needed here. A new *layer*, or a package that isn't
 * under any of the three, is exactly what `every production file belongs to a sanctioned layer`
 * below is for.
 */
class ArchitectureTest {

    private val basePackage = "io.github.invertisment.qrtools"
    private val layers = listOf("core", "infra", "glue")

    @Test
    fun `core must not import android framework types`() {
        filesUnder("core").assertTrue { file -> file.imports.none { it.name.startsWith("android") } }
    }

    @Test
    fun `core must not import infra or glue packages`() {
        filesUnder("core").assertTrue { file ->
            file.imports.none { isUnderLayer(it.name, "infra") || isUnderLayer(it.name, "glue") }
        }
    }

    @Test
    fun `infra must not import glue packages`() {
        filesUnder("infra").assertTrue { file -> file.imports.none { isUnderLayer(it.name, "glue") } }
    }

    @Test
    fun `every production file belongs to a sanctioned layer`() {
        // Production only (not test sources): a test helper like ArchitectureTest itself has no
        // reason to live under core, infra, or glue, and shouldn't be forced to.
        Konsist.scopeFromProduction().files.assertTrue { file ->
            val packageName = file.packagee?.name ?: return@assertTrue false
            layers.any { layer -> isUnderLayer(packageName, layer) }
        }
    }

    private fun filesUnder(layer: String) =
        Konsist.scopeFromProject().files.filter { isUnderLayer(it.packagee?.name, layer) }

    /** True if [packageOrImportName] is `$basePackage.$layer` itself, or nested under it. */
    private fun isUnderLayer(packageOrImportName: String?, layer: String): Boolean {
        val prefix = "$basePackage.$layer"
        return packageOrImportName == prefix || packageOrImportName?.startsWith("$prefix.") == true
    }
}
