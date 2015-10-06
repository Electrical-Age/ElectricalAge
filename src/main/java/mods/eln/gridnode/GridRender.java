package mods.eln.gridnode;

import mods.eln.misc.UtilsClient;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import static org.lwjgl.opengl.GL11.*;

/**
 * Created by svein on 25/08/15.
 */
public abstract class GridRender extends TransparentNodeElementRender {
    private final GridDescriptor descriptor;
    private final ResourceLocation cableTexture;
    private ArrayList<Catenary> catenaries = new ArrayList<Catenary>();
    private float idealRenderingAngle;

    public GridRender(TransparentNodeEntity tileEntity, TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        this.descriptor = (GridDescriptor) descriptor;

        cableTexture = new ResourceLocation("eln", this.descriptor.cableTexture);
    }

    @Override
    public void draw() {
        glPushMatrix();
        glRotatef(idealRenderingAngle, 0, 1, 0);
        descriptor.draw();
        glPopMatrix();

        UtilsClient.bindTexture(cableTexture);
        // TODO: Try not to need this. (How? Math.)
        glDisable(GL_CULL_FACE);
        for (Catenary catenary : catenaries) {
            catenary.draw();
        }
        glEnable(GL_CULL_FACE);
    }

    private Vec3 readVec(DataInputStream stream) throws IOException {
        return Vec3.createVectorHelper(stream.readFloat(), stream.readFloat(), stream.readFloat());
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            for (Catenary catenary : catenaries) {
                catenary.destroy();
            }
            catenaries.clear();
            idealRenderingAngle = stream.readFloat();
            int linkCount = stream.readInt();
            for (int i = 0; i < linkCount; i++) {
                // Links always come in pairs.
                Vec3 splus = readVec(stream);
                Vec3 tplus = readVec(stream);
                Vec3 sgnd = readVec(stream);
                Vec3 tgnd = readVec(stream);
                Vec3 dplus = splus.subtract(tplus).normalize();
                Vec3 dgnd = sgnd.subtract(tgnd).normalize();
                double straightV = dplus.dotProduct(dgnd);
                dplus = splus.subtract(tgnd).normalize();
                dgnd = sgnd.subtract(tplus).normalize();
                double crossV = dplus.dotProduct(dgnd);
                if (crossV < straightV) {
                    catenaries.add(new Catenary(splus, tplus));
                    catenaries.add(new Catenary(sgnd, tgnd));
                } else {
                    catenaries.add(new Catenary(splus, tgnd));
                    catenaries.add(new Catenary(sgnd, tplus));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean cameraDrawOptimisation() {
        return false;
    }

    private class Catenary {
        final int list;

        final Vec3 origin = Vec3.createVectorHelper(0, 0, 0);
        final int box[] = {
                3, 7, 5, 3, 5, 1,
                4, 8, 6, 4, 6, 2,
                1, 6, 5, 1, 2, 6,
                3, 8, 7, 3, 4, 8
        };
        // Maps box coordinates (above) to texture coordinates.
        final int boxTex[] = {
                0, 0, // 1
                0, 1, // 2
                0, 1, // 3
                0, 0, // 4
                1, 0, // 5
                1, 1, // 6
                1, 1, // 7
                1, 0, // 8
        };
        private final double cableWidth = 0.05;

        // TODO: Lighting and such should not be the same across the entire cable.
        // Probably need make physical "cable" blocks, to make minecraft cooperate.
        // The individual blocks should do the rendering.
        // ...later. Much later.
        Catenary(Vec3 start, Vec3 end) {
            // These are the central vertices of the catenary.
            Vec3[] catenary = getConnectionCatenary(start, end);

            list = glGenLists(1);
            glNewList(list, GL_COMPILE);
            glBegin(GL_TRIANGLES);

            // Four points at the starting pole.
            Vec3 previous[] = spread(start, catenary[0]);
            final World worldObj = tileEntity.getWorldObj();
            for (int i = 0; i < catenary.length - 1; i++) {
                // Some more points at intermediate junctions.
                Vec3 next[] = spread(catenary[i], catenary[i + 1]);
                drawBox(worldObj, previous, next);
                previous = next;
            }
            // Finally, at the ending pole. We'll just translate the second-to-last points to fit.
            Vec3 last[] = translate(previous, catenary[catenary.length - 2].subtract(catenary[catenary.length - 1]));
            drawBox(worldObj, previous, last);

            glEnd();
            glEndList();
        }

        private void drawBox(World world, Vec3[] a, Vec3[] b) {
            Vec3 v[] = new Vec3[]{a[0], a[1], a[2], a[3], b[0], b[1], b[2], b[3]};

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
            for (int i = 0; i < box.length; i++) {
                final int bc = box[i] - 1;
                glTexCoord2f(boxTex[bc * 2], boxTex[bc * 2 + 1]);
                vertex(v[bc]);
            }
        }

        private void vertex(Vec3 vec3) {
            glVertex3d(vec3.xCoord, vec3.yCoord, vec3.zCoord);
        }

        private Vec3[] translate(Vec3[] start, Vec3 delta) {
            Vec3 ret[] = new Vec3[start.length];
            for (int i = 0; i < start.length; i++) {
                ret[i] = start[i].addVector(delta.xCoord, delta.yCoord, delta.zCoord);
            }
            return ret;
        }

        private Vec3[] spread(Vec3 a, Vec3 b) {
            // We want to draw a box-shaped cable following the catenary.
            // To start with, compute a vector perpendicular to the first
            // catenary segment, then rotate it around the catenary to form four points.
            final Vec3 delta = b.subtract(a);
            // This is just to copy.
            // We don't care what r is, so long as it's linearly independent of delta.
            final Vec3 r = delta.normalize();
            r.rotateAroundY(1);
            // This gives us one vector which is perpendicular to delta.
            final Vec3 x1 = multiply(delta.crossProduct(r).normalize(), cableWidth);
            // And this, another, perpendicular to delta and x1.
            final Vec3 y1 = multiply(delta.crossProduct(x1).normalize(), cableWidth);
            // Now just invert those to get the other two corners.
            final Vec3 x2 = negate(x1), y2 = negate(y1);
            return translate(new Vec3[]{x1, y1, y2, x2}, a);
        }

        private Vec3 negate(Vec3 v) {
            return v.subtract(origin);
        }

        Vec3 multiply(Vec3 a, double b) {
            return Vec3.createVectorHelper(
                    a.xCoord * b,
                    a.yCoord * b,
                    a.zCoord * b
            );
        }

        // This function borrowed from Immersive Engineering. Check them out!
        private Vec3[] getConnectionCatenary(Vec3 start, Vec3 end) {
            // We don't have any vertical lines, possibly ever, but definitely not right now.
            final boolean vertical = false;

            // TODO: Thermal heating.
            final double slack = 1.005;
            final int vertices = 16;

            if (vertical)
                return new Vec3[]{Vec3.createVectorHelper(end.xCoord, end.yCoord, end.zCoord)};

            double dx = (end.xCoord) - (start.xCoord);
            double dy = (end.yCoord) - (start.yCoord);
            double dz = (end.zCoord) - (start.zCoord);
            double dw = Math.sqrt(dx * dx + dz * dz);
            double k = Math.sqrt(dx * dx + dy * dy + dz * dz) * slack;
            double l = 0;
            int limiter = 0;
            while (limiter < 300) {
                limiter++;
                l += 0.01;
                if (Math.sinh(l) / l >= Math.sqrt(k * k - dy * dy) / dw)
                    break;
            }
            double a = dw / 2 / l;
            double p = (0 + dw - a * Math.log((k + dy) / (k - dy))) * 0.5;
            double q = (dy + 0 - k * Math.cosh(l) / Math.sinh(l)) * 0.5;

            Vec3[] vex = new Vec3[vertices];

            for (int i = 0; i < vertices; i++) {
                float n1 = (i + 1) / (float) vertices;
                double x1 = 0 + dx * n1;
                double z1 = 0 + dz * n1;
                double y1 = a * Math.cosh(((Math.sqrt(x1 * x1 + z1 * z1)) - p) / a) + q;
                vex[i] = Vec3.createVectorHelper(start.xCoord + x1, start.yCoord + y1, start.zCoord + z1);
            }
            return vex;
        }

        public void draw() {
            glCallList(list);
        }

        public void destroy() {
            glDeleteLists(list, 1);
        }

    }
}
