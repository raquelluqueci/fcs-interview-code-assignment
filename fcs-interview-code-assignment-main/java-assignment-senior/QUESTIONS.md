# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes. Today we have three different data-access styles living side by side: Store/Product use
active-record Panache entities directly from the REST resource, Warehouse goes through a proper
hexagonal port (WarehouseStore / WarehouseRepository) with a domain model separate from the JPA
entity, and Fulfilment (bonus) is a plain Panache entity with the validation logic inline in the
resource. That's three levels of ceremony for what is structurally the same kind of CRUD problem.

I would not force everything into the heaviest (Warehouse-style hexagonal) pattern - that's
over-engineering for entities like Product/Store that have no real business rules beyond basic
validation. Conversely I wouldn't collapse Warehouse into a plain Panache entity either, because
it genuinely has domain rules (location capacity, replace semantics) that benefit from being
testable without a database. My rule of thumb: reach for the port/use-case split only when there
is non-trivial business logic to isolate and unit-test; plain Panache active-record is fine for
straightforward CRUD. What I would refactor is making that choice consistent and documented (e.g.
a short ADR) so the next person doesn't reinvent a fourth style. I'd also unify the exception
mapping - right now every resource redeclares its own `ErrorMapper`, which is duplicated JAX-RS
`ExceptionMapper<Exception>` providers; that should be a single top-level provider.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
Spec-first (Warehouse): the YAML is the contract, so frontend/consumer teams can start against a
mock or generated client before the backend is implemented, and the interface can't silently
drift from what was published - the compiler enforces it. Cons: an extra generation step in the
build (slower feedback loop while iterating), generated DTOs that don't map 1:1 to the domain
model (I had to hand-map `com.warehouse.api.beans.Warehouse` to the domain `Warehouse`), and
it's easy to end up fighting the generator on edge cases (nullable fields, custom validation).

Code-first (Product/Store): faster to iterate locally, one less moving part, JAX-RS annotations
double as the documentation. Cons: no contract to review/version until someone bolts on
swagger-generation from the annotations, and it's easier for the implementation to drift from
whatever was verbally agreed with consumers.

My choice: spec-first for anything with external/cross-team consumers or a stable public
contract (that's exactly the Warehouse case here), code-first for internal, fast-moving CRUD
endpoints like Product/Store where the "consumer" is the same team shipping the backend. Given
this is one small monolith, I'd probably standardize on generating an OpenAPI doc FROM the code
(quarkus-smallrye-openapi) for Product/Store rather than hand-writing YAML for everything - that
gets the contract-visibility benefit without the codegen friction, and reserve full spec-first
codegen for endpoints that genuinely have external consumers.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
Priority order, highest value first:

1. Unit tests on the domain/use-case layer (CreateWarehouseUseCase, ReplaceWarehouseUseCase,
   ArchiveWarehouseUseCase, LocationGateway). These carry the actual business rules (uniqueness,
   capacity, stock, replace semantics) and run in milliseconds with no container, so they're the
   cheapest place to pin down edge cases and the first thing I'd run in CI.
2. REST/integration tests (@QuarkusTest) per resource, covering the happy path plus each
   documented validation rule end-to-end (400/404/409/201/204), using Dev Services so they run
   against a real Postgres without extra setup. That's what I added for Warehouse and Fulfilment.
3. A handful of true end-to-end smoke tests (@QuarkusIntegrationTest, like the existing
   WarehouseEndpointIT) that exercise the packaged artifact - good for catching packaging/config
   issues that unit and @QuarkusTest can miss, but slow, so kept to a minimum.

I would deliberately not chase line-coverage percentages - e.g. I'm not adding tests for trivial
getters or the Panache boilerplate in ProductRepository. Coverage stays effective over time by:
tying it to the pyramid above (push new business rules to use-case tests, not REST tests, so
they run fast and fail with a precise message), treating a bug fix as incomplete without a
regression test at the lowest layer that can reproduce it, and running `mvn test` in CI on every
PR so drift is caught immediately rather than at release time.
```