# Create: Railbound — Trainsets Design

**Date:** 2026-10-02 (updated 2026-10-03 for the phased release plan — see §15)
**Status:** Approved; Phase 1 scope set by the user's phase plan
**Sub-project:** #1 of the Railbound roadmap (trainsets). Catenary, signals, tracks and the network planner are separate sub-projects with their own specs.

---

## 1. Goal

Give players complete, MTR-quality trains as **single inventory items** that behave exactly like **real Create trains**. A carriage is placed from an item, renders as one detailed model (not visible blocks), has a walkable interior with seats, and works with all of Create's train systems.

### Success criteria

- A player can build a station, enter assembly mode, right-click a trainset item on the track, press **Assemble Train**, and drive or schedule the result like any Create train.
- Trainset carriages and player-built carriages can be assembled into the same train.
- Players can walk inside passenger carriages, right-click a seat to sit (two seats per block), and doors open automatically at stations.
- Box cars and tank cars are accessible through Create's Portable Storage Interface and Portable Fluid Interface.
- New designs can be added with a datapack plus a resource pack, with no code.

## 2. Platform and dependencies

| Item | Value |
|---|---|
| Minecraft | 1.21.1 |
| Loader | NeoForge (only target; no Forge 1.20.1, no Fabric) |
| Create | Required, `[6.0.10, 6.1)` |
| Build | ModDevGradle, JDK 21 |
| Libraries (via Create) | Flywheel 1.0.x (rendering), Ponder, Registrate (registration) |
| Maven | `maven.createmod.net` (Create, Ponder, Flywheel), `maven.ithundxr.dev/snapshots` (Registrate) |
| Optional compat | Create: Steam 'n' Rails NeoForge port — must not conflict; no integration code in Phase 1 |
| Mod ID | `railbound` (working ID; display name to be checked for conflicts before first public release — see §13) |

Reference sources live in `Create-Source/` (Create `mc1.21.1/dev`, Steam 'n' Rails `1.20/dev`, Steam 'n' Rails 1.21.1 port). They are read-only references; nothing is copied into or modified in them.

## 3. Scope

Releases follow the user's phase plan (§15). This spec covers the trainset system as a whole; **Phase 1 is the first release**.

### In Phase 1

- Trainset item (one item type, many designs) placed via Create's station assembly flow.
- Hidden-structure carriages (§5) with walkable interiors, double seats and station doors.
- Four designs:

| Category | Phase 1 design |
|---|---|
| `locomotive` | Steam locomotive (Create-style: needs no fuel) |
| `passenger` | Standard passenger coach (prototype exists) |
| `box_car` | Items carriage — 160 stacks, scrolling chest-style GUI (§5.7) |
| `tank_car` | Fluid carriage — 144 buckets by default, fill/drain GUI (§5.7) |

