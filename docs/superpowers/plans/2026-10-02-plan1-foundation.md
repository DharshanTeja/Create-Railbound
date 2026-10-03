# Railbound Plan 1: Foundation and First Build — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A NeoForge 1.21.1 mod that builds, loads alongside Create, reads trainset designs from datapacks (skipping invalid ones), syncs them to clients, and shows one trainset item per design in a creative tab.

**Architecture:** Designs are plain Java records decoded with Mojang codecs from `data/<ns>/railbound/trainsets/*.json` by a server reload listener. A pure `DesignLoader` + `DesignValidator` decide what is valid; a static `TrainsetDesigns` holder stores the result; a payload syncs it to clients. One `trainset` item carries a design ID in a data component.

**Tech Stack:** Java 21, NeoForge 21.1.219 via ModDevGradle 2.0.141, Gradle 8.14.3 wrapper, Create 6.0.10-280 (slim) + Ponder 1.0.82 + Flywheel 1.0.6 + Registrate MC1.21-1.3.0+67, JUnit 5 through MDG's unit-test support.

**Spec:** `docs/superpowers/specs/2026-10-02-trainsets-design.md` (this plan implements §2, §7 and the item/creative-tab parts of §4; later plans cover the rest — see "Plan sequence" at the end).

## Global Constraints

