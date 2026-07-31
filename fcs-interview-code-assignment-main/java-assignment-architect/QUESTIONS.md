# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes, but I would refactor by boundary and risk rather than forcing every endpoint into
the same amount of architecture. This implementation intentionally uses a richer
hexagonal shape for Warehouse and Fulfilment: REST adapters call use cases, use cases
depend on ports, and validators enforce business invariants such as active business unit
uniqueness, location capacity, replacement constraints, and fulfilment quotas.

That shape is justified where rules matter. Warehouse replacement is not just a CRUD
update; it archives one active unit, creates a successor with the same Business Unit
Code, preserves history, and validates stock/capacity continuity. Fulfilment also has
cross-entity constraints across Warehouse, Product, and Store. Those rules are easier to
reason about when they live in domain services and use cases instead of being embedded
inside JAX-RS handlers.

I would not immediately move Product and Store into the same full structure. They are
currently simpler CRUD resources, and adding ports/use cases only for symmetry would add
noise. I would refactor them when they gain meaningful business behavior or when their
side effects become more important. Store already has one such concern: synchronizing
with a legacy system after the database transaction succeeds. That is correctly treated
as a transactional side effect rather than ordinary persistence code.

The main maintenance improvement I would make is to document this rule explicitly:
simple CRUD can stay simple, but domain rules, replacement workflows, quotas, external
side effects, and audit-sensitive behavior should live behind application/domain
boundaries. I would also keep shared exception mapping and response translation in
adapter-level components so each resource does not reinvent error handling.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
Contract-first, as used by the Warehouse API, is strongest when the API is a product
contract. The YAML can be reviewed independently, shared with consumers before the
implementation is complete, used to generate clients or mocks, and versioned as an API
artifact. The generated interface also makes drift visible during development: the
handler has to implement the published shape.

The cost is operational friction. There is an extra generation step, generated DTOs may
not match the domain model, and edge cases such as nullable fields, custom status codes,
or domain-specific error shapes require deliberate mapping in the adapter layer. That is
acceptable for Warehouse because Warehouse exposes the most important business workflow
and includes replacement semantics that other systems are likely to care about.

Code-first, as used by Product and Store, is faster and clearer for small internal CRUD
surfaces. The JAX-RS resource directly shows the behavior, iteration is cheap, and there
is less generated code to understand. The risk is that the contract becomes implicit. If
another team consumes it, accidental path, payload, or status-code changes are harder to
notice.

My architectural preference is contract-first for stable or cross-team APIs, especially
Warehouse and any future cost-control or fulfilment workflow consumed by other systems.
For simple internal CRUD, I would accept code-first but still publish generated OpenAPI
documentation from annotations so the contract remains visible. The important rule is not
"YAML everywhere"; it is choosing the level of contract governance that matches consumer
risk.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
I would follow a test pyramid that mirrors the architecture.

First, domain/use-case tests should carry most of the business-rule coverage. Warehouse
creation, archive, replacement, and Fulfilment association rules are the highest-risk
areas because they protect capacity, stock continuity, active/archived state, and
cross-entity quotas. These tests should exercise the use cases and validators through
ports or test doubles, not through HTTP, so they stay fast and precise.

Second, REST integration tests should cover adapter behavior: request/response mapping,
HTTP status codes, exception mapping, generated Warehouse API integration, and
transactional side effects such as Store legacy synchronization after commit. These
tests should be fewer and broader than domain tests. They prove wiring; they should not
repeat every business-rule permutation already covered at the use-case level.

Third, I would keep a small smoke layer for packaged/runtime confidence. It should prove
that the application starts with its generated sources and persistence configuration and
that the most important flows can be exercised end to end. I would not prioritize UI
tests because this code base has no UI, and I would only add performance tests when a
measurable throughput or latency requirement exists.

Coverage should remain effective through review rules rather than vanity metrics: every
new business invariant needs a focused domain test, every externally visible status code
needs at least one adapter test, and every production bug should add the lowest-level
regression test that would have caught it. Line coverage is useful as a signal, but the
real goal is protecting domain behavior and API contracts.
```
