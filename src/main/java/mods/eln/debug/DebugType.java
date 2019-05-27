package mods.eln.debug;

public enum DebugType {
    MNA, SIX_NODE, TRANSPARENT_NODE, SIMPLE_NODE, MECHANICAL, NODE, SOUND, GUI, RENDER, NETWORK, FILE, CONSOLE, LEGACY, OTHER

    // MNA: For any code in mod.eln.mna.*
    // SIX_NODE: For any code in mod.eln.sixnode.*
    // TRANSPARENT_NODE: For any code in mod.eln.transparentnode.*
    // SIMPLE_NODE: For any code in mod.eln.simplenode.*
    // MECHANICAL: For any code in mod.eln.mechanical.*
    // NODE: For any code in mod.eln.node.*
    // SOUND: For any code in the sound engine(s)
    // GUI: For any code in the GUI libraries
    // RENDER: For any code in the render loader or renderer classes
    // NETWORK: For any network debugging
    // FILE: Anywhere you touch files
    // CONSOLE: Anywhere you're handling commands
    // LEGACY: For any calls that are not converted that go to Utils.println()
}
