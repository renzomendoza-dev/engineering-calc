# Fluid Properties Reference Data

Used by `FluidPropertiesResolver` in `PipePressureLossCalculator`
(`mechanical.pipe` package, calc-core). One file per fluid, named
`{fluid-key}.json` (e.g. `water.json`), same principle as `reference/pipes/`.

## Schema

```json
{
  "fluid": "WATER",
  "fluidName": "Water",
  "source": "...",
  "confidence": "verified | placeholder",
  "densityUnit": "kg/m3",
  "viscosityUnit": "Pa.s",
  "properties": [
    { "temperatureC": 20, "density": 998.2, "dynamicViscosity": 0.001002 }
  ]
}
```

`FluidPropertiesResolver.resolve(fluidKey, temperature)` should:
- Find the two bracketing `temperatureC` rows and **linearly interpolate**
  both `density` and `dynamicViscosity` — same interpolation principle
  used elsewhere in this app (fire pump curve, etc.).
- Throw `CalculationException` if the requested temperature falls outside
  the table's range — never extrapolate.

## Current coverage

- `water.json` — verified, standard textbook density/viscosity values,
  0-100 degC. Safe to use as-is.

## Adding a fluid later

Same shape, new file (e.g. `glycol-30pct.json`, `diesel.json`). Mark
`"confidence": "placeholder"` until the source values are confirmed against
a real datasheet, matching the convention already used for uPVC/PPR pipe
dimensions and the fire pump reference data.
