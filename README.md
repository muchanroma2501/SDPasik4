# PC Build Studio

PC Build Studio is a Java 17 and Spring Boot web application for selecting PC hardware, validating compatibility on the server, assembling builds, and exporting reports. The browser interface is Russian-language, responsive, and uses plain HTML, CSS, and JavaScript. Hardware models and metadata come from the existing in-memory component catalog; no database or frontend build pipeline is required.

Repository: <https://github.com/muchanroma2501/SDPasik2>

## Requirements and commands

- JDK 17 or newer
- Maven 3.6+ or the included Maven Wrapper

Run from the repository root:

```powershell
.\mvnw.cmd spring-boot:run
```

```bash
./mvnw spring-boot:run
```

Open <http://localhost:8080>. Package and run the executable JAR with:

```powershell
.\mvnw.cmd clean package
java -jar target/sdpasik2-1.0.0.jar
```

Run automated tests with `.\mvnw.cmd test` (Windows) or `./mvnw test` (Unix).

## API

The supported platform values are exactly `intel_nvidia` and `amd`. Preset build types are `gaming`, `budget`, and `workstation`. Report types are `summary` and `full`; formats are `html` and `json`.

| Method | Endpoint | Behavior |
|---|---|---|
| `GET` | `/api/pc/presets` | Returns the gaming, office/budget, and workstation configurations with component specifications. |
| `GET` | `/api/pc/catalog?ecosystem=intel_nvidia` | Returns server-supported CPUs, GPUs, motherboards, RAM, storage, PSUs, and cooling models for the selected platform. The default ecosystem is `intel_nvidia`. |
| `POST` | `/api/pc/diagnose` | Returns structured `READY`, `REVIEW`, or `BLOCKED` diagnostics without constructing a PC. Compatibility problems are returned as JSON checks. |
| `POST` | `/api/pc/custom` | Validates and builds the submitted custom selection. Required fields: `ecosystem`, `cpu`, `gpu`, `motherboard`, `ram`, `storage`, and `powerSupply`; `coolingSystem` is optional and `hasRGB` is a boolean. |
| `POST` | `/api/pc/build` | Builds an existing named pipeline. Requires `ecosystem`, `buildType`, `ram`, and `storage`; `coolingSystem` is optional and `hasRGB` is a boolean. |
| `GET` | `/api/reports/export?reportType=summary&format=html` | Backward-compatible Bridge export using the legacy default gaming configuration. |
| `POST` | `/api/reports/export` | Exports the actual submitted selection with Bridge report and exporter choices. |
| `POST` | `/api/diagnostics/run` | Runs the separate simulated CPU/GPU/thermal stress-test facade for the submitted configuration and returns HTML. |

Example diagnostics/build request:

```json
{
  "ecosystem": "intel_nvidia",
  "cpu": "Intel Core i7-14700K",
  "gpu": "NVIDIA GeForce RTX 4070 Ti 12GB",
  "motherboard": "Z790 ATX",
  "ram": "32GB DDR5",
  "storage": "1TB NVMe SSD",
  "powerSupply": "850W 80+ Gold",
  "coolingSystem": "280mm AIO Liquid Cooling",
  "hasRGB": true
}
```

Example report request (the selection is nested under `configuration`):

```json
{
  "reportType": "full",
  "format": "json",
  "configuration": {
    "ecosystem": "amd",
    "cpu": "AMD Ryzen 5 7600",
    "gpu": "AMD Radeon RX 6600 8GB",
    "motherboard": "B650 ATX",
    "ram": "32GB DDR5",
    "storage": "1TB NVMe SSD",
    "powerSupply": "650W 80+ Gold",
    "coolingSystem": "Stock AMD Wraith Stealth",
    "hasRGB": false
  }
}
```

Malformed JSON and invalid selections return readable JSON errors. Invalid or unknown input is rejected with HTTP 400; a well-formed but incompatible build is rejected with HTTP 422. The diagnose endpoint is intentionally different: it returns HTTP 200 with a `BLOCKED` result so clients can display each failed check.

