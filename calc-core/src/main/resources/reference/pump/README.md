# Pump TDH / Power Reference Data

Used by `PumpPowerCalculator` (`mechanical.pump` package, calc-core).

## Files

- `motor-kw-steps.json` — IEC 60072 standard electric motor power ratings
  (kW). Used to round computed shaft power up to a stockable motor size.

## Confidence

Marked `"confidence": "verify"` — standard IEC step values, but confirm
against the specific motor suppliers/catalogues you're targeting before
treating this as final for a real procurement spec. Same caveat pattern as
the fire pump suite's NEMA HP table (`reference/firepump/motor-hp-steps.json`)
— this is the SI/general-purpose equivalent, kept as a separate file/scale
rather than converting between HP and kW at runtime, since the two
standards' step values don't convert cleanly onto each other (they're
independently standardized lists, not unit conversions of the same table).

## No diesel/other driver data here

Same as the fire pump reference data: this covers electric motors only.
General-purpose pumps sized here are assumed electrically driven; if a
diesel or other driver is relevant, treat computed shaft power as the basis
and consult the engine manufacturer's lineup directly — no standard step
table exists to resolve against.
