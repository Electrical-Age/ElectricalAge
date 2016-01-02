package mods.eln.misc;

import mods.eln.misc.Obj3D.Obj3DPart;

import java.io.IOException;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Obj3DFolder {

    HashMap<String, Obj3D> nameToObjHash = new HashMap<String, Obj3D>();

    public void loadAllElnModels() {
        try {
            // Find location of electrical age jar file.
            CodeSource codeSource = Obj3DFolder.class.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                String jarFilePath = codeSource.getLocation().getPath();
                jarFilePath = jarFilePath.substring(5, jarFilePath.indexOf("!"));
                JarFile jarFile = new JarFile(URLDecoder.decode(jarFilePath, "UTF-8"));
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    String filename = entries.nextElement().getName();
                    if (filename.startsWith("assets/eln/model/") && filename.endsWith(".obj")) {
                        filename = filename.substring(filename.indexOf("/model/") + 7, filename.length());
                        loadObj(filename);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadObj(String modelPath) {
		// modelPath is the path inside the model folder with the name of the obj file
        Obj3D obj = new Obj3D();
        if (obj.loadFile(modelPath)) {
            String tag = modelPath.replaceAll(".obj", "").replaceAll(".OBJ", "");
            tag = tag.substring(tag.lastIndexOf('/') + 1, tag.length());
            nameToObjHash.put(tag, obj);	// name of the file, without extension
            Utils.println("Model '"+ modelPath +"' loaded");
        }
    }

    public Obj3D getObj(String obj3DName) {
        return nameToObjHash.get(obj3DName);
    }

    public Obj3DPart getPart(String objName, String partName) {
        Obj3D obj = getObj(objName);
        if (obj == null) return null;
        return obj.getPart(partName);
    }

    public void draw(String objName, String partName) {
        Obj3DPart part = getPart(objName, partName);
        if (part != null) part.draw();
    }
}