- **The Gradle project root is `D:\Games\Create-Railbound\Mod`.** Every file path and command in this plan is relative to `Mod/` (run `gradlew.bat` from there). `docs/`, `art/` and `Create-Source/` stay at the repository top level, i.e. `..\` from `Mod/`.
- Minecraft `1.21.1`, NeoForge only, Java 21 toolchain.
- Create is required: mods.toml `versionRange="[6.0.10,6.1.0)"`; build uses `create_version = 6.0.10-280`.
- Mod ID `railbound`; base package `dev.railbound`; display name `Create: Railbound`.
- Design files live at `data/<namespace>/railbound/trainsets/<id>.json`.
- Every carriage design has **exactly 2 bogeys** and **exactly 1 anchor**; width is 1 or 3.
- A broken design file must never crash the game — it is skipped with a log line naming the file and the reason.
- License placeholder `All Rights Reserved` until the user chooses one.
- **No git in this plan.** The project stays local until the first successful build (user preference). Each task ends with a "Checkpoint" instead of a commit. Task 8 asks the user about `git init`.
- Never modify anything under `Create-Source/` (read-only reference repos).

## Review Focus

1. **Syntactically broken or non-object JSON in a design file** → that file is skipped with a log line; all other designs still load. (Test: Task 5 `nonObjectJsonIsSkippedNotThrown`; Minecraft's JSON scanner already skips unparseable files.)
2. **An item stack whose design no longer exists** (datapack removed) → item name shows "Unknown Trainset (id)", tooltip is empty, nothing crashes. (Test: Task 7 `unknownDesignShowsUnknownName`.)
3. **Layout typos** (missing palette character, wrong row width, wrong layer count) → design rejected with a message naming the layer and row. (Tests: Task 4.)
4. **Wrong bogey or anchor counts** (1 or 3 bogeys, 0 or 2 anchors) → design rejected. (Tests: Task 4.)
5. **No designs at all** (all packs removed) → empty sync payload round-trips and the creative tab is simply empty. (Test: Task 6 `emptyMapRoundTrips`.)

---

## File structure (created by this plan)

```
D:\Games\Create-Railbound\Mod\
  settings.gradle                     project + plugin repositories
  build.gradle                        MDG, Create deps, unit tests, metadata task
  gradle.properties                   all versions in one place
  gradlew, gradlew.bat, gradle/wrapper/*   copied from Create-Source/Create
  src/main/templates/META-INF/neoforge.mods.toml
  src/main/java/dev/railbound/
    Railbound.java                    @Mod entry point, rl() helper, event wiring
    registry/RailboundComponents.java data component: trainset design ID
    registry/RailboundItems.java      the trainset item
    registry/RailboundTabs.java       creative tab
    trainset/design/                  pure data: records, enums, codecs
      TrainsetCategory.java  PowerType.java  CarriageSize.java  BogeySpec.java
      DoorSpec.java  CargoSpec.java  LayoutSpec.java  TrainsetDesign.java
      PartType.java  FrameShape.java  HiddenPart.java  LayoutCell.java
      LayoutParser.java  DesignValidator.java
    trainset/load/
      DesignLoader.java               JSON map -> valid designs + error list (pure)
      TrainsetDesigns.java            static holder of current designs
      TrainsetDesignManager.java      server reload listener
    trainset/item/
      TrainsetItem.java               item with design component
      TrainsetNames.java              name + tooltip text (pure)
    network/
      SyncTrainsetDesignsPayload.java
      RailboundNetwork.java           payload registration + datapack sync
  src/main/resources/
    data/railbound/railbound/trainsets/coach_standard.json   sample design
    assets/railbound/lang/en_us.json
    assets/railbound/models/item/trainset.json
  src/test/java/dev/railbound/
    testutil/TestDesigns.java
    trainset/design/TrainsetDesignCodecTest.java
    trainset/design/LayoutParserTest.java
    trainset/design/DesignValidatorTest.java
    trainset/load/DesignLoaderTest.java
    trainset/load/TrainsetDesignsTest.java
    network/SyncTrainsetDesignsPayloadTest.java
    trainset/item/TrainsetNamesTest.java
```

---

### Task 1: Gradle project skeleton and first launch

**Files:**
- Create: `settings.gradle`, `build.gradle`, `gradle.properties`
- Create (copy): `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`
- Create: `C:\Users\dhars\.gradle\gradle.properties` (user-level, machine-specific)
- Create: `src/main/templates/META-INF/neoforge.mods.toml`
- Create: `src/main/java/dev/railbound/Railbound.java`
- Create: `src/main/resources/assets/railbound/lang/en_us.json`

**Interfaces:**
- Produces: `Railbound.MOD_ID` (`"railbound"`), `Railbound.LOGGER` (`org.slf4j.Logger`), `Railbound.rl(String path)` → `ResourceLocation` in the `railbound` namespace. Later tasks add lines to the `Railbound` constructor.

- [ ] **Step 1: Copy the Gradle wrapper from Create's repo**

Run (PowerShell, from `D:\Games\Create-Railbound\Mod`):
```powershell
Copy-Item ..\Create-Source\Create\gradlew, ..\Create-Source\Create\gradlew.bat -Destination .
New-Item -ItemType Directory -Force gradle\wrapper | Out-Null
Copy-Item ..\Create-Source\Create\gradle\wrapper\gradle-wrapper.jar, ..\Create-Source\Create\gradle\wrapper\gradle-wrapper.properties -Destination gradle\wrapper
```
Expected: `gradle\wrapper\gradle-wrapper.properties` contains `gradle-8.14.3-bin.zip`.

- [ ] **Step 2: Point Gradle at JDK 21 for this machine**

Gradle 8.14.3 cannot run on the JDK 25 that is first on PATH. Create `C:\Users\dhars\.gradle\gradle.properties` (create the folder if missing) containing exactly:
```properties
org.gradle.java.home=C:/Program Files/Java/jdk-21.0.12
```
In IntelliJ: Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JVM = `jdk-21.0.12`.

- [ ] **Step 3: Write `settings.gradle`**

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = 'https://maven.neoforged.net/releases' }
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.9.0'
}

rootProject.name = 'create-railbound'
```

- [ ] **Step 4: Write `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx3G
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true

minecraft_version=1.21.1
minecraft_version_range=[1.21.1,1.21.2)
neo_version=21.1.219
parchment_minecraft_version=1.21.1
parchment_version=2024.11.17

create_version=6.0.10-280
ponder_version=1.0.82
flywheel_version=1.0.6
registrate_version=MC1.21-1.3.0+67

mod_id=railbound
mod_name=Create: Railbound
mod_license=All Rights Reserved
mod_version=0.1.0
mod_group_id=dev.railbound
mod_authors=dharshantejamsk
mod_description=Ready-made trainsets for Create.
```

- [ ] **Step 5: Write `build.gradle`**

```groovy
plugins {
    id 'java-library'
    id 'idea'
    id 'net.neoforged.moddev' version '2.0.141'
}

version = mod_version
group = mod_group_id

base {
    archivesName = mod_id
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

neoForge {
    version = project.neo_version

    parchment {
        mappingsVersion = project.parchment_version
        minecraftVersion = project.parchment_minecraft_version
    }

    runs {
        configureEach {
            systemProperty 'forge.logging.markers', 'REGISTRIES'
            systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
            logLevel = org.slf4j.event.Level.DEBUG
        }
        client {
            client()
        }
        server {
            server()
            programArgument '--nogui'
        }
        gameTestServer {
            type = 'gameTestServer'
        }
    }

    mods {
        "${mod_id}" {
            sourceSet(sourceSets.main)
        }
    }

    unitTest {
        enable()
        testedMod = mods.railbound
    }
}

repositories {
    maven { url = 'https://maven.createmod.net' }                                  // Create, Ponder, Flywheel
    maven { url = 'https://maven.ithundxr.dev/snapshots' }                         // Registrate
    maven { url = 'https://raw.githubusercontent.com/Fuzss/modresources/main/maven' } // config api port used by Ponder
}

dependencies {
    implementation("com.simibubi.create:create-${minecraft_version}:${create_version}:slim") { transitive = false }
    implementation("net.createmod.ponder:ponder-neoforge:${ponder_version}+mc${minecraft_version}")
    compileOnly("dev.engine-room.flywheel:flywheel-neoforge-api-${minecraft_version}:${flywheel_version}")
    runtimeOnly("dev.engine-room.flywheel:flywheel-neoforge-${minecraft_version}:${flywheel_version}")
    implementation("com.tterrag.registrate:Registrate:${registrate_version}")

    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

test {
    useJUnitPlatform()
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

var generateModMetadata = tasks.register('generateModMetadata', ProcessResources) {
    var replaceProperties = [
            minecraft_version_range: minecraft_version_range,
            neo_version            : neo_version,
            mod_id                 : mod_id,
            mod_name               : mod_name,
            mod_license            : mod_license,
            mod_version            : mod_version,
            mod_authors            : mod_authors,
            mod_description        : mod_description,
    ]
    inputs.properties replaceProperties
    expand replaceProperties
    from 'src/main/templates'
    into 'build/generated/sources/modMetadata'
}
sourceSets.main.resources.srcDir generateModMetadata
neoForge.ideSyncTask generateModMetadata
```

- [ ] **Step 6: Write `src/main/templates/META-INF/neoforge.mods.toml`**

```toml
modLoader="javafml"
loaderVersion="[4,)"
license="${mod_license}"

[[mods]]
modId="${mod_id}"
version="${mod_version}"
displayName="${mod_name}"
authors="${mod_authors}"
description='''${mod_description}'''

[[dependencies.${mod_id}]]
    modId="neoforge"
    type="required"
    versionRange="[${neo_version},)"
    ordering="NONE"
    side="BOTH"

[[dependencies.${mod_id}]]
    modId="minecraft"
    type="required"
    versionRange="${minecraft_version_range}"
    ordering="NONE"
    side="BOTH"

[[dependencies.${mod_id}]]
    modId="create"
    type="required"
    versionRange="[6.0.10,6.1.0)"
    ordering="AFTER"
    side="BOTH"
```

- [ ] **Step 7: Write `src/main/java/dev/railbound/Railbound.java`**

```java
package dev.railbound;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Railbound.MOD_ID)
public final class Railbound {
    public static final String MOD_ID = "railbound";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Railbound(IEventBus modBus, ModContainer container) {
        LOGGER.info("Create: Railbound loading");
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
```

- [ ] **Step 8: Write `src/main/resources/assets/railbound/lang/en_us.json`**

```json
{
  "itemGroup.railbound": "Railbound"
}
```

- [ ] **Step 9: Build**

Run: `.\gradlew.bat build`
Expected: `BUILD SUCCESSFUL`; `build\libs\railbound-0.1.0.jar` exists. (First run downloads Minecraft, NeoForge and Create — several minutes.)

- [ ] **Step 10: Launch the dev client**

Run: `.\gradlew.bat runClient`
Expected: game reaches the title screen; the log contains `Create: Railbound loading`; **Mods** menu lists both *Create* and *Create: Railbound*. Close the game.

- [ ] **Step 11: Checkpoint** — no commit (project stays local until Task 8).

---

### Task 2: Design records, codecs and the sample coach

**Files:**
- Create: `src/main/java/dev/railbound/trainset/design/{TrainsetCategory,PowerType,CarriageSize,BogeySpec,DoorSpec,CargoSpec,LayoutSpec,TrainsetDesign}.java`
- Create: `src/main/resources/data/railbound/railbound/trainsets/coach_standard.json`
- Create: `src/test/java/dev/railbound/testutil/TestDesigns.java`
- Test: `src/test/java/dev/railbound/trainset/design/TrainsetDesignCodecTest.java`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces (all in `dev.railbound.trainset.design`):
  - `enum TrainsetCategory implements StringRepresentable` — `PASSENGER("passenger")`, `BOX_CAR("box_car")`, `TANK_CAR("tank_car")`, `LOCOMOTIVE("locomotive")`, `MULTIPLE_UNIT("multiple_unit")`; `static Codec<TrainsetCategory> CODEC`.
  - `enum PowerType implements StringRepresentable` — `NONE, STEAM, DIESEL, ELECTRIC` (ids lowercase); `CODEC`.
  - `record CarriageSize(int length, int width, int height)`; `CODEC`.
  - `record BogeySpec(int z, ResourceLocation style)`; `CODEC` (`style` defaults to `create:standard`).
  - `record DoorSpec(String part, BlockPos pos)`; `CODEC`.
  - `record CargoSpec(int itemSlots, int fluidMb)`; JSON keys `item_slots`, `fluid_mb`; `static CargoSpec NONE`; `CODEC`.
  - `record LayoutSpec(Map<String, String> palette, List<List<String>> layers)`; `CODEC`.
  - `record TrainsetDesign(String name, TrainsetCategory category, CarriageSize size, List<BogeySpec> bogeys, PowerType power, CargoSpec cargo, List<DoorSpec> doors, LayoutSpec layout)`; `static Codec<TrainsetDesign> CODEC`.
  - Test helper `dev.railbound.testutil.TestDesigns` with `sampleJson()`, `sample()`, `parse(String json)`, `withBogeys`, `withSize`, `withLayout`, `withDoors`, `withName`.

- [ ] **Step 1: Write the sample design `src/main/resources/data/railbound/railbound/trainsets/coach_standard.json`**

Layout key: layer index = height (0 = floor level), row index = position along the carriage (0 = front), character index = across (0 = left, x = −1).

```json
{
  "name": "trainset.railbound.coach_standard",
  "category": "passenger",
  "size": { "length": 16, "width": 3, "height": 3 },
  "bogeys": [
    { "z": 3, "style": "create:standard" },
    { "z": 12, "style": "create:standard" }
  ],
  "power": "none",
  "doors": [
    { "part": "door_left_front",  "pos": [-1, 0, 2] },
    { "part": "door_right_front", "pos": [1, 0, 2] },
    { "part": "door_left_rear",   "pos": [-1, 0, 13] },
    { "part": "door_right_rear",  "pos": [1, 0, 13] }
  ],
  "layout": {
    "palette": {
      "#": "frame:floor",
      "l": "frame:floor_wall_left",
      "r": "frame:floor_wall_right",
      "L": "frame:wall_left",
      "R": "frame:wall_right",
      "t": "frame:roof",
      "q": "frame:roof_wall_left",
      "p": "frame:roof_wall_right",
      "S": "seat",
      "D": "door",
      "A": "anchor",
      ".": "air"
    },
    "layers": [
      ["l#r", "l#r", "D#D", "S#S", "S#S", "S#S", "S#S", "S#S", "S#S", "S#S", "S#S", "S#S", "S#S", "D#D", "l#r", "l#r"],
      ["L.R", "L.R", "D.D", "L.R", "L.R", "L.R", "L.R", "L.R", "L.R", "L.R", "L.R", "L.R", "L.R", "D.D", "L.R", "L.R"],
      ["qtp", "qtp", "qtp", "qtp", "qtp", "qtp", "qtp", "qtp", "qAp", "qtp", "qtp", "qtp", "qtp", "qtp", "qtp", "qtp"]
    ]
  }
}
```

Each layer has exactly 16 rows: doors at rows 2 and 13 (both layers), seats at rows 3–12 (ten rows), vestibule walls at rows 0–1 and 14–15, and the anchor in the roof layer at row 8.

- [ ] **Step 2: Write the test helper `src/test/java/dev/railbound/testutil/TestDesigns.java`**

```java
package dev.railbound.testutil;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.railbound.trainset.design.BogeySpec;
import dev.railbound.trainset.design.CarriageSize;
import dev.railbound.trainset.design.DoorSpec;
import dev.railbound.trainset.design.LayoutSpec;
import dev.railbound.trainset.design.TrainsetDesign;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public final class TestDesigns {
    public static final String SAMPLE_PATH = "/data/railbound/railbound/trainsets/coach_standard.json";

    private TestDesigns() {}

    public static JsonElement sampleJson() {
        try (InputStream in = Objects.requireNonNull(TestDesigns.class.getResourceAsStream(SAMPLE_PATH), SAMPLE_PATH)) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static TrainsetDesign sample() {
        return TrainsetDesign.CODEC.parse(JsonOps.INSTANCE, sampleJson()).getOrThrow();
    }

    public static TrainsetDesign parse(String json) {
        return TrainsetDesign.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    public static TrainsetDesign withBogeys(TrainsetDesign d, List<BogeySpec> bogeys) {
        return new TrainsetDesign(d.name(), d.category(), d.size(), bogeys, d.power(), d.cargo(), d.doors(), d.layout());
    }

    public static TrainsetDesign withSize(TrainsetDesign d, CarriageSize size) {
        return new TrainsetDesign(d.name(), d.category(), size, d.bogeys(), d.power(), d.cargo(), d.doors(), d.layout());
    }

    public static TrainsetDesign withLayout(TrainsetDesign d, LayoutSpec layout) {
        return new TrainsetDesign(d.name(), d.category(), d.size(), d.bogeys(), d.power(), d.cargo(), d.doors(), layout);
    }

    public static TrainsetDesign withDoors(TrainsetDesign d, List<DoorSpec> doors) {
        return new TrainsetDesign(d.name(), d.category(), d.size(), d.bogeys(), d.power(), d.cargo(), doors, d.layout());
    }

    public static TrainsetDesign withName(TrainsetDesign d, String name) {
        return new TrainsetDesign(name, d.category(), d.size(), d.bogeys(), d.power(), d.cargo(), d.doors(), d.layout());
    }
}
```

- [ ] **Step 3: Write the failing test `src/test/java/dev/railbound/trainset/design/TrainsetDesignCodecTest.java`**

```java
package dev.railbound.trainset.design;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.railbound.testutil.TestDesigns;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrainsetDesignCodecTest {

    @Test
    void decodesSampleCoach() {
        TrainsetDesign d = TestDesigns.sample();
        assertEquals("trainset.railbound.coach_standard", d.name());
        assertEquals(TrainsetCategory.PASSENGER, d.category());
        assertEquals(new CarriageSize(16, 3, 3), d.size());
        assertEquals(List.of(
                new BogeySpec(3, ResourceLocation.fromNamespaceAndPath("create", "standard")),
                new BogeySpec(12, ResourceLocation.fromNamespaceAndPath("create", "standard"))), d.bogeys());
        assertEquals(PowerType.NONE, d.power());
        assertEquals(CargoSpec.NONE, d.cargo());
        assertEquals(new DoorSpec("door_left_front", new BlockPos(-1, 0, 2)), d.doors().get(0));
        assertEquals(3, d.layout().layers().size());
        assertEquals("frame:floor", d.layout().palette().get("#"));
    }

    @Test
    void optionalFieldsDefault() {
        TrainsetDesign d = TestDesigns.parse("""
                {"name":"n","category":"box_car","size":{"length":2,"width":1,"height":1},
                 "bogeys":[{"z":0},{"z":1}],
                 "layout":{"palette":{"A":"anchor","#":"frame:floor"},"layers":[["A","#"]]}}
                """);
        assertEquals(PowerType.NONE, d.power());
        assertEquals(CargoSpec.NONE, d.cargo());
        assertEquals(List.of(), d.doors());
        assertEquals(ResourceLocation.fromNamespaceAndPath("create", "standard"), d.bogeys().get(0).style());
    }

    @Test
    void roundTrips() {
        TrainsetDesign d = TestDesigns.sample();
        JsonElement encoded = TrainsetDesign.CODEC.encodeStart(JsonOps.INSTANCE, d).getOrThrow();
        assertEquals(d, TrainsetDesign.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
    }

    @Test
    void rejectsUnknownCategory() {
        DataResult<TrainsetDesign> r = TrainsetDesign.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"name":"n","category":"spaceship","size":{"length":2,"width":1,"height":1},
                 "bogeys":[{"z":0},{"z":1}],"layout":{"palette":{},"layers":[]}}
                """));
        assertTrue(r.error().isPresent());
    }

    @Test
    void cargoUsesSnakeCaseKeys() {
        TrainsetDesign d = TestDesigns.parse("""
                {"name":"n","category":"tank_car","size":{"length":2,"width":1,"height":1},
                 "bogeys":[{"z":0},{"z":1}],"cargo":{"item_slots":0,"fluid_mb":64000},
                 "layout":{"palette":{"A":"anchor","#":"frame:floor"},"layers":[["A","#"]]}}
                """);
        assertEquals(new CargoSpec(0, 64000), d.cargo());
    }
}
```

- [ ] **Step 4: Run the test to verify it fails**

Run: `.\gradlew.bat test --tests "dev.railbound.trainset.design.TrainsetDesignCodecTest"`
Expected: compilation FAILS — `cannot find symbol: class TrainsetDesign`.

- [ ] **Step 5: Write the enums**

`TrainsetCategory.java`:
```java
package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum TrainsetCategory implements StringRepresentable {
    PASSENGER("passenger"),
    BOX_CAR("box_car"),
    TANK_CAR("tank_car"),
    LOCOMOTIVE("locomotive"),
    MULTIPLE_UNIT("multiple_unit");

    public static final Codec<TrainsetCategory> CODEC = StringRepresentable.fromEnum(TrainsetCategory::values);

    private final String id;

    TrainsetCategory(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
```

`PowerType.java`:
```java
package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum PowerType implements StringRepresentable {
    NONE("none"),
    STEAM("steam"),
    DIESEL("diesel"),
    ELECTRIC("electric");

    public static final Codec<PowerType> CODEC = StringRepresentable.fromEnum(PowerType::values);

    private final String id;

    PowerType(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
```

- [ ] **Step 6: Write the small records**

`CarriageSize.java`:
```java
package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

public record CarriageSize(int length, int width, int height) {
    public static final Codec<CarriageSize> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.POSITIVE_INT.fieldOf("length").forGetter(CarriageSize::length),
            ExtraCodecs.POSITIVE_INT.fieldOf("width").forGetter(CarriageSize::width),
            ExtraCodecs.POSITIVE_INT.fieldOf("height").forGetter(CarriageSize::height)
    ).apply(i, CarriageSize::new));
}
```

`BogeySpec.java`:
```java
package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record BogeySpec(int z, ResourceLocation style) {
    public static final ResourceLocation DEFAULT_STYLE = ResourceLocation.fromNamespaceAndPath("create", "standard");

    public static final Codec<BogeySpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("z").forGetter(BogeySpec::z),
            ResourceLocation.CODEC.optionalFieldOf("style", DEFAULT_STYLE).forGetter(BogeySpec::style)
    ).apply(i, BogeySpec::new));
}
```

`DoorSpec.java`:
```java
package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record DoorSpec(String part, BlockPos pos) {
    public static final Codec<DoorSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("part").forGetter(DoorSpec::part),
            BlockPos.CODEC.fieldOf("pos").forGetter(DoorSpec::pos)
    ).apply(i, DoorSpec::new));
}
```

`CargoSpec.java`:
```java
package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

public record CargoSpec(int itemSlots, int fluidMb) {
    public static final CargoSpec NONE = new CargoSpec(0, 0);

    public static final Codec<CargoSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("item_slots", 0).forGetter(CargoSpec::itemSlots),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("fluid_mb", 0).forGetter(CargoSpec::fluidMb)
    ).apply(i, CargoSpec::new));
}
```

`LayoutSpec.java`:
```java
package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

public record LayoutSpec(Map<String, String> palette, List<List<String>> layers) {
    public static final Codec<LayoutSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("palette").forGetter(LayoutSpec::palette),
            Codec.STRING.listOf().listOf().fieldOf("layers").forGetter(LayoutSpec::layers)
    ).apply(i, LayoutSpec::new));
}
```

- [ ] **Step 7: Write `TrainsetDesign.java`**

```java
package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record TrainsetDesign(
        String name,
        TrainsetCategory category,
        CarriageSize size,
        List<BogeySpec> bogeys,
        PowerType power,
        CargoSpec cargo,
        List<DoorSpec> doors,
        LayoutSpec layout) {

    public static final Codec<TrainsetDesign> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(TrainsetDesign::name),
            TrainsetCategory.CODEC.fieldOf("category").forGetter(TrainsetDesign::category),
            CarriageSize.CODEC.fieldOf("size").forGetter(TrainsetDesign::size),
            BogeySpec.CODEC.listOf().fieldOf("bogeys").forGetter(TrainsetDesign::bogeys),
            PowerType.CODEC.optionalFieldOf("power", PowerType.NONE).forGetter(TrainsetDesign::power),
            CargoSpec.CODEC.optionalFieldOf("cargo", CargoSpec.NONE).forGetter(TrainsetDesign::cargo),
            DoorSpec.CODEC.listOf().optionalFieldOf("doors", List.of()).forGetter(TrainsetDesign::doors),
            LayoutSpec.CODEC.fieldOf("layout").forGetter(TrainsetDesign::layout)
    ).apply(i, TrainsetDesign::new));
}
```

- [ ] **Step 8: Run the tests to verify they pass**

Run: `.\gradlew.bat test --tests "dev.railbound.trainset.design.TrainsetDesignCodecTest"`
Expected: 5 tests PASS.

- [ ] **Step 9: Checkpoint** — no commit.

---

### Task 3: Layout vocabulary and parser

**Files:**
- Create: `src/main/java/dev/railbound/trainset/design/{PartType,FrameShape,HiddenPart,LayoutCell,LayoutParser}.java`
- Modify: `src/main/java/dev/railbound/trainset/design/TrainsetDesign.java` (add `seatCount()`)
- Test: `src/test/java/dev/railbound/trainset/design/LayoutParserTest.java`

**Interfaces:**
- Consumes: `TrainsetDesign`, `LayoutSpec`, `CarriageSize` (Task 2).
- Produces:
  - `enum PartType` — `FRAME("frame"), SEAT("seat"), DOOR("door"), ANCHOR("anchor"), CAB("cab"), CARGO_ITEM("cargo_item"), CARGO_FLUID("cargo_fluid"), AIR("air")`; `String id()`; `static Optional<PartType> byId(String)`.
  - `enum FrameShape` — `FLOOR, FLOOR_WALL_LEFT, FLOOR_WALL_RIGHT, WALL_LEFT, WALL_RIGHT, ROOF, ROOF_WALL_LEFT, ROOF_WALL_RIGHT, PARTITION, FULL` (ids lowercase); `String id()`; `static Optional<FrameShape> byId(String)`. (Collision geometry for each shape is defined in Plan 2.)
  - `record HiddenPart(PartType type, Optional<FrameShape> shape)`; `static Optional<HiddenPart> parse(String text)` — `"frame:<shape>"` or a bare non-frame type id.
  - `record LayoutCell(BlockPos pos, HiddenPart part)` — `pos.x` across (−width/2..+width/2), `pos.y` layer, `pos.z` row (0 = front).
  - `LayoutParser.parse(TrainsetDesign)` → `List<LayoutCell>` excluding `AIR`; throws `IllegalStateException` on an invalid layout (callers validate first).
  - `TrainsetDesign.SEATS_PER_BLOCK = 2`; `int seatCount()`.

- [ ] **Step 1: Write the failing test `LayoutParserTest.java`**

```java
package dev.railbound.trainset.design;

import dev.railbound.testutil.TestDesigns;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LayoutParserTest {

    @Test
    void parsesFrameWithShape() {
        assertEquals(Optional.of(new HiddenPart(PartType.FRAME, Optional.of(FrameShape.FLOOR_WALL_LEFT))),
                HiddenPart.parse("frame:floor_wall_left"));
    }

    @Test
    void parsesBareTypes() {
        assertEquals(Optional.of(new HiddenPart(PartType.SEAT, Optional.empty())), HiddenPart.parse("seat"));
        assertEquals(Optional.of(new HiddenPart(PartType.CARGO_FLUID, Optional.empty())), HiddenPart.parse("cargo_fluid"));
    }

    @Test
    void rejectsBadParts() {
        assertTrue(HiddenPart.parse("frame").isEmpty(), "frame needs a shape");
        assertTrue(HiddenPart.parse("frame:triangle").isEmpty(), "unknown shape");
        assertTrue(HiddenPart.parse("seat:floor").isEmpty(), "only frames take a shape");
        assertTrue(HiddenPart.parse("rocket").isEmpty(), "unknown type");
        assertTrue(HiddenPart.parse("frame:floor:extra").isEmpty(), "too many parts");
    }

    @Test
    void mapsCharactersToPositions() {
        List<LayoutCell> cells = LayoutParser.parse(TestDesigns.sample());
        assertTrue(cells.contains(new LayoutCell(new BlockPos(-1, 0, 3), new HiddenPart(PartType.SEAT, Optional.empty()))),
                "left seat at row 3");
        assertTrue(cells.contains(new LayoutCell(new BlockPos(1, 0, 2), new HiddenPart(PartType.DOOR, Optional.empty()))),
                "right door at row 2");
        assertTrue(cells.contains(new LayoutCell(new BlockPos(0, 2, 8), new HiddenPart(PartType.ANCHOR, Optional.empty()))),
                "anchor in the roof layer, row 8");
    }

    @Test
    void skipsAir() {
        List<LayoutCell> cells = LayoutParser.parse(TestDesigns.sample());
        assertTrue(cells.stream().noneMatch(c -> c.part().type() == PartType.AIR));
        assertTrue(cells.stream().noneMatch(c -> c.pos().equals(new BlockPos(0, 1, 5))), "aisle above floor is air");
    }

    @Test
    void sampleHasFortySeats() {
        assertEquals(40, TestDesigns.sample().seatCount());
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `.\gradlew.bat test --tests "dev.railbound.trainset.design.LayoutParserTest"`
Expected: compilation FAILS — `cannot find symbol: class HiddenPart`.

- [ ] **Step 3: Write `PartType.java` and `FrameShape.java`**

```java
package dev.railbound.trainset.design;

import java.util.Arrays;
import java.util.Optional;

public enum PartType {
    FRAME("frame"),
    SEAT("seat"),
    DOOR("door"),
    ANCHOR("anchor"),
    CAB("cab"),
    CARGO_ITEM("cargo_item"),
    CARGO_FLUID("cargo_fluid"),
    AIR("air");

    private final String id;

    PartType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<PartType> byId(String id) {
        return Arrays.stream(values()).filter(t -> t.id.equals(id)).findFirst();
    }
}
```

```java
package dev.railbound.trainset.design;

import java.util.Arrays;
import java.util.Optional;

public enum FrameShape {
    FLOOR("floor"),
    FLOOR_WALL_LEFT("floor_wall_left"),
    FLOOR_WALL_RIGHT("floor_wall_right"),
    WALL_LEFT("wall_left"),
    WALL_RIGHT("wall_right"),
    ROOF("roof"),
    ROOF_WALL_LEFT("roof_wall_left"),
    ROOF_WALL_RIGHT("roof_wall_right"),
    PARTITION("partition"),
    FULL("full");

    private final String id;

    FrameShape(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<FrameShape> byId(String id) {
        return Arrays.stream(values()).filter(s -> s.id.equals(id)).findFirst();
    }
}
```

- [ ] **Step 4: Write `HiddenPart.java` and `LayoutCell.java`**

```java
package dev.railbound.trainset.design;

import java.util.Optional;

public record HiddenPart(PartType type, Optional<FrameShape> shape) {

    public static Optional<HiddenPart> parse(String text) {
        String[] bits = text.split(":", -1);
        if (bits.length > 2) {
            return Optional.empty();
        }
        Optional<PartType> type = PartType.byId(bits[0]);
        if (type.isEmpty()) {
            return Optional.empty();
        }
        if (type.get() == PartType.FRAME) {
            if (bits.length != 2) {
                return Optional.empty();
            }
            return FrameShape.byId(bits[1]).map(shape -> new HiddenPart(PartType.FRAME, Optional.of(shape)));
        }
        return bits.length == 1 ? Optional.of(new HiddenPart(type.get(), Optional.empty())) : Optional.empty();
    }
}
```

```java
package dev.railbound.trainset.design;

import net.minecraft.core.BlockPos;

public record LayoutCell(BlockPos pos, HiddenPart part) {
}
```

- [ ] **Step 5: Write `LayoutParser.java`**

```java
package dev.railbound.trainset.design;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LayoutParser {
    private LayoutParser() {}

    /** Converts the text layout into cells. Throws IllegalStateException if the layout is invalid — validate first. */
    public static List<LayoutCell> parse(TrainsetDesign design) {
        LayoutSpec layout = design.layout();
        int half = design.size().width() / 2;

        Map<String, HiddenPart> palette = new HashMap<>();
        layout.palette().forEach((key, value) -> palette.put(key, HiddenPart.parse(value)
                .orElseThrow(() -> new IllegalStateException("Invalid palette entry '" + key + "' -> '" + value + "'"))));

        List<LayoutCell> cells = new ArrayList<>();
        for (int y = 0; y < layout.layers().size(); y++) {
            List<String> rows = layout.layers().get(y);
            for (int z = 0; z < rows.size(); z++) {
                String row = rows.get(z);
                for (int i = 0; i < row.length(); i++) {
                    String symbol = String.valueOf(row.charAt(i));
                    HiddenPart part = palette.get(symbol);
                    if (part == null) {
                        throw new IllegalStateException("Character '" + symbol + "' is not in the palette");
                    }
                    if (part.type() != PartType.AIR) {
                        cells.add(new LayoutCell(new BlockPos(i - half, y, z), part));
                    }
                }
            }
        }
        return List.copyOf(cells);
    }
}
```

- [ ] **Step 6: Add `seatCount()` to `TrainsetDesign.java`**

Inside the record body, after `CODEC`:
```java
    public static final int SEATS_PER_BLOCK = 2;

    /** Total seats; only call on a validated design. */
    public int seatCount() {
        long seatBlocks = LayoutParser.parse(this).stream()
                .filter(cell -> cell.part().type() == PartType.SEAT)
                .count();
        return (int) seatBlocks * SEATS_PER_BLOCK;
    }
