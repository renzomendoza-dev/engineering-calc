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

**Almost nothing in this directory is `verified` yet** — only three C factor
rows taken straight from Table A.27.2.4.8.2. The rest needs confirming against
a copy of NFPA 13 2019:

| Data | Status | What would clear it |
|---|---|---|
| Hazard presets: design area | ✅ checked | 19.3.3.1.4 — 1500 ft² light/ordinary, 2500 ft² extra |
| Hazard presets: hose + duration | ✅ checked | Table 19.3.3.1.2 — 100/30, 250/60–90, 500/90–120 |
| Hazard presets: max coverage | ✅ checked | Table 10.2.4.2.1(a)(b)(c) — 225 / 130 / 100 ft² |
| Hazard presets: **density** | `verify` ×5 | Must be read off the plotted curves in Figure 19.3.3.1.1 (p. 13‑162); not checkable from page text |
| Fitting equivalent lengths | `verify` ×8 | A second read of the transcription, especially the two non-monotonic cells |
| K-factors | `verify` ×10 | §7.2.1–7.2.2 on **p. 13‑49** (the page supplied started at 7.2.3) |
| C factors: preaction, CPVC, ductile iron | `verify` ×3 | Mapping decisions + a source for ductile iron 140 |
| Pipe 3-1/2" / 5" / 8" | `verify` | ASME B36.10M Sch 40 dimensions (not an NFPA document) |

Rows stay at `verify` while **any** of their fields is unconfirmed, so the
hazard presets are still tagged `verify` even though four of their five fields
are now checked — density is the holdout. Per-field detail is in each row's
`sourceNote`.

**Citations were renumbered in the 2019 edition** and ours were written against
the older numbering. Confirmed so far: the fitting chart is 27.2.3.1.1 (was
23.4.3.1.1), and hose stream/duration is **Table 19.3.3.1.2** (was 11.2.3.1.2).
`reference/storage/fire-water-duration.json` still carries that old 11.2.3.1.2
citation for the same data while being labelled 2019 — its numbers match, only
the pointer is stale.

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

Sources to confirm: Figure 19.3.3.1.1 (density/area curves),
Table 11.2.3.1.2 (hose stream + duration), Table 10.2.4.2.1 (protection areas).

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

### `hazen-williams-c.json` — `verify`

| Pipe type | Service | C |
|---|---|---|
| Steel | wet | 120 |
| Steel | dry / preaction | 100 |
| Copper | any | 150 |
| CPVC | any | 150 |
| Ductile iron (underground, cement lined) | any | 140 |

Source: **Table A.27.2.4.8.2** (Annex A, "Suggested ε-Factor for Aged Pipe"),
which pairs each C with an equivalent absolute roughness. Those ε values are
stored alongside (`epsilonFactorInch`/`epsilonFactorMm`) for display only —
Hazen-Williams takes C, not roughness, so nothing in calc-core reads them.

Per-row confidence differs here:

- `verified` — straight from the annex: steel wet (120, ε 0.004 in.), steel dry
  (100, ε 0.015 in.), copper (150, ε 0.000084 in.).
- `verify` — **our mapping**, not the annex's wording: preaction treated as dry
  service, and CPVC read as the annex's "Plastic" row. A specific listed CPVC
  product's C comes from its listing.
- `verify` — ductile iron 140 isn't in the annex at all and still needs a
  source; its ε is `null` rather than invented.

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

Tagged `verify` because it was transcribed from an image of the printed page —
the source is authoritative, the transcription has not had a second pass. The
calculator warns on every calculation that uses one of these values until the
tag becomes `verified`.

Transcribed as published, deliberately not "corrected":

- **Butterfly valve is non-monotonic**: 12 ft at 4 in. but 9 ft at 5 in.
- **Vane type flow switch is non-monotonic**: 30 ft at 4 in. but 16 ft at
  6 in. (5 in. is not published). Together with the butterfly row, these are
  the two cells most worth re-reading against the printed page — a
  monotonicity check over the whole chart flags exactly these two and nothing
  else.
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

### `sprinkler-k-factors.json` — `verify`

Nominal K-factors 2.8 through 25.2 for a form's dropdown. Orifice sizes are
recorded only for 5.6 (1/2 in.), 8.0 (17/32 in.), 11.2 (5/8 in.) and
14.0 (3/4 in.); the rest are `null` rather than guessed. The calculator always
takes K from the caller, since a listed sprinkler's actual K comes from its
data sheet.

## Related data outside this directory

- **Pipe inside diameters** — `reference/pipes/bi.json` / `gi.json`
  (steel SCH40, ASME B36.10M). Sprinkler work added **3-1/2", 5" and 8"**,
  which are `verify`: the file has no per-row confidence field, so they are
  flagged here and in `reference/pipes/README.md` instead. Schedule 10 is not
  present; the file's `schedules` array already allows adding it with no code
  change.
