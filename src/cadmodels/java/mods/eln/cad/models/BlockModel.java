package mods.eln.cad.models;

import org.erisia.cad.Model;

/**
 * A NeoForge block model (neoforge:obj): assets/eln/models/block/NAME.{obj,mtl,json}, each material a 16 px sprite
 * painted from the palette into assets/eln/textures/blocks/SPRITES/MATERIAL.png.
 */
public record BlockModel(String name, Model model, String sprites, String particle) {
}
