package mods.eln.misc

import org.lwjgl.opengl.GL11

/**
 * A series of convenience functions for drawing with OpenGL.
 */


inline fun<T> preserveMatrix(body: () -> T): T {
    GL11.glPushMatrix()
    val ret = body()
    GL11.glPopMatrix()
    return ret
}
