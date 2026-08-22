# Duct Sizing Reference Data

Used by `DuctSizingCalculator` (`mechanical.duct` package, calc-core).

## Files

- `duct-roughness.json` — absolute roughness (mm) by duct material, used by
  `DuctRoughnessResolver` in the Colebrook-White / Swamee-Jain friction
  factor calculation. **Confidence: verified** — transcribed from ASHRAE
  Fundamentals 2017, Table 1. Two entries (galvanized spiral steel,
  flexible duct) are cross-validated exactly against this same chapter's
  own worked Example 6.
- `duct-velocity-limits.json` — recommended maximum airflow velocity (m/s)
  by duct location, acoustic rating (NC/RC), and duct shape, used by
  `DuctVelocityLimitResolver` to suggest sensible defaults for the
  `VELOCITY` sizing method. **Confidence: verified** — transcribed from
  ASHRAE Fundamentals 2017, Table 12. These are **acoustic design targets,
  not code-mandated limits** — exceeding them isn't a compliance failure,
  it risks noticeable duct noise.

Both replace an earlier `"confidence": "verify"` placeholder version of
`duct-roughness.json` that used generic, non-ASHRAE-sourced figures — that
version should be considered superseded.

## Some roughness entries required a representative-value choice

Where ASHRAE's Table 1 gives a range without a single stated design value
(e.g. Aluminum: 0.037-0.061 mm, Concrete: 0.30-3.0 mm), a representative
point value was picked using engineering judgment — documented per-entry in
each material's `sourceNote` field. The range itself is ASHRAE-sourced; only
the specific point chosen within that range is a judgment call. Concrete's
range is unusually wide (10x from low to high) — worth exposing the full
range in the UI rather than silently defaulting to the single picked value
for that material specifically.

## Air properties: cross-validated, still no reference file needed

`AirPropertiesResolver` (analytical: ideal gas law + International Standard
Atmosphere + Sutherland's Law, no JSON) was cross-checked against ASHRAE
Fundamentals Chapter 3's stated standard air properties (20°C, 101.325 kPa:
density 1.21 kg/m3, viscosity 18.1 uN*s/m2). The analytical formulas
reproduce viscosity almost exactly (18.13 vs. 18.1 uN*s/m2) and density
within normal rounding (1.204 vs. 1.21 kg/m3). No change needed to this
resolver's design — still no JSON file, still no humidity correction (dry
air assumption).

## Not yet incorporated: flexible duct compression correction

ASHRAE's Equations (22)-(23) (p.21.6) give a Pressure Drop Correction
Factor (PDCF) for flexible duct that isn't fully stretched:

```
PDCF = 1 + 0.58 * Kc * e^(-0.00496*D)
Kc = ((L_FE - L) / L_FE) * 100
```

where `L_FE` is the fully-extended length, `L` is the actual installed
length, and `D` is the duct diameter in mm. This is a real, verified,
citable enhancement — flexible duct resistance increases substantially
when compressed (ASHRAE's own Example 6 shows a 30%-compressed flexible
duct having ~795% higher pressure loss than the same duct fully stretched).

**Not implemented in the initial `DuctSizingCalculator` build** — the
calculator currently treats flexible duct via its stretched-condition
roughness value only (0.9 mm), same as any other material. Worth adding as
a follow-up enhancement: an optional `installedLength` vs. `fullyExtendedLength`
input pair on `FLEXIBLE_DUCT_STRETCHED` selections, feeding this correction
factor into the friction loss result when the two lengths differ.
