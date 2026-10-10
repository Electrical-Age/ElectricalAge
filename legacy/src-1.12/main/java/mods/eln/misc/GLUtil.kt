package mods.eln.misc

import org.lwjgl.opengl.GL11

/**
 * A series of convenience functions for drawing with OpenGL.
 */


inline fun <T> preserveMatrix(body: () -> T): T {
    val ret: T
    try {
        GL11.glPushMatrix()
        ret = body()
    } finally {
        GL11.glPopMatrix()
    }
    return ret
}