```

- [ ] **Step 7: Run the tests to verify they pass**

Run: `.\gradlew.bat test --tests "dev.railbound.trainset.design.*"`
Expected: all `TrainsetDesignCodecTest` and `LayoutParserTest` tests PASS.

- [ ] **Step 8: Checkpoint** — no commit.

---

### Task 4: Design validator

**Files:**
- Create: `src/main/java/dev/railbound/trainset/design/DesignValidator.java`
- Test: `src/test/java/dev/railbound/trainset/design/DesignValidatorTest.java`

**Interfaces:**
- Consumes: `TrainsetDesign`, `HiddenPart.parse`, `LayoutParser.parse`, `PartType`, `DoorSpec`, `BogeySpec` (Tasks 2–3).
- Produces: `DesignValidator.validate(TrainsetDesign)` → `List<String>` of human-readable problems; empty list means valid.

- [ ] **Step 1: Write the failing test `DesignValidatorTest.java`**

```java
package dev.railbound.trainset.design;

import dev.railbound.testutil.TestDesigns;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DesignValidatorTest {

    private static final TrainsetDesign SAMPLE = TestDesigns.sample();

    private static void assertProblem(TrainsetDesign d, String expectedFragment) {
        List<String> problems = DesignValidator.validate(d);
        assertTrue(problems.stream().anyMatch(p -> p.contains(expectedFragment)),
                () -> "expected a problem containing '" + expectedFragment + "' but got " + problems);
    }

    @Test
    void sampleIsValid() {
        assertEquals(List.of(), DesignValidator.validate(SAMPLE));
    }

    @Test
    void rejectsOneBogey() {
        assertProblem(TestDesigns.withBogeys(SAMPLE, List.of(SAMPLE.bogeys().get(0))), "exactly 2 bogeys, found 1");
    }

    @Test
    void rejectsThreeBogeys() {
        List<BogeySpec> three = new ArrayList<>(SAMPLE.bogeys());
        three.add(new BogeySpec(8, BogeySpec.DEFAULT_STYLE));
        assertProblem(TestDesigns.withBogeys(SAMPLE, three), "exactly 2 bogeys, found 3");
    }

    @Test
    void rejectsBogeyOutsideCarriage() {
        assertProblem(TestDesigns.withBogeys(SAMPLE, List.of(
                new BogeySpec(3, BogeySpec.DEFAULT_STYLE), new BogeySpec(16, BogeySpec.DEFAULT_STYLE))), "bogey z=16");
    }

    @Test
    void rejectsBogeysAtSamePosition() {
        assertProblem(TestDesigns.withBogeys(SAMPLE, List.of(
                new BogeySpec(5, BogeySpec.DEFAULT_STYLE), new BogeySpec(5, BogeySpec.DEFAULT_STYLE))), "different positions");
    }

    @Test
    void rejectsWidthTwo() {
        assertProblem(TestDesigns.withSize(SAMPLE, new CarriageSize(16, 2, 3)), "width must be 1 or 3");
    }

    @Test
    void rejectsWrongLayerCount() {
        assertProblem(TestDesigns.withSize(SAMPLE, new CarriageSize(16, 3, 4)), "3 layers, expected height 4");
    }

    @Test
    void rejectsWrongRowCount() {
        assertProblem(TestDesigns.withSize(SAMPLE, new CarriageSize(17, 3, 3)), "layer 0 has 16 rows, expected length 17");
    }

    @Test
    void rejectsWrongRowWidth() {
        List<List<String>> layers = copyLayers();
        layers.get(1).set(4, "L..R");
        assertProblem(TestDesigns.withLayout(SAMPLE, new LayoutSpec(SAMPLE.layout().palette(), layers)),
                "layer 1 row 4 has 4 cells, expected width 3");
    }

    @Test
    void rejectsCharacterMissingFromPalette() {
        List<List<String>> layers = copyLayers();
        layers.get(0).set(5, "S#X");
        assertProblem(TestDesigns.withLayout(SAMPLE, new LayoutSpec(SAMPLE.layout().palette(), layers)),
                "layer 0 row 5: character 'X' is not in the palette");
    }

    @Test
    void rejectsUnknownPaletteValue() {
        Map<String, String> palette = new HashMap<>(SAMPLE.layout().palette());
        palette.put("S", "sofa");
        assertProblem(TestDesigns.withLayout(SAMPLE, new LayoutSpec(palette, SAMPLE.layout().layers())),
                "palette 'S': unknown part 'sofa'");
    }

    @Test
    void rejectsMultiCharacterPaletteKey() {
        Map<String, String> palette = new HashMap<>(SAMPLE.layout().palette());
        palette.put("SS", "seat");
        assertProblem(TestDesigns.withLayout(SAMPLE, new LayoutSpec(palette, SAMPLE.layout().layers())),
                "palette key 'SS' must be a single character");
    }

    @Test
    void rejectsMissingAnchor() {
        List<List<String>> layers = copyLayers();
        layers.get(2).set(8, "qtp");
        assertProblem(TestDesigns.withLayout(SAMPLE, new LayoutSpec(SAMPLE.layout().palette(), layers)),
                "exactly 1 anchor, found 0");
    }

    @Test
    void rejectsTwoAnchors() {
        List<List<String>> layers = copyLayers();
        layers.get(2).set(3, "qAp");
        assertProblem(TestDesigns.withLayout(SAMPLE, new LayoutSpec(SAMPLE.layout().palette(), layers)),
                "exactly 1 anchor, found 2");
    }

    @Test
    void rejectsDoorSpecNotOnDoorCell() {
        assertProblem(TestDesigns.withDoors(SAMPLE, List.of(new DoorSpec("door_left_front", new BlockPos(-1, 0, 5)))),
                "door 'door_left_front'");
    }

    @Test
    void rejectsBlankName() {
        assertProblem(TestDesigns.withName(SAMPLE, " "), "name must not be empty");
    }

    private static List<List<String>> copyLayers() {
        List<List<String>> layers = new ArrayList<>();
        SAMPLE.layout().layers().forEach(layer -> layers.add(new ArrayList<>(layer)));
        return layers;
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `.\gradlew.bat test --tests "dev.railbound.trainset.design.DesignValidatorTest"`
Expected: compilation FAILS — `cannot find symbol: class DesignValidator`.

- [ ] **Step 3: Write `DesignValidator.java`**

```java
package dev.railbound.trainset.design;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class DesignValidator {
    public static final int REQUIRED_BOGEYS = 2;

    private DesignValidator() {}

    public static List<String> validate(TrainsetDesign design) {
        List<String> errors = new ArrayList<>();
        CarriageSize size = design.size();

        if (design.name().isBlank()) {
            errors.add("name must not be empty");
        }
        if (size.width() != 1 && size.width() != 3) {
            errors.add("width must be 1 or 3, got " + size.width());
        }

        List<BogeySpec> bogeys = design.bogeys();
        if (bogeys.size() != REQUIRED_BOGEYS) {
            errors.add("must have exactly 2 bogeys, found " + bogeys.size());
        }
        for (BogeySpec bogey : bogeys) {
            if (bogey.z() < 0 || bogey.z() >= size.length()) {
                errors.add("bogey z=" + bogey.z() + " is outside carriage length " + size.length());
            }
        }
        if (bogeys.size() == REQUIRED_BOGEYS && bogeys.get(0).z() == bogeys.get(1).z()) {
            errors.add("bogeys must be at different positions");
        }

        List<String> layoutErrors = validateLayoutStructure(design);
        errors.addAll(layoutErrors);
        if (layoutErrors.isEmpty()) {
            validateLayoutContents(design, errors);
        }
        return errors;
    }

    private static List<String> validateLayoutStructure(TrainsetDesign design) {
        List<String> errors = new ArrayList<>();
        LayoutSpec layout = design.layout();
        CarriageSize size = design.size();

        for (Map.Entry<String, String> entry : layout.palette().entrySet()) {
            if (entry.getKey().length() != 1) {
                errors.add("palette key '" + entry.getKey() + "' must be a single character");
            } else if (HiddenPart.parse(entry.getValue()).isEmpty()) {
                errors.add("palette '" + entry.getKey() + "': unknown part '" + entry.getValue() + "'");
            }
        }

        if (layout.layers().size() != size.height()) {
            errors.add("layout has " + layout.layers().size() + " layers, expected height " + size.height());
        }
        for (int y = 0; y < layout.layers().size(); y++) {
            List<String> rows = layout.layers().get(y);
            if (rows.size() != size.length()) {
                errors.add("layer " + y + " has " + rows.size() + " rows, expected length " + size.length());
                continue;
            }
            for (int z = 0; z < rows.size(); z++) {
                String row = rows.get(z);
                if (row.length() != size.width()) {
                    errors.add("layer " + y + " row " + z + " has " + row.length() + " cells, expected width " + size.width());
                    continue;
                }
                for (int i = 0; i < row.length(); i++) {
                    String symbol = String.valueOf(row.charAt(i));
                    if (!layout.palette().containsKey(symbol)) {
                        errors.add("layer " + y + " row " + z + ": character '" + symbol + "' is not in the palette");
                    }
                }
            }
        }
        return errors;
    }

    private static void validateLayoutContents(TrainsetDesign design, List<String> errors) {
        List<LayoutCell> cells = LayoutParser.parse(design);

        long anchors = cells.stream().filter(c -> c.part().type() == PartType.ANCHOR).count();
        if (anchors != 1) {
            errors.add("must have exactly 1 anchor, found " + anchors);
        }

        Set<BlockPos> doorCells = cells.stream()
                .filter(c -> c.part().type() == PartType.DOOR)
                .map(LayoutCell::pos)
                .collect(Collectors.toSet());
        for (DoorSpec door : design.doors()) {
            if (!doorCells.contains(door.pos())) {
                errors.add("door '" + door.part() + "' at " + door.pos().toShortString() + " is not a door cell");
            }
        }
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `.\gradlew.bat test --tests "dev.railbound.trainset.design.*"`
Expected: all design tests PASS (16 in `DesignValidatorTest`).

- [ ] **Step 5: Checkpoint** — no commit.

---

### Task 5: Design loading (pure loader, holder, reload listener)

**Files:**
- Create: `src/main/java/dev/railbound/trainset/load/{DesignLoader,TrainsetDesigns,TrainsetDesignManager}.java`
- Modify: `src/main/java/dev/railbound/Railbound.java`
- Test: `src/test/java/dev/railbound/trainset/load/{DesignLoaderTest,TrainsetDesignsTest}.java`

**Interfaces:**
- Consumes: `TrainsetDesign.CODEC`, `DesignValidator.validate` (Tasks 2, 4).
- Produces:
  - `DesignLoader.load(Map<ResourceLocation, JsonElement>)` → `DesignLoader.LoadResult(Map<ResourceLocation, TrainsetDesign> designs, List<String> errors)`; errors are formatted `"<id>: <problem>"`, in ID order.
  - `TrainsetDesigns.all()` → unmodifiable, ID-sorted `Map<ResourceLocation, TrainsetDesign>`; `TrainsetDesigns.get(ResourceLocation)` → `Optional<TrainsetDesign>`; `TrainsetDesigns.replace(Map<ResourceLocation, TrainsetDesign>)`.
  - `TrainsetDesignManager` — `SimpleJsonResourceReloadListener` over directory `"railbound/trainsets"`.

- [ ] **Step 1: Write the failing tests**

`DesignLoaderTest.java`:
```java
package dev.railbound.trainset.load;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.railbound.testutil.TestDesigns;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DesignLoaderTest {

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("railbound", path);
    }

    @Test
    void loadsValidDesign() {
        DesignLoader.LoadResult result = DesignLoader.load(Map.of(id("coach_standard"), TestDesigns.sampleJson()));
        assertEquals(Set.of(id("coach_standard")), result.designs().keySet());
        assertEquals(List.of(), result.errors());
    }

    @Test
    void skipsUndecodableDesignAndKeepsOthers() {
        Map<ResourceLocation, JsonElement> input = new HashMap<>();
        input.put(id("coach_standard"), TestDesigns.sampleJson());
        input.put(id("bad_category"), JsonParser.parseString("""
                {"name":"n","category":"spaceship","size":{"length":2,"width":1,"height":1},
                 "bogeys":[{"z":0},{"z":1}],"layout":{"palette":{},"layers":[]}}"""));
        DesignLoader.LoadResult result = DesignLoader.load(input);
        assertEquals(Set.of(id("coach_standard")), result.designs().keySet());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).startsWith("railbound:bad_category: "));
    }

    @Test
    void skipsInvalidDesignWithAllProblems() {
        JsonElement oneBogey = JsonParser.parseString("""
                {"name":"n","category":"box_car","size":{"length":2,"width":1,"height":1},
                 "bogeys":[{"z":0}],
                 "layout":{"palette":{"#":"frame:floor"},"layers":[["#","#"]]}}""");
        DesignLoader.LoadResult result = DesignLoader.load(Map.of(id("broken"), oneBogey));
        assertTrue(result.designs().isEmpty());
        assertTrue(result.errors().contains("railbound:broken: must have exactly 2 bogeys, found 1"));
        assertTrue(result.errors().contains("railbound:broken: must have exactly 1 anchor, found 0"));
    }

    @Test
    void nonObjectJsonIsSkippedNotThrown() {
        DesignLoader.LoadResult result = DesignLoader.load(Map.of(id("weird"), new JsonPrimitive("not a design")));
        assertTrue(result.designs().isEmpty());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).startsWith("railbound:weird: "));
    }

    @Test
    void errorsAreInIdOrder() {
        Map<ResourceLocation, JsonElement> input = new HashMap<>();
        input.put(id("zeta"), new JsonPrimitive(1));
        input.put(id("alpha"), new JsonPrimitive(1));
        List<String> errors = DesignLoader.load(input).errors();
        assertTrue(errors.get(0).startsWith("railbound:alpha"));
        assertTrue(errors.get(1).startsWith("railbound:zeta"));
    }
}
```

`TrainsetDesignsTest.java`:
```java
package dev.railbound.trainset.load;

