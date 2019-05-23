package mods.eln.debug;

import mods.eln.Eln;

import java.util.ArrayList;

public class DebugPrint {

    private ArrayList<DebugType> enabledTypes;

    public DebugPrint(ArrayList<DebugType> enabled) {
        enabledTypes = enabled;
        Eln.logger.info("Debugger enabled?: " + Eln.debugEnabled);
        Eln.logger.info("Enabled Debugging types: " + enabledTypes);

    }

    public void add(DebugType t) {
        enabledTypes.add(t);
    }

    public void remove(DebugType t) {
        if (enabledTypes.contains(t)) {
            enabledTypes.remove(t);
        }
    }

    public void clear() {
        enabledTypes.clear();
    }

    public ArrayList<DebugType> get() {
        try {
            return (ArrayList<DebugType>) enabledTypes.clone();
        } catch (Exception e) {
            Eln.logger.error("Error, cannot clone: " + e);
            return new ArrayList<>();
        }
    }

    public void println(DebugType type, String str) {
        if (Eln.debugEnabled) {
            if (enabledTypes.contains(type)) {
                Eln.logger.info("[" + type.name() + "]: " + str);
            }
        }
    }

    public void print(DebugType type, String str) {
        if (Eln.debugEnabled) {
            if (enabledTypes.contains(type)) {
                Eln.logger.info("[" + type.name() + "]: " + str);
            }
        }
    }

    public void println(DebugType type, String format, Object... data) {
        println(type, String.format(format, data));
    }

    public void print(DebugType type, String format, Object... data) {
        print(type, String.format(format, data));
    }


}
