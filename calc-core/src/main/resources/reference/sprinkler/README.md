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

**Nothing in this directory is `verified` yet.** The list below is the full set
to confirm against a copy of NFPA 13 2019.

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

The calculator takes C per pipe segment, so this table is a reference/default
set only.

### `fitting-equivalent-lengths.json` — `placeholder`, intentionally empty

Schema only: fitting type x nominal size -> equivalent length in feet, keyed by
the same `nominalSize` strings as `reference/pipes/*.json`. **No values are
entered**, because guessing an equivalent length silently changes every
downstream pressure.

To populate: NFPA 13 2019 **Table 23.4.3.1.1**. Until then, either
- pass `explicitEquivalentLengthFt` on the fitting, or
- leave the segment's fitting list empty.

A segment that references a fitting type/size with no published length throws
`CalculationException`.

**C factor basis (open item):** published equivalent lengths assume C = 120.
NFPA 13 requires multiplying them by `(120/C)^1.85` for other C factors (about
0.713 for C = 100). The calculator does **not** apply that adjustment yet —
with the table empty there is nothing to adjust. Decide this when the table is
entered; for a dry system (C = 100) it matters.

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
