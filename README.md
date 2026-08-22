# engineering-calc

A Java multi-module Maven project providing stateless engineering calculators — electrical (PEC/NEC-aligned), mechanical (pipe hydraulics, fire pumps), acoustics (NFPA 72), and smoke control (NFPA 92) — behind a REST API. No persistence, no authentication: every endpoint takes an input, returns a calculated result.

## Modules

| Module | Type | Description |
|---|---|---|
| [`calc-core`](calc-core) | Plain Java library | All calculation logic, domain validation, and reference-data lookups. Zero Spring dependency by design — usable standalone. |
| [`calc-api`](calc-api) | Spring Boot 4.1.0 app | Thin REST layer wrapping `calc-core`. `Controller → Service → Mapper → DTO`. |

`calc-api` depends on `calc-core`; `calc-core` never depends on `calc-api`.

## Tech stack

- **Java 21**
- **Spring Boot 4.1.0** (`calc-api` only — `spring-boot-starter-webmvc`, `spring-boot-starter-validation`)
- **[Indriya](https://github.com/unitsofmeasurement/indriya) 2.2.4** (JSR-385) — typed physical quantities (length, pressure, flow rate, etc.) for calc-core inputs/outputs where a quantity has a natural SI type
- **Jackson** — reference-data JSON deserialization (`calc-core`) and REST payloads (`calc-api`, via the Spring Boot BOM)
- **springdoc-openapi 3.1.0** — OpenAPI docs + Swagger UI
- **JUnit 5** (`junit-jupiter` 6.0.3)
- **Maven** (multi-module reactor build)

## Architecture

```
calc-core/
  com.renzoproject.calc.core/
    Calculator.java              interface Calculator<Input, Result> { Result calculate(Input input); }
    electrical/                  voltage drop, conduit fill, wire sizing, motor FLC/locked-rotor/conductor sizing
    mechanical/                  pipe velocity/pressure loss, pump TDH/power, fire pump sizing suite, water storage (domestic/fire)
    acoustics/                   distance attenuation, fire alarm audibility (NFPA 72)
    smokecontrol/                smoke production (plume), t-squared growth variant, natural vent area (NFPA 92)
    common/                      shared reference data (air properties) reusable across domains
    exception/                   CalculationException — the one exception type every calculator throws
  src/main/resources/reference/  JSON reference tables (PEC tables, pipe materials, fire pump curves, NFPA thresholds, ...)

calc-api/
  com.renzoproject.calc_api/
    electrical/, mechanical/, acoustics/, smokecontrol/
      {feature}/
        {Feature}Controller.java   @RestController — one thin method per endpoint, @Valid request body
        {Feature}Service.java      instantiates the calc-core Calculator, no business logic
        {Feature}Mapper.java       pure Request→Input / Result→Response mapping, no logic
        {Feature}Request.java      inbound DTO — Bean Validation covers structural bounds only
        {Feature}Response.java     outbound DTO — mirrors the calc-core Result record
    config/                       CORS policy, OpenAPI metadata
    exception/                    GlobalExceptionHandler — the one @RestControllerAdvice for the whole API
```

**Validation is layered on purpose**: Bean Validation (`@NotNull`, `@Positive`, ...) at the DTO boundary catches structural problems fast with field-level 400s. Domain rules that calc-core itself must enforce regardless of caller (e.g. "fire base height must be below ceiling height") live exclusively in the record's compact constructor and throw `CalculationException`, which `GlobalExceptionHandler` turns into a 400. Neither layer duplicates the other.

## API reference

Interactive docs (Swagger UI) are served at the application root — `http://localhost:8080/` once running. Raw OpenAPI JSON is at `/v3/api-docs`.

### Electrical — `/api/electrical`

| Method | Path | Calculator |
|---|---|---|
| POST | `/voltage-drop` | Conductor voltage drop (DC/1φ/3φ, exact impedance method) |
| POST | `/conduit-fill` | Smallest conduit trade size for a given conductor set (PEC 10.1.1.1) |
| POST | `/wire-sizing` | Smallest conductor size meeting PEC ampacity requirements |
| POST | `/motor-flc` | Motor full-load current + minimum conductor ampacity (PEC Art. 4.30) |
| POST | `/locked-rotor` | Locked-rotor current for disconnect/controller sizing |
| POST | `/motor-conductor-sizing` | One-step branch-circuit sizing from HP/voltage (chains FLC → wire sizing) |
| GET | `/reference/*` | 22 lookup endpoints backing the above (conductor sizes/materials, ampacity/impedance/adjustment tables, insulation types, motor HP/voltage tables, ...) |

### Mechanical — `/api/mechanical`

| Method | Path | Calculator |
|---|---|---|
| POST | `/pipe-velocity` | Pipe flow velocity, `V = Q / A` |
| POST | `/pipe-pressure-loss` | Darcy-Weisbach friction loss (Colebrook-White / Swamee-Jain) |
| POST | `/pump/tdh` | Total Dynamic Head (static + friction + residual + velocity head) |
| POST | `/pump/power` | Hydraulic/shaft power and motor sizing from a duty point |
| POST | `/firepump/demand` | Required fire pump flow/pressure from hydraulic demand |
| POST | `/firepump/capacity` | Standard fire pump capacity rounding |
| POST | `/firepump/curve-validation` | NFPA 20 pump curve shape validation (rated/churn/overload) |
| POST | `/firepump/power` | Fire pump brake horsepower + recommended motor size |
| POST | `/storage/domestic` | Domestic water storage volume from occupant count (LPCD) or fixture units (WSFU) |
| POST | `/storage/fire` | Fire water storage volume from rated pump flow + NFPA 13 hazard classification duration |
| GET | `/reference/pipe-materials` | Supported pipe materials |
| GET | `/reference/lpcd-consumption-table` | Per-capita consumption by occupancy type (domestic storage) |
| GET | `/reference/wsfu-demand-table` | WSFU → peak demand, NSPC 2009 Table B.5.4 (domestic storage) |
| GET | `/reference/fire-water-duration-table` | Hazard classification → duration range, NFPA 13 Table 11.2.3.1.2 (fire storage) |

### Acoustics — `/api/acoustics`

| Method | Path | Calculator |
|---|---|---|
| POST | `/distance-attenuation` | Sound pressure level at a target distance (inverse-square law) |
| POST | `/fire-alarm-audibility` | NFPA 72 audibility compliance check for a notification appliance |

### Smoke control — `/api/smoke-control`

| Method | Path | Calculator |
|---|---|---|
| POST | `/plume` | NFPA 92 Heskestad plume smoke production (steady design fire) |
| POST | `/plume-tsquared` | Same plume correlation, driven by a t-squared growth fire capped at a peak HRR |
| POST | `/vent-area` | Required natural smoke vent area (buoyancy-driven vent sizing) |

## Running locally

### Maven

```bash
./calc-api/mvnw -f pom.xml -pl calc-core,calc-api -am clean install
./calc-api/mvnw -f pom.xml -pl calc-api spring-boot:run
```

The API comes up on **http://localhost:8080**, with Swagger UI at the root.

### Docker

```bash
docker build -t engineering-calc .
docker run -p 8080:8080 engineering-calc
```

The included `Dockerfile` is a multi-stage build: a Maven/Temurin builder stage runs the reactor build (`calc-core` + `calc-api` together, so the inter-module dependency resolves without a separate install step), then the resulting Spring Boot fat jar is copied into a slim `eclipse-temurin:21-jre-alpine` runtime image and run as a non-root user.

### CORS

`calc-api` allows `GET`/`POST` from `http://localhost:3000` only (`config/CorsConfig.java`) — intended for a local frontend during development. No credentials/cookies are enabled (v1 has no auth).

## Testing

```bash
./calc-api/mvnw -f pom.xml -pl calc-core,calc-api -am test
```

Every calculator has a corresponding `*CalculatorTest` in `calc-core` (unit tests, often including a spot-check against a hand-verified or real-project reference calculation) and a `*ControllerTest` in `calc-api` (`@SpringBootTest` + `MockMvc`, exercising the full HTTP round trip including validation failure paths).

## Conventions

- **One exception type**: `CalculationException` (calc-core) is the only way a calculation fails; `GlobalExceptionHandler` (calc-api) is the only place that translates it to an HTTP response. No feature introduces its own exception type or advice class.
- **Reference data lives in JSON**, loaded once per resolver instance and cached (`calc-core/src/main/resources/reference/**`), not hardcoded in calculators.
- **Sealed interfaces over nullable flat fields** wherever a calculator's input or result genuinely branches into shapes with different data (e.g. pipe sizing mode, plume regime) — see `DiameterSpec`, `PlumeRegime`.
- **calc-api DTOs never reuse calc-core's domain types directly** in a response — each response DTO mirrors the corresponding `Result` record field-for-field, keeping the HTTP contract decoupled from internal refactors.
