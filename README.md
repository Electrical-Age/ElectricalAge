# Minecraft Mod - Codename : ELN

[![Build Status](https://travis-ci.org/Electrical-Age/ElectricalAge.svg?branch=1.7.10-MNA)](https://travis-ci.org/Electrical-Age/ElectricalAge)

Electrical Age (ELN) is a Minecraft Mod offering the ability to perform large-scale in-game electrical simulations.

Look at the official project website [electrical-age.net](https://electrical-age.net/) and [the Wiki](http://wiki.electrical-age.net/) to get general information. [This official Minecraft forum post](http://www.minecraftforum.net/topic/2741783-172forge-electrical-age-mod-beta-146/) is updated frequently.

## How to get started

**ElectricalAge is Minecraft 1.7.10 compatible only. Forge is needed.**

1. Download the [last mod release](https://github.com/Electrical-Age/ElectricalAge/releases/download/BETA-1.10/ElectricalAge_BETA-1.10_r50.jar) (other versions are available [here](https://github.com/Electrical-Age/ElectricalAge/releases)). Copy the Jar file to the `.minecraft/mods/` directory (or related folder depending on your platform).
2. Get started with the tutorial map or start playing on the offical demonstration map. The two official maps can be [downloaded from here](https://github.com/Electrical-Age/ElectricalAge/releases/download/BETA-1.10/ElectricalAge_tutorialMap_BETA-1.9_r41.zip). They must be copied to the `.minecraft/saves/` (or related) directory.
3. Launch Minecraft using the `1.7.10-Forge` profile, select the tutorial map and enjoy!

### Building from source

Alternatively, you can compile and launch the current development version.
[Download](https://github.com/Electrical-Age/ElectricalAge/archive/1.7.10-MNA.zip) or clone the `1.7.10-MNA` branch. Then build and launch the tutorial map using Gradle:

```sh
$ git clone https://github.com/Electrical-Age/ElectricalAge.git
$ cd ElectricalAge
$ gradle unzipTutoMap runClient
```

## Contributing

We appreciate any help from the community to improve the mod. You can find more information [here](./CONTRIBUTING.md).

## ABOUT

Here is some highlighted features:

A better simulation
> Electrical simulation with resistive and capacitive effects. Behaviour similar to those of real life objects.

Multiple electrical machines and components
> Furnaces, Solar panels, Wind turbines, Batteries, Capacitors, ...

Break the cube
> Cables, sensors, actuators, alarms, etc. can be placed on each face (outer and inner) of a cube, which allows a significant reduction of the consumed space by electrical installations.

Night-lighting revisited
> Lamps, switches, captors, ...

Small and big electrical consumers
> From lamps and electrical furnaces to miners and transporters...

Incredible tools
> XRay scanner, flashlight, portable mining drill...

Interoperability
> Old redstone circuits can be exploited with electrical <-> redstone converters.

Game lifetime/complexity extended
> A consequent list of new raw materials and items...

## CURRENT STATE

Electrical Age is still in **Beta**.
Use at your own risk and do map backup frequently.

## MAIN DEVELOPERS

- **Dolu1990** (Code guru, concepts, some 3D models)
- **lambdaShade** (3D models/texturing/graphics maestro, concepts, some sounds and lines of code)
- **cm0x4D** (Sound engineer, code and 3D models/texturing, concepts)
- **metc** (Website/Wiki webmaster)

## MAIN CONTRIBUTORS

Code/models:

- **Svein Ove Aas** (Thermistor, Powerpole, some tweaks)
- **bloxgate** (some tweaks)
- **DrummerMC** (bug fix)
- **ltouroumov** (bug fix)
- **meelock** (typo fix)
- **Sukasa** (code enhancement)
- **DrummingFish** (GUI text parsing, cleaning/refactoring, some tweaks)

Languages:

- **bomdia** (it_IT)
- **Ahtsm** (zh_CN)
- **dcbrwn** (ru_RU)
- **XxCoolGamesxX** (es_ES - deprecated)

Mod promotion:

- **TheBroBeans** (initial promotor)
- **don_bruce/BenPlotz** (forum expert, [video tutorials](https://www.youtube.com/channel/UCRYhOQhspQqIBvL8kiDu2Rw))
- **Baughn** (forum expert)
- ...

The full list of contributors is [available here](https://github.com/Electrical-Age/ElectricalAge/graphs/contributors).

## LICENSE

The source code of this mod is licensed under the LGPL V3.0 licence. See http://www.gnu.org/copyleft/lesser.html for more information.

All graphics and 3D models are licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License. To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/.

![logo](https://raw.githubusercontent.com/Electrical-Age/electrical-age.github.io/master/assets/favicon.ico)
