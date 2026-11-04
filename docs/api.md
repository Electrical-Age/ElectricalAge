# Electrical Age's API

Other mods (and EA's own new devices) use this API to put machines on Electrical Age's circuits. It has two
levels:

- **`mods.eln.api`, the Machine API.** Most mods only need this. You give a machine's nominal spec and get volts and
  joules back. EA handles the circuit, the node at your block, brownouts, trips, saving and syncing.
- **`mods.eln.api.lowlevel`.** EA's circuit parts, for the few devices that are neither a consumer nor a producer of
  power. You own the consequences there (see [The low level](#the-low-level)).

Both levels follow the same rules:

- Call them on the server thread. Calls from other threads throw `IllegalStateException`.
- Every value is checked. A negative, NaN or infinite amount throws `IllegalArgumentException`, and a builder lists
  every problem it finds in one exception.
- Units are in the names (`ratedWatts`, `takeJoules`, `heatCapacityJPerK`).
- Signals are levels from 0 to 1, never volts.
- No EA internals appear in the signatures. The build compiles the API apart from the rest of EA and checks it
  (see [The eln-api jar](#the-eln-api-jar)).

Every code sample on this page is compiled and tested. It is a region of a file under
`src/test/java/mods/eln/api/docs/`, and `ApiDocSamplesTest` fails if the page and the file differ.

## The eln-api jar

EA's build makes the API into jars of its own, for other mods to compile against. Each comes with a `-sources` and a
`-javadoc` jar:

- **`build/libs/eln-api-<version>.jar`:** `mods.eln.api`, `mods.eln.api.lowlevel` and `mods.eln.api.spi`.
- **`build/libs/eln-api-testkit-<version>.jar`:** `mods.eln.api.testkit`, for tests only.

`./gradlew publishAllPublicationsToBuildRepoRepository` also writes them to `build/repo` as `mods.eln:eln-api` and
`mods.eln:eln-api-testkit`.

The jars are for compiling only, and EA's own mod jar is the implementation. In your mod's `build.gradle`:

- Add `compileOnly files("libs/eln-api-<version>.jar")`, or `compileOnly "mods.eln:eln-api:<version>"` from a Maven
  repository.
- Don't ship the jar or jar-in-jar it. EA's jar has the same classes, and two copies of a package break the game.
- In a dev run, EA must be loaded as a mod. For example, put its jar in the run's `mods` folder.

What is in the jars:

- They hold the API alone. `mods.eln.api.spi` is how the API finds EA's implementation, and you don't use it.
- EA keeps some packages out of the jars. These are `mods.eln.api.impl` and `mods.eln.api.element`, which hosts
  machines on EA's own elements (see [Machines inside EA itself](#machines-inside-ea-itself)).
- The API's classes are compiled against Minecraft and NeoForge alone, so no signature can name an EA internal.
- The build also compiles this page's samples and EA's hidden test machine against the jars alone, as another mod
  would (`compileApiConsumer`, `compileApiConsumerTest`). `ApiJarTest` checks that no class in the jars names an EA
  class outside them.

**Versions.** `ElnApi.API_VERSION` is a semantic version (major.minor.patch). It is also in each jar's manifest as
`Eln-Api-Version`. EA promises the following:

- Within a major version, changes are additive: new types, methods, enum constants and builder settings. Defaults,
  units and behaviour you rely on stay the same, so a mod compiled against 1.x runs on any EA with API 1.y where y is
  x or more.
- Anything removed or changed incompatibly comes with a new major version.
- Interfaces that EA implements (`Machine`, `MachineView`, `MachineRegistry` and the low level's parts) may gain
  methods. Interfaces that you implement (`BurnHandler`, the listeners) don't change within a major version.
- An enum may gain constants, so a `switch` over `MachineState` or `PortStatus` needs a `default` branch.

`API_VERSION` is a constant, so javac copies it into your classes, and you get the version you compiled against.
`ElnApi.apiVersion()` gives the installed EA's version.

## Without Electrical Age

If EA is a hard dependency (`type = "required"` in your `neoforge.mods.toml`), you have nothing to do. If EA is
optional, its classes aren't there when it isn't installed. Any class of yours that names a `mods.eln.api` type then
fails with `NoClassDefFoundError` as soon as it is used. To avoid this:

- Check `ModList.get().isLoaded("eln")`.
- Keep every API use in classes that run only after that check passes.

```java src/test/java/mods/eln/api/docs/OptionalEa.java#guard
/** Is EA installed? Ask before anything that reaches mods.eln.api. */
public static final boolean ELN = ModList.get().isLoaded("eln");

/** In the mod's constructor. This class names no mods.eln.api type, so it loads with or without EA. */
public static void init(IEventBus modBus, Supplier<BlockEntityType<MillBlockEntity>> mill) {
    if (ELN) EaCompat.register(modBus, mill);   // EaCompat, and the API with it, loads only here
}

/** The mill's energy this tick: from EA's line when EA is there, else none (it runs on its own fuel). */
public static double joulesFor(BlockEntity mill, double wanted) {
    return ELN ? EaCompat.take(mill, wanted) : 0;
}

/** Every mention of mods.eln.api is in classes like this one, reached only behind ELN. */
static final class EaCompat {
    static void register(IEventBus modBus, Supplier<BlockEntityType<MillBlockEntity>> mill) {
        Mill.register(modBus, mill);
    }

    static double take(BlockEntity mill, double wanted) {
        return ElnApi.machines().of(mill).takeJoules(wanted);
    }
}
```

There are two ways to get this wrong:

- **The API is used without the check.** If EA is missing, the class that uses it fails with
  `NoClassDefFoundError: mods/eln/api/...`. Only the check above prevents this.
- **The API's classes are there but EA isn't.** This happens when the eln-api jar was put on the runtime classpath
  (`implementation` instead of `compileOnly`) or shipped. Then the API's entry points (`ElnApi.machines()`,
  `Circuit.create`, `ElnTestKit`) throw `IllegalStateException`. The message says that EA isn't loaded and points
  here. `MachineSpec` builders, `Meters` and the enums don't need EA and still work.

## A consumer machine

A spec says what the machine is, electrically:

```java src/test/java/mods/eln/api/docs/Mill.java#spec
public static final MachineSpec SPEC = MachineSpec.consumer(Tier.V200)
    .ratedWatts(2000)                        // the most it draws at 200 V, idle included
    .idleWatts(20)                           // drawn whenever it is powered
    .power(RelativeFace.BACK, RelativeFace.DOWN)
    .build();                                // checks every setting, lists every problem at once
```

The defaults are the safe choices:

- `minVoltageRatio(0.75)` sets the undervoltage floor, with `onBrownout(Brownout.STOP)`.
- `tripCooldownSeconds(3)` sets how long a trip waits before it resets.
- Overvoltage (over 1.3 x nominal) trips the machine rather than burning it.
- There is no heat model unless you set one.
- Power connects on every face not given to a signal or the bus.

`MachineSpec` is immutable. `build()` checks every setting and lists every problem in one exception.

To make another mod's block entity type a machine, register it at common setup:

```java src/test/java/mods/eln/api/docs/Mill.java#register
/** In the mod's constructor: the mill's block entity type becomes a machine at common setup. */
public static void register(IEventBus modBus, Supplier<BlockEntityType<MillBlockEntity>> type) {
    modBus.addListener(FMLCommonSetupEvent.class, e -> ElnApi.machines().register(type.get(), be -> SPEC));
}
```

The owner's work is plain code against `Machine`. The same code runs in game and on a test bench:

```java src/test/java/mods/eln/api/docs/Mill.java#work
/** The mill's work, one call per server tick: an item takes 3 kJ, paid tick by tick. */
public static final class Work {
    public static final double JOULES_PER_ITEM = 3000;
    private double paid;

    /** True when an item is done. */
    public boolean tick(Machine m, boolean hasInput) {
        if (!hasInput) return false;                  // no ask: the mill draws its idle watts only
        paid += m.takeJoules(m.ratedJoulesPerTick()); // ask a tick's rated energy, get what the line gave
        if (paid < JOULES_PER_ITEM) return false;
        paid -= JOULES_PER_ITEM;
        return true;
    }
}
```

In game, the block entity's server ticker gets the handle and calls the work:

```java src/test/java/mods/eln/api/docs/MillBlockEntity.java#tick
/** The block's server ticker. */
public static void serverTick(Level level, BlockPos pos, BlockState state, MillBlockEntity be) {
    Machine m = ElnApi.machines().of(be);         // the same handle for as long as the block entity lasts
    if (be.work.tick(m, be.input > 0)) {
        be.input--;
        be.items++;
        be.setChanged();
    }
}
```

There are two ways to take energy:

- **`takeJoules(J)`** takes up to J and returns what it got. Use it for continuous work, such as heaters, motors, or
  work paid tick by tick as above.
- **`tryTakeJoules(J)`** takes all of J or nothing. Use it for a step that needs all its energy at once. A step bigger
  than the buffer (two ticks of rated energy) can never succeed, so it throws.

An ask above `ratedJoulesPerTick()` is capped to it and logged once.

EA handles the block's lifecycle. When the block entity joins the level (placed or loaded), EA puts an unsaved node of
its own at the block and connects it to the cables beside it. When the block entity goes, the node goes too, and the
`Machine` handle reads `DETACHED`.

Faces are relative to the block's `facing` (or `horizontal_facing`) property, and turning the block reconnects them.
Every face is a leaf: power never passes through your block from one face to another. The machine's state (buffer,
temperature, a trip) is saved in the block entity's `eln:machine` data attachment, so you don't save anything by hand.

## Timing: the one-tick start

The order inside one game tick is:

1. EA's simulator steps, in `ServerTickEvent.Pre`, before block entities tick. Each machine draws what it planned.
2. Each machine books what the step delivered: its state, heat and buffer.
3. Your block entity ticks and takes or gives energy.
4. Each machine plans the next step from what you asked.

So what a consumer asks for in tick N sizes its load for tick N+1, exactly one tick later. The buffer holds two ticks
of rated energy.

A machine that asks for nothing draws only its idle watts and doesn't refill its buffer. The first ask of a run
therefore gets only what was left in the buffer, which is often nothing, and **work starts a tick after the first
ask**. This is not a fault: the step that would pay for that ask has already run. You have three ways to handle it:

- Start the work a tick late. The Linear Servo Drive stands still for a tick, then goes on.
- Ask in the tick before you need the energy.
- Keep asking a little while ready.

Once asks continue, the buffer stays ahead of them.

When asks stop, the draw falls within two ticks: the first step after the last ask is planned on half of it. A machine
loaded from a save runs on at its first step if its line is in range, with no power-up hold.

## A producer

A producer feeds the line. The owner gives energy, and the machine feeds it in from the next tick:

```java src/test/java/mods/eln/api/docs/Dynamo.java#dynamo
public static final MachineSpec SPEC = MachineSpec.producer(Tier.V200).ratedWatts(1500).build();

/** Energy left in the fuel, J. */
private double fuelJoules;

public void addFuel(double joules) {
    fuelJoules += joules;
}

/** One server tick: offer what the fuel holds; giveJoules accepts what fits in the machine's queue. */
public void tick(Machine m) {
    fuelJoules -= m.giveJoules(Math.min(fuelJoules, m.bufferCapacityJoules()));
}
```

The output is a source behind a droop resistance:

- 1.05 x nominal volts with no load.
- Rated watts at nominal volts.
- Current-limited at rated watts / nominal volts, so a short circuit is not a disaster.

About 5% of the energy is lost in the droop at rated power. `giveJoules` accepts up to the queue's room (two ticks of
rated output) and returns what it took; keep the rest. Only a trip stops a producer's output.

## Signals and the bus

```java src/test/java/mods/eln/api/docs/Gate.java#spec
public static final MachineSpec SPEC = MachineSpec.consumer(Tier.V50).ratedWatts(200)
    .signalIn("enable", RelativeFace.LEFT, 1)    // reads 1 while no cable is connected there
    .signalOut("busy", RelativeFace.RIGHT)       // floats (high impedance) until first set
    .signalIn("speed", RelativeFace.FRONT, CablePosition.FLOOR, 1)
    .bus(RelativeFace.UP)
    .power(RelativeFace.BACK)
    .build();
```

```java src/test/java/mods/eln/api/docs/Gate.java#tick
public static void tick(Machine m) {
    double got = 0;
    if (m.signal("enable") >= 0.5)
        got = m.takeJoules(m.ratedJoulesPerTick() * m.signal("speed"));
    if (got > 0) m.setSignal("busy", 1);
    else m.releaseSignal("busy");                // let the wire float again
    m.bus().write(BUSY_CHANNEL, got > 0 ? 1 : 0); // a bus write lasts one tick: write every tick
}
```

**Inputs:**

- `signal(name)` reads the input as 0..1.
- With no cable connected, an input reads its default.
- `signalConnected(name)` tells a wired input from an unwired one, for "a wire overrides the buttons".

**Outputs:**

- An output floats (high impedance) until it is first set. `releaseSignal` lets it float again.
- `CablePosition.FLOOR` or `CEILING` puts a signal on one cable position of a side face, so two signals can share a
  face.

**The bus:**

- `bus(face)` gives the machine a field bus port that links to a voxel bus cable.
- It has 16 channels, numbered like the dyes (0 is white), with levels 0..1.
- What you write in a tick is read in the next, and a write lasts one tick.
- With several writers on one channel, the highest wins.

## Heat

```java src/test/java/mods/eln/api/docs/Kiln.java#kiln
public static final MachineSpec SPEC = MachineSpec.consumer(Tier.V200).ratedWatts(4000)
    .heatCapacityJPerK(20000)      // J/K: with the loss, a time constant of 20000 / 50 = 400 s
    .heatLossWPerK(50)             // W/K to the ambient where it stands
    .overheatAbsoluteCelsius(300)  // trips OVERHEAT_TRIPPED at 300 C, whatever its ambient; resets 10 K under that
    .build();

public static void tick(Machine m) {
    double got = m.takeJoules(m.ratedJoulesPerTick());
    m.heatJoules(0.3 * got);       // 30 % of its work ends up as its own heat
}
```

The heat model is first order, and its settings are named:

- Heat capacity (J/K) and loss to ambient (W/K). Their ratio is the time constant.
- The trip point, given once, as one of two kinds (you choose; neither is the default):
  - absolute, `overheatAbsoluteCelsius` or `overheatAbsoluteKelvin`: the temperature at which it trips, whatever its
    ambient. A machine rated for 300 C trips at 300 C, in a cool place or in the Nether.
  - relative, `overheatRiseCelsius` or `overheatRiseKelvin` (the same number: a rise of 1 C is 1 K): how far over its
    ambient it trips, so a machine that runs in a cool place runs in a hot one too.
- `overheatResetKelvin`, how far under the trip point the trip resets after its cooldown. The default is 10 K, or halfway
  down to ambient if that is nearer.

Heat comes from three places:

- A consumer's idle watts.
- Delivered energy the full buffer couldn't hold.
- `heatJoules`: your own losses.

The model cools to the ambient where the machine stands, which EA sets by biome and depth. `celsius()` reads the
machine's temperature in degrees C. The kiln above trips at 300 C anywhere. With `overheatRiseKelvin(280)` instead it
would trip at 300 C at 20 C ambient, and at 370 C in the Nether (90 C). An ambient at or over an absolute trip point keeps
the machine tripped until it cools.

## States, trips and the energy books

| State | Meaning |
|---|---|
| `RUNNING` | The line is in range. Takes are served from the buffer. |
| `UNPOWERED` | No line (under 2% of nominal), or the line came back less than half a second ago. |
| `UNDERVOLTAGE` | `Brownout.STOP` only: the line is under the floor. The machine draws idle watts only and takes return 0. It runs again once the line has held above the recovery threshold for half a second, so a line that sags under the machine's own load doesn't flip it every tick. |
| `OVERVOLTAGE_TRIPPED` | The line went over 1.3 x nominal. It resets after the cooldown spent below 1.1 x nominal, or never if the spec burns (see [Burning](#burning)). |
| `OVERHEAT_TRIPPED` | The machine went over its trip point (the absolute one, or its ambient plus its rise). It resets after the cooldown spent under the reset point. |
| `DETACHED` | The machine is in no circuit: its block entity went, or its type was never registered. |

`Brownout.SLOW` has no floor. The machine keeps running on whatever arrives, with its draw capped at
rated x (U/U_nom)^2. Alone on a line weaker than it, it never pulls past the line's maximum power, so it can't collapse
its own line. It learns the line from its own steps, and doesn't know its neighbours. Several `SLOW` machines on a weak
line share it as resistors at their ratings would, in proportion to their ratings. A machine heavier than the line takes
the most it can with the others drawing. If the machines are each lighter than the line but heavier together, they settle
at their rated resistances, past the line's maximum power, as EA's resistor machines always did. None draws less than it
would as such a resistor.
Choose `SLOW` for loads that can run slower; `STOP` is the honest default.

`onTrip(listener)` is called once per trip, on the server thread, with a `TripCause`. The cause gives the measured
value, the limit, and whether the machine burns.

The energy books are exact at every tick:

- A consumer: `joulesDelivered() == joulesTaken() + bufferJoules() + joulesLost()`.
- A producer: `joulesGiven() == joulesDelivered() + bufferJoules() + joulesLost()`.

`respec(spec)` swaps in other ratings while the machine runs, for upgrades. The faces, signals and burn must stay the
same.

### Burning

By default overvoltage only trips a machine. A spec can make it burn instead. EA can't burn another mod's block in place,
so you choose what a burn does to your block (Baughn: "a callback. Let the API user choose how to handle it"):

```java src/test/java/mods/eln/api/docs/Press.java#burn
public static final MachineSpec SPEC = MachineSpec.consumer(Tier.V200).ratedWatts(3000)
    .burnsOnOvervoltage(Press::burn)   // overvoltage burns it: it never resets
    .build();

/** Once, on the server thread: the press breaks and drops itself. Left standing, it would stay tripped. */
static void burn(BurnHandler.Burn burn) {
    burn.level().destroyBlock(burn.pos(), true);
}
```

- The handler gets the level, position, block entity and `TripCause` (with `burns()` true). It is called once, at the
  end of the simulator's tick, where changing the world is safe, and only if the block entity is still there.
- Whatever the handler does, the machine stays `OVERVOLTAGE_TRIPPED` for good: it draws nothing and its takes return 0.
  The burn is saved, so a reload doesn't bring it back or call the handler again. Only a new block entity starts afresh.
- A handler that does nothing is allowed: the block stays, dead, as a broken machine would.
- A handler that throws is logged; the machine stays burnt.
- There is no burn without a handler: a spec can't burn your block with nothing to say what that does.
- EA's own elements pass EA's burn-out handler, which only they accept. They burn out EA's way until repaired.
- On a bench there is no world, so a burn only latches the trip.

## Reading a machine: views, tooltips, meters

`MachineView` is read-only, so it is safe to hand to other code. On the client, `ElnApi.machines().of(be)` is a synced
view: the state is synced at once, the numbers at most once a second. The `eln:machine` block capability answers for
hosted machines and for EA's own devices on the API:

```java src/test/java/mods/eln/api/docs/MillBlockEntity.java#client
/** Anywhere, server or client: the machine's view at a block, or null if there is none. */
public static MachineView viewAt(Level level, BlockPos pos) {
    return level.getCapability(ElnApi.MACHINE, pos, null);
}
```

With Jade installed, a machine at your block shows EA's lines without any code of yours: its voltage, power, state and
buffer (and temperature, with a heat model), read on the server. If your mod shows its own, turn EA's off in the spec
with `.jadeLines(false)`.

`Meters` formats readings as EA's meters, Jade lines and tooltips do: three significant digits with an SI prefix.
It has `volts`, `amps`, `watts`, `joules`, `ohms`, `kelvin` and `celsius`.

```java src/test/java/mods/eln/api/docs/Mill.java#tooltip
/** Tooltip lines for any machine's view (MachineView: read-only, also on the client). */
public static List<String> tooltip(MachineView v) {
    String line = Meters.volts(v.volts()) + "  " + Meters.watts(v.watts());
    String state = v.state().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    return v.spec().thermal().isPresent() ? List.of(line, state, Meters.celsius(v.celsius())) : List.of(line, state);
}
```

## Machines inside EA itself

Everything above is for a machine on your own block entity: that is the only kind another mod can make. EA's own
devices can be machines too (the Linear Servo Drive is one). They are written inside EA, on EA's element classes, with
`mods.eln.api.element`, which is not in the eln-api jar. This section is for EA's developers; you can skip it.

The two kinds compared:

| | A machine on your block entity | A machine on an EA element |
|---|---|---|
| Made by | `ElnApi.machines().register(type, be -> spec)` at common setup | `Machines.forElement(this, spec)` in the element's constructor (`mods.eln.api.element`, which is EA-internal and not in the eln-api jar) |
| Handle | `ElnApi.machines().of(be)`, a `Machine` | the returned `ElementMachine` |
| Node | EA's own unsaved node, tied to the block entity | the element's node |
| Faces | each face is a leaf terminal; the first live one draws | one power terminal on every power face (the element may pass power through) |
| Saved | the `eln:machine` data attachment | the element's NBT, under `api.machine.*` |
| Burn | the spec's burn handler; the machine stays tripped (see [Burning](#burning)) | EA's own burn-out and repair |
| Bus face | linked to a voxel bus cable | not supported (transparent nodes have no bus port) |

An EA element delegates its faces and Jade lines to the machine. This sample is the only one on the page that names EA's
own types:

```java src/test/java/mods/eln/api/docs/HeaterElement.java#element
static final MachineSpec SPEC = MachineSpec.consumer(Tier.V50).ratedWatts(500).build();

final ElementMachine machine;

public HeaterElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
    super(node, descriptor);
    // in the constructor: its parts must be in the element's lists before readFromNBT
    machine = Machines.forElement(this, SPEC);
    slowProcessList.add(dt -> machine.takeJoules(machine.ratedJoulesPerTick())); // the work, after the machine's books
}

@Override
public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
    return machine.electricalLoad(side, lrdu);
}

@Override
public int getConnectionMask(Direction side, LRDU lrdu) {
    return machine.connectionMask(side, lrdu);
}

@Override
public Map<String, String> getWaila() {
    Map<String, String> info = super.getWaila();
    info.putAll(machine.wailaLines());
    return info;
}
```

## Testing

`mods.eln.api.testkit` runs machines in EA's real simulator from JUnit, with no world. You compile your tests against
the eln-api and eln-api-testkit jars (`testCompileOnly`), and EA's code does the work, so its mod jar goes on the
tests' runtime classpath (`testRuntimeOnly`). EA's jar nests two libraries that the tests then need as well:
`org.apache.commons:commons-math3:3.6.1` and `org.jetbrains.kotlin:kotlin-stdlib`. EA's build compiles this page's
tests against the jars alone and runs them with EA's classes. Nobody has tried this setup in another mod's build yet.

`verifySpec` runs a canned property suite on a spec:

```java src/test/java/mods/eln/api/docs/MillTest.java#verify
@Test
void millSpecKeepsTheContract() {
    ElnTestKit.verifySpec(Mill.SPEC);   // throws an AssertionError with a readable report if not
}
```

Each check runs on a fresh bench with a standard owner, which asks a tick's rated energy every tick or keeps a
producer's queue full. The checks are:

- **A voltage sweep from 0 to 2 x nominal:** the state at each point and the work the machine gets.
- **Rated power at nominal volts.**
- **Step loads:** the rise a tick after the ask, the fall within two ticks, and only idle draw in between.
- **`tryTakeJoules`:** all or nothing.
- **Brownout:** `STOP` stops and recovers without chatter; `SLOW` runs slower.
- **A weak line.**
- **Overvoltage:** the trip and its cooldown, or the burn.
- **Overheat and its reset,** with a heat model, at 20 C and at 90 C ambient (the Nether). A model too slow to cool in
  an hour of game time is skipped, with the reason.
- **For producers:** the droop, the current limit, and output paced by what is given.

On every tick of every check, the books must balance exactly, the buffer must stay in bounds, and a consumer's draw
must stay under rated x (U/U_nom)^2, agreeing with the circuit's current. A failure throws an `AssertionError` whose
message is the report, with the end of the failed check's bench trace.

A bench is a supply behind its resistance, a line, and machines on it, stepped tick by tick in the game's order:

```java src/test/java/mods/eln/api/docs/MillTest.java#bench
@Test
void millGrindsAboutThreeItemsInFiveSecondsAtNominal() {
    Bench bench = ElnTestKit.bench().supply(200, 0.05);   // 200 V behind 0.05 ohm
    Mill.Work work = new Mill.Work();
    int[] items = {0};
    BenchMachine mill = bench.add(Mill.SPEC).owner(m -> {
        if (work.tick(m, true)) items[0]++;
    });
    bench.ticks(100);                                     // 5 s, in the game's tick order
    assertEquals(MachineState.RUNNING, mill.state());
    assertTrue(items[0] >= 2 && items[0] <= 3, bench.trace()); // 99 J a tick from the second ask on
}
```

`bench.trace()` is one line per tick: the volts, then each machine's amps, buffer and state. `BenchMachine.input`
drives signal inputs and `output` reads outputs. `bench.load(ohms)` gives a producer something to feed.

**The debug trace.** Start the server with `-Deln.api.debug=true` to log every machine and circuit host, connect,
disconnect, port link change and trip, with its volts and ohms (logger `eln.api`). It is off by default.

## The low level

`mods.eln.api.lowlevel` is EA's circuit parts, for the few devices the Machine API can't express. **You own the
consequences:**

- Nothing has ratings, a buffer, brownout or overvoltage protection unless you build it.
- A resistor you set to 0.01 ohm on a 3.2 kV line draws 1 GW, and EA will solve it.
- A load on two faces passes power through your block, and the line is cut when your block entity unloads.
- A circuit saves nothing. Build it again each time your block entity loads.

The API still keeps some things safe:

- Calls off the server thread throw.
- Values are checked.
- Every voltage source has an internal resistance. An ideal one makes two sources in parallel unsolvable.
- Parts of one circuit can't be wired into another.
- A circuit's parts join and leave the simulation together, with the block entity the circuit is hosted at.

```java src/test/java/mods/eln/api/docs/LoadBank.java#build
public final Circuit circuit;
public final Load in, probe;
public final ResistorLoad bank;
public final Watchdog guard;

/** The bank's parts; probeAt is null for none (a JUnit bench has no world to reach into). */
public LoadBank(BlockPos probeAt) {
    circuit = Circuit.create("load bank");
    in = circuit.load("in");
    bank = circuit.resistorToGround("bank", in, 100);         // 400 W at 200 V
    guard = circuit.watchdogs().volts(in, -10, 250)
        .onTrip(trip -> bank.setOhms(Double.POSITIVE_INFINITY)); // a trip only calls you: open the bank
    probe = circuit.load("probe");
    circuit.resistorToGround("probe bleed", probe, 1e6);        // a voltmeter's resistance
    if (probeAt != null) link = circuit.connect(probe, PortRef.of(probeAt, Direction.UP, PortKind.POWER));
}
```

The circuit's parts are:

- **`Load`:** a node.
- **`SignalLoad`:** a signal node, read as a level 0..1.
- **`SignalSource`:** a signal node with a gate output that floats until set.
- **`ResistorLoad`:** a resistor between two loads, or `resistorToGround`. `setOhms(Double.POSITIVE_INFINITY)` opens
  it.
- **`VoltageSource`:** volts behind ohms (more than 0), against ground.
- **`Watchdogs`:** limits on volts, amps or watts, checked after every step. A watchdog trips after its hold time
  (0.25 s by default, so a switching transient doesn't trip it). It calls its listeners once and stays tripped until
  `reset()`. A trip does nothing else; what it means is your listener's to do.

Host the circuit at your block entity. It joins at the next tick, so this is safe from `onLoad`:

```java src/test/java/mods/eln/api/docs/LoadBankBlockEntity.java#host
@Override
public void onLoad() {
    super.onLoad();
    if (!(getLevel() instanceof ServerLevel)) return;  // circuits are the server's
    bank = new LoadBank(worldPosition.below(3));      // a fresh circuit each load: circuits save nothing
    bank.circuit.hostAt(this, new FaceConnection(RelativeFace.BACK, bank.in));
}
```

Hosting works like this:

- Each `FaceConnection(face, load)` offers its load to cables at every cable position of that face. A `SignalLoad`
  takes signal cables; any other load takes power cables.
- A block entity hosts one circuit, and a machine's block entity can't host one.
- When the block entity goes, the circuit leaves the simulation for good, and the new block entity builds a new one.

**Ports.** `circuit.connect(load, port)` links a load to an existing EA port as a cable would. A `PortRef` names the
port by:

- the EA block's position;
- its face: the face a cable from the neighbour on that side would meet;
- the cable position on that face (`PortPosition`: EA's left, right, up and down of the face). `ANY` takes the first
  one with a port of that kind.
- its kind (`POWER` or `SIGNAL`).

Save a `PortRef` with `PortRef.CODEC`, which has fixed keys, under a key of your own. The link is checked before every
step. If the port's block changed, the link is dropped before the step and the port is resolved again, so a cable that
is broken and placed back links again by itself. `status()` says why a link isn't connected; nothing is swallowed.

| `PortStatus` | Meaning |
|---|---|
| `CONNECTED` | Current flows. |
| `NOT_HOSTED` | The circuit isn't in a world yet (or it is on a bench). |
| `NOT_LOADED` | No EA block, and its chunk isn't loaded. |
| `NO_NODE` | No EA block there. |
| `NOT_SIMULATED` | The block is there but out of the simulation. |
| `NO_PORT` | The block has no port at that face and position, or it burnt out. |
| `WRONG_KIND` | The port is of the other kind. |
| `REMOVED` | The link was removed, or the circuit left the world. |

```java src/test/java/mods/eln/api/docs/LoadBank.java#reading
/** The probe's reading, or why there is none (PortStatus: NO_NODE, NOT_LOADED, WRONG_KIND...). */
public String probeReading() {
    if (link == null) return "no probe";
    if (link.status() != PortStatus.CONNECTED) return link.status().name();
    return mods.eln.api.Meters.volts(probe.volts());
}
```

Some things are left out of the low level on purpose:

- Part NBT: the parts hold nothing worth saving.
- A field bus: EA's bus isn't electrical. Use the Machine API's `bus(face)`.
- Two-terminal sources, capacitors, inductors and thermal parts.

On a bench, `bench.attach(circuit)` and `bench.connect(circuit, load)` run a circuit in JUnit.
