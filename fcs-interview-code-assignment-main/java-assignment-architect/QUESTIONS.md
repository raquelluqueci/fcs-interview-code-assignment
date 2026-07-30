# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes. Today there are three coexisting styles: `Product`/`Store` as active-record Panache
entities accessed directly from the REST resource, `Warehouse` behind a hexagonal port
(`WarehouseStore`) implemented by `WarehouseRepository`, and now `FulfilmentAssociation`
following the same hexagonal shape. I would consolidate everything onto the hexagonal
style used by Warehouse/Fulfilment, for one concrete reason: business invariants (unique
business unit code, location capacity, quotas) currently live in domain use cases that
depend only on ports, which makes them unit-testable with a plain in-memory
implementation and zero Quarkus/DB bootstrap - as shown by the `*UseCaseTest` classes in
this PR, which run in milliseconds with no Dev Services. `ProductResource`/`StoreResource`
mix persistence, validation and HTTP concerns in the same class; any rule added there
(e.g. "product name must be unique across active products") could only be tested through
a full `@QuarkusTest` with a running Postgres.
That said, I would NOT force it for the sake of dogma: Product and Store, as they stand,
have no business invariants beyond "exists" and "name is unique" (enforced by a DB
constraint already). Introducing a port/use case layer there today would be
over-engineering for the actual behavior present. The refactor becomes worth it the
moment a real invariant shows up (as happened for Warehouse's capacity/location rules,
and now Fulfilment's quotas) - i.e., let the domain complexity justify the pattern,
not the other way around.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
Contract-first (OpenAPI -> generated interface, Warehouse's approach):
+ The YAML is the single source of truth, reviewable independently of code, and can be
  shared with API consumers/other teams before the implementation exists.
+ The generated interface enforces the contract at compile time: if the implementation
  drifts from the spec (wrong path, missing param), the build fails.
+ Client SDKs / mocks can be generated from the same file.
- Extra build step and generated sources add friction (as I hit in this exercise: the
  generated `Warehouse` bean uses `String` for `capacity`/`stock` unless the schema states
  `type: integer`, and the generator needs a full Maven cycle to regenerate after any
  YAML change - slower inner loop than editing a hand-written class).
- Anything the generator doesn't model well (custom status codes, fine-grained response
  variants) needs vendor-specific escape hatches - I used RESTEasy Reactive's
  `@ResponseStatus` to force 201 on creation since the generated interface's return type
  is fixed to the DTO, not `Response`.

Code-first (Product/Store's approach):
+ Faster to iterate - no generation step, the resource class IS the contract.
+ Full control over response types/status codes without fighting a generator.
- The contract only exists implicitly in the code; nothing prevents accidental breaking
  changes, and there's no artifact to hand to a frontend/consumer team ahead of time.
- Documentation (if any) has to be maintained by hand and tends to rot.

My choice: contract-first (OpenAPI) for every endpoint that is a stable, external-facing
API consumed by other teams/services - which in this domain is arguably all of
Warehouse, Product and Store, since they're all part of the same "Warehouse colocation
management" public surface. I'd bring Product and Store under the same
`quarkus-openapi-generator` pipeline as Warehouse for consistency, and reserve code-first
only for truly internal, single-consumer endpoints where the iteration speed matters more
than contract governance (e.g. an internal admin/debug endpoint).
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
Priority order, highest ROI first:

1. Domain unit tests (use cases + validators), no Quarkus/DB. This is where the actual
   business risk lives - duplicate business unit codes, location capacity math,
   replacement invariants, fulfilment quotas. They run in milliseconds, don't need Dev
   Services/Postgres, and pinpoint the exact broken rule instead of a generic HTTP 400.
   This is the bulk of what I wrote here (`CreateWarehouseUseCaseTest`,
   `ReplaceWarehouseUseCaseTest`, `ArchiveWarehouseUseCaseTest`).
2. REST/integration tests (`@QuarkusTest` + REST Assured) for the "wiring": correct HTTP
   status codes, correct path/JSON mapping, exception-to-response translation. These
   exist to catch integration mistakes (wrong `@Path`, wrong exception mapper priority,
   wrong default status code) that unit tests structurally cannot see. I keep these
   fewer and coarser than the unit tests - one or two happy-path tests plus one test per
   distinct HTTP status a client can observe (400/404/201/204), not a cartesian product
   of every validation rule again (that's already covered at the unit level).
3. Contract/schema tests only where a generated OpenAPI client exists (Warehouse) -
   validating the generated bean/interface actually match the YAML, catching accidental
   drift between the two.
4. I would deliberately NOT invest early in end-to-end/UI tests or performance tests -
   there's no UI in this codebase and no evidence yet of a performance-sensitive path.

To keep coverage effective over time rather than decorative:
- Every new domain exception/validation rule requires a corresponding unit test in the
  same PR - enforced by review, not tooling, at this project's size.
- Prefer testing behavior through the public port/use case API, not implementation
  details, so refactors (e.g. changing `WarehouseRepository`'s query strategy) don't
  require touching the domain tests.
- Track integration test count deliberately: it should grow O(1) per new use case (one
  or two REST tests), while unit tests grow with the actual number of business rules.
  If integration tests start growing faster than that, it is a sign that validation
  logic is leaking into the REST layer again.
```
