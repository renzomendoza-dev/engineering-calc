# Patch: add hydraulic roughness data to existing pipe material files

Add a top-level `"hydraulics"` block to each of the four existing files in
`calc-core/src/main/resources/reference/pipes/` (gi.json, bi.json,
upvc.json, ppr.json). This is a PATCH to existing files, not new files —
add the block alongside the existing `"material"`, `"schedules"`, etc.
fields, do not remove or restructure anything already there.

## gi.json — add:
```json
"hydraulics": {
  "absoluteRoughnessMm": 0.15,
  "confidence": "verified",
  "source": "Typical galvanized steel pipe absolute roughness (Moody chart reference values)"
}
```

## bi.json — add:
```json
"hydraulics": {
  "absoluteRoughnessMm": 0.045,
  "confidence": "verified",
  "source": "Typical new commercial steel pipe absolute roughness (Moody chart reference values)"
}
```

## upvc.json — add:
```json
"hydraulics": {
  "absoluteRoughnessMm": 0.0015,
  "confidence": "placeholder",
  "source": "Generic PVC absolute roughness reference value — same placeholder caveat as this file's dimension data, verify against PNS 65 / manufacturer data"
}
```

## ppr.json — add:
```json
"hydraulics": {
  "absoluteRoughnessMm": 0.007,
  "confidence": "placeholder",
  "source": "Generic PPR absolute roughness reference value — same placeholder caveat as this file's dimension data"
}
```

## Resolver note

`PipeRoughnessResolver.resolve(material)` reads this block from the same
material JSON that `PipeDimensionResolver` already loads — no need for a
second file-loading pass if `JsonPipeDimensionResolver` already parses the
whole file into memory; just expose the `hydraulics.absoluteRoughnessMm`
field through a small additional interface, or fold roughness resolution
into the same loader class that already parses these files.
