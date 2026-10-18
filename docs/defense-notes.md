# Defense Notes — Assignment #4

> Short practice answers. Adjust them to your own words before the defense.

## What is the Facade pattern?

**English:** A Facade gives a simple interface to a group of classes. A client can use one clear operation instead of coordinating many steps.

**По-русски:** Фасад предоставляет простой интерфейс к группе классов и скрывает последовательность вызовов.

## Why is a facade useful in this project?

**English:** A PC build needs a platform factory, catalog lookup, diagnostics, and a Builder. `PCConfiguratorFacade` coordinates these steps for the REST controller.

**По-русски:** Контроллеру не нужно знать порядок выбора фабрики, поиска компонентов, проверки и сборки.

## Which classes are part of the subsystem?

**English:** The subsystem includes `ComponentFactoryProvider`, `ComponentCatalog`, `BuildDiagnosticsService`, `CompatibilityChecker`, the existing factories, order factories, and Builders.

**По-русски:** Эти классы выполняют реальные операции, которые объединяет фасад.

## How does the facade simplify the client?

**English:** The controller calls methods such as `buildCustom` and `diagnose`. It does not create factories or Builders.

**По-русски:** Клиент передаёт DTO и получает ответ; детали координации скрыты.

## Why do subsystem classes not depend on the facade?

**English:** The facade depends on the subsystem, not the other way around. This avoids a circular dependency and keeps the subsystem reusable.

**По-русски:** Направление зависимости только от фасада к подсистемам; обратной ссылки нет.

## Can advanced clients use subsystem classes directly?

**English:** Yes. Catalog, factory, Builder, order-factory, and diagnostic classes can be used directly when a client needs lower-level control.

**По-русски:** Фасад упрощает обычный сценарий, но не запрещает прямое использование частей подсистемы.

## What is the difference between Facade and Builder?

**English:** The Builder constructs one `Computer` step by step. The Facade coordinates the whole application operation, including diagnostics and Builder use.

**По-русски:** Builder отвечает за создание объекта, Facade — за координацию процесса.

## What is the difference between Facade and Abstract Factory?

**English:** An Abstract Factory creates a related family of components. The Facade chooses the platform factory and coordinates other services too.

**По-русски:** Абстрактная фабрика создаёт компоненты семейства, а фасад организует полный сценарий.

## Why is Bridge still used for reports?

**English:** Report content and output format can change separately. Summary or full reports can use HTML or JSON exporters.

**По-русски:** Bridge разделяет вид отчёта и формат экспорта.

## How are diagnostics and errors handled?

**English:** Diagnostics return individual checks and a `READY`, `REVIEW`, or `BLOCKED` status. A warning allows a build; an error blocks it. Invalid API input has a readable JSON error.

**По-русски:** Диагностика показывает каждую проверку; предупреждения не блокируют сборку, ошибки блокируют.

## Which Clean Code principles did you apply?

**English:** I used small focused classes, one shared power calculator, information hiding, meaningful names, and validation before building. The controller only handles HTTP.

**По-русски:** Ответственность разделена, формула не дублируется, ошибки выявляются до сборки.

## What are the power values?

**English:** Required power is CPU TDP plus GPU TDP plus 150 watts. Recommended power is 10 percent higher, rounded up. This is an educational estimate.

**По-русски:** Это учебная оценка, а не гарантия рекомендаций производителя.
