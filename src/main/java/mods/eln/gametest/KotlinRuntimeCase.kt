package mods.eln.gametest

import mods.eln.node.Synchronizable

/**
 * P1 step 0: Kotlin compiles, and its stdlib (jarJar'd in the mod jar) loads on the 26.x server. Exercises
 * Synchronizable (ported as-is) and a stdlib collection call.
 */
class KotlinRuntimeCase : TestCase {
    override fun id() = "kotlin_runtime"

    override fun width() = 3

    override fun ticks() = 1

    override fun build(ctx: TestContext) {}

    override fun measure(ctx: TestContext) {
        val s = Synchronizable(1)
        s.value = 2
        ctx.check("kotlin: Synchronizable marks a change pending", s.pending, "")
        ctx.check("kotlin: reading clears pending", s.value == 2 && !s.pending, "")
        ctx.check("kotlin: stdlib (listOf/sumOf) works", listOf(1, 2, 3).sumOf { it * 2 } == 12, "")
    }
}