- Works with all of Create's navigation, schedules and signals. Automatic running keeps Create's rule: a conductor (a mob or Blaze Burner seated at the loco's controls) must be given a schedule by the player.
- Wrench pickup of disassembled carriages (§5.5); goods carriages connect to Portable Storage/Fluid Interfaces.
- Power-source hook (no fuel needed in Phase 1).
- Data-driven design format, converter tool, Blockbench template.

### Later phases (§15)

- Phase 2: more carriage types, crafting recipes and crafting components (steam engine, bogeys), Ponder scenes, coupler/bogey/anti-climber blocks.
- Phase 3: diesel locomotives and multiple units that burn fuel.
- Phase 4: electric locomotives with catenary and power generation.

### Out of scope for now

- Live coupling/shunting while trains run.
- Alternative liveries, distance LOD, dynamic interior lighting.
- Signals, track types, network planner (separate sub-projects).

## 4. Player flow

1. Place a Create station beside straight track and open it; switch to **assembly mode**.
2. Right-click the track with a trainset item.
3. The item validates space, places **two Create bogeys** and the carriage's hidden blocks, and is consumed.
4. Repeat with more items: each click places the next carriage behind the previous one, with a **1-block gap**. Sneak + right-click places a carriage reversed (e.g. the rear cab car of a multiple unit).
5. Optionally add player-built carriages in the same assembly zone (coupler parts arrive in Phase 2).
6. Press **Assemble Train**. The result is a normal Create train. To run it automatically, seat a conductor at the loco's controls and give it a schedule, as in Create.
7. When disassembled at a station, carriages stay visible as models. **Shift + right-click with a wrench** picks the whole carriage up as its item (goods carriages must be empty first, §5.5).

## 5. Carriage construction (approach: hidden-structure carriage)

Each carriage is made of real Create bogeys plus **invisible blocks** whose shapes give collision, seats, doors, cargo and controls. One **anchor** block draws the full model. Because the result is made of blocks, Create assembles and runs it with no special handling.

Fallback: if hidden-structure carriages prove unworkable during implementation, switch to approach 2 (body rendered by a custom bogey style over a minimal block set). Approach 3 (own vehicle simulation) is rejected because it breaks Create compatibility.

### 5.1 Bogeys

- **Every carriage has exactly two bogeys.** Designs with any other count fail validation.
- Bogeys are Create's own bogey blocks (default `create:standard`), placed in the block directly above the track, as Create does when a player uses Railway Casing (`StationBlockEntity`, verified in source).

### 5.2 Hidden block types

| Block | Purpose | Built on |
|---|---|---|
| `frame` | Floor, thin walls, roof, partitions. One block with a `shape` property (`floor`, `floor_wall_left`, `floor_wall_right`, `wall_left`, `wall_right`, `roof`, `roof_wall_left`, `roof_wall_right`, `partition`, `full`) so walls can be 2 px thick and floor/roof can combine with a side wall in one block | Plain block, invisible render |
| `seat` (double) | Two seats per block, left and right halves | Create `SeatBlock` behaviour + mixins (§5.4) |
| `door` | Opens/closes automatically at stations | Create sliding door (`SlidingDoorBlock` / `SlidingDoorMovementBehaviour`), invisible render |
| `cargo_item` / `cargo_fluid` | Box car / tank car storage, merged by Create into the contraption inventory reachable by Portable Storage/Fluid Interfaces | Create 6 `MountedItemStorageType` / `MountedFluidStorageType` API |
| `cab` | Driver controls plus the conductor seat behind them; required to drive the train and to run schedules | Create's own `TRAIN_CONTROLS` block — Create's conductor check (`CarriageContraption.inControl`) accepts only that exact block, so it stays visible inside the cab model — plus a seat directly behind it |
| `anchor` | One per carriage; stores design ID; renders the model in world and in contraption; owns carriage removal and item drop | Our block + block entity |

All hidden blocks render nothing themselves, let light through, and show no selection outline except seats (so players can see what they will click).

### 5.3 Attachment without glue

Register a Create `BlockMovementChecks` **attached check** so a carriage's hidden blocks stick to each other and to its bogeys during assembly. Blocks of different carriages never attach (the 1-block gap guarantees separation). No Super Glue is needed.

### 5.4 Double seats

Create stores seats as one `BlockPos` per seat (`Contraption.seats`) and places a passenger at the block centre (`AbstractContraptionEntity.getPassengerPosition`). Two seats per block require three mixins:

1. When a double-seat block is added to a contraption, register its position **twice** (two seat indices).
2. Offset a passenger ±0.25 block across the seat block, chosen by which of the two indices they occupy.
3. On right-click, pick the seat index from the side of the block the player clicked.

**Right-click to sit** must work both on moving trains (contraption interaction) and on parked, disassembled carriages (block interaction). Each seat half has an interaction shape matching the modeled seat.

### 5.5 Breaking and pickup

- **Shift + right-click with Create's wrench** on any part of a disassembled carriage removes the **entire carriage** and gives back **exactly one** item for its design.
- **Goods carriages must be empty** to be picked up with the wrench; otherwise pickup is refused with an action-bar message.
- **Breaking** any hidden block (mining, explosions, commands) also removes the whole carriage and drops exactly one item; a goods carriage spills its items like a broken chest (its fluid is lost).
- The anchor coordinates removal so several parts breaking at once never drop more than one item.
- Individual hidden blocks can never be left behind or broken loose.

### 5.6 Cross-section (one passenger seat row)

```
  roof frame (thin) ─────────────
 │wall│ seat seat │aisle│ seat seat │wall│
 │ 2px│  block -1 │  0  │  block +1 │2px │
  floor frame (thin) ─────────────
          bogey (Create)
          ═══ track ═══
```

Passenger carriages are **3 blocks wide**, with 2+2 seating around a centre aisle.

### 5.7 Goods carriages

Both goods carriages work on parked carriages **and** on assembled trains (Create 6's `MountedItemStorage.handleInteraction` opens custom menus on moving contraptions), and both are reachable by Create's Portable Storage/Fluid Interfaces.

| | Items carriage (`box_car`) | Fluid carriage (`tank_car`) |
|---|---|---|
| Capacity | **160 stacks** — a 2×2×2 Create vault (8 × Create's `vaultCapacity`, default 20) | **144 buckets** — a 3×3×2 Create tank (18 × Create's `fluidTankCapacity`, default 8 buckets) |
| Follows Create's config | Yes — scales if a server changes `vaultCapacity` | Yes — scales if a server changes `fluidTankCapacity` |
| Open | Shift + right-click with an empty hand | Shift + right-click with an empty hand |
| GUI | Double-chest-style grid (54 slots visible) with a scrollbar over all 160 stacks | Fill slot (put in filled buckets/containers), drain slot (put in empty containers to take fluid out), and a gauge showing the fluid, amount and maximum |

A design's `cargo` field (§7.2) sets which kind it is; the capacities above are the Phase 1 defaults.

## 6. Rendering

- **Body:** the anchor block entity renders the static body via a **Flywheel block entity visual**. Create's `ContraptionVisual` embeds block entity visuals inside moving contraptions (verified in source), so the same visual runs parked and moving. A standard `BlockEntityRenderer` renders the same models when Flywheel's backend is off.
- **Doors:** each `door` block entity renders its own door part, offset by Create's existing sliding-door open amount (0→1, `LerpedFloat`). Doors slide along the carriage toward the nearer end.
- **Bogeys:** Create's bogey renderer, unchanged.
- **Glass:** cutout rendering (vanilla glass style: each pixel fully clear or solid), so interiors are visible without sorting artefacts.
- **Lighting:** provided by Create's contraption lighting.
- **Models:** OBJ files loaded through NeoForge's OBJ model loader and registered as additional models. Client discovers parts by scanning `models/trainset/*/` in resource packs.
- **Texture budget:** at most **512×512** per design (textures go into the shared block atlas). Repeated parts share UV regions.
- **Inventory icon:** the item renders a scaled 3D copy of its design's model (custom item renderer); no separate icon art.

## 7. Design data format

### 7.1 Split between data and assets

| Part | Location | Read by |
|---|---|---|
| Design data (size, bogeys, layout, seats, cargo, power) | `data/<ns>/railbound/trainsets/<id>.json` | Server; synced to clients |
| Visuals (OBJ parts, texture) | `assets/<ns>/models/trainset/<id>/…`, `assets/<ns>/textures/trainset/<id>.png` | Client |

Designs are loaded by a server **reload listener** (`SimpleJsonResourceReloadListener` over `railbound/trainsets`) and sent to clients with a sync payload on join and after `/reload`, so clients receive the design list (creative tab, tooltips, placement preview). *(Changed from a NeoForge datapack registry during Plan 1: a datapack registry fails the whole world load on a single bad entry, which contradicts §7.3, and it cannot be reloaded.)*

### 7.2 Design JSON

```json
{
  "name": "trainset.railbound.coach_standard",
  "category": "passenger",
  "size": { "length": 16, "width": 3, "height": 3 },
  "bogeys": [ { "z": 4, "style": "create:standard" }, { "z": 12, "style": "create:standard" } ],
  "power": "none",
  "cargo": { "item_slots": 0, "fluid_mb": 0 },
  "doors": [ { "part": "door_left_front", "pos": [-1, 0, 2] } ],
  "layout": {
    "palette": { "#": "frame:floor", "|": "frame:wall", "S": "seat", "D": "door", "A": "anchor", ".": "air" },
    "layers": [ [ "|S.S|" ] ]
  }
}
```

- `category`: `passenger` | `box_car` | `tank_car` | `locomotive` | `multiple_unit`.
- `power`: `none` | `steam` | `diesel` | `electric`. In Phase 1 all values behave the same (no fuel); Phase 3 adds fuel for `diesel` and Phase 4 catenary power for `electric` (§11).
- `layout.layers`: one row of text per block along the carriage, one layer per height level, characters mapped through `palette`.
- Coordinates are carriage-local: Z along the track (front = −Z), X across (−1, 0, +1), Y up from the carriage floor layer.

### 7.3 Validation

A design is rejected (skipped, with a log line naming the design and the reason) if:
- it does not have exactly 2 bogeys;
- it has no `anchor` or more than one;
- a palette entry names an unknown block type, or a layer row has the wrong width;
- it is wider than 3 blocks or has a non-integer length.

A broken pack never crashes the game. A design whose client model is missing renders as a grey placeholder box and logs a warning; the carriage still works.

## 8. Asset pipeline

### 8.1 Converter

A **Java Gradle task** in this repo, run before resources are processed:

```
art/trainsets/<id>/
  <id>.bbmodel          model + `layout` marker group (Blockbench)
  design.source.json    name, category, cargo size, power (hand-written)
        │  converter
        ▼
build output (packaged into the jar):
  data/railbound/railbound/trainsets/<id>.json
  assets/railbound/models/trainset/<id>/body.obj, door_*.obj (+ .mtl, loader JSON)
  assets/railbound/textures/trainset/<id>.png
```

The build **fails** if: the texture exceeds 512×512; the design does not have exactly 2 bogey markers; layout markers do not line up with the block grid or model; any model rule in §8.3 is broken.

Generated files are **not** kept in source control; the `.bbmodel` and `design.source.json` are the source of truth.

### 8.2 Layout markers

Each `.bbmodel` has a `layout` group containing marker cubes named by block type: `frame_floor`, `frame_wall`, `frame_roof`, `frame_partition`, `seat`, `door`, `cargo_item`, `cargo_fluid`, `cab`, `anchor`, `bogey`. A marker may span a region (e.g. the whole floor). The converter snaps markers to the block grid to build `layout`. Markers are never exported to OBJ.

### 8.3 Model rules (checked by the converter)

| Rule | Value |
|---|---|
| Format | Blockbench Generic Model (`free`) |
| Scale / axes | 16 units = 1 block; front faces −Z; y = 0 is the carriage floor (top of the bogey block) |
| Width | Body within x −24..24 |
| Length | Whole blocks; gangways/buffers may extend ≤ 4 units past the ends |
| Height | ≤ 3 blocks above the floor plus roof equipment |
| Underframe | Nothing below y = −8; underfloor equipment only between bogeys |
| Groups | `body` → `body.obj`; `door_<left/right>_<n>` → one OBJ each; `layout` → markers; `ref_*` → never exported |
| Texture | One per design, ≤ 512×512, repeated parts share UVs, Create palette (brass `#d9a441`), vanilla-style glass |

### 8.4 Workflow

1. Start from `art/trainsets/_template.bbmodel` (floor plane, 3-wide guide, Create bogey reference via Reference Models plugin, example markers, named door groups).
2. Claude builds the model through the Blockbench MCP (or the user edits by hand).
3. Run model checks (coplanar faces, UV bounds).
4. User reviews renders in the Blockbench MCP panel; fixes until approved.
5. Converter runs in the build; test in game.

Required Blockbench plugins: Reference Models, Structure Importer, Simplify Models, Texture Stitcher (optional: Missing Texture Highlighter, Cameras).

## 9. Placement rules

- Only works on track belonging to a station that is in assembly mode, on straight track of the standard gauge.
- By default the carriage's front faces the station end of the assembly track. **Sneak + right-click places it reversed** (front facing away from the station) — needed for the rear cab car of a multiple unit, or a second locomotive at the back.
- Carriages fill the assembly track from the station outward: the item scans the assembly track for the last bogey or carriage block already present and places the new carriage directly behind it with a 1-block gap. On an empty assembly track it starts at the station end.
- The item is consumed only on successful placement.

## 10. Couplers *(Phase 2)*

- A placeable **coupler part** (not a chain type) that players attach to the ends of their own carriages.
- At assembly, a coupler (with buffers) is drawn across the gap between any two adjacent carriages that have couplers — trainset carriages always count as having them.
- Linking is still done by Create's assembly; couplers do not join or split running trains.

## 11. Power hook

- Each design declares `power`. Phase 1 exposes it through a small interface on the carriage (e.g. "requires external power: yes/no; currently powered: yes/no") that always reports powered. The Phase 1 steam locomotive needs no fuel, like a standard Create train.
- Phase 3 implements fuel for diesel designs (coal, wood and its sub-types such as planks, and fuels from other mods).
- Phase 4 implements the electric side: catenary supplies power, and electric designs run only while in contact with a powered wire.

## 12. Testing and error handling

### 12.1 Automated

| Level | Coverage | Tool |
|---|---|---|
| Unit | Design JSON parsing and validation; layout text → block positions; converter (`.bbmodel` → layout, OBJ, texture check); placement planning (track direction + click → all positions incl. gap); double-seat offsets | JUnit |
| In-world | Place + Assemble succeeds; 2 bogeys per carriage; seat count matches design; right-click seat seats the correct side; a seated conductor at the loco's controls runs a schedule; Portable Storage Interface pulls from box car; Fluid Interface drains tank car; box car holds 160 stacks and tank car 144 buckets by default; wrench pickup returns one item and is refused while cargo remains; disassembly keeps anchor and model; breaking a part removes the carriage and drops one item | NeoForge GameTests |
| Build | Every design in `art/` converts cleanly | `gradle build` |

### 12.2 Manual (per release)

- Dev client: visuals, door animation at stations, interior visible through windows, inventory icon.
- Local dedicated server with 2 players: seats, doors and models stay in sync.
- Performance: 10-carriage train, frame rate with Flywheel on and off.
- Compatibility: Create only; Create + Steam 'n' Rails NeoForge port.

### 12.3 Error handling

| Situation | Behaviour |
|---|---|
| Station not in assembly mode / not an assembly track | Action-bar message; nothing placed |
| Space blocked | Message + Create-style outlines on blocking blocks |
| Curved, too short, or non-standard-gauge track | Message with the reason; nothing placed |
| Invalid design in a datapack | Skipped at load with a log line |
| Client model missing | Grey placeholder + warning; carriage still works |
| Datapack removed while carriages exist | Anchor keeps the design ID; carriage keeps working with placeholder; wrench returns the item |
| Partial destruction (explosion, commands) | Rest of carriage removed; exactly one item drops |
| Unsupported Create version | Mod refuses to load (`[6.0.10, 6.1)` required); seat mixins re-checked on every Create update |

## 13. Decisions and deferred items

| Topic | Decision |
|---|---|
| Approach | Hidden-structure carriage; fall back to custom-bogey rendering if it proves unworkable |
| Width | 3 blocks, two seats per block |
| Release scope | Phased (§15); Phase 1 = steam loco, passenger coach, items carriage, fluid carriage |
| Couplers | Phase 2; visual part, linked at assembly; live coupling deferred |
| Power | Phase 1 free (steam, like Create); fuel in Phase 3; catenary power in Phase 4 |
| Automatic running | Create's rule kept: a seated conductor at the controls must be given a schedule |
| Doors | Create's train-door behaviour |
| Pickup | Shift + right-click with Create's wrench; goods carriages must be empty |
| Goods capacity | 160 stacks / 144 buckets by default, scaling with Create's vault and tank config |
| Goods GUI | Shift + right-click with an empty hand; scrolling chest grid / fill-drain-gauge |
| Disassembled carriages | Keep rendering |
| Liveries | One per design for now |
| Crafting recipes | Phase 2, with dedicated components (steam engine, bogeys) |
| Mod display name | "Railbound" is also the name of an existing puzzle game; check for conflicts before the first public release. Mod ID `railbound` is used until then |
| Source control | Git since the first build; `Plan.txt` (the user's personal plan) is never committed |

## 14. Existing assets

- `art/trainsets/coach_standard/coach_standard.bbmodel` — approved look for the passenger coach (203 cubes, 10 rows of 2+2 seats, 4 sliding-door groups with see-through door windows, vanilla-style glass). Its texture is an unoptimised 1024×2048 sheet and must be reworked to ≤ 512×512 and given layout markers before it passes the converter.

## 15. Release phases

From the user's phase plan. Each phase is its own release; this spec's trainset system underpins all of them.

| Phase | Theme | Contents |
|---|---|---|
| **1** | First version of Railbound | Steam loco (no fuel, Create-style), passenger coach, items carriage (160 stacks), fluid carriage (144 buckets); full Create navigation and schedules with a seated conductor; wrench pickup; goods GUIs; Portable Interface support |
| **2** | More carriages and crafting | More carriage types; improvements to Phase 1; crafting recipes with dedicated components (steam engine for locos, bogeys for carriages); Ponder scenes; new blocks/models: bogeys, couplers (not chain type), anti-climbers |
| **3** | Fuel trains | Diesel locomotives that accept coal, wood (and sub-types such as planks) and fuels from other mods; 2 locomotive models; 2 multiple units (one passenger, one goods) |
| **4** | Electric trains | Power generator (working name), catenary pole, wire holder, catenary wire (max 24 blocks per span), power connector (accepts our generator or connectors from Create: Power Grid / Create: New Age); electric designs run only while touching a powered wire; 2 electric locomotive types with 2 carriages each, and 2 EMUs. Catenary height is set once the models exist |

### 15.1 Building in sub-phases

Each phase is built in small sub-phases. Every sub-phase gets its own implementation plan, its own models and tests, a review, and a commit before the next one starts — so each piece of code and each model is finished and debugged in isolation.

**Phase 1 sub-phases**

| Sub-phase | Delivers | In-game check |
|---|---|---|
| 1.0 Foundation ✅ | Design format, validation, datapack loading, client sync, trainset item, creative tab | Done (2026-10-03) |
| 1.1 Coach on rails | Frame blocks and anchor, placement on the assembly track (§9), glue-free attachment, Create assembly, wrench pickup and breaking (§5.5); grey-box visual; the four deferred Plan 1 review fixes | Place a coach, assemble it, move it, pick it up |
| 1.2 Coach interior | Double seats with right-click sitting (§5.4), station doors | Sit on either seat half; doors open at a station |
| 1.3 Coach visuals | OBJ models, Flywheel visual and fallback renderer, door animation, 3D inventory icon, `.bbmodel` converter (§6, §8); coach texture reworked to ≤ 512×512 | Real coach model in game |
| 1.4 Steam locomotive | Cab with Create train controls and conductor seat, loco model | Drive it; a seated conductor runs a schedule |
| 1.5 Items carriage | 160-stack storage, scrolling chest GUI, Portable Storage Interface, model (§5.7) | Fill by hand and by interface; wrench refuses while loaded |
| 1.6 Fluid carriage | 144-bucket tank, fill/drain/gauge GUI, Portable Fluid Interface, model (§5.7) | Fill with buckets and pipes; gauge shows the amount |
| 1.7 Phase 1 release | Compatibility and performance pass, polish, release build | 129-mod profile and multiplayer |

Phases 2–4 are split into sub-phases when their turn comes.
