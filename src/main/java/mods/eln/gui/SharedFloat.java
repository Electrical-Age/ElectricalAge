package mods.eln.gui;

import java.io.DataInputStream;
import java.io.IOException;

public class SharedFloat {

    int networkUUID;
    boolean syncBoot = true;
    boolean syncNew = false;
    float syncValue = 0;

    public SharedFloat(int networkUUID) {
        this.networkUUID = networkUUID;
    }

    public void clientNetworkUnserialize(DataInputStream stream) {
        float readed;
        try {
            readed = stream.readFloat();
            if (syncBoot || syncValue != readed) {
                syncBoot = true;
                syncValue = readed;
            }
            syncBoot = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	/*
    public void clientSend(float value) {
        try {
	    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        DataOutputStream stream = new DataOutputStream(bos);   	
	
	        preparePacketForServer(stream);
			
			stream.writeByte(TransformerElement.unserializeRatio);
			stream.writeFloat(value);
			
			sendPacketToServer(bos);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}*/
}
