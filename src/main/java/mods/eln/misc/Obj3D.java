package mods.eln.misc;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.io.*;
import java.util.*;

public class Obj3D {
    private Timer updateTimer = null;
    private boolean locked = false;

    List<Vertex> vertex = new ArrayList<Vertex>();
    List<Uv> uv = new ArrayList<Uv>();

    // Model obj properties read from the txt file
    Map<String, String> nameToStringHash = new Hashtable<String, String>();

    public float xDim, yDim, zDim;
    public float xMin = 0, yMin = 0, zMin = 0;
    public float xMax = 0, yMax = 0, zMax = 0;
    public float dimMax, dimMaxInv;

    private String dirPath;

	public void bindTexture(String texFilename){
		ResourceLocation textureResource = new ResourceLocation("eln", "model/" + dirPath + "/" + texFilename);
		UtilsClient.bindTexture(textureResource);
	}

    public static class FaceGroup {
        String mtlName = null;
        public ResourceLocation textureResource;
        List<Face> face = new ArrayList<Face>();
        boolean listReady = false;
        int glList;

        public void bindTexture() {
            UtilsClient.bindTexture(textureResource);
        }

        public BoundingBox boundingBox() {
            float xMin = 0, xMax = 0, yMin = 0, yMax = 0, zMin = 0, zMax = 0;
            for (Face f : face) {
                for (Vertex v : f.vertex) {
                    xMin = xMax = v.x;
                    yMin = yMax = v.y;
                    zMin = zMax = v.z;
                    break;
                }
            }
            for (Face f : face) {
                for (Vertex v : f.vertex) {
                    xMin = Math.min(xMin, v.x);
                    xMax = Math.max(xMax, v.x);
                    yMin = Math.min(yMin, v.y);
                    yMax = Math.max(yMax, v.y);
                    zMin = Math.min(zMin, v.z);
                    zMax = Math.max(zMax, v.z);
                }
            }
            return new BoundingBox(xMin, xMax, yMin, yMax, zMin, zMax);
        }

        public void draw() {
            if (textureResource != null) {
                bindTexture();
                drawNoBind();
            } else {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                drawNoBind();
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            }
        }

        private void drawVertex() {
            drawVertex(0, 0);
        }

        private void drawVertex(float offsetX, float offsetY) {
            int mode = 0;

            for (Face f : face) {
                if (f.vertexNbr != mode) {
                    if (mode != 0)
                        GL11.glEnd();
                    switch (f.vertexNbr) {
                        case 3:
                            GL11.glBegin(GL11.GL_TRIANGLES);
                            break;
                        case 4:
                            GL11.glBegin(GL11.GL_QUADS);
                            break;
                        case 6:
                            //	GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
                            break;
                        case 8:
                            //	GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
                            break;
                    }

                    mode = f.vertexNbr;
                }

                GL11.glNormal3f(f.normal.x, f.normal.y, f.normal.z);
                for (int idx = 0; idx < mode; idx++) {
                    if (f.uv[idx] != null)
                        GL11.glTexCoord2f(f.uv[idx].u+offsetX, f.uv[idx].v+offsetY);
                    GL11.glVertex3f(f.vertex[idx].x, f.vertex[idx].y, f.vertex[idx].z);
                }
            }

            if (mode != 0)
                GL11.glEnd();
        }

        public void drawNoBind() {
            if (!listReady) {
                listReady = true;
                glList = GL11.glGenLists(1);

                GL11.glNewList(glList, GL11.GL_COMPILE);
                drawVertex();
                GL11.glEndList();
            }

            GL11.glCallList(glList);
        }
    }

    public class Obj3DPart {
        // TODO(Baughn): Profile, see if it makes sense to use vertex arrays.

        List<Vertex> vertex;
        List<Uv> uv;

        List<FaceGroup> faceGroup = new ArrayList<FaceGroup>();

        Map<String, Float> nameToFloatHash = new Hashtable<String, Float>();

        public float xMin = 0, yMin = 0, zMin = 0;
        public float xMax = 0, yMax = 0, zMax = 0;
        private BoundingBox boundingBox = null;

        float ox, oy, oz;
        float ox2, oy2, oz2;

