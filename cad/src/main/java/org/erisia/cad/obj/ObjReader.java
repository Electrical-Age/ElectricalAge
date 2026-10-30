package org.erisia.cad.obj;

import org.erisia.cad.Face;
import org.erisia.cad.Flat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads an OBJ (either target: the block loader's 0..1 corner frame, or EA's Obj3D centred -0.5..0.5) into faces in px
 * (0..16, y up), so the checker can look at models not made with this library. Each face's material is
 * "part/usemtl"; its solid is a Flat of its own (the checker can't tell which faces make one solid).
 */
public final class ObjReader {
    private ObjReader() {
    }

    /** What the file was made by, from its header comment. */
    public record Model(List<Face> faces, String header, List<String> problems) {
    }

    /** centred: Obj3D (-0.5..0.5); else the block loader's corner frame (0..1). */
    public static Model read(Path obj, boolean centred) throws IOException {
        List<double[]> v = new ArrayList<>();
        List<float[]> vt = new ArrayList<>();
        List<Face> faces = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        String part = "", mat = "", header = "";
        int lineNo = 0;
        for (String line : Files.readAllLines(obj)) {
            lineNo++;
            if (line.startsWith("#")) {
                if (header.isEmpty()) header = line.substring(1).trim();
                continue;
            }
            String[] f = line.trim().split("\\s+");
            switch (f[0]) {
                case "v" -> {
                    double o = centred ? 0.5 : 0;
                    v.add(new double[]{(Double.parseDouble(f[1]) + o) * 16, (Double.parseDouble(f[2]) + o) * 16, (Double.parseDouble(f[3]) + o) * 16});
                }
                case "vt" -> vt.add(new float[]{Float.parseFloat(f[1]), f.length > 2 ? Float.parseFloat(f[2]) : 0});
                case "o", "g" -> part = f.length > 1 ? f[1] : "";
                case "usemtl" -> mat = f.length > 1 ? f[1] : "";
                case "f" -> {
                    int n = f.length - 1;
                    double[] xyz = new double[3 * n];
                    float[] uv = new float[2 * n];
                    boolean hasUv = true;
                    try {
                        for (int i = 0; i < n; i++) {
                            String[] idx = f[i + 1].split("/");
                            double[] p = v.get(index(idx[0], v.size()));
                            System.arraycopy(p, 0, xyz, 3 * i, 3);
                            if (idx.length > 1 && !idx[1].isEmpty()) {
                                float[] t = vt.get(index(idx[1], vt.size()));
                                uv[2 * i] = t[0];
                                uv[2 * i + 1] = t[1];
                            } else hasUv = false;
                        }
                    } catch (RuntimeException e) {
                        problems.add("line " + lineNo + ": " + e);
                        continue;
                    }
                    faces.add(Flat.face(xyz, hasUv ? uv : null, 0xFFFFFFFF, part + "/" + mat, part + ":" + lineNo));
                }
                default -> {
                }
            }
        }
        return new Model(faces, header, problems);
    }

    static int index(String s, int size) {
        int i = Integer.parseInt(s);
        return i < 0 ? size + i : i - 1;
    }
}
