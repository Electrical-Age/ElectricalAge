package mods.eln.robotics.computer.screen;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dynamo's pen sets (Prefs): the colours of the desk, the menu bar, window frames and titles, buttons and
 * requesters. A window's contents (text, logs, lists, editors) keep their own colours in every set. The scene carries
 * the set's name ({@code (pens "Amber")}), so every display and the monitor draw alike.
 */
public record Pens(int desk, int bar, int barText, int titleActive, int titleActiveText, int titleIdle, int titleIdleText,
                   int button, int ask, int askText, int askButton) {
    /** The sets by name, the first the default (today's slate and cream). */
    public static final Map<String, Pens> SETS = new LinkedHashMap<>();

    static {
        SETS.put("Grid Blue", new Pens(0xFF3B5470, 0xFFE8E0C8, 0xFF14161A, 0xFFE0A030, 0xFF14161A, 0xFF5A6070, 0xFFD8D2C0, 0xFF5A5A66, 0xFFD8D2C0, 0xFF14161A, 0xFF8A90A0));
        SETS.put("Amber", new Pens(0xFF2A1C0E, 0xFFF0B040, 0xFF1A1006, 0xFFF0B040, 0xFF1A1006, 0xFF6A4A20, 0xFFF0D8A0, 0xFF7A5A28, 0xFFF0D8A0, 0xFF1A1006, 0xFF8A6A3A));
        SETS.put("Paper", new Pens(0xFFB8B4A8, 0xFFF4F0E6, 0xFF202020, 0xFF3060A0, 0xFFF4F0E6, 0xFF8A8A8A, 0xFFF4F0E6, 0xFF707078, 0xFFF4F0E6, 0xFF202020, 0xFF8A8A8A));
    }

    public static final Pens DEFAULT = SETS.get("Grid Blue");

    /** This set's name (the default's if it isn't one of the sets). */
    public String name() {
        for (var e : SETS.entrySet()) if (e.getValue().equals(this)) return e.getKey();
        return "Grid Blue";
    }

    /** The set called name, or the default. */
    public static Pens of(String name) {
        return name == null ? DEFAULT : SETS.getOrDefault(name, DEFAULT);
    }
}
