package mods.eln.gl;

import mods.eln.misc.UtilsClient;

import net.minecraft.resources.Identifier;

/**
 * The subset of OpenGL 1.1 that EA's 1.12 render code uses, recorded instead of executed (P3, the "draw shim").
 * 26.x has no raw GL: the client renderer opens a {@link GlFrame} around each node's draw, the 1.12 code runs against
 * this class unchanged (only its import differs), and the frame's vertices are submitted as custom geometry with a
 * RenderType per state (texture, blending, culling, lighting). Server-safe: no client classes here.
 *
 * Supported: the matrix stack, colour, normals, texture coordinates, glBegin/glEnd with every polygon mode (converted
 * to quads), enable/disable of TEXTURE_2D/LIGHTING/BLEND/CULL_FACE, blend functions (alpha or additive), display lists
 * (recorded calls, replayed), push/pop attrib. Lines and points are dropped; GUI-only and lightmap calls are no-ops.
 */
public final class GL11 {
    private GL11() {
    }

    // ---- constants (the real GL values, so code passing ints around keeps working)
    public static final int GL_POINTS = 0x0000, GL_LINES = 0x0001, GL_LINE_LOOP = 0x0002, GL_LINE_STRIP = 0x0003,
        GL_TRIANGLES = 0x0004, GL_TRIANGLE_STRIP = 0x0005, GL_TRIANGLE_FAN = 0x0006, GL_QUADS = 0x0007,
        GL_QUAD_STRIP = 0x0008, GL_POLYGON = 0x0009;
    public static final int GL_TEXTURE_2D = 0x0DE1, GL_LIGHTING = 0x0B50, GL_BLEND = 0x0BE2, GL_CULL_FACE = 0x0B44,
        GL_DEPTH_TEST = 0x0B71, GL_ALPHA_TEST = 0x0BC0, GL_SCISSOR_TEST = 0x0C11, GL_LINE_SMOOTH = 0x0B20,
        GL_LIGHT0 = 0x4000, GL_LIGHT1 = 0x4001, GL_RESCALE_NORMAL = 0x803A;
    public static final int GL_ZERO = 0, GL_ONE = 1, GL_SRC_COLOR = 0x0300, GL_ONE_MINUS_SRC_COLOR = 0x0301,
        GL_SRC_ALPHA = 0x0302, GL_ONE_MINUS_SRC_ALPHA = 0x0303, GL_DST_COLOR = 0x0306;
    public static final int GL_COMPILE = 0x1300, GL_COMPILE_AND_EXECUTE = 0x1301;
    public static final int GL_SMOOTH = 0x1D01, GL_FLAT = 0x1D00, GL_BACK = 0x0405, GL_LEQUAL = 0x0203,
        GL_GREATER = 0x0204, GL_NICEST = 0x1102, GL_LINE_SMOOTH_HINT = 0x0C52;
    public static final int GL_CURRENT_BIT = 0x0001, GL_POLYGON_BIT = 0x0008, GL_LIGHTING_BIT = 0x0040,
        GL_DEPTH_BUFFER_BIT = 0x0100, GL_ENABLE_BIT = 0x2000, GL_COLOR_BUFFER_BIT = 0x4000;
    public static final int GL_TEXTURE = 0x1702, GL_TEXTURE_ENV = 0x2300, GL_TEXTURE_ENV_MODE = 0x2200,
        GL_TEXTURE_ENV_COLOR = 0x2201, GL_MODULATE = 0x2100, GL_REPLACE = 0x1E01, GL_TEXTURE_MIN_FILTER = 0x2801,
        GL_TEXTURE_MAG_FILTER = 0x2800, GL_NEAREST = 0x2600, GL_LINEAR = 0x2601, GL_POSITION = 0x1203,
        GL_SCISSOR_BOX = 0x0C10, GL_BLEND_SRC = 0x0BE1, GL_BLEND_DST = 0x0BE0, GL_T4 = 0;

    static GlFrame f() {
        return GlFrame.current();
    }

    // ---- matrices
    public static void glPushMatrix() {
        f().pushMatrix();
    }

    public static void glPopMatrix() {
        f().popMatrix();
    }

    public static void glLoadIdentity() {
        f().loadIdentity();
    }

    public static void glTranslatef(float x, float y, float z) {
        f().translate(x, y, z);
    }

    public static void glTranslated(double x, double y, double z) {
        glTranslatef((float) x, (float) y, (float) z);
    }

