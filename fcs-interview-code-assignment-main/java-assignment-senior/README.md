# java-assignment-senior — Pragmatic Variant

Completion of the [warehouse case study](../case-study/BRIEFING.md) written the way a pragmatic
**senior developer** would: minimum viable class count, logic co-located where it is used, and
direct use of the framework's error model. The architecture-first counterpart lives in
[`java-assignment-architect`](../java-assignment-architect/README.md); the untouched original brief
is in [`java-assignment`](../java-assignment/README.md). See the
[repository README](../../README.md) for the full comparison and setup details.

## Contents

- [Design philosophy](#design-philosophy)
- [What was implemented](#what-was-implemented)
- [Key decisions](#key-decisions)
- [Package layout](#package-layout)
- [Build & test](#build--test)
- [Related documents](#related-documents)

## Design philosophy

Deliver the exact same behavior with the fewest moving parts. Validations live inline in the use
cases and throw `WebApplicationException` with the right HTTP status directly; the bonus feature is
a single Panache entity plus one REST resource. No speculative abstractions — the existing
hexagonal skeleton of the Warehouse package is respected, but nothing new is added on top of it.

## What was implemented

| Task | Where |
|---|---|
| `LocationGateway.resolveByIdentifier` | `location/LocationGateway.java` (+ `@ApplicationScoped`) |
| Store legacy sync **after commit** | `stores/StoreResource` via `TransactionSynchronizationRegistry` (`afterCompletion`, only on `STATUS_COMMITTED`) |
| Warehouse create / get / list / archive / replace | `warehouses/adapters/restapi/WarehouseResourceImpl` + use cases |
| All warehouse validations (unique BU code, valid location, max warehouses per location, capacity/stock rules) | inline in `warehouses/domain/usecases/*` |
| Replace-specific rules (capacity accommodates stock, stock match) | `warehouses/domain/usecases/ReplaceWarehouseUseCase` |
| Bonus: fulfilment associations (max 2 WH/product/store, 3 WH/store, 5 products/WH) | `fulfilment/Fulfilment` (Panache entity) + `fulfilment/FulfilmentResource` |
| Assignment questions | [`QUESTIONS.md`](QUESTIONS.md) |

## Key decisions

- **`TransactionSynchronizationRegistry` over CDI events** — registers the legacy-gateway call as
  an `afterCompletion` callback guarded by `STATUS_COMMITTED`, avoiding the classic
  self-invocation pitfall of `@Transactional` without introducing extra classes.
- **`getAll()` filters `archivedAt is null`** — the warehouse listing was returning archived rows
  (real bug found and fixed during implementation).
- **201 on POST via injected `RoutingContext`** — RESTEasy Reactive reads the response status from
  the OpenAPI-generated interface method, so the implementation sets it explicitly on the Vert.x
  response.
- **DB-level uniqueness** for fulfilment associations (`@UniqueConstraint` on the triple) in
  addition to the inline checks.

## Package layout

```
src/main/java/com/fulfilment/application/monolith/
├── fulfilment/          # Fulfilment entity + FulfilmentResource (bonus)
├── location/
├── products/
├── stores/
└── warehouses/
    ├── adapters/{database,restapi}/
    └── domain/{models,ports,usecases}/
```

## Build & test

This module builds standalone or through the [parent aggregator](../pom.xml):

```sh
# from the repository root — standalone
JAVA_HOME=/usr/local/opt/openjdk@17 mvn -f fcs-interview-code-assignment-main/java-assignment-senior/pom.xml clean test

# or the whole reactor (architect + senior)
JAVA_HOME=/usr/local/opt/openjdk@17 mvn -f fcs-interview-code-assignment-main/pom.xml clean test
```

Tests use Quarkus Dev Services (disposable PostgreSQL container); with Podman instead of Docker,
export `DOCKER_HOST` pointing at the Podman socket first (see the
[repository README](../../README.md#installation--setup)). Test suite: **39 tests** — use case unit
tests, REST-Assured integration tests for the Warehouse and Fulfilment APIs, and a store
legacy-sync test proving the gateway only fires after the database transaction commits
(`WarehouseEndpointIT` additionally runs under `@QuarkusIntegrationTest` via failsafe).

## Related documents

- [`CODE_ASSIGNMENT.md`](CODE_ASSIGNMENT.md) — the task list this module fulfils
- [`QUESTIONS.md`](QUESTIONS.md) — answered from the senior developer's perspective
- [`../case-study/BRIEFING.md`](../case-study/BRIEFING.md) — domain briefing
- [`../java-assignment-architect/README.md`](../java-assignment-architect/README.md) — the architecture-first counterpart