import dev.railbound.testutil.TestDesigns;
import dev.railbound.trainset.design.TrainsetDesign;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TrainsetDesignsTest {

    @AfterEach
    void reset() {
        TrainsetDesigns.replace(Map.of());
    }

    @Test
    void replaceAndGet() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("railbound", "coach_standard");
        TrainsetDesign sample = TestDesigns.sample();
        TrainsetDesigns.replace(Map.of(id, sample));
        assertEquals(Optional.of(sample), TrainsetDesigns.get(id));
        assertEquals(Optional.empty(), TrainsetDesigns.get(ResourceLocation.fromNamespaceAndPath("railbound", "missing")));
    }

    @Test
    void allIsSortedById() {
        Map<ResourceLocation, TrainsetDesign> input = new HashMap<>();
        input.put(ResourceLocation.fromNamespaceAndPath("railbound", "zeta"), TestDesigns.sample());
        input.put(ResourceLocation.fromNamespaceAndPath("railbound", "alpha"), TestDesigns.sample());
        TrainsetDesigns.replace(input);
        assertEquals(List.of("alpha", "zeta"),
                TrainsetDesigns.all().keySet().stream().map(ResourceLocation::getPath).toList());
    }

    @Test
    void allIsUnmodifiable() {
        TrainsetDesigns.replace(Map.of());
        assertThrows(UnsupportedOperationException.class,
                () -> TrainsetDesigns.all().put(ResourceLocation.fromNamespaceAndPath("x", "y"), TestDesigns.sample()));
    }
}
```

- [ ] **Step 2: Run them to verify they fail**

Run: `.\gradlew.bat test --tests "dev.railbound.trainset.load.*"`
Expected: compilation FAILS — `cannot find symbol: class DesignLoader`.

- [ ] **Step 3: Write `DesignLoader.java`**

```java
package dev.railbound.trainset.load;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.railbound.trainset.design.DesignValidator;
import dev.railbound.trainset.design.TrainsetDesign;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class DesignLoader {
    private DesignLoader() {}

    public record LoadResult(Map<ResourceLocation, TrainsetDesign> designs, List<String> errors) {}

    public static LoadResult load(Map<ResourceLocation, JsonElement> jsons) {
        Map<ResourceLocation, TrainsetDesign> designs = new TreeMap<>();
        List<String> errors = new ArrayList<>();

        new TreeMap<>(jsons).forEach((id, json) -> {
            DataResult<TrainsetDesign> decoded = TrainsetDesign.CODEC.parse(JsonOps.INSTANCE, json);
            Optional<TrainsetDesign> design = decoded.result();
            if (design.isEmpty()) {
                errors.add(id + ": " + decoded.error().map(DataResult.Error::message).orElse("could not be decoded"));
                return;
            }
            List<String> problems = DesignValidator.validate(design.get());
            if (!problems.isEmpty()) {
                problems.forEach(problem -> errors.add(id + ": " + problem));
                return;
            }
            designs.put(id, design.get());
        });
        return new LoadResult(designs, errors);
    }
}
```

- [ ] **Step 4: Write `TrainsetDesigns.java`**

```java
package dev.railbound.trainset.load;

