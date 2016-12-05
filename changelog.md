## 1.13.0

![Power station](http://i.imgur.com/BdrK2dG.jpg)

Let's call this the "Power grid" release.

The chief feature added this time is electrical utility poles, which allow you
to transfer electricity further and faster than ever before. Although chiefly of
use on modded servers with large energy sinks such as Applied Energistics, you
may find that they're handy for getting that suburb look in your base.

To connect two utility poles, right-click them with a stack of high-voltage
cable.

They come in two variants: With or without transformers. Only the former can be
attached to old-fashioned ground cables.

Utility poles run at a nominal voltage of 12,800 volts, for just over 80kW of
power capacity. A second tier of poles will be added, though possibly not during
the 1.7.10 cycle. I am now gearing up for the port to 1.10.2, but others are
still working on 1.7.10, so this may or may not be the final 1.7.10 release.

### Community spotlight

There is an ongoing series of tutorials, to be found
[on the wiki](https://wiki.electrical-age.net/index.php?title=Examples), and
which now includes an embryonic power-pole tutorial.

As always, we will grant wiki editing access to anyone who shows up
[in gitter](https://gitter.im/Electrical-Age/Support) or
[on irc](https://qchat.rizon.net/?channels=electricalage) and asks.

### Features

- Tier-1 utility poles

- Analog chips

  Similarly to the digital AND/OR/NAND etc. chips added in the previous release,
  these let you break out the signal processor's function into multiple
  blocks. We've also added an OpAmp chip, so please have fun playing with it.

- The macerator now macerates AE2 ores

  The correct way to make it work with mod ores is to install AOBD, but AOBD
  lacked support for AE2.
  
### Bugfixes

- WAILA support now works on (some) 'ghost' blocks, and uses less memory.
