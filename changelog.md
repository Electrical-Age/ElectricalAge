## 1.14.2

### Bugfixes

- Fixed a world-destroying bug in the saving code, which only affected Windows.

  If you've been affected, see
  https://github.com/Electrical-Age/ElectricalAge/issues/673 for a possible
  recovery method.
  
- Typing /eln without a parameter will now list the commands.

## 1.14.1

### Features

- IC2 steam should now work in the steam turbine. You won't get distilled water back, mind you.

### Bugfixes

- Some debug-prints were not marked as debug, and could spam players under certain circumstances.

- The Sample-and-Hold chip had a single global sample across all chips.

## 1.14.0

![Fuel Heat Furnace](https://i.imgur.com/BaaoHiY.png)

This time you get a lot of small features, plus bugfixes.

Did you know? Heat turbines get dramatically less efficient if you don't run
them at prcisely the right temperature and voltage. This means that, although
they aren't bad when optimally used, actually doing so requires clever signal
processing. You should attempt to use the minimum number of heat turbines
possible at any given time.

But I know that most of us will keep building banks like this one.

You can find the download
[at Curseforge](https://minecraft.curseforge.com/projects/electrical-age), as
usual.

### Community spotlight

Nesze is [building a digital computer](https://puu.sh/tL65Y/ed5af7c4ee.png)
using signal processors...

We're looking forward to seeing the result.

---

There is an ongoing series of tutorials, to be found
[on the wiki](https://wiki.electrical-age.net/index.php?title=Examples), and
which now includes an embryonic power-pole tutorial.

As always, we will grant wiki editing access to anyone who shows up
[in gitter](https://gitter.im/Electrical-Age/Support) or
[on irc](https://qchat.rizon.net/?channels=electricalage) and asks.

### Features in this release

- WAILA support has been expanded to (nearly) all blocks. Enjoy!

- The transformer has been renamed and re-modelled. It's now called the DC-DC
  converter. This should reduce confusion.
  
- Added a NOT function to the signal processor.
  Use it as such, for example: "!(B*C)"

- Added a Fuel Heat Furnace.

  This works like the normal heat furnace, but can produce up to 25kW of
  heat. It burns heavy oils and gasolines, but not gases. If you're using a mod
  such as Immersive Engineering to fractionate your oil, then this is a great
  option for making the diesel oil useful.
  
  For gasoline, unless you're extremely careful, this will not be nearly as
  efficient as burning it in the gas turbine. Even if you *are* extremely
  careful, it still won't be quite as good. It is, however, excellent for
  running a bank of starter turbines.
  
- Completely rewrote the way we loop sounds.

  You'll notice this the moment you step into a turbine hall. Sounds are now
  looped client-side, which should prevent glitches from server lag, and
  pitch/volume are adjusted appropriately.
  
  There's a high chance of bugs in this code, though we think we've squashed the
  worst ones. If you notice anything broken, please tell us.
  
  We've also replaced some of the sound files.
  
- Added sound to the gas turbine, steam turbine and generators.

  Whooosh!
  
- Rebalanced the heating value of the various liquid fuels.

  In general they're all slightly increased, but some are far more so than
  others. You'll find that one bucket of gasoline lasts far longer than one
  bucket of ethanol, and additionally the power output of the gas turbine is
  capped by maximum *fluid throughput*, not kilowatt output. This means you'll
  need more gas turbines if you're burning a lesser fuel.
  
  The values used match real life, but the game-balance intent is to encourage
  using non-renewable fuel. Install Buildcraft! Make use of those oil wells! Two
  thousand buckets will last a while, almost certainly.
  
- Rebalanced the gas turbine efficiency curve.

  To make up for the above, the gas turbine must now be spun up to 650 rads/s
  before it will start working. The efficiency curve is tighter in general;
  you'll see about 85% efficiency at 900 rads/s, hopefully encouraging more
  complex builds.
  
  Mind you, 85% is still likely better than what you got before the fuel value
  rebalancing.
  
- You are now able to burn a few more fuel variants...

  Anything not already on the list is due to oversights. Take a look at
  [FuelRegistry.kt](https://github.com/Electrical-Age/ElectricalAge/blob/3e7db53eac084b4f2770139949630d01f72a8767/src/main/java/mods/eln/fluid/FuelRegistry.kt),
  check if your preferred fuel is there, and if not consider opening an issue or
  filing a pull request.
  
- Many items can now be auto-inserted into blocks by right-clicking them.

  Try it!

- New config option for oredicting chips. This is mostly useful for GregTech players.

- Added 2x3 solar panels.

  These panels have a footprint of 2x3 blocks, but a maximum power output of 8
  smaller panels, and a voltage of 100V. They exist in rotating and non-rotating
  variants. The overall intent is to encourage using them above the smaller
  panels, as server load is proportional to number of panels.
  
  We also replaced the solar panel icons. IMO the new ones should be easy to
  recognise.

- Changed the plate machine to take 1 ingot per plate, not 4.

  This is primarily because *that's what every single other mod does*. We added
  a config option in case you prefer the old behaviour, but why would you?
  
- Added a Sample and Hold chip.

  This has one analog and one digital input. It latches the analog input when
  given a signal on the digital input, outputting it until the next time it gets
  such a signal.
  
- Added recipes for the various analog chips.

  ...oops?
  
- The Auto-Miner can now output to any inventory, not just vanilla chests.

  Is this a bugfix or a feature?


### Known bugs

- The generators sometimes fail to pull very much power, preventing them from
  spinning up.
  
  We're on it. A workaround is to add a transformer between it and its power
  source, *set to "Isolated" mode*.
  
### Bugfixes

- Breaking the electrical power exporters by hand no longer destroys the block.

- Fixed a graphical glitch in the industrial data logger.

- Street lamps can no longer be placed on top of each other.

- Fixed a crash bug in the utility-pole code.

- Fixed a memory leak when Eln is run on (some) empty servers.

- Fixed the redstone-to-voltage converter not always updating when it should.

- Fixed thermal probes connecting to non-thermal cables.

- Fixed battery tooltip showing as J instead of kJ.

- Fixed middle-click not working for SixNode multiblocks.

- Also fixed WAILA not working properly, ditto.

- Steam turbines can now be built with iron instead of aluminum, if aluminum doesn't exist. This makes no sense.

- Machines now lose all progress if you change what they're doing. No more quick-compressed diamonds.

- Fixed fuses blowing simply from connecting them.

- Fixed NPE in the WAILA code for fire buzzers.

- Misc. typo fixes.

- Fixed the probable cause of Minecraft crashes sometimes destroying all placed Eln items.

  We still recommend taking backups, but I feel good about this one.