import dev.railbound.trainset.design.TrainsetDesign;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** The designs currently in effect: set by the server reload listener and by the client sync payload. */
public final class TrainsetDesigns {
    private static volatile Map<ResourceLocation, TrainsetDesign> designs = Map.of();

    private TrainsetDesigns() {}

    public static Map<ResourceLocation, TrainsetDesign> all() {
        return designs;
    }

    public static Optional<TrainsetDesign> get(ResourceLocation id) {
        return Optional.ofNullable(designs.get(id));
    }

    public static void replace(Map<ResourceLocation, TrainsetDesign> newDesigns) {
        designs = Collections.unmodifiableMap(new TreeMap<>(newDesigns));
    }
}
```

- [ ] **Step 5: Write `TrainsetDesignManager.java`**

```java
package dev.railbound.trainset.load;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import dev.railbound.Railbound;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads data/<namespace>/railbound/trainsets/*.json on every datapack (re)load. */
public final class TrainsetDesignManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "railbound/trainsets";

    public TrainsetDesignManager() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager, ProfilerFiller profiler) {
        DesignLoader.LoadResult result = DesignLoader.load(jsons);
        result.errors().forEach(error -> Railbound.LOGGER.error("Skipping trainset design {}", error));
        TrainsetDesigns.replace(result.designs());
        Railbound.LOGGER.info("Loaded {} trainset design(s)", result.designs().size());
    }
}
```

- [ ] **Step 6: Register the listener in `Railbound.java`**

Add imports:
```java
import dev.railbound.trainset.load.TrainsetDesignManager;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
```
Add to the constructor, after the `LOGGER.info` line:
```java
        NeoForge.EVENT_BUS.addListener(Railbound::onAddReloadListeners);
