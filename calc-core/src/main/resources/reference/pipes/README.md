# Pipe Dimension Reference Data

Used by `PipeDimensionResolver` in `calc-core` to resolve a nominal pipe size
(or a calculated raw diameter) to real internal/outer diameters.

## File layout

One JSON file per material, named `{material-code}.json`:

- `gi.json`   — Galvanized Iron / Galvanized Steel pipe (ANSI/ASME B36.10M)
- `bi.json`   — Black Iron (uncoated steel) pipe (ANSI/ASME B36.10M — same
  dimensions as GI, different material/roughness flag downstream)
- `upvc.json` — unplasticized PVC pressure pipe
- `ppr.json`  — Polypropylene Random Copolymer pressure pipe

Add more by dropping a new `{code}.json` file with the same shape — no code
change needed as long as `PipeDimensionResolver` loads the directory
dynamically rather than hardcoding filenames.

## JSON shape

```json
{
  "material": "GI",
  "materialName": "Galvanized Iron Pipe",
  "standard": "ANSI/ASME B36.10M",
  "source": "Standard NPS Schedule dimension tables (high confidence)",
  "confidence": "verified",
  "schedules": [
    {
      "schedule": "SCH40",
      "sizes": [
        {
          "nominalSize": "1/2",
          "nominalLabel": "1/2\" (DN15)",
          "dn": 15,
          "outsideDiameterMm": 21.3,
          "wallThicknessMm": 2.77,
          "internalDiameterMm": 15.76
        }
      ]
    }
  ]
}
```

### Field notes

- `nominalSize` — machine-friendly key (e.g. `"1/2"`, `"2"`, `"110"` for
  metric-OD materials like PPR). Used as the lookup key from
  `NominalSize.nominalLabel()` in `DiameterSpec`.
- `nominalLabel` — human-readable, shown in frontend dropdowns.
- `dn` — nominal diameter in mm (DN), useful for metric-first materials.
- `internalDiameterMm` — the value actually used in `V = Q/A` math. This is
  the whole reason the resolver exists — never let a calculator use
  `nominalSize` as if it were the bore.
- `confidence` — `"verified"` (established standard, safe to trust) or
  `"placeholder"` (generic/international figures, NOT confirmed against a
  specific PH manufacturer datasheet or PNS standard — replace before
  relying on it for real design work).

## Known gaps / to replace

- **uPVC**: populated with generic ASTM D1785 Schedule 40 values as a
  placeholder. PH plumbing/water piping is more commonly specified to
  **PNS 65:1996** and manufacturer-specific data (Neltex, Atlantic, Moldex
  publish PDF datasheets on their sites). Swap `upvc.json` once you have
  those — PNS 65 isn't freely published like ASTM/ISO standards.
- **PPR**: populated with widely-cited DIN 8077/ISO 15874 PN10/PN16/PN20
  figures as a placeholder, low confidence on exact wall thickness per size.
  Recommend cross-checking against a specific brand's datasheet
  (e.g. a local PPR supplier catalog) before treating as final.
- **GI / BI, 3-1/2" / 5" / 8"**: added for sprinkler hydraulic calculations,
  which need steel sizes up to 8". Still `verify` — `PipeSizeRow` has no
  per-row confidence field, so they are flagged here rather than in the JSON.
  The three rows:

  | NPS | OD | Wall | ID |
  |---|---|---|---|
  | 3-1/2" | 101.6 mm (4.000") | 5.74 mm (0.226") | 90.12 mm |
  | 5" | 141.3 mm (5.563") | 6.55 mm (0.258") | 128.20 mm |
  | 8" | 219.1 mm (8.625") | 8.18 mm (0.322") | 202.74 mm |

  Checks done so far, none of which is a primary source:
  - `ID = OD − 2 × wall` holds on **every row of both files**, not just these
    three, so there is no arithmetic slip anywhere.
  - All three ODs are exact standard inch values.
  - All three OD/wall pairs match a published vendor dimension chart.
  - Schedule 40 equals STD wall for NPS 1/8 through 10, so an STD table
    confirms these rows too.

  What would clear them: **ASME B36.10M** itself, or any engineering handbook
  reproducing it (Crane TP-410 Appendix B, ASHRAE Fundamentals ch. 22,
  Machinery's Handbook) or a pipe manufacturer's catalogue. Note 3-1/2" and 5"
  are the two sizes most online charts skip, which is exactly why they are the
  ones still open.

  Note `resolveNextStandardSize` can now return these, so a design-mode pipe
  velocity calculation that previously failed with "no size large enough" may
  now resolve to 5" or 8".
- **GI / BI (1/8" through 3", 4", 6")**: ANSI B36.10M Schedule 40 — standard,
  stable, safe to use as-is. Their recorded provenance is only "standard NPS
  schedule dimension tables", so whatever source clears the three sizes above
  is worth running over the whole file in one pass. BI pipe uses identical dimensions to GI (same steel pipe standard);
  the separate file exists so `PipeDimensionResolver` and the future
  pressure-loss calculator's roughness lookup can key on material
  independently (BI has different absolute roughness than GI once coated).

## Resolver rounding rule

`resolveNextStandardSize(calculatedMinDiameter, material, schedule)` should
select the **smallest size whose `internalDiameterMm` is >= the calculated
minimum diameter**, i.e. round up. If no size in the given
material/schedule is large enough, throw `CalculationException` rather than
silently returning the largest available size (per your earlier
edge-case decision).
