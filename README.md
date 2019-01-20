# Minecraft Mod - Codename : ELN

Electrical Age (ELN) is a Minecraft Mod offering the ability to perform large-scale in-game electrical simulations.

Look at the official project website [electrical-age.net](https://electrical-age.net/) and [the Wiki](http://wiki.electrical-age.net/) to get general information.

This is a fork of Electrical Age, maintained by Jared Dunbar (jrddunbr) and Graham Nothup (Grissess), with the intent to add features and push for active development, which is incorporated back into the main distribution as they see fit.

## How to get started

**ElectricalAge is Minecraft 1.7.10 compatible only. Forge is needed.**

1. Visit the [releases](https://github.com/jrddunbr/ElectricalAge/releases) page on GitHub for the latest stable build.

2. Download the latest build, and add the jar to your launcher. Be sure that you are using Forge 1.7.10, revision 1614.

3. Launch, and have fun!

4. Download the [test world](https://eln.ja13.org/worlds/latest.zip).

### Building from source

You can compile and launch the current development version.
[Download](https://github.com/jrddunbr/ElectricalAge/archive/base.zip) or clone the `base` branch. Then build and launch using Gradle:

```bash
$ git clone https://github.com/jrddunbr/ElectricalAge.git
$ cd ElectricalAge
$ ./gradlew setupDecompWorkspace
$ ./gradlew runClient
```

## Contributing

We appreciate any help from the community to improve the mod. You can find more information [here](./CONTRIBUTING.md).

## ABOUT

Here is some highlighted features:

A better simulation
> Electrical simulation with resistive and capacitive effects. Behavior similar to those of real life objects.

Multiple electrical machines and components
> Furnaces, Solar panels, Wind turbines, Batteries, Capacitors, ...

Break the cube
> Cables, sensors, actuators, alarms, etc. can be placed on each face (outer and inner) of a cube, which allows a significant reduction of the consumed space by electrical installations.

Night-lighting revisited
> Lamps, switches, captors, ...

Small and big electrical consumers
> From lamps and electrical furnaces to miners and transporters...

Incredible tools
> X-Ray scanner, flashlight, portable mining drill...

Interoperability
> Old redstone circuits can be exploited with electrical <-> redstone converters.

Game lifetime/complexity extended
> A consequent list of new raw materials and items...

## CURRENT STATE

Electrical Age is still in **Beta**.
Use at your own risk and do map backup frequently and between mod updates.

## MAIN DEVELOPERS

- **Dolu1990** (Code guru, concepts, some 3D models)
- **Svein Ove Aas, aka. Baughn** (Code, some 3D models, concepts)
- **jrddunbr** (concepts, ideas, code)
- **Grissess** (code, models, textures, sounds, math, MNA work)
- **cm0x4D** (Sound engineer, code and 3D models/texturing, concepts)
- **lambdaShade** (3D models/texturing/graphics maestro, concepts, some sounds and lines of code)
- **metc** (Website/Wiki webmaster)

## MAIN CONTRIBUTORS

Code/models:

- **OmegaHaxors** (major updates to balance, new features)
- **lashtear** (small bugfixes and new features)
- **bloxgate** (some tweaks)
- **DrummerMC** (bug fix)
- **ltouroumov** (bug fix)
- **meelock** (typo fix)
- **Sukasa** (code enhancement)
- **DrummingFish** (GUI text parsing, cleaning/refactoring, some tweaks)

Languages:

TO BE UPDATED

- **bomdia** (it_IT)
- **Ahtsm** (zh_CN)
- **dcbrwn** (ru_RU)
- **XxCoolGamesxX** (es_ES - deprecated)

Mod promotion:

- **TheBroBeans** (initial promotor)
- **don_bruce/BenPlotz** (forum expert, [video tutorials](https://www.youtube.com/channel/UCRYhOQhspQqIBvL8kiDu2Rw))
- **Baughn** (forum expert)
- ...

The full list of contributors is [available here](https://github.com/jrddunbr/ElectricalAge/graphs/contributors).

## LICENSE

The source code of this mod is licensed under the LGPL V3.0 licence. See http://www.gnu.org/copyleft/lesser.html for more information.

Most graphics and all 3D models are licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License. To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/. These should all be attributed to the Electrical Age team, with the following exceptions:

- src/main/resources/assets/eln/textures/blocks/2x3solarpanel.png
  Designed by [Luis Prado](https://thenounproject.com/Luis/).
- src/main/resources/assets/eln/textures/blocks/scanner.png
  Designed by [Creative Stall](https://thenounproject.com/creativestall/).

Some graphics are public domain. These are:

- src/main/resources/assets/eln/textures/blocks/smallsolarpanel.png
- src/main/resources/assets/eln/textures/blocks/smallrotatingsolarpanel.png
- src/main/resources/assets/eln/textures/blocks/2x3rotatingsolarpanel.png

![logo](https://raw.githubusercontent.com/jrddunbr/electrical-age.github.io/master/assets/favicon.ico)
