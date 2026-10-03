package io.github.invertisment.qrtools.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

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

    /** Packages and classes whose results depend on the outside world rather than the arguments. */
    private val impureApis = listOf(
        "java.io",
        "java.nio.file",
        "java.net",
        "kotlin.io",
        "java.time.Clock",
        "java.util.Random",
        "java.security.SecureRandom",
        "kotlin.random",
    )

    @Test
    fun `core must not import android framework types`() {
        filesIn("core").assertTrue { file -> file.imports.none { it.name.startsWith("android") } }
    }

    @Test
    fun `core production code must not import filesystem, network, clock or randomness`() {
        // Production only: a core test may legitimately seed a random generator or read a
        // fixture file. This catches imports, not calls that need none (e.g. `Instant.now()` on
        // an already-imported type) — that part stays a review concern.
        Konsist.scopeFromProduction().files
            .filter { layerOf(it.packagee?.name) == "core" }
            .assertTrue { file -> file.imports.none { import -> impureApis.any { isSameOrNestedUnder(import.name, it) } } }
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

    @Test
    fun `every entry point declared in the manifest is a production class in a glue package`() {
        // The manifest is the platform's own list of what it may start, so it — not a hand-kept
        // list — is what decides which classes are entry points. A name that no longer resolves
        // to a class (e.g. after a package move) fails here too, instead of only at runtime.
        val productionClasses = Konsist.scopeFromProduction().classes().mapNotNull { it.fullyQualifiedName }.toSet()
        val violations = manifestEntryPoints().filterNot { it in productionClasses && layerOf(it) == "glue" }
        assertEquals(emptyList<String>(), violations, "manifest entry points that aren't production glue classes")
    }

    /**
     * Fully qualified class names of every component in the main manifest that Android can start:
     * activities, services, receivers, providers, and a custom `Application` if one is set. Relative
     * names (`.share.glue.ShareQrActivity`) resolve against [basePackage], which is the module's
     * `namespace` — the manifest itself carries no `package` attribute.
     */
    private fun manifestEntryPoints(): List<String> {
        val manifest = File("src/main/AndroidManifest.xml")
        check(manifest.isFile) { "expected the manifest at ${manifest.absolutePath} (tests run from the app module)" }
        val document = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(manifest)
        return listOf("application", "activity", "service", "receiver", "provider")
            .flatMap { tag -> document.getElementsByTagName(tag).let { nodes -> (0 until nodes.length).map { nodes.item(it) as Element } } }
            .mapNotNull { element -> element.getAttributeNS(ANDROID_NS, "name").takeIf { it.isNotEmpty() } }
            .map { name ->
                when {
                    name.startsWith(".") -> basePackage + name
                    '.' !in name -> "$basePackage.$name"
                    else -> name
                }
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

    /** True if [name] is [parent] itself or nested under it — `java.util.Random`, but not `java.util.RandomAccess`. */
    private fun isSameOrNestedUnder(name: String, parent: String): Boolean =
        name == parent || name.startsWith("$parent.")

    private fun segmentsUnderBase(name: String?): List<String> =
        if (name?.startsWith("$basePackage.") == true) name.removePrefix("$basePackage.").split('.') else emptyList()

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
