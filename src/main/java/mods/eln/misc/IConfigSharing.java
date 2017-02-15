package mods.eln.misc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface IConfigSharing {
    void serializeConfig(DataOutputStream stream) throws IOException;

    void deserialize(DataInputStream stream) throws IOException;
}
