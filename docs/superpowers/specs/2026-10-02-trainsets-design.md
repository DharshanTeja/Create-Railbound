# Create: Railbound — Trainsets v1 Design

**Date:** 2026-10-02
**Status:** Draft for review
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
| Optional compat | Create: Steam 'n' Rails NeoForge port — must not conflict; no integration code in v1 |
| Mod ID | `railbound` (working ID; display name to be checked for conflicts before first public release — see §13) |

Reference sources live in `Create-Source/` (Create `mc1.21.1/dev`, Steam 'n' Rails `1.20/dev`, Steam 'n' Rails 1.21.1 port). They are read-only references; nothing is copied into or modified in them.

## 3. Scope

### In v1

- Trainset item (one item type, many designs) placed via Create's station assembly flow.
- Hidden-structure carriages (§5) with walkable interiors.
- Design categories and initial designs:

| Category | v1 designs |
|---|---|
| `locomotive` | Steam locomotive, diesel locomotive, electric locomotive |
| `multiple_unit` | Multiple-unit cab car (cab + passenger seats); pairs with passenger coaches |
| `passenger` | Standard passenger coach (prototype exists) |
| `box_car` | Box car (item cargo) |
| `tank_car` | Tank car (fluid cargo) |

- Coupler part for player-built carriages (visual, linked at assembly).
- Power-source hook (no fuel needed in v1).
- Data-driven design format, converter tool, Blockbench template.

### Explicitly out of v1

- Crafting recipes (deferred by the user; items obtainable in creative/commands until decided).
- Live coupling/shunting while trains run (possible later sub-project).
- Fuel or electric power requirements (catenary sub-project adds them).
- Alternative liveries, distance LOD, dynamic interior lighting.
- Catenary, signals, track types, network planner (separate sub-projects).

## 4. Player flow

1. Place a Create station beside straight track and open it; switch to **assembly mode**.
2. Right-click the track with a trainset item.
3. The item validates space, places **two Create bogeys** and the carriage's hidden blocks, and is consumed.
4. Repeat with more items: each click places the next carriage behind the previous one, with a **1-block gap**. Sneak + right-click places a carriage reversed (e.g. the rear cab car of a multiple unit).
5. Optionally add player-built carriages (with coupler parts) in the same assembly zone.
6. Press **Assemble Train**. The result is a normal Create train.
7. When disassembled at a station, carriages stay visible as models. Breaking any part, or using a wrench on it, removes the whole carriage and returns its item.

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
| `cab` | Driver controls; required for a train to be drivable | Create train controls block |
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

- Breaking any hidden block, or right-clicking it with a wrench, removes the **entire carriage** and drops/returns **exactly one** item for its design.
- The anchor coordinates removal so explosions or commands that hit several parts never drop more than one item.
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

Designs are loaded as a NeoForge **datapack registry** with a network codec so clients receive the design list (creative tab, tooltips, placement preview).

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
- `power`: `none` | `steam` | `diesel` | `electric`. In v1 all values behave the same (no fuel); the field is the hook for the catenary sub-project.
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

## 10. Couplers

- A placeable **coupler part** that players attach to the ends of their own carriages.
- At assembly, a coupler (with buffers) is drawn across the gap between any two adjacent carriages that have couplers — trainset carriages always count as having them.
- Linking is still done by Create's assembly; couplers do not join or split running trains.

## 11. Power hook

- Each design declares `power`. v1 exposes it through a small interface on the carriage (e.g. "requires external power: yes/no; currently powered: yes/no") that always reports powered.
- The catenary sub-project will implement the provider side and a config option to make electric and multiple-unit designs require overhead wires.

## 12. Testing and error handling

### 12.1 Automated

| Level | Coverage | Tool |
|---|---|---|
| Unit | Design JSON parsing and validation; layout text → block positions; converter (`.bbmodel` → layout, OBJ, texture check); placement planning (track direction + click → all positions incl. gap); double-seat offsets | JUnit |
| In-world | Place + Assemble succeeds; 2 bogeys per carriage; seat count matches design; right-click seat seats the correct side; Portable Storage Interface pulls from box car; Fluid Interface drains tank car; disassembly keeps anchor and model; breaking a part removes the carriage and drops one item | NeoForge GameTests |
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
| Couplers | Visual part, linked at assembly; live coupling deferred |
| Power | Free in v1, hook for catenary |
| Doors | Create's train-door behaviour |
| Disassembled carriages | Keep rendering; wrench returns item |
| Liveries | One per design in v1 |
| Crafting recipes | Deferred — the user will decide later |
| Mod display name | "Railbound" is also the name of an existing puzzle game; check for conflicts before the first public release. Mod ID `railbound` is used until then |
| Source control | Project stays local (no git) until the first successful build |

## 14. Existing assets

- `art/trainsets/coach_standard/coach_standard.bbmodel` — approved look for the passenger coach (191 cubes, 10 rows of 2+2 seats, 4 sliding-door groups, vanilla-style glass). Its texture is an unoptimised 1024×2048 sheet and must be reworked to ≤ 512×512 and given layout markers before it passes the converter.
