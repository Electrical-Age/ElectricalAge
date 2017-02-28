package mods.eln.gridnode

import mods.eln.misc.UtilsClient
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeElementRender
import mods.eln.node.transparent.TransparentNodeEntity
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Vec3

import java.io.DataInputStream
import java.io.IOException
import java.util.ArrayList

import org.lwjgl.opengl.GL11.*

abstract class GridRender(tileEntity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) : TransparentNodeElementRender(tileEntity, descriptor) {
    private val descriptor: GridDescriptor
    private val cableTexture: ResourceLocation
    private val catenaries = ArrayList<Catenary>()
    private var idealRenderingAngle: Float = 0.toFloat()

    init {
        this.descriptor = descriptor as GridDescriptor

        cableTexture = ResourceLocation("eln", this.descriptor.cableTexture)
    }

    override fun draw() {
        descriptor.draw(idealRenderingAngle)

        UtilsClient.bindTexture(cableTexture)
        // TODO: Try not to need this. (How? Math.)
        glDisable(GL_CULL_FACE)
        for (catenary in catenaries) {
            catenary.draw()
        }
        glEnable(GL_CULL_FACE)
    }

    @Throws(IOException::class)
    private fun readVec(stream: DataInputStream): Vec3 {
        return Vec3.createVectorHelper(stream.readFloat().toDouble(), stream.readFloat().toDouble(), stream.readFloat().toDouble())
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        try {
            for (catenary in catenaries) {
                catenary.destroy()
            }
            catenaries.clear()
            idealRenderingAngle = stream.readFloat()
            val linkCount = stream.readInt()
            for (i in 0..linkCount - 1) {
                // Links always come in pairs.
                val splus = readVec(stream)
                val tplus = readVec(stream)
                val sgnd = readVec(stream)
                val tgnd = readVec(stream)
                var dplus = splus.subtract(tplus).normalize()
                var dgnd = sgnd.subtract(tgnd).normalize()
                val straightV = dplus.dotProduct(dgnd)
                dplus = splus.subtract(tgnd).normalize()
                dgnd = sgnd.subtract(tplus).normalize()
                val crossV = dplus.dotProduct(dgnd)
                if (crossV < straightV) {
                    catenaries.add(Catenary(splus, tplus))
                    catenaries.add(Catenary(sgnd, tgnd))
                } else {
                    catenaries.add(Catenary(splus, tgnd))
                    catenaries.add(Catenary(sgnd, tplus))
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    override fun cameraDrawOptimisation(): Boolean {
        return false
    }

    private inner class Catenary// TODO: Lighting and such should not be the same across the entire cable.
    // Probably need make physical "cable" blocks, to make minecraft cooperate.
    // The individual blocks should do the rendering.
    // ...later. Much later.
    internal constructor(start: Vec3, end: Vec3) {
        internal val list: Int

        internal val origin = Vec3.createVectorHelper(0.0, 0.0, 0.0)
        internal val box = intArrayOf(3, 7, 5, 3, 5, 1, 4, 8, 6, 4, 6, 2, 1, 6, 5, 1, 2, 6, 3, 8, 7, 3, 4, 8)
        // Maps box coordinates (above) to texture coordinates.
        internal val boxTex = intArrayOf(0, 0, // 1
                0, 1, // 2
                0, 1, // 3
                0, 0, // 4
                1, 0, // 5
                1, 1, // 6
                1, 1, // 7
                1, 0)// 8
        private val cableWidth = 0.05

        init {
            // These are the central vertices of the catenary.
            val catenary = getConnectionCatenary(start, end)

            list = glGenLists(1)
            glNewList(list, GL_COMPILE)
            glBegin(GL_TRIANGLES)

            if (start.xCoord == end.xCoord && start.zCoord == end.zCoord) {
                // Poles right on top of each other? No catenaries here.
                drawBox(spread(start, end), spread(end, start))
            } else {
                // Four points at the starting pole.
                var previous = spread(start, catenary[0])
                for (i in 0..catenary.size - 1 - 1) {
                    // Some more points at intermediate junctions.
                    val next = spread(catenary[i], catenary[i + 1])
                    drawBox(previous, next)
                    previous = next
                }
                // Finally, at the ending pole. We'll just translate the second-to-last points to fit.
                val last = translate(previous, catenary[catenary.size - 2].subtract(catenary[catenary.size - 1]))
                drawBox(previous, last)
            }
            glEnd()
            glEndList()
        }

        private fun drawBox(from: Array<Vec3>, to: Array<Vec3>) {
            val v = arrayOf(from[0], from[1], from[2], from[3], to[0], to[1], to[2], to[3])

            // Figure out the lighting.
            //            Vec3 middle = Vec3.createVectorHelper(0, 0, 0);
            //            for (Vec3 x : v) {
            //                middle = middle.addVector(x.xCoord, x.yCoord, x.zCoord);
            //            }
            //            middle = multiply(middle, v.length).addVector(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
            //            glColor3d(
            //                    139 / 255.0,
            //                    69 / 255.0,
            //                    19 / 255.0);

            // And let's draw it all.
            for (i in box.indices) {
                val bc = box[i] - 1
                glTexCoord2f(boxTex[bc * 2].toFloat(), boxTex[bc * 2 + 1].toFloat())
                glVertex3f(v[bc].xCoord.toFloat(), v[bc].yCoord.toFloat(), v[bc].zCoord.toFloat())
            }
        }

        private fun translate(start: Array<Vec3>, delta: Vec3): Array<Vec3> {
            return start.mapIndexed { i, vec3 -> vec3.addVector(delta.xCoord, delta.yCoord, delta.zCoord) }.toTypedArray()
        }

        private fun spread(a: Vec3, b: Vec3): Array<Vec3> {
            // We want to draw a box-shaped cable following the catenary.
            // To start with, compute a vector perpendicular to the first
            // catenary segment, then rotate it around the catenary to form four points.
            val delta = b.subtract(a)
            // This is just to copy.
            // We don't care what r is, so long as it's linearly independent of delta.
            val r = delta.normalize()
            r.rotateAroundY(1f)
            r.rotateAroundX(1f)
            // This gives us one vector which is perpendicular to delta.
            val x1 = multiply(delta.crossProduct(r).normalize(), cableWidth)
            // And this, another, perpendicular to delta and x1.
            val y1 = multiply(delta.crossProduct(x1).normalize(), cableWidth)
            // Now just invert those to get the other two corners.
            val x2 = negate(x1)
            val y2 = negate(y1)
            return translate(arrayOf(x1, y1, y2, x2), a)
        }

        private fun negate(v: Vec3): Vec3 {
            return v.subtract(origin)
        }

        internal fun multiply(a: Vec3, b: Double): Vec3 {
            return Vec3.createVectorHelper(
                    a.xCoord * b,
                    a.yCoord * b,
                    a.zCoord * b
            )
        }

        // This function borrowed from Immersive Engineering. Check them out!
        private fun getConnectionCatenary(start: Vec3, end: Vec3): Array<Vec3> {
            // TODO: Thermal heating.
            val slack = 1.005
            val vertices = 16

            val dx = end.xCoord - start.xCoord
            val dy = end.yCoord - start.yCoord
            val dz = end.zCoord - start.zCoord
            val dw = Math.sqrt(dx * dx + dz * dz)
            val k = Math.sqrt(dx * dx + dy * dy + dz * dz) * slack
            var l = 0.0
            var limiter = 0
            while (limiter < 300) {
                limiter++
                l += 0.01
                if (Math.sinh(l) / l >= Math.sqrt(k * k - dy * dy) / dw)
                    break
            }
            val a = dw / 2.0 / l
            val p = (0 + dw - a * Math.log((k + dy) / (k - dy))) * 0.5
            val q = (dy + 0 - k * Math.cosh(l) / Math.sinh(l)) * 0.5

            return (0..vertices - 1).map {
                val n1 = (it + 1) / vertices.toFloat()
                val x1 = 0 + dx * n1
                val z1 = 0 + dz * n1
                val y1 = a * Math.cosh((Math.sqrt(x1 * x1 + z1 * z1) - p) / a) + q
                Vec3.createVectorHelper(start.xCoord + x1, start.yCoord + y1, start.zCoord + z1)
            }.toTypedArray()
        }

        fun draw() {
            glCallList(list)
        }

        fun destroy() {
            glDeleteLists(list, 1)
        }
    }
}