```
Add the method to the class:
```java
    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new TrainsetDesignManager());
    }
```

- [ ] **Step 7: Run the tests to verify they pass**

Run: `.\gradlew.bat test --tests "dev.railbound.trainset.load.*"`
Expected: 8 tests PASS.

- [ ] **Step 8: Check the listener in game**

Run: `.\gradlew.bat runClient`, create a new creative world.
Expected: log contains `Loaded 1 trainset design(s)` and no `Skipping trainset design` lines. Close the game.

- [ ] **Step 9: Checkpoint** — no commit.

---

### Task 6: Syncing designs to clients

**Files:**
- Create: `src/main/java/dev/railbound/network/{SyncTrainsetDesignsPayload,RailboundNetwork}.java`
- Modify: `src/main/java/dev/railbound/Railbound.java`
- Test: `src/test/java/dev/railbound/network/SyncTrainsetDesignsPayloadTest.java`

**Interfaces:**
- Consumes: `TrainsetDesign.CODEC` (Task 2), `TrainsetDesigns` (Task 5), `Railbound.rl` (Task 1).
- Produces:
  - `record SyncTrainsetDesignsPayload(Map<ResourceLocation, TrainsetDesign> designs) implements CustomPacketPayload`; `TYPE`; `StreamCodec<ByteBuf, SyncTrainsetDesignsPayload> STREAM_CODEC`.
  - `RailboundNetwork.register(RegisterPayloadHandlersEvent)` and `RailboundNetwork.onDatapackSync(OnDatapackSyncEvent)`.

- [ ] **Step 1: Write the failing test `SyncTrainsetDesignsPayloadTest.java`**

```java
package dev.railbound.network;

