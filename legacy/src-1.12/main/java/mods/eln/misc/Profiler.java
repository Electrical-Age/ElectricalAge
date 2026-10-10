package mods.eln.misc;

import java.util.LinkedList;

public class Profiler {

    static class ProfilerData {
        String name;
        long nano;

        ProfilerData(String name, long nano) {
            this.name = name;
            this.nano = nano;
        }
    }

    LinkedList<ProfilerData> list = new LinkedList<Profiler.ProfilerData>();

    void reset() {
        list.clear();
    }

    public void add(String name) {
        list.add(new ProfilerData(name, System.nanoTime()));
    }

    public void stop() {
        add(null);
    }

    public String toString() {
        String str = "";
        ProfilerData last = null;
        for (ProfilerData p : list) {
            if (last != null) {
                str += last.name + " in " + (p.nano - last.nano) / 1000 + "  ";
            }
            last = p;
        }

        return str;
    }
}
