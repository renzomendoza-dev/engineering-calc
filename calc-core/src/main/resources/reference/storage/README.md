# Water Storage Reference Data (`reference/storage/`)

Used by `DomesticWaterStorageCalculator` and `FireWaterStorageCalculator`
(`mechanical.storage` package, calc-core).

This folder should contain **three** JSON files. If you're only seeing one
or two, some are missing — check against this list:

| File | Used by | Confidence |
|---|---|---|
| `wsfu-demand.json` | `FixtureUnitDemandResolver` (domestic, `FIXTURE_UNIT` mode) | **verified** |
| `lpcd-consumption.json` | `PerCapitaConsumptionResolver` (domestic, `OCCUPANT_LOAD` mode) | placeholder |
| `fire-water-duration.json` | `FireWaterDurationResolver` (fire water storage) | **verified** |

---

## `wsfu-demand.json`

WSFU total -> peak demand flow (GPM), split by system type (flush tanks vs.
flushometer valves). Transcribed from **NSPC (National Standard Plumbing
Code) 2009, Table B.5.4**.

**Confidence: verified** — cross-validated against a real worked example
from the Revised National Plumbing Code of the Philippines (Annex A,
p.171): that document's own example works out to 1,866 WSFU -> 19.7 L/s.
Interpolating this table at 1,866 WSFU gives 19.66 L/s — a 0.21% deviation.
Two independently-published documents (US NSPC, PH NPC) agree almost
exactly.

**Resolver behavior**: linearly interpolate between bracketing `wsfu` rows
for the requested system type. `gpmFlushValves` is `null` below WSFU=5 (not
listed in the source table) — throw `CalculationException` rather than
extrapolate or substitute. Both system types converge above WSFU=1000 (this
is already reflected in the data, not something the resolver needs to
special-case). Throw if `totalWsfu` exceeds 10,000 (table's upper bound).

Fixture-to-WSFU weighting (i.e. "how many WSFU does one water closet
count as") is a separate concern, already covered by Table 6-5 from the
actual Philippine NPC (not part of this folder's files).

---

## `lpcd-consumption.json`

Per-capita water consumption rate (liters/person/day) by occupancy type.
Used for the `OCCUPANT_LOAD` demand mode.

**Confidence: placeholder** — generic, commonly-cited rule-of-thumb
figures, NOT sourced from a confirmed Philippine code. NSPC Chapter 10 and
Appendix B were checked and do not contain this data (NSPC assumes
continuous municipal supply, no storage-by-population methodology exists
there). Worth verifying against: National Building Code of the Philippines
(PD 1096) IRR, LWUA design standards, DPWH guidelines, or an
as-yet-unreviewed chapter of the Philippine NPC itself.

**Resolver behavior**: simple flat lookup by `occupancyType`, no
interpolation. Throw `CalculationException` if the type isn't found.

---

## `fire-water-duration.json`

Hazard classification -> required water supply duration (minutes) and hose
stream allowance (GPM). Transcribed from **NFPA 13 (2019 edition), Table
11.2.3.1.2**.

**Confidence: verified** — directly transcribed from a cited NFPA 13 table.

**Important**: Ordinary Hazard (60-90 min) and Extra Hazard (90-120 min)
are **ranges**, not single values — NFPA 13 subdivides these classes
further (Ordinary Hazard Group 1/2, Extra Hazard commodity classification)
in ways this summary table doesn't capture. `FireWaterDurationResolver`
should return both `durationMinutesMin` and `durationMinutesMax` and let
the calculator input/UI require the engineer to pick within that range (or
explicitly default to the max as a conservative choice) — never silently
collapse the range to one number. Light Hazard has no ambiguity (fixed 30
minutes, min==max).

**Note for later**: this table also carries hose stream allowance GPM per
hazard class, which isn't currently wired into `FirePumpDemandCalculator`
(that calculator takes `requiredFlow` as a direct input and assumes the
user has already included any hose stream allowance). Worth revisiting as
a future enhancement, not done here to avoid re-touching an already-shipped
calculator.

**Also note**: `reference/firepump/curve-requirements.json` and
`standard-capacities.json` (from the earlier fire pump suite) are still
marked `"confidence": "verify"` for edition-year uncertainty. Since this
duration table is confirmed as NFPA 13 2019, it's worth eventually
double-checking those two fire pump files are also consistent with the 2019
edition (or whichever edition the Philippine Fire Code IRR actually
references) for consistency across the whole fire-protection reference set.