        public Obj3DPart(List<Vertex> vertex, List<Uv> uv) {
            this.vertex = vertex;
            this.uv = uv;
        }

        void clear() {
            faceGroup.clear();
            boundingBox = null;
            xMin = 0;
            yMin = 0;
            zMin = 0;
            xMax = 0;
            yMax = 0;
            zMax = 0;
        }

        void addVertex(Vertex v) {
            vertex.add(v);
            xMin = Math.min(xMin, v.x);
            yMin = Math.min(yMin, v.y);
            zMin = Math.min(zMin, v.z);
            xMax = Math.max(xMax, v.x);
            yMax = Math.max(yMax, v.y);
            zMax = Math.max(zMax, v.z);
            boundingBox = null;
        }

        public float getFloat(String name) {
            return nameToFloatHash.get(name);
        }

        public void draw(float angle, float x, float y, float z) {
            if (locked) return;

            GL11.glPushMatrix();

            GL11.glTranslatef(ox, oy, oz);
            GL11.glRotatef(angle, x, y, z);
            GL11.glTranslatef(-ox, -oy, -oz);
            draw();

            GL11.glPopMatrix();
        }

        public void draw(float angle, float x, float y, float z, float texOffsetX, float texOffsetY) {
            if (locked) return;

            GL11.glPushMatrix();

            GL11.glTranslatef(ox, oy, oz);
            GL11.glRotatef(angle, x, y, z);
            GL11.glTranslatef(-ox, -oy, -oz);
            draw(texOffsetX,texOffsetY);

            GL11.glPopMatrix();
        }

        public void draw(float angle, float x, float y, float z, float angle2, float x2, float y2, float z2) {
            if (locked) return;

            GL11.glPushMatrix();

            GL11.glTranslatef(ox, oy, oz);
            GL11.glRotatef(angle, x, y, z);
            GL11.glTranslatef(ox2, oy2, oz2);
            GL11.glRotatef(angle2, x2, y2, z2);
            GL11.glTranslatef(-ox2, -oy2, -oz2);
            GL11.glTranslatef(-ox, -oy, -oz);
            draw();

            GL11.glPopMatrix();
        }

        public void drawNoBind(float angle, float x, float y, float z) {
            if (locked) return;

            GL11.glPushMatrix();

            GL11.glTranslatef(ox, oy, oz);
            GL11.glRotatef(angle, x, y, z);
            GL11.glTranslatef(-ox, -oy, -oz);
            drawNoBind();

            GL11.glPopMatrix();
        }

        public void drawNoBind() {
            if (locked) return;

            for (FaceGroup fg : faceGroup) {
                fg.drawNoBind();
            }
        }

        public void draw() {
            if (locked) return;

            //	Minecraft.getMinecraft().mcProfiler.startSection("OBJ");
            for (FaceGroup fg : faceGroup) {
                fg.draw();
            }
            //	Minecraft.getMinecraft().mcProfiler.endSection();
        }

		public void draw(float texOffsetX, float texOffsetY) {
            if (locked) return;

			//	Minecraft.getMinecraft().mcProfiler.startSection("OBJ");
			for (FaceGroup fg : faceGroup) {
				fg.drawVertex(texOffsetX,texOffsetY);
			}
			//	Minecraft.getMinecraft().mcProfiler.endSection();
		}

        // Returns the bounding box of the vertices we'd draw.
        public BoundingBox boundingBox() {
            if (boundingBox == null) {
                BoundingBox box = BoundingBox.mergeIdentity();
                for (FaceGroup fg : faceGroup) {
                    box = box.merge(fg.boundingBox());
                }
                boundingBox = box;
            }
            return boundingBox;
        }
    }

    Hashtable<String, Obj3DPart> nameToPartHash = new Hashtable<String, Obj3DPart>();

    public class Vertex {
        Vertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        Vertex(String[] value) {
            x = Float.parseFloat(value[0]);
            y = Float.parseFloat(value[1]);
            z = Float.parseFloat(value[2]);
        }

        public float x, y, z;
    }

    class Uv {
        Uv(float u, float v) {
            this.u = u;
            this.v = v;
        }

