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
- **GI / BI**: ANSI B36.10M Schedule 40 — standard, stable, safe to use
  as-is. BI pipe uses identical dimensions to GI (same steel pipe standard);
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
