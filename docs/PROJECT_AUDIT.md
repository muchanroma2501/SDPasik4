# Project Audit

## Baseline

- Java 17 Maven application using Spring Boot 3.3.5 and Spring Web; Maven Wrapper is present.
- The clean baseline command `.\mvnw.cmd test` succeeds, but the repository initially has no automated tests.
- Git was already dirty at audit time: `README.md`, `FullDiagnosticReport.java`, `HtmlReportExporter.java`, and `static/index.html` were modified, and the Assignment 4 stress-test facade/controller files were untracked. Those existing changes are retained and integrated.

## Existing behavior and design patterns

- Assignment 2 contains the static `ComponentCatalog`, Intel/NVIDIA and AMD abstract factories, immutable `Computer`, Builder implementations, and gaming/budget/workstation order factories.
- `/api/pc/presets`, `/api/pc/catalog`, `/api/pc/custom`, and `/api/pc/build` are implemented by `PCConfiguratorController`. Custom selection is assembled there; named builds use the Factory Method order factories.
- `/api/reports/export` uses the Assignment 3 Bridge reports/exporters, but only exports a newly created default gaming PC. The frontend currently calls that GET endpoint.
- Assignment 4 already contains a separate simulated CPU/GPU/thermal stress-test facade and `/api/diagnostics/run`; these are retained as their own demonstration workflow.
- `BridgePatternDemo` is a Spring `CommandLineRunner`, so it runs during normal application startup. `ComputerOrderFactory` also writes build progress to standard output.
- The existing static page is one large `index.html` with inline CSS and JavaScript, depends on remote font/icon CDNs, and uses browser-side power/socket checks as build gates. It includes useful platform, component, RGB, history, stress-test, and report controls.
- Cooling models exist in `ComponentCatalog`, and the Builder supports a cooling selection, but the custom request and frontend do not yet expose it.

## Implementation constraints

- Reuse the existing component catalog as the hardware source and the Builder/Factory/Bridge implementations as the underlying pattern participants.
- Keep the existing API paths and response fields, add structured server diagnostics, and add a POST report flow that uses the submitted configuration.
- Preserve the existing stress-test facade and frontend workflow while separating compatibility diagnostics from simulated benchmark/thermal tests.