        Uv(String[] value) {
            u = Float.parseFloat(value[0]);
            v = Float.parseFloat(value[1]);
        }

        public float u, v;
    }

    class Normal {
        Normal(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        Normal(String[] value) {
            x = Float.parseFloat(value[0]);
            y = Float.parseFloat(value[1]);
            z = Float.parseFloat(value[2]);
        }

        Normal(Vertex o, Vertex a, Vertex b) {
            float a_x = a.x - o.x;
            float a_y = a.y - o.y;
            float a_z = a.z - o.z;

            float b_x = b.x - o.x;
            float b_y = b.y - o.y;
            float b_z = b.z - o.z;

            x = a_y * b_z - a_z * b_y;
            y = a_z * b_x - a_x * b_z;
            z = a_x * b_y - a_y * b_x;

            float norme = (float) Math.sqrt(x * x + y * y + z * z);

            x /= norme;
            y /= norme;
            z /= norme;
        }

        public float x, y, z;
    }

    class Face {
        Face(Vertex[] vertex, Uv[] uv, Normal normal) {
            this.vertex = vertex;
            this.uv = uv;
            this.normal = normal;
            vertexNbr = vertex.length;
        }

        public Vertex[] vertex;
        public Uv[] uv;
        Normal normal;
        public int vertexNbr;
    }

    public ResourceLocation getModelResourceLocation(String name) {
        return new ResourceLocation("eln", "model/" + dirPath + "/" + name);
    }

    /**
     * Load a resource (obj, mtl, txt file) for a model.
     * @param filePath the path from the "assets/eln" folder
     * @return the  {@code BufferedReader} or null if the resource does not exist
     */
    private BufferedReader getResourceAsStream(String filePath) {
        final String path = "assets/eln/" + filePath;
        try {
            InputStream in = getClass().getClassLoader().getResourceAsStream(path);
            return new BufferedReader(new InputStreamReader(in, "UTF-8"));
        } catch (Exception e) {
            // Utils.println("Unable to load the resource '" + path + "' !");
            return null;
        }
    }

    public boolean loadFile(final String filePath) {
        return loadFile(filePath, false);
    }

    public boolean loadFile(final String filePath, boolean reload) {
        Obj3DPart part = null;
        FaceGroup fg = null;

        if (reload) {
            locked = true;
            vertex.clear();
            uv.clear();
            xMax = 0;
            yMin = 0;
            zMin = 0;
            xMax = 0;
            yMax = 0;
            zMax = 0;
        }

        dirPath = filePath.substring(0, filePath.lastIndexOf('/'));
        String mtlName = null;

        try {
            {
                BufferedReader bufferedReader = getResourceAsStream("model/" + filePath);
                if (bufferedReader == null) {
                    Utils.println(String.format(" - failed to load obj '%s'", filePath));
                    return false;
                }

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] words = line.split(" ");
                    if (words[0].equals("o")) {
                        if (reload) {
                            part = nameToPartHash.get(words[1]);
                            if (part != null) {
                                part.clear();
                            } else {
                                part = new Obj3DPart(vertex, uv);
                                nameToPartHash.put(words[1], part);
                            }
                        } else {
                            part = new Obj3DPart(vertex, uv);
                            nameToPartHash.put(words[1], part);
                        }

                    } else if (words[0].equals("v")) {
                        Vertex v = new Vertex(Float.parseFloat(words[1]), Float.parseFloat(words[2]), Float.parseFloat(words[3]));
                        part.addVertex(v);
                        xMin = Math.min(xMin, v.x);
                        yMin = Math.min(yMin, v.y);
                        zMin = Math.min(zMin, v.z);
                        xMax = Math.max(xMax, v.x);
                        yMax = Math.max(yMax, v.y);
                        zMax = Math.max(zMax, v.z);
                    } else if (words[0].equals("vt")) {
                        part.uv.add(new Uv(Float.parseFloat(words[1]),
                                1 - Float.parseFloat(words[2])));
                    } else if (words[0].equals("f")) {
                        int vertexNbr = words.length - 1;
                        if (vertexNbr == 3) {
                            Vertex[] verticeId = new Vertex[vertexNbr];
                            Uv[] uvId = new Uv[vertexNbr];
                            for (int idx = 0; idx < vertexNbr; idx++) {
                                String[] id = words[idx + 1].split("/");

                                verticeId[idx] = part.vertex.get(Integer.parseInt(id[0]) - 1);
                                if (id.length > 1 && !id[1].equals("")) {
                                    uvId[idx] = part.uv.get(Integer.parseInt(id[1]) - 1);
                                } else {
                                    uvId[idx] = null;
                                }
                            }
                            fg.face.add(new Face(verticeId, uvId, new Normal(verticeId[0], verticeId[1], verticeId[2])));
                        } else {
                            Utils.println("obj assert vertexNbr != 3");
                        }
                    } else if (words[0].equals("mtllib")) {
                        mtlName = words[1];
                    } else if (words[0].equals("usemtl")) {
                        fg = new FaceGroup();
                        fg.mtlName = words[1];
                        part.faceGroup.add(fg);
                    }
                }
            }

