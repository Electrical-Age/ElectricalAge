# Minecraft Mod - Codename : ELN

[![Build Status](https://travis-ci.org/Electrical-Age/ElectricalAge.svg?branch=ports/1.10)](https://travis-ci.org/Electrical-Age/ElectricalAge)
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2FElectrical-Age%2FElectricalAge.svg?type=shield)](https://app.fossa.io/projects/git%2Bgithub.com%2FElectrical-Age%2FElectricalAge?ref=badge_shield)

Electrical Age (ELN) is a Minecraft Mod offering the ability to perform large-scale in-game electrical simulations.

Look at the official project website [electrical-age.net](https://electrical-age.net/) and [the Wiki](http://wiki.electrical-age.net/) for general information. [This official Minecraft forum post](http://www.minecraftforum.net/topic/2741783-172forge-electrical-age-mod-beta-146/) is occasionally updated. You can also visit our Discord server.

<img src="https://img.shields.io/discord/463476274527076374.svg?style=for-the-badge">

## How to get started

**The newest Electrical Age is Minecraft 1.10.2 compatible only. Forge is needed.**

1. Download the [last mod release](https://minecraft.curseforge.com/projects/electrical-age) (other versions are available through the same link). Copy the Jar file to the `.minecraft/mods/` directory (or related folder depending on your platform).
2. Get started with the tutorial map or start playing on the offical demonstration map. The two official maps can be [downloaded from here](https://github.com/Electrical-Age/ElectricalAge/releases/download/BETA-1.10/ElectricalAge_tutorialMap_BETA-1.9_r41.zip). They must be copied to the `.minecraft/saves/` (or related) directory.
3. Launch Minecraft using the `1.10.2-Forge` profile, select the tutorial map and enjoy!

Alternately to the above: Search for Electrical Age using the Curse launcher. Add it to your profile, and enjoy.

### Building from source

This option is primarily for developers. If you take it, make sure to join our Discord first; see the chat button above.

To build Electrical Age, you need to already have Git and the Java development kit installed. You should also have IDEA, which is what we recommend for working on it. You don't have to be running Linux, but it helps; you can typically install git and the JDK with your package manager, if they don't come preinstalled. Windows users are on your own.

Once the prerequisites are in place, run these commands:

```sh
$ git clone https://github.com/Electrical-Age/ElectricalAge.git
$ cd ElectricalAge
$ git checkout <branch you want to work on>  # Optional. The main development branch is also the default.
$ gradle setupDecompWorkspace
$ gradle build  # Confirm that everything works.
$ gradle unzipTutoMap runClient  # To launch the tutorial map, which is also useful for testing. You only need to unzip once.
```

For more information, see Discord and [HACKING.md](HACKING.md).

## Contributing

We appreciate any help from the community to improve the mod, but please follow the pull request and issue guidelines. You can find the basic guidelines whenever you open one. For more information, go [here](./CONTRIBUTING.md).

## ABOUT

Here are some highlighted features:

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
- **Svein Ove Aas, aka. Baughn** (Code, some 3D models, concepts)
- **cm0x4D** (Sound engineer, code and 3D models/texturing, concepts)
- **lambdaShade** (3D models/texturing/graphics maestro, concepts, some sounds and lines of code)
- **metc** (Website/Wiki webmaster)

## MAIN CONTRIBUTORS

Code/models:

- **bloxgate** (some tweaks)
- **DrummerMC** (bug fix)
- **ltouroumov** (bug fix)
- **meelock** (typo fix)
- **Sukasa** (code enhancement)
- **DrummingFish** (GUI text parsing, cleaning/refactoring, some tweaks)
- **lolmegaxde1** (lots of work on the 1.10 port)

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

Most graphics and all 3D models are licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License. To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/. These should all be attributed to the Electrical Age team, with the following exceptions:

- src/main/resources/assets/eln/textures/blocks/2x3solarpanel.png
  Designed by [Luis Prado](https://thenounproject.com/Luis/).
- src/main/resources/assets/eln/textures/blocks/scanner.png
  Designed by [Creative Stall](https://thenounproject.com/creativestall/).

Some graphics are public domain. These are:

- src/main/resources/assets/eln/textures/blocks/smallsolarpanel.png
- src/main/resources/assets/eln/textures/blocks/smallrotatingsolarpanel.png
- src/main/resources/assets/eln/textures/blocks/2x3rotatingsolarpanel.png

![logo](https://raw.githubusercontent.com/Electrical-Age/electrical-age.github.io/master/assets/favicon.ico)


[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2FElectrical-Age%2FElectricalAge.svg?type=large)](https://app.fossa.io/projects/git%2Bgithub.com%2FElectrical-Age%2FElectricalAge?ref=badge_large)
