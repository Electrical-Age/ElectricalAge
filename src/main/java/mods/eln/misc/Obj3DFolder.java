package mods.eln.misc;

import mods.eln.misc.Obj3D.Obj3DPart;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility class used to load all eln models and corresponding obj files.
 */
public class Obj3DFolder {

    private Map<String, Obj3D> nameToObjHash = new HashMap<String, Obj3D>();

    /**
     * Load all obj models available in the release mod asset folder.
     */
    public void loadAllElnModels() {
        // 26.x: the mod jar can't be scanned under the module system, so script/gen_model_index.py lists the models
        try (java.io.InputStream in = mods.eln.Eln.class.getResourceAsStream("/assets/eln/model/index.txt")) {
            if (in == null) {
                mods.eln.Eln.LOG.error("Electrical Age: assets/eln/model/index.txt missing, no models loaded");
                return;
            }
            int modelCount = 0;
            for (String line : new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).split("\n")) {
                String filename = line.trim();
                if (filename.isEmpty()) continue;
                Utils.println(String.format("Loading model %03d '%s'", ++modelCount, filename));
                loadObj(filename);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadModelsRecursive(File folder, Integer modelCount) {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                loadModelsRecursive(file, modelCount);
            } else if (file.getName().toLowerCase().endsWith(".obj")) {
                String filename = file.getPath().replaceAll("\\\\", "/");
                filename = filename.substring(filename.indexOf("/model/") + 7, filename.length());
                Utils.println(String.format("Loading model %03d '%s'", ++modelCount, filename));
                loadObj(filename);
            }
        }
    }

    /**
     * Load an obj file of a model.
     *
     * @param modelPath path inside model folder (ex. Vumeter/Vumeter.obj)
     */
    private void loadObj(String modelPath) {
        Obj3D obj = new Obj3D();
        if (obj.loadFile(modelPath)) {
            String tag = modelPath.replaceAll(".obj", "").replaceAll(".OBJ", "");
            tag = tag.substring(tag.lastIndexOf('/') + 1, tag.length());
            tag = tag.toLowerCase(java.util.Locale.ROOT); // 1.12 port: asset names are lower case, lookups too
            if (nameToObjHash.containsKey(tag)) {
                Utils.println("Double load of model " + tag);
            }
            nameToObjHash.put(tag, obj);    // name of the file, without extension
            Utils.println(String.format(" - model '%s' loaded", modelPath));
        } else {
            Utils.println(String.format(" - unable to load model '%s'", modelPath));
        }
    }

    public Obj3D getObj(String obj3DName) {
        return nameToObjHash.get(obj3DName.toLowerCase(java.util.Locale.ROOT));
    }

    public Obj3DPart getPart(String objName, String partName) {
        Obj3D obj = getObj(objName);
        if (obj == null) return null;
        return obj.getPart(partName);
    }

    // 26.x port: draw() removed (client rendering; redone in P3)
}
