package mods.eln.misc;

import mods.eln.misc.Obj3D.Obj3DPart;

import java.util.HashMap;

public class Obj3DFolder {

    HashMap<String, Obj3D> nameToObjHash = new HashMap<String, Obj3D>();

	/* public void loadFolder(String modName, String folderName) {

			URI url;
			try {
				URL rec = mods.eln.Eln.class.getResource("/assets/" + modName +  folderName);
				
				if(rec == null) {
					Utils.println("if(rec == null)");
				} else {
					url = rec.toURI();
					if (url == null) {
						Utils.println("if(url == null)");
					} else {
					    File dir = new File(url);
						//File dir = new File(mods.eln.Eln.class.getResource(folderName).getFile());
					    File[] lol = dir.listFiles();
					    for (File file : dir.listFiles()) {
						    if (file.isFile()) {
						    	if(file.getName().endsWith(".obj")||file.getName().endsWith(".OBJ")) {
							    //	String fileName = folder + file.getName();
							    	Obj3D obj =  new Obj3D();
							    	obj.loadFile(modName,folderName + "/" + file.getName());
							    	//Utils.println( '"' + folderName + "/" + file.getName() + '"' + ',');
							    	String tag = file.getName().replaceAll(".obj", "").replaceAll(".OBJ", "");
							    	nameToObjHash.put(tag,obj);
						    	}
						    }
						    if(file.isDirectory()) {
						    	//String bi = file.getName();
						    	loadFolder(modName,folderName + "/" + file.getName());
						    }
						}	
					}
				}
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}*/

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