    public static void glRotatef(float angle, float x, float y, float z) {
        f().rotate(angle, x, y, z);
    }

    public static void glRotated(double angle, double x, double y, double z) {
        glRotatef((float) angle, (float) x, (float) y, (float) z);
    }

    public static void glScalef(float x, float y, float z) {
        f().scale(x, y, z);
    }

    public static void glScaled(double x, double y, double z) {
        glScalef((float) x, (float) y, (float) z);
    }

    // ---- per-vertex state
    public static void glColor3f(float r, float g, float b) {
        glColor4f(r, g, b, 1f);
    }

    public static void glColor4f(float r, float g, float b, float a) {
        f().color(r, g, b, a);
    }

    public static void glColor3d(double r, double g, double b) {
        glColor4f((float) r, (float) g, (float) b, 1f);
    }

    public static void glColor4d(double r, double g, double b, double a) {
        glColor4f((float) r, (float) g, (float) b, (float) a);
    }

    public static void glColor3ub(byte r, byte g, byte b) {
        glColor4f((r & 0xFF) / 255f, (g & 0xFF) / 255f, (b & 0xFF) / 255f, 1f);
    }

    public static void glColor4ub(byte r, byte g, byte b, byte a) {
        glColor4f((r & 0xFF) / 255f, (g & 0xFF) / 255f, (b & 0xFF) / 255f, (a & 0xFF) / 255f);
    }

    public static void glNormal3f(float x, float y, float z) {
        f().normal(x, y, z);
    }

    public static void glTexCoord2f(float u, float v) {
        f().texCoord(u, v);
    }

    public static void glTexCoord2d(double u, double v) {
        glTexCoord2f((float) u, (float) v);
    }

    public static void glBegin(int mode) {
        f().begin(mode);
    }

    public static void glEnd() {
        f().end();
    }

    public static void glVertex3f(float x, float y, float z) {
        f().vertex(x, y, z);
    }

    public static void glVertex3d(double x, double y, double z) {
        glVertex3f((float) x, (float) y, (float) z);
    }

    public static void glVertex2f(float x, float y) {
        glVertex3f(x, y, 0f);
    }

    // ---- capabilities
    public static void glEnable(int cap) {
        f().setCap(cap, true);
    }

    public static void glDisable(int cap) {
        f().setCap(cap, false);
    }

    public static boolean glIsEnabled(int cap) {
        return f().isCap(cap);
    }

    public static void glBlendFunc(int src, int dst) {
        f().blendFunc(src, dst);
    }

    public static void glPushAttrib(int mask) {
        f().pushAttrib();
    }

    public static void glPopAttrib() {
        f().popAttrib();
    }

    /** EA's texture binding (1.12 UtilsClient.bindTexture). */
    public static void bindTexture(Identifier texture) {
        f().texture(texture);
    }

    /** 1.12 lightmap override (OpenGlHelper.setLightmapTextureCoords to 240,240): draw full-bright or not. */
    public static void setFullBright(boolean on) {
        f().fullBright(on);
    }

    // ---- display lists: the calls between glNewList and glEndList, replayed by glCallList
    public static int glGenLists(int range) {
        return GlFrame.genLists(range);
    }

    public static void glNewList(int list, int mode) {
        f().newList(list, mode == GL_COMPILE_AND_EXECUTE);
    }

    public static void glEndList() {
        f().endList();
    }

    public static void glCallList(int list) {
        f().callList(list);
    }

    public static void glDeleteLists(int list, int range) {
        GlFrame.deleteLists(list, range);
    }

    // ---- accepted and ignored (depth, alpha test, shading, texture parameters, lines, scissor, fixed-function lights)
    public static void glDepthMask(boolean flag) {
    }

    public static void glDepthFunc(int func) {
    }

    public static void glAlphaFunc(int func, float ref) {
    }

    public static void glShadeModel(int mode) {
    }

    public static void glCullFace(int mode) {
    }

    public static void glLineWidth(float width) {
    }

    public static void glPointSize(float size) {
    }

    public static void glHint(int target, int mode) {
    }

    public static void glTexEnvi(int target, int pname, int param) {
    }

    public static void glTexParameteri(int target, int pname, int param) {
    }

    public static void glScissor(int x, int y, int w, int h) {
    }

    public static int glGetInteger(int pname) {
        return 0;
    }

    public static boolean glGetBoolean(int pname) {
        return false;
    }
}
