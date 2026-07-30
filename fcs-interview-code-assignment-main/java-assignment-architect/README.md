# java-assignment-architect — DDD/Hexagonal Variant

Completion of the [warehouse case study](../case-study/BRIEFING.md) written the way a **software
architect** would: strict ports-and-adapters separation, domain exceptions, and one class per
responsibility. The functionally equivalent pragmatic counterpart lives in
[`java-assignment-senior`](../java-assignment-senior/README.md); the untouched original brief is in
[`java-assignment`](../java-assignment/README.md). See the [repository README](../../README.md) for
the full comparison and setup details.

## Contents

- [Design philosophy](#design-philosophy)
- [What was implemented](#what-was-implemented)
- [Key decisions](#key-decisions)
- [Package layout](#package-layout)
- [Build & test](#build--test)
- [Related documents](#related-documents)

## Design philosophy

The domain never depends on JAX-RS, Panache, or any adapter concern. Use cases enforce every
business invariant and signal violations through a dedicated exception hierarchy
(`WarehouseDomainException` and subclasses), which a `WarehouseExceptionMapper` translates to HTTP
responses at the boundary. Cross-cutting side effects (the legacy store sync) are modeled as CDI
events observed only after the database transaction commits.

## What was implemented

| Task | Where |
|---|---|
| `LocationGateway.resolveByIdentifier` | `location/LocationGateway.java` (+ `@ApplicationScoped`) |
| Store legacy sync **after commit** | `stores/StoreLegacySyncEvent` + `StoreLegacySyncObserver` (`@Observes(during = AFTER_SUCCESS)`) |
| Warehouse create / get / list / archive / replace | `warehouses/adapters/restapi/WarehouseResourceImpl` + use cases |
| All warehouse validations (unique BU code, valid location, max warehouses per location, capacity/stock rules) | `warehouses/domain/validation/WarehouseValidator` |
| Replace-specific rules (capacity accommodates stock, stock match) | `warehouses/domain/usecases/ReplaceWarehouseUseCase` |
| Bonus: fulfilment associations (max 2 WH/product/store, 3 WH/store, 5 products/WH) | `fulfilment/` (full hexagonal slice) |
| Assignment questions | [`QUESTIONS.md`](QUESTIONS.md) |

## Key decisions

- **CDI events over `TransactionSynchronizationRegistry`** for the post-commit legacy sync —
  declarative, idiomatic Quarkus, keeps transaction plumbing out of the resource.
- **`findByBusinessUnitCode` filters `archivedAt is null`** — after a replace there are two rows
  with the same BU code; without the filter the archived one could be returned (real bug found
  during implementation).
- **201 on POST via a scoped `ContainerResponseFilter`** — the OpenAPI-generated interface returns
  the bean (not `Response`), so the created status is set by a filter bound to that exact resource
  method.
- **Fulfilment as its own aggregate** with `ProductResolver`/`StoreResolver` ports, so its domain
  does not couple to the active-record style of `Product`/`Store`.

## Package layout

```
src/main/java/com/fulfilment/application/monolith/
├── fulfilment/
│   ├── adapters/{database,restapi}/
│   └── domain/{exceptions,models,ports,usecases,validation}/
├── location/
├── products/
├── stores/
└── warehouses/
    ├── adapters/{database,restapi}/
    └── domain/{exceptions,models,ports,usecases,validation}/
```

## Build & test

This module builds standalone or through the [parent aggregator](../pom.xml):

```sh
# from the repository root — standalone
JAVA_HOME=/usr/local/opt/openjdk@17 mvn -f fcs-interview-code-assignment-main/java-assignment-architect/pom.xml clean test

# or the whole reactor (architect + senior)
JAVA_HOME=/usr/local/opt/openjdk@17 mvn -f fcs-interview-code-assignment-main/pom.xml clean test
```

Tests use Quarkus Dev Services (disposable PostgreSQL container); with Podman instead of Docker,
export `DOCKER_HOST` pointing at the Podman socket first (see the
[repository README](../../README.md#installation--setup)). Test suite: **43 tests** — unit tests
for every use case with in-memory test doubles, REST-Assured integration tests for the Warehouse
and Fulfilment APIs, and a store legacy-sync test proving the gateway only fires after the
database transaction commits.

## Related documents

- [`CODE_ASSIGNMENT.md`](CODE_ASSIGNMENT.md) — the task list this module fulfils
- [`QUESTIONS.md`](QUESTIONS.md) — answered from the architect's perspective
- [`../case-study/BRIEFING.md`](../case-study/BRIEFING.md) — domain briefing
- [`../java-assignment-senior/README.md`](../java-assignment-senior/README.md) — the pragmatic counterpart