import dev.railbound.testutil.TestDesigns;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SyncTrainsetDesignsPayloadTest {

    private static SyncTrainsetDesignsPayload roundTrip(SyncTrainsetDesignsPayload payload) {
        ByteBuf buf = Unpooled.buffer();
        SyncTrainsetDesignsPayload.STREAM_CODEC.encode(buf, payload);
        return SyncTrainsetDesignsPayload.STREAM_CODEC.decode(buf);
    }

    @Test
    void designsRoundTrip() {
        var payload = new SyncTrainsetDesignsPayload(Map.of(
                ResourceLocation.fromNamespaceAndPath("railbound", "coach_standard"), TestDesigns.sample()));
        assertEquals(payload.designs(), roundTrip(payload).designs());
    }

    @Test
    void emptyMapRoundTrips() {
        assertEquals(Map.of(), roundTrip(new SyncTrainsetDesignsPayload(Map.of())).designs());
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `.\gradlew.bat test --tests "dev.railbound.network.*"`
Expected: compilation FAILS — `cannot find symbol: class SyncTrainsetDesignsPayload`.

- [ ] **Step 3: Write `SyncTrainsetDesignsPayload.java`**

```java
package dev.railbound.network;

import dev.railbound.Railbound;
import dev.railbound.trainset.design.TrainsetDesign;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record SyncTrainsetDesignsPayload(Map<ResourceLocation, TrainsetDesign> designs) implements CustomPacketPayload {
    public static final Type<SyncTrainsetDesignsPayload> TYPE = new Type<>(Railbound.rl("sync_trainset_designs"));

    public static final StreamCodec<ByteBuf, SyncTrainsetDesignsPayload> STREAM_CODEC =
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.fromCodec(TrainsetDesign.CODEC))
                    .map(SyncTrainsetDesignsPayload::new, payload -> new HashMap<>(payload.designs()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `.\gradlew.bat test --tests "dev.railbound.network.*"`
Expected: 2 tests PASS.

- [ ] **Step 5: Write `RailboundNetwork.java`**

```java
package dev.railbound.network;

import dev.railbound.trainset.load.TrainsetDesigns;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class RailboundNetwork {
    public static final String PROTOCOL_VERSION = "1";

    private RailboundNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION).playToClient(
                SyncTrainsetDesignsPayload.TYPE,
                SyncTrainsetDesignsPayload.STREAM_CODEC,
                (payload, context) -> TrainsetDesigns.replace(payload.designs()));
    }

    /** Fires when a player joins and after /reload; sends the current designs to the affected players. */
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        SyncTrainsetDesignsPayload payload = new SyncTrainsetDesignsPayload(TrainsetDesigns.all());
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }
}
```

- [ ] **Step 6: Wire it in `Railbound.java`**

Add import:
```java
import dev.railbound.network.RailboundNetwork;
```
Add to the constructor:
```java
        modBus.addListener(RailboundNetwork::register);
        NeoForge.EVENT_BUS.addListener(RailboundNetwork::onDatapackSync);
```

- [ ] **Step 7: Build and run all tests**

Run: `.\gradlew.bat build`
Expected: `BUILD SUCCESSFUL`, all tests PASS.

- [ ] **Step 8: Checkpoint** — no commit.

---

### Task 7: Trainset item, data component, creative tab and text

**Files:**
- Create: `src/main/java/dev/railbound/registry/{RailboundComponents,RailboundItems,RailboundTabs}.java`
- Create: `src/main/java/dev/railbound/trainset/item/{TrainsetItem,TrainsetNames}.java`
- Create: `src/main/resources/assets/railbound/models/item/trainset.json`
- Modify: `src/main/resources/assets/railbound/lang/en_us.json`
- Modify: `src/main/java/dev/railbound/Railbound.java`
- Test: `src/test/java/dev/railbound/trainset/item/TrainsetNamesTest.java`

**Interfaces:**
- Consumes: `TrainsetDesigns.get/all` (Task 5), `TrainsetDesign.name()/category()/size()/seatCount()` (Tasks 2–3), `Railbound.MOD_ID` (Task 1).
- Produces:
  - `RailboundComponents.TRAINSET_DESIGN` — `DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>>`.
  - `RailboundItems.TRAINSET` — `DeferredItem<TrainsetItem>` registered as `railbound:trainset`.
  - `RailboundTabs.MAIN` — creative tab listing one stack per loaded design.
  - `TrainsetItem.of(ResourceLocation)` → `ItemStack`; `TrainsetItem.designId(ItemStack)` → `@Nullable ResourceLocation`.
  - `TrainsetNames.displayName(@Nullable ResourceLocation, Function<ResourceLocation, Optional<TrainsetDesign>>)` → `Component`; `TrainsetNames.tooltip(TrainsetDesign)` → `List<Component>`.

- [ ] **Step 1: Write the failing test `TrainsetNamesTest.java`**

```java
package dev.railbound.trainset.item;

import dev.railbound.testutil.TestDesigns;
import dev.railbound.trainset.design.TrainsetDesign;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TrainsetNamesTest {

    private static final ResourceLocation COACH = ResourceLocation.fromNamespaceAndPath("railbound", "coach_standard");

    private static TranslatableContents contents(Component component) {
        return assertInstanceOf(TranslatableContents.class, component.getContents());
    }

    @Test
    void knownDesignUsesItsNameKey() {
        TrainsetDesign sample = TestDesigns.sample();
        Component name = TrainsetNames.displayName(COACH, id -> Optional.of(sample));
        assertEquals("trainset.railbound.coach_standard", contents(name).getKey());
    }

    @Test
    void unknownDesignShowsUnknownName() {
        Component name = TrainsetNames.displayName(COACH, id -> Optional.empty());
        TranslatableContents c = contents(name);
        assertEquals("item.railbound.trainset.unknown", c.getKey());
        assertEquals("railbound:coach_standard", c.getArgs()[0]);
    }

    @Test
    void missingComponentShowsEmptyName() {
        Component name = TrainsetNames.displayName(null, id -> Optional.empty());
        assertEquals("item.railbound.trainset.empty", contents(name).getKey());
    }

    @Test
    void tooltipListsCategoryLengthAndSeats() {
        List<Component> lines = TrainsetNames.tooltip(TestDesigns.sample());
        assertEquals(3, lines.size());
        assertEquals("tooltip.railbound.category.passenger", contents(lines.get(0)).getKey());
        assertEquals("tooltip.railbound.length", contents(lines.get(1)).getKey());
        assertEquals(16, contents(lines.get(1)).getArgs()[0]);
        assertEquals("tooltip.railbound.seats", contents(lines.get(2)).getKey());
        assertEquals(40, contents(lines.get(2)).getArgs()[0]);
    }

    @Test
    void tooltipOmitsSeatsWhenThereAreNone() {
        TrainsetDesign boxCar = TestDesigns.parse("""
                {"name":"n","category":"box_car","size":{"length":2,"width":1,"height":1},
                 "bogeys":[{"z":0},{"z":1}],
                 "layout":{"palette":{"A":"anchor","#":"frame:floor"},"layers":[["A","#"]]}}""");
        List<Component> lines = TrainsetNames.tooltip(boxCar);
        assertEquals(2, lines.size());
        assertEquals("tooltip.railbound.category.box_car", contents(lines.get(0)).getKey());
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `.\gradlew.bat test --tests "dev.railbound.trainset.item.*"`
Expected: compilation FAILS — `cannot find symbol: class TrainsetNames`.

- [ ] **Step 3: Write `TrainsetNames.java`**

```java
package dev.railbound.trainset.item;

import dev.railbound.trainset.design.TrainsetDesign;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class TrainsetNames {
    private TrainsetNames() {}

    public static Component displayName(@Nullable ResourceLocation designId,
                                        Function<ResourceLocation, Optional<TrainsetDesign>> lookup) {
        if (designId == null) {
            return Component.translatable("item.railbound.trainset.empty");
        }
        return lookup.apply(designId)
                .<Component>map(design -> Component.translatable(design.name()))
                .orElseGet(() -> Component.translatable("item.railbound.trainset.unknown", designId.toString()));
    }

    public static List<Component> tooltip(TrainsetDesign design) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.railbound.category." + design.category().getSerializedName())
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.railbound.length", design.size().length())
                .withStyle(ChatFormatting.GRAY));
        int seats = design.seatCount();
        if (seats > 0) {
            lines.add(Component.translatable("tooltip.railbound.seats", seats).withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `.\gradlew.bat test --tests "dev.railbound.trainset.item.*"`
Expected: 5 tests PASS.

- [ ] **Step 5: Write `RailboundComponents.java`**

```java
package dev.railbound.registry;

import dev.railbound.Railbound;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RailboundComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Railbound.MOD_ID);

    /** Which trainset design an item stack places. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> TRAINSET_DESIGN =
            COMPONENTS.register("trainset_design", () -> DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build());

    private RailboundComponents() {}
}
```

- [ ] **Step 6: Write `TrainsetItem.java`**

```java
package dev.railbound.trainset.item;

import dev.railbound.registry.RailboundComponents;
import dev.railbound.registry.RailboundItems;
import dev.railbound.trainset.load.TrainsetDesigns;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TrainsetItem extends Item {

    public TrainsetItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(ResourceLocation designId) {
        ItemStack stack = new ItemStack(RailboundItems.TRAINSET.get());
        stack.set(RailboundComponents.TRAINSET_DESIGN.get(), designId);
        return stack;
    }

    @Nullable
    public static ResourceLocation designId(ItemStack stack) {
        return stack.get(RailboundComponents.TRAINSET_DESIGN.get());
    }

    @Override
    public Component getName(ItemStack stack) {
        return TrainsetNames.displayName(designId(stack), TrainsetDesigns::get);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation id = designId(stack);
        if (id != null) {
            TrainsetDesigns.get(id).ifPresent(design -> tooltip.addAll(TrainsetNames.tooltip(design)));
        }
    }
}
```

- [ ] **Step 7: Write `RailboundItems.java` and `RailboundTabs.java`**

```java
package dev.railbound.registry;

import dev.railbound.Railbound;
import dev.railbound.trainset.item.TrainsetItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RailboundItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Railbound.MOD_ID);

    public static final DeferredItem<TrainsetItem> TRAINSET =
            ITEMS.register("trainset", () -> new TrainsetItem(new Item.Properties().stacksTo(16)));

    private RailboundItems() {}
}
```

```java
package dev.railbound.registry;

import dev.railbound.Railbound;
import dev.railbound.trainset.item.TrainsetItem;
import dev.railbound.trainset.load.TrainsetDesigns;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RailboundTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Railbound.MOD_ID);

    /** One stack per loaded design. Contents refresh when the creative screen rebuilds (e.g. on rejoin). */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.railbound"))
                    .icon(() -> new ItemStack(RailboundItems.TRAINSET.get()))
                    .displayItems((parameters, output) ->
                            TrainsetDesigns.all().keySet().forEach(id -> output.accept(TrainsetItem.of(id))))
                    .build());

    private RailboundTabs() {}
}
```

- [ ] **Step 8: Register everything in `Railbound.java`**

Add imports:
```java
import dev.railbound.registry.RailboundComponents;
import dev.railbound.registry.RailboundItems;
import dev.railbound.registry.RailboundTabs;
```
Add to the constructor, before the listener lines:
```java
        RailboundComponents.COMPONENTS.register(modBus);
        RailboundItems.ITEMS.register(modBus);
        RailboundTabs.TABS.register(modBus);
