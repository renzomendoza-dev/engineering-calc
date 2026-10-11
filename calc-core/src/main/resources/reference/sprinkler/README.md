# Sprinkler Hydraulics Reference Data

Used by `mechanical.sprinkler`'s resolvers. NFPA 13, 2019 edition. All values
are US units: psi, gpm, feet, inches, ft2.

## Confidence tags

Every file carries a file-level `confidence`, and most rows carry their own:

- `verified` — cross-checked against the published standard.
- `verify` — a well-known standard value, entered from general knowledge and
  **not yet confirmed against the printed table**. Safe for development and
  sanity-checking; confirm before relying on it for real design work.
- `placeholder` — not known. Never silently treated as zero: the calculator
  throws instead of guessing.

**Checked against the printed NFPA 13 2019 so far:** all five hazard presets,
all thirteen K-factors, the whole C factor table, and seven of the eight
fitting rows. Still outstanding: the **vane type flow switch** row, which may
not belong in the standard's table at all, and the three added pipe sizes
(an ASME document, not an NFPA one).

### Cross-checking against a 2016-edition copy

A 2016-edition copy of four of these tables was used as a second reader. It is
worth knowing what that exercise found, because the same source is likely to be
used again:

| Finding | Outcome |
|---|---|
| 91 of 102 fitting cells agreed | Seven rows promoted to `verified` |
| Butterfly valve 12 ft @ 4 in. → 9 ft @ 5 in. | **Confirmed published**, not a slip |
| No vane type flow switch row | Our 8th row called into question |
| C values: all ten rows agreed | Already `verified`; 2019 adds "brass" to the copper/stainless row |
| K-factors: 3 metric values differed | **Ours upheld** — see below |
| Light hazard coverage prose | **Ours corrected** — it conflated two axes of the table |

Two of its own cells are provably wrong (a 220 ft² entry whose paired 18.6 m²
means 200, and a thread column reading 1, 2, 3, 4 in. NPT in sequence — there
is no 4 in. NPT sprinkler, so that is a spreadsheet autofill). **Treat it as a
second reader, not as a source.** Where it disagrees, the disagreement has to
be resolved on its merits rather than by deferring to either copy.

The K-factor disagreement resolved cleanly that way: the metric column is
`round(K × 14.29)` on twelve of thirteen rows, and the three disputed cells are
exactly the three where that pattern picks our value. No data changed.

| Data | Status | Source / what would clear it |
|---|---|---|
| **Hazard presets** (all 5 fields) | ✅ `verified` | Figure 19.3.3.1.1, §19.3.3.1.4, Table 19.3.3.1.2, Table 10.2.4.2.1(a)(b)(c) |
| **K-factors** (13 rows) | ✅ `verified` | Table 7.2.2.1 |
| **C factors** (11 rows) | ✅ `verified` | Table 27.2.4.8.1 (normative) + Table A.27.2.4.8.2 (ε) |
| **Fitting equivalent lengths** (7 of 8 rows) | ✅ `verified` | Table 27.2.3.1.1, second read done — 91 of 102 cells agreed across three sources |
| Fitting: **vane type flow switch** | `verify` ×1 | Confirm the row exists in the printed 2019 table — two independent reproductions of the chart do not contain it |
| Pipe 3-1/2" / 5" / 8" | `verify` | ASME B36.10M Sch 40 wall thicknesses (not an NFPA document). ID = OD − 2t checks out on every row, and the ODs are exact standard inch values, so only the three wall figures are unconfirmed |

A row stays at `verify` while **any** of its fields is unconfirmed; per-field
detail is in each row's `sourceNote`.

### How to read the density/area curves

Each curve slopes from a **lower density over a larger area** down-right to a
**higher density over the minimum area**. The pair stored for each hazard is
that minimum-area end — the usual starting design point, not the only valid
one. Full spans, for a UI that lets an engineer pick elsewhere on the curve:

| Hazard | Large-area end | Stored (minimum-area end) |
|---|---|---|
| Light | 0.07 @ 3000 ft² | **0.10 @ 1500 ft²** |
| Ordinary 1 | 0.10 @ 4000 ft² | **0.15 @ 1500 ft²** |
| Ordinary 2 | 0.15 @ 4000 ft² | **0.20 @ 1500 ft²** |
| Extra 1 | 0.20 @ 5000 ft² | **0.30 @ 2500 ft²** |
| Extra 2 | 0.30 @ 5000 ft² | **0.40 @ 2500 ft²** |

Only the stored endpoint exists as structured data today; the spans are
recorded in the JSON's `curveNote`.

