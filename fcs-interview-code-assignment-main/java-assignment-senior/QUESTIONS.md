# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes, but I would keep the refactor pragmatic. This implementation uses different levels
of structure for different parts of the application: Product and Store are simple
Panache/JAX-RS resources, Warehouse has a separate domain model with ports and use
cases, and Fulfilment is implemented more directly in the resource with Panache queries
and inline validation.

That is not automatically wrong. Product and Store are straightforward CRUD and do not
need a heavy domain layer until they gain real business rules. Warehouse does need the
extra boundary because creation, archive, and replacement include rules around active
Business Unit Codes, location capacity, stock matching, and preserving history.
Fulfilment sits in the middle: it is still small, but it already has quota rules across
Warehouse, Product, and Store, so I would watch it closely. If those rules grow, I would
extract a validator or use case before the resource becomes hard to maintain.

The refactor I would do first is not a broad rewrite. I would document when to use each
style, remove avoidable duplication in exception mapping, and keep persistence queries
out of resources once they start representing business decisions rather than simple
lookups. That gives the team consistency without turning a small assignment into an
architecture exercise.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
Spec-first is useful when the API contract is more important than local iteration speed.
For Warehouse, the YAML makes sense because replacement, archive, and creation are core
business operations. A published contract helps consumers understand paths, payloads,
and response expectations, and generated code makes accidental drift easier to catch.

The downside is friction. Generated DTOs need mapping, schema changes require a
generation step, and edge cases can be awkward when the generator's model does not match
the application's domain model. For a small internal endpoint, that overhead can slow
the team down more than it helps.

Code-first works well for Product and Store while they remain simple CRUD resources. The
resource class is readable, changes are quick, and response handling is under direct
control. The trade-off is that the contract is less explicit unless documentation is
generated and reviewed.

My choice would be spec-first for Warehouse and any external or cross-team workflow, and
code-first for simple internal CRUD. I would still expose generated OpenAPI
documentation for Product and Store so consumers can see the contract, but I would not
force all endpoints through code generation unless the API governance need justifies it.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
I would prioritize the tests that protect behavior with the least runtime cost.

First, Warehouse use-case tests should cover the core rules: unique active Business Unit
Codes, valid locations, capacity limits, stock consistency, archive behavior, and
replacement semantics. Those rules should be tested below HTTP so failures point to the
business decision that broke.

Second, REST/integration tests should cover the visible API behavior: happy paths,
representative validation failures, expected status codes, JSON mapping, generated
Warehouse endpoint wiring, and transaction-sensitive Store behavior. I would also cover
Fulfilment at the resource level while it remains implemented directly in the resource,
because that is where its quota checks currently live.

Third, I would keep only a few full runtime smoke tests for packaging and configuration
confidence. They are valuable, but slower, so they should exercise the main flows rather
than duplicate all validation cases.

I would not chase line coverage for its own sake. Coverage stays useful when each new
business rule gets a focused test, each bug fix adds a regression test, and integration
tests are used to prove wiring rather than every permutation of domain behavior.
```