            {
                BufferedReader bufferedReader = getResourceAsStream("model/" + dirPath + "/" + mtlName);
                if (bufferedReader == null) {
                    Utils.println(String.format(" - failed to load mtl '%s'", mtlName));
                    return false;
                }

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] words = line.split(" ");
                    if (words[0].equals("newmtl")) {
                        mtlName = words[1];
                    } else if (words[0].equals("map_Kd")) {
                        for (Obj3DPart partPtr : nameToPartHash.values()) {
                            for (FaceGroup fgroup : partPtr.faceGroup) {
                                if (fgroup.mtlName != null && fgroup.mtlName.equals(mtlName))
                                    fgroup.textureResource = getModelResourceLocation(words[1]);
                            }
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        part = null;

        try {
            final String txtPath = filePath.replace(".obj", ".txt").replace(".OBJ", ".txt");
            BufferedReader bufferedReader = getResourceAsStream("model/" + txtPath);
            if (bufferedReader == null) {
                Utils.println(String.format(" - failed to load txt '%s'", txtPath));
            }
            else {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] words = line.split(" ");
                    if (words[0].equals("o")) {
                        part = nameToPartHash.get(words[1]);
                    } else if (words[0].equals("f")) {
                        if (words[1].equals("originX")) {
                            part.ox = Float.valueOf(words[2]);
                        } else if (words[1].equals("originY")) {
                            part.oy = Float.valueOf(words[2]);
                        } else if (words[1].equals("originZ")) {
                            part.oz = Float.valueOf(words[2]);
                        } else if (words[1].equals("originX2")) {
                            part.ox2 = Float.valueOf(words[2]);
                        } else if (words[1].equals("originY2")) {
                            part.oy2 = Float.valueOf(words[2]);
                        } else if (words[1].equals("originZ2")) {
                            part.oz2 = Float.valueOf(words[2]);
                        } else {
                            part.nameToFloatHash.put(words[1], Float.valueOf(words[2]));
                        }
                    } else if (words[0].equals("s")) {
                        nameToStringHash.put(words[1], words[2]);
                    } else if (!reload && words.length == 2 && words[0].equals("r")) {
                        int refresh = Integer.parseInt(words[1]);
                        if (refresh != 0) {
                            updateTimer = new Timer();
                            updateTimer.scheduleAtFixedRate(new TimerTask() {
                                @Override
                                public void run() {
                                    Utils.println("Reloading model data from " + filePath);
                                    loadFile(filePath, true);
                                }
                            }, refresh, refresh);
                        }
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        xDim = xMax - xMin;
        yDim = yMax - yMin;
        zDim = zMax - zMin;

        dimMax = Math.max(Math.max(xMax, yMax), zMax);
        dimMaxInv = 1.0f / dimMax;

        if (reload) {
            locked = false;
        }

        return true;
    }

    public Obj3DPart getPart(String part) {
        return nameToPartHash.get(part);
    }

    public void draw(String part) {
        Obj3DPart partPtr = getPart(part);
        if (partPtr != null)
            partPtr.draw();
    }

    public String getString(String name) {
        return nameToStringHash.get(name); // Property read from the txt file
    }
}