### Design area is a wet-pipe figure

`designAreaSqFt` is the **wet pipe** value. Table 27.2.4.7.2 (Minimum Design
Area) shows dry pipe systems take a uniformly 30 percent larger area:

| Hazard | Wet | Dry |
|---|---|---|
| Light | 1500 ft² | 1950 ft² |
| Ordinary | 1500 ft² | 1950 ft² |
| Extra | 2500 ft² | 3250 ft² |

The presets are not keyed on system type, so a dry or preaction design started
from a preset is **undersized by 30 percent**. This is not a calculation
defect — `SprinklerHydraulicCalculator` takes `designAreaSqFt` as an input and
never looks it up — but a form offering these presets should apply the uplift
or warn. Recorded in the JSON's `dryPipeAreaNote`; not yet modelled.

*Misprint in the printed table:* its SI column reads 2500 ft² **(130 m²)** for
extra hazard wet. 2500 ft² is about 232 m², and every neighbouring row converts
correctly (1500/140, 1950/180, 3250/300), so this is read as a typo for (230).
Only ft² values are stored, so nothing downstream depends on it.

**Citations were renumbered in the 2019 edition** and ours were written against
the older numbering. Confirmed and corrected so far: the fitting chart is
27.2.3.1.1 (was 23.4.3.1.1), and hose stream/duration is **Table 19.3.3.1.2**
(was 11.2.3.1.2 — also corrected in
`reference/storage/fire-water-duration.json`, which carried the same stale
pointer for the same data). Treat any remaining pre-2019 citation in this repo
as suspect until checked. A third has since turned up: the C factors were cited
to Annex Table A.27.2.4.8.2 when the normative source is **Table 27.2.4.8.1** in
the body of Chapter 27.

## Files

### `hazard-presets.json` — `verify`

Density/area minimum points, hose stream, duration, max coverage per hazard.

| Hazard | Density (gpm/ft2) | Area (ft2) | Hose (gpm) | Duration (min) | Coverage (ft2) |
|---|---|---|---|---|---|
| Light | 0.10 | 1500 | 100 | 30 | 225 |
| Ordinary Group 1 | 0.15 | 1500 | 250 | 90 | 130 |
| Ordinary Group 2 | 0.20 | 1500 | 250 | 90 | 130 |
| Extra Group 1 | 0.30 | 2500 | 500 | 120 | 100 |
| Extra Group 2 | 0.40 | 2500 | 500 | 120 | 100 |

Sources: Figure 19.3.3.1.1 (density/area curves, p. 13‑162 — **density values
still to confirm**), Table 19.3.3.1.2 (hose stream + duration, ✅ confirmed),
Table 10.2.4.2.1(a)(b)(c) (protection areas, ✅ confirmed), §19.3.3.1.4
(minimum design areas, ✅ confirmed).

Notes:

- density/area are the **minimum point** of each curve (lowest density at its
  paired maximum area). Any point along the curve is a valid design choice, so
  these are presets, not limits — which is why the calculator takes density and
  area as inputs instead of looking them up.
- Where NFPA 13 publishes a duration *range* (Ordinary 60–90, Extra 90–120),
  the conservative upper bound is stored.
- **Overlaps `reference/storage/fire-water-duration.json`**, which holds hose
  stream and duration for the coarser LIGHT/ORDINARY/EXTRA classes used by
  `FireWaterStorageCalculator`. That file cannot express the Group 1/2 split the
  density/area curves need, so the two coexist deliberately. If one changes,
  change both.

### `hazen-williams-c.json` — `verified`

**NFPA 13 2019 Table 27.2.4.8.1**, read from the printed page. Ten rows are
published; this file carries eleven because the combined "copper tube, brass or
stainless steel" row is split into three, while black and galvanized steel are
folded together — the table gives them identical values, so separate rows would
only resolve to the same numbers.

| Pipe type key | Service | C | Published row |
|---|---|---|---|
| `STEEL` | wet | 120 | Black steel (wet systems including deluge) |
| `STEEL` | dry | 100 | Black steel (dry systems including preaction) |
| `STEEL` | preaction | 100 | same row — the table names preaction |
| `COPPER` | any | 150 | Copper tube, brass or stainless steel |
| `BRASS` | any | 150 | same row |
| `STAINLESS_STEEL` | any | 150 | same row |
| `CPVC` | any | 150 | Plastic (listed) all |
| `DUCTILE_IRON_UNDERGROUND` | any | 140 | Cement-lined cast- or ductile iron |
| `UNLINED_CAST_OR_DUCTILE_IRON` | any | 100 | Unlined cast or ductile iron |
| `ASBESTOS_CEMENT` | any | 140 | Asbestos cement |
| `CONCRETE` | any | 140 | Concrete |