## Assignment #4 — configuration Facade

`com.aitu.sdp.assignment4.facade.PCConfiguratorFacade` is the high-level API used by `PCConfiguratorController`. It coordinates the `ComponentFactoryProvider`, the real `ComponentCatalog`, `BuildDiagnosticsService`/`CompatibilityChecker`, the existing Factory Method order factories, and the existing Builder. It resolves platform and component models, obtains server diagnostics, blocks errors, permits warnings, and returns the existing `PCResponse` fields plus diagnostics and power values. The controller only binds HTTP routes and transport DTOs.

The facade does not replace its collaborators: the catalog, factories, builders, diagnostics, and order factories remain independently usable. None of those subsystem classes refers back to the facade. `PCReportFacade` is a distinct facade for Assignment 3 report composition. It validates and builds the submitted configuration through the configurator facade, then connects an existing Bridge report type to the requested exporter. The legacy GET report stays available.

### Educational power rules

These are deterministic educational estimates, not guaranteed manufacturer recommendations:

- `requiredPowerWatts = CPU TDP + GPU TDP + 150`
- `recommendedPowerWatts = ceil(requiredPowerWatts * 1.10)`
- PSU below required power: blocking `ERROR`
- PSU at or above required power but below recommended power: allowed `WARNING` (`REVIEW`)
- PSU at or above recommended power: `PASS`
- CPU/motherboard socket mismatch, unknown required models, and platform/family mismatch block construction.

The Builder and diagnostics use the shared `PowerRequirementCalculator`. The catalog contains no RAM-generation compatibility metadata, so the application does not claim to validate RAM generation, thermals, benchmarks, FPS, or performance in the compatibility diagnostic.

## Existing pattern assignments

- **Abstract Factory:** `ComponentFactory`, `IntelNvidiaComponentFactory`, and `AMDComponentFactory` create hardware from a compatible component family.
- **Factory Method:** `ComputerOrderFactory` and the gaming, budget, and workstation order factories select a concrete Builder pipeline.
- **Builder:** `ComputerBuilder` implementations construct the immutable `Computer` and retain construction-time required-field, socket, and minimum-power validation.
- **Bridge:** `ComputerReport` refinements (`SummarySpecsReport`, `FullDiagnosticReport`) and `ReportExporter` implementations (`HtmlReportExporter`, `JsonReportExporter`) vary report content and output format independently.
- **Facade:** `PCConfiguratorFacade` coordinates the normal PC configuration workflow. The pre-existing `SystemStressTestFacade` remains a separate assignment demonstration for simulated CPU, GPU, and thermal collaborators; its stress metrics are not compatibility or hardware certification.

`BridgePatternDemo` remains runnable as a standalone Java demonstration and is not invoked during Spring Boot startup.

## Frontend and browser storage

The static UI is organized as `src/main/resources/static/index.html`, `css/app.css`, and `js/app.js`. It retrieves both platform catalogs and the presets from the API, debounces diagnostics, discards stale requests, and leaves final compatibility decisions to the server. HTML report previews use a sandboxed iframe; data values are rendered with text nodes rather than interpolated into HTML. Recent actions and configurations are saved in browser `localStorage`, with load/clear controls.

## Tests

`.\mvnw.cmd test` runs:

- Unit tests for model resolution, sockets, minimum and recommended PSU thresholds, unknown models, invalid platform values, and facade build/rejection behavior.
- Spring MockMvc tests for presets, both catalogs, diagnostics, custom and named builds, report export for the requested components, malformed JSON, and unsupported content types.

## Project structure

```text
com.aitu.sdp
├── assignment2          # Hardware model, catalog, factories, Builder, HTTP DTOs
├── assignment3          # Bridge reports and report controller
└── assignment4          # Configuration/report facades, diagnostics, stress-test facade
src/main/resources/static
├── css/app.css
├── js/app.js
└── index.html
docs                    # Audit, UML, assignment report draft, defense notes
```
