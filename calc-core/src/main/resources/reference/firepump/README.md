# Fire Pump Reference Data

Used by the fire pump sizing suite in `calc-core` (`mechanical.firepump`
package). Same principle as `reference/pipes/`: reference/code data lives in
JSON, calculators stay pure computation, nothing is hardcoded into Java.

## Files

- `standard-capacities.json` — NFPA 20 listed fire pump rated capacities
  (GPM). Used by `FirePumpCapacityResolver` to round a computed demand flow
  up to the nearest standard size.
- `curve-requirements.json` — the NFPA 20 percentage rule for acceptable
  pump curve shape (churn pressure ceiling, overload flow/pressure floor).
  Used by `FirePumpCurveValidationCalculator`.
- `motor-hp-steps.json` — standard NEMA electric motor horsepower steps.
  Used by the driver-sizing resolver in `FirePumpPowerCalculator` to round
  computed brake horsepower up to a stockable motor size.

## Confidence — read before trusting

All three files are marked `"confidence": "verify"`, not `"verified"`.
Unlike the pipe dimension standards (ANSI B36.10M), NFPA 20 is periodically
revised and the Philippine Fire Code IRR references a specific edition —
confirm which edition applies to your jurisdiction/project before treating
these numbers as final for an actual permit submission. The percentages
(140% churn ceiling, 150%/65% overload rule) have been stable across recent
NFPA 20 editions but verify against the edition you're citing.

`motor-hp-steps.json` covers **electric motor drivers only** (NEMA steps).
Diesel engine drivers — common and often code-preferred for fire pumps —
don't follow a clean standard step table; they follow whatever the engine
manufacturer offers. `FirePumpPowerCalculator`'s driver recommendation
should note this distinction rather than silently applying the electric
motor table to a diesel-driven pump. Treat the diesel case as "round BHP up,
then consult manufacturer's engine lineup" rather than a resolver lookup.

## Schema

### `standard-capacities.json`
```json
{
  "standard": "NFPA 20 (edition TBD — verify against your project's applicable code year)",
  "confidence": "verify",
  "unit": "GPM",
  "capacities": [25, 50, 100, ...]
}
```

### `curve-requirements.json`
```json
{
  "standard": "NFPA 20 (edition TBD — verify)",
  "confidence": "verify",
  "churnMaxPercentOfRated": 140,
  "overloadFlowPercentOfRated": 150,
  "overloadMinPressurePercentOfRated": 65
}
```

### `motor-hp-steps.json`
```json
{
  "standard": "NEMA standard motor horsepower ratings",
  "confidence": "verify",
  "driverType": "ELECTRIC",
  "unit": "HP",
  "steps": [1, 1.5, 2, 3, 5, ...]
}
```