**The table's own footnote:** *the authority having jurisdiction is permitted to
allow other C values.* These are defaults an AHJ may override — which is the
reason the calculator takes C per pipe segment instead of looking it up.

Two naming traps worth knowing:

- `DUCTILE_IRON_UNDERGROUND` is a **historical key**, kept so the API doesn't
  break. The standard doesn't say "underground"; it says *cement-lined*. The
  lining is what carries the 140, and an unlined main is 100 — a 40 percent
  C difference, so resolving the wrong one under-predicts friction loss badly.
- `STAINLESS_STEEL` is **150, not** the carbon-steel 120/100. `STEEL` must not
  be used for stainless.

**Citation corrected, then verified.** These values were originally cited to
**Table A.27.2.4.8.2** (Annex A, "Suggested ε-Factor for Aged Pipe"). That annex
table is real and is where the ε values come from, but it is *explanatory*
material. The **normative** source for C is **Table 27.2.4.8.1** in the body of
Chapter 27, renumbered 28.2.4.8.1 in the 2022 edition. This was the third
citation in this repo found pointing at the wrong table; see the renumbering
warning above.

Reading the printed table changed **no existing value** — steel 120/100, copper
150, CPVC 150 and cement-lined iron 140 were all confirmed — but it resolved
every open question and added five rows:

- **Preaction** was carried as *this app's mapping* of preaction onto dry
  service. The table names it outright: "Black steel (dry systems including
  preaction)". The caveat is withdrawn.
- **CPVC** was carried as our reading of the annex's generic "Plastic" row. The
  normative row is "Plastic (listed) all" — unqualified, so listed CPVC falls
  squarely inside it. No longer treated as interpretation.
- **Ductile iron 140** had *no source at all*. It is "Cement-lined cast- or
  ductile iron" in the printed table.

ε values stay stored alongside (`epsilonFactorInch`/`epsilonFactorMm`) for
display only — Hazen-Williams takes C, not roughness, so nothing in calc-core
reads them. The annex covers fewer materials than the normative table, so the
rows it omits carry `null` rather than an invented figure.

**One secondary-source claim did not survive the printed page:** a "dry/preaction
with listed nitrogen, C=120" row. It is not in the 2019 table and was not added.
It appears to be a later-edition change.

The annex also lists **steel (new) at C=143**, not exposed here: NFPA 13 design
practice uses the aged values, and pipe age isn't a dimension this table is
keyed on. Its ε of 0.045 mm does match `reference/pipes/bi.json`'s
`absoluteRoughnessMm`, the Darcy-Weisbach figure the pipe package uses.

The calculator takes C per pipe segment, so this table is a reference/default
set only.

### `fitting-equivalent-lengths.json` — `verify`

NFPA 13 2019 **Table 27.2.3.1.1** (Equivalent Schedule 40 Steel Pipe Length
Chart), in feet, keyed by the same `nominalSize` strings as
`reference/pipes/*.json`. All 8 rows x 15 columns of published values are
entered (102 values; the dashes in the table are omitted, not zeroed).

| Fitting | 1/2 | 3/4 | 1 | 1-1/4 | 1-1/2 | 2 | 2-1/2 | 3 | 3-1/2 | 4 | 5 | 6 | 8 | 10 | 12 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 45° elbow | — | 1 | 1 | 1 | 2 | 2 | 3 | 3 | 3 | 4 | 5 | 7 | 9 | 11 | 13 |
| 90° standard elbow | 1 | 2 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 10 | 12 | 14 | 18 | 22 | 27 |
| 90° long-turn elbow | 0.5 | 1 | 2 | 2 | 2 | 3 | 4 | 5 | 5 | 6 | 8 | 9 | 13 | 16 | 18 |
| Tee or cross (flow turned 90°) | 3 | 4 | 5 | 6 | 8 | 10 | 12 | 15 | 17 | 20 | 25 | 30 | 35 | 50 | 60 |
| Butterfly valve | — | — | — | — | — | 6 | 7 | 10 | — | 12 | 9 | 10 | 12 | 19 | 21 |
| Gate valve | — | — | — | — | — | 1 | 1 | 1 | 1 | 2 | 2 | 3 | 4 | 5 | 6 |
| Vane type flow switch | — | — | 6 | 9 | 10 | 14 | 17 | 22 | — | 30 | — | 16 | 22 | 29 | 36 |
| Swing check | — | — | 5 | 7 | 9 | 11 | 14 | 16 | 19 | 22 | 27 | 32 | 45 | 55 | 65 |

