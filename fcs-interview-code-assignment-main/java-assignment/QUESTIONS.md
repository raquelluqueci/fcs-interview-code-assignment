# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes, but I would refactor gradually and based on the amount of business logic behind each
resource. The current code base mixes a few styles: Product uses a Panache repository,
Store uses the active-record Panache style directly from the resource, and Warehouse already
has a domain model, ports, and a repository adapter, although several operations still need
to be completed.

I would keep simple CRUD endpoints simple. Product and Store do not yet carry the same level
of business rules as Warehouse, so forcing them into a full port/use-case structure immediately
would add ceremony without much value. However, I would move toward clearer boundaries where
there are rules, external side effects, or transactional concerns. Store is a good example:
the legacy-system synchronization is a side effect and should be coordinated after the database
transaction succeeds, not mixed as ordinary persistence logic in the resource.

For Warehouse, I would keep and complete the existing direction: REST handler -> use case ->
port -> repository. The Warehouse rules are business rules, not HTTP rules: unique business
unit code, valid location, location capacity, stock/capacity consistency, archive behavior,
and replacement semantics. Keeping those rules in a use-case layer makes them easier to test
without a running HTTP server or database, while the repository remains responsible only for
storage concerns.

So my refactoring choice would be: avoid a "one pattern everywhere" rewrite, document the rule
for choosing a persistence style, and extract shared error handling/transactional side effects
where duplication or risk is already visible.

```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
Both approaches are valid, but they optimize for different risks.

The OpenAPI-first approach used by Warehouse is valuable when the API is a contract shared
with other teams, clients, or services. The YAML can be reviewed before implementation,
versioned explicitly, used to generate clients or mocks, and used to detect drift between
the published API and the handler implementation. The trade-off is friction: the generated
types may not match the domain model exactly, changes require regeneration, and some response
details can be harder to express than in hand-written JAX-RS code.

The code-first approach used by Product and Store is faster for small internal CRUD endpoints.
The resource class is easy to read, easy to change, and gives full control over status codes
and response shapes. The downside is governance: unless documentation is generated from
annotations and reviewed, the contract is implicit and can drift accidentally.

My choice would be contract-first for stable external or cross-team APIs, especially Warehouse,
because Warehouse contains the most important business operations and replacement semantics.
For Product and Store, I would accept code-first while they remain simple, but I would still
generate and publish OpenAPI documentation from the code so consumers have a visible contract.
If Product or Store become externally consumed or acquire more complex workflows, I would move
them to the same contract-first discipline.

```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
I would prioritize tests by business risk and feedback speed.

First, I would cover domain/use-case behavior with focused unit tests. Warehouse creation,
archive, and replacement carry the most important rules: location validity, capacity limits,
stock consistency, unique business unit code, and preserving the correct state during
replacement. These tests should run without Quarkus or a database by using in-memory ports or
test doubles. That gives fast feedback and clear failures when a business rule changes.

Second, I would add integration tests around the REST layer and persistence wiring. These
should verify that HTTP endpoints map requests and responses correctly, that expected status
codes are returned, and that transaction boundaries work as intended, especially for Store
legacy synchronization after commit. I would keep these fewer than unit tests because they are
slower and should not duplicate every domain edge case.

Third, I would keep a small number of packaged-application or smoke tests for the most critical
flows: create a Store, create a Warehouse, archive a Warehouse, and replace a Warehouse. These
tests are useful for catching configuration, generated API, and runtime packaging issues, but
they should not become the main place where business rules are tested.

To keep coverage effective over time, every new rule or bug fix should add a regression test at
the lowest layer that can prove the behavior. I would review coverage by scenario and risk, not
by line percentage alone. A high line-coverage number is less useful than confidence that the
core business invariants and externally visible API contracts are protected.

```