```

The finished constructor reads:
```java
    public Railbound(IEventBus modBus, ModContainer container) {
        LOGGER.info("Create: Railbound loading");
        RailboundComponents.COMPONENTS.register(modBus);
        RailboundItems.ITEMS.register(modBus);
        RailboundTabs.TABS.register(modBus);
        modBus.addListener(RailboundNetwork::register);
        NeoForge.EVENT_BUS.addListener(Railbound::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(RailboundNetwork::onDatapackSync);
    }
```

- [ ] **Step 9: Add the placeholder item model `src/main/resources/assets/railbound/models/item/trainset.json`**

(Plan 4 replaces this with a 3D renderer.)
```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "minecraft:item/minecart"
  }
}
```

- [ ] **Step 10: Replace `src/main/resources/assets/railbound/lang/en_us.json`**

```json
{
  "itemGroup.railbound": "Railbound",
  "item.railbound.trainset": "Trainset",
  "item.railbound.trainset.empty": "Trainset (no design)",
  "item.railbound.trainset.unknown": "Unknown Trainset (%s)",
  "trainset.railbound.coach_standard": "Standard Passenger Coach",
  "tooltip.railbound.category.passenger": "Passenger Coach",
  "tooltip.railbound.category.box_car": "Box Car",
  "tooltip.railbound.category.tank_car": "Tank Car",
  "tooltip.railbound.category.locomotive": "Locomotive",
  "tooltip.railbound.category.multiple_unit": "Multiple Unit",
  "tooltip.railbound.length": "Length: %s blocks",
  "tooltip.railbound.seats": "Seats: %s"
}
```

- [ ] **Step 11: Build and run all tests**

Run: `.\gradlew.bat build`
Expected: `BUILD SUCCESSFUL`; all tests PASS (42 total: codec 5, layout 6, validator 16, loader 5, holder 3, payload 2, names 5).

- [ ] **Step 12: Check the item in game**

Run: `.\gradlew.bat runClient`, create a new creative world, open the creative inventory.
Expected: a **Railbound** tab containing one item named **Standard Passenger Coach** (minecart icon) with tooltip lines *Passenger Coach*, *Length: 16 blocks*, *Seats: 40*. `/give @s railbound:trainset` gives an item named **Trainset (no design)**. Close the game.

- [ ] **Step 13: Checkpoint** — no commit.

---

### Task 8: First-build milestone verification

**Files:**
- Create (temporary, inside the dev world only): `run/saves/<world>/datapacks/railbound_broken/pack.mcmeta` and `.../data/railbound/railbound/trainsets/broken.json`
- Create: `run/server/eula.txt` (only if the server asks)

**Interfaces:**
- Consumes: everything above. Produces: the verified first build.

- [ ] **Step 1: Clean build**

Run: `.\gradlew.bat clean build`
Expected: `BUILD SUCCESSFUL`; 42 tests PASS; `build\libs\railbound-0.1.0.jar` exists.

- [ ] **Step 2: Broken-datapack check (Review Focus 1)**

In the dev world from Task 7 (`run\saves\<world name>\`), create `datapacks\railbound_broken\pack.mcmeta`:
```json
{ "pack": { "pack_format": 48, "description": "Railbound broken design test" } }
```
and `datapacks\railbound_broken\data\railbound\railbound\trainsets\broken.json`:
```json
{
  "name": "trainset.railbound.broken",
  "category": "box_car",
  "size": { "length": 2, "width": 1, "height": 1 },
  "bogeys": [ { "z": 0 } ],
  "layout": { "palette": { "#": "frame:floor" }, "layers": [ [ "#", "#" ] ] }
}
```
Also create `datapacks\railbound_broken\data\railbound\railbound\trainsets\garbage.json` containing just `{ not json`.
Launch `.\gradlew.bat runClient`, open the world, run `/reload`.
Expected in the log: `Skipping trainset design railbound:broken: must have exactly 2 bogeys, found 1`, `Skipping trainset design railbound:broken: must have exactly 1 anchor, found 0`, a Minecraft parse error for `garbage.json`, and `Loaded 1 trainset design(s)`. The game keeps running and the coach is still in the creative tab. Delete the `railbound_broken` folder afterwards.

- [ ] **Step 3: Dedicated server check**

Run: `.\gradlew.bat runServer`. If it stops asking for the EULA, set `eula=true` in `run\server\eula.txt` and run again.
Expected: server reaches `Done (…)! For help, type "help"`, the log shows `Loaded 1 trainset design(s)`, and no client-only class errors. Type `stop`.

- [ ] **Step 4: Multiplayer sync check**

With the server from Step 3 running, start `.\gradlew.bat runClient`, choose Multiplayer → Direct Connect → `localhost`.
Expected: the Railbound creative tab shows **Standard Passenger Coach** with its tooltip — proving the sync payload reached a separate client. Stop both.

- [ ] **Step 5: Hand back to the user**

Report the build result and ask: "First build is done. Do you want me to `git init` the project now, with `Create-Source/`, `build/`, `run/` and `.gradle/` in `.gitignore`?" Do not initialise git without a yes.

---

## Plan sequence (later plans, written after this one is done)

| Plan | Delivers | Spec sections |
|---|---|---|
| 2 — Carriage placement and assembly | Hidden block types with collision shapes, glue-free attachment, placement planner (assembly mode, gap, reversed placement), anchor removal/pickup, GameTests; grey-box rendering | §4, §5.1–5.3, §5.5, §9, §12 |
| 3 — Seats, doors, cargo, cab | Double-seat mixins and right-click sitting, Create sliding-door integration, mounted item/fluid storage, train controls | §5.2, §5.4, §12 |
| 4 — Rendering and converter | OBJ loading, Flywheel visual + fallback renderer, door animation, 3D item renderer, `.bbmodel` converter Gradle task, Blockbench template | §6, §8 |
| 5 — Couplers and power hook | Coupler part and rendering, power interface | §10, §11 |
| 6 — Content | Seven designs built in Blockbench and converted; coach texture reworked to ≤ 512×512 | §3, §14 |

> **Superseded (2026-10-03).** This plan is **sub-phase 1.0** of Phase 1. The roadmap above was replaced by the user's release phases (spec §15), each built in small sub-phases with one plan each: 1.1 Coach on rails · 1.2 Coach interior · 1.3 Coach visuals · 1.4 Steam locomotive · 1.5 Items carriage · 1.6 Fluid carriage · 1.7 Phase 1 release. Plan files are named `…-phase1-N-<name>.md`.