**Second read complete for seven of the eight rows.** The transcription was
checked cell-for-cell against two independent reproductions of the same NFPA
chart — a 2016-edition copy (numbered 23.4.3.1.1 there) and a commercial
sprinkler design program's NFPA table. **91 of the 102 values agree exactly
across all three**, so those seven rows are now `verified` and the calculator
no longer warns when it uses them.

The 11 values that remain are the whole vane type flow switch row.

> ### ⚠ The vane type flow switch row is not confirmed
>
> **Neither reproduction contains a vane type flow switch row at all.** The
> 2016 chart has seven rows; the design program lists alarm valve, dry pipe
> valve, strainer and mushroom valve as extras but no flow switch. Our row's
> values are also the least plausible in the file — 30 ft at 4 in. then 16 ft
> at 6 in., with 5 in. absent entirely.
>
> Either the 2019 edition added the row, or it was transcribed in error.
> **Check the printed 2019 Table 27.2.3.1.1.** If the row isn't there, delete
> it rather than re-tagging it — a waterflow device's equivalent length then
> comes from its own listing, not from NFPA 13, and callers should pass
> `explicitEquivalentLengthFt`.

Published as printed, deliberately not "corrected":

- **Butterfly valve is non-monotonic**: 12 ft at 4 in. but 9 ft at 5 in. This
  was the other cell under suspicion and it is **confirmed real** — the same
  pair appears in both independent sources. A test now pins it so nobody
  "fixes" it later.
- **Swing check values are averages**, per the table's own footnote, because
  equivalent length varies with valve design.
- **1/2 in. appears at all** only because NFPA 13 Sections 29.4/29.5 allow that
  size (the table says so in its note).
- **10 in. and 12 in. are included** even though `reference/pipes/*.json` stops
  at 8 in., so the fitting data doesn't need revisiting when larger pipe is
  added.

A fitting/size combination the table leaves blank still throws
`CalculationException`; pass `explicitEquivalentLengthFt` for those.

**C factor basis — applied.** These lengths are in feet of C = 120 pipe. NFPA 13
requires rescaling them for other C factors, and the calculator now does:

```
factor = (C / 120)^1.85
```

which reproduces NFPA's published multipliers — 0.713 at C = 100, 1.16 at
C = 130, 1.33 at C = 140, 1.51 at C = 150. Note the direction: a fitting's own
loss barely depends on the pipe it sits in, so in rougher pipe (lower C, more
loss per foot) it takes *fewer* feet of that pipe to match the fitting. Dry and
preaction steel at C = 100 therefore get **shorter** equivalent lengths than the
printed chart, not longer. (`(120/C)^1.85` is the inverse and does not match
the published factors.)

Each segment row reports the factor it used as `fittingCFactorAdjustment`, so a
row can be reconciled against the printed chart. An **explicitly supplied**
`explicitEquivalentLengthFt` is never rescaled — the caller's basis is unknown,
and silently rescaling someone else's number would be worse than leaving it
alone.

### `sprinkler-k-factors.json` — `verified`

**Table 7.2.2.1** (Sprinkler Discharge Characteristics Identification), all 13
rows, K = 1.4 through 28.0, with each row's published K range, L/min/bar
equivalent, discharge as a percentage of a K-5.6 head, and thread type.

Two corrections came out of checking this against the printed table: the
earlier data listed only 10 rows (1.4, 1.9 and 28.0 were missing), and it
carried an `orificeNominal` field — **the standard's table publishes a thread
type, not an orifice size**. Those orifice values (17/32 in., 5/8 in. …) were
legacy descriptors from general knowledge and have been replaced by the
published `threadType`.

The calculator always takes K from the caller, since a listed sprinkler's
actual K comes from its data sheet and need only fall inside the published
range.

**Dry-type sprinklers:** the table's own note says these nominal values are for
sprinkler *selection*, and that dry-type sprinklers use an adjusted K for
hydraulic calculation (27.2.4.10.3). This application does not apply that
adjustment — a caller calculating a dry-type sprinkler must pass the adjusted K.

## Related data outside this directory

- **Pipe inside diameters** — `reference/pipes/bi.json` / `gi.json`
  (steel SCH40, ASME B36.10M). Sprinkler work added **3-1/2", 5" and 8"**,
  which are `verify`: the file has no per-row confidence field, so they are
  flagged here and in `reference/pipes/README.md` instead. Schedule 10 is not
  present; the file's `schedules` array already allows adding it with no code
  change.
