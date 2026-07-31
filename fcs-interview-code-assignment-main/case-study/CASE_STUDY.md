# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**
Accurate cost allocation starts with agreeing on the business meaning of cost. A Warehouse or Store can be evaluated by direct operational spend, shared overhead, inventory holding cost, transportation cost, or the cost-to-serve a specific product/store flow. If those definitions are mixed, the tool may be technically correct but commercially misleading.

The main challenge is attribution. Labor and lease costs may belong mostly to one site, while transportation and overhead often need allocation rules based on volume, distance, stock turns, number of orders, or service level. The design should separate raw cost capture from allocation logic so finance can change allocation methods without rewriting operational integrations. It should also keep a clear audit trail: source system, accounting period, allocation rule, version, and responsible business unit.

From a technical perspective, I would model costs as time-bound facts linked to business entities such as Warehouse, Store, Product, Location, and Business Unit Code. I would avoid overwriting historical records when an entity changes; instead, I would preserve effective dates and status transitions. This matters because cost trends, budget variance, and replacement decisions depend on comparing like with like across time.

Key discovery questions:

- Which cost categories are in scope first: labor, rent, utilities, inventory holding, transport, depreciation, technology, or all of them?
- Which costs are directly attributable and which require allocation rules?
- What is the reporting grain: daily, weekly, monthly, by order, by product, by store, or by warehouse?
- Which system is the source of truth for each cost category?
- How should shared costs be allocated when one Warehouse serves multiple Stores or Products?
- What audit evidence does Finance need when a number is challenged?
- Do archived or replaced Warehouses remain visible in historical reports under the same Business Unit Code?

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**
Cost optimization should focus on reducing waste without degrading availability, delivery reliability, or store replenishment quality. The first step is observability: identify where cost is created, which drivers explain it, and whether the cost is structural or operational. For example, excess capacity at one location, repeated replenishment from a distant Warehouse, or low stock turns may all require different responses.

I would start with a baseline: cost per Warehouse, cost per Store served, cost per product family, cost per unit of stock, and cost per fulfillment route. Then I would prioritize opportunities by business impact, implementation effort, reversibility, and operational risk. Quick wins might include improving product-to-warehouse assignment rules, removing duplicate or low-value movements, and making capacity constraints visible earlier. Larger initiatives might include warehouse consolidation, renegotiating transport lanes, automation, or changing the replacement strategy for underperforming sites.

Technically, the system should support scenario analysis rather than only static reporting. For example, it should allow Finance and Operations to estimate the cost effect of moving product fulfillment from one Warehouse to another before making the operational change. It should also expose guardrails: a cheaper assignment is not acceptable if it breaches capacity, stock, maximum warehouse-per-store constraints, or expected service levels.

Key discovery questions:

- What does "service quality" mean in measurable terms: lead time, stock availability, order accuracy, replenishment frequency, or SLA compliance?
- Which cost drivers are controllable by Operations and which are fixed commitments?
- Are there known constraints on Warehouse capacity, Store coverage, product handling, or transport contracts?
- What time horizon matters for optimization: current month, next quarter, annual budget, or long-term network design?
- Who approves changes that affect fulfillment assignments or warehouse replacement?
- How should the tool present trade-offs between savings, risk, and customer impact?
- What minimum savings threshold justifies an operational change?

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**
Integration with financial systems is essential because operational systems explain what happened, while financial systems usually define the official accounting numbers. Without integration, teams will reconcile spreadsheets manually, reports will disagree, and cost decisions may be based on stale or partial data.

The benefit is a shared view between Finance and Operations. Finance gets traceable operational context behind each cost line, and Operations gets timely feedback on whether a Warehouse, Store, or Business Unit Code is performing within budget. Real-time or near-real-time synchronization can also detect anomalies earlier, such as an unexpected cost spike after a Warehouse replacement or a transport cost increase linked to a new fulfillment pattern.

Technically, I would treat the integration as a controlled data contract. Each inbound financial event should include source identifiers, accounting period, currency, cost category, amount, entity reference, and reconciliation status. The tool should be idempotent, observable, and resilient: duplicate messages should not double-count costs, failures should be retryable, and every imported cost should be traceable back to the financial source. For reporting, I would distinguish posted/approved finance data from operational estimates.

Key discovery questions:

- Which financial systems are involved: ERP, general ledger, procurement, payroll, transport billing, or data warehouse?
- Is the integration required to be real time, near real time, or batch by accounting period?
- What identifiers can reliably connect finance data to Warehouses, Stores, Products, Locations, and Business Unit Codes?
- How are corrections, reversals, accruals, and late invoices represented?
- Which system owns exchange rates, tax treatment, and cost category mappings?
- What reconciliation workflow is needed when operational and financial data do not match?
- What SLAs, audit controls, and access restrictions apply to financial data?

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**
Budgeting and forecasting turn cost control from a retrospective report into a management capability. In fulfillment, costs are affected by demand, stock levels, capacity, site availability, transport patterns, and replacement decisions. A useful system should help leaders understand expected spend, detect variance early, and choose corrective actions before the budget is consumed.

I would design the capability around drivers, not only historical averages. The forecast should consider active Warehouses and Stores, location capacity, product demand, current stock, planned replacements, seasonal patterns, inflation assumptions, and known operational changes. It should also separate baseline forecast, approved budget, actuals, and scenario forecasts. This makes variance analysis actionable: the company can see whether a deviation came from demand growth, inefficient routing, cost inflation, or a planned network change.

From a technical perspective, the system should store forecast versions and assumptions. Forecasts are decisions made with the information available at a point in time; overwriting them destroys accountability. A practical design would support import of actual financials, operational driver data, and manual assumptions, then expose variance reports by Warehouse, Store, Location, Product, and Business Unit Code.

Key discovery questions:

- What planning horizon is required: rolling 13 weeks, monthly forecast, annual budget, or multi-year network plan?
- Which drivers materially affect cost and are available with reliable data quality?
- Who owns assumptions such as demand growth, inflation, labor rates, and transport pricing?
- How often should forecasts be refreshed, and who approves a new baseline?
- What variance thresholds should trigger alerts or management review?
- Do replacement projects need separate capital and operating expense views?
- How should the tool handle new, archived, and replaced Warehouses in trend comparisons?

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**
Warehouse replacement is a business continuity event as much as a technical state transition. Reusing the Business Unit Code keeps the operational identity of the area, but the old and new physical Warehouses must remain distinguishable for historical analysis. If the old Warehouse is simply overwritten, the company loses the ability to explain past spend, compare replacement performance, and audit decisions.

Cost history should be preserved because the replacement decision is usually justified by a financial hypothesis: lower operating cost, better capacity utilization, improved service level, lower transport cost, or reduced risk. The new Warehouse should be measured against that hypothesis. That requires a clean timeline showing when the old Warehouse was active, when it was archived, when the new one started, and which costs belong to each period.

Technically, the design should treat replacement as an auditable operation: archive the old Warehouse, create the new active Warehouse with the same Business Unit Code, preserve historical cost facts, and prevent reports from accidentally blending old and new physical-site performance. The same Business Unit Code can support continuity in executive reporting, while internal identifiers and effective dates preserve traceability. Budget controls should validate capacity, stock continuity, expected transition cost, and any temporary overlap costs.

Key discovery questions:

- What financial business case justified the replacement, and which metrics prove success?
- Should reports show the Business Unit Code as one continuous line, separate physical Warehouse periods, or both?
- Are transition costs, duplicate running costs, write-offs, or relocation costs in scope?
- How should committed budget be transferred from the old Warehouse to the new one?
- What data must remain immutable after the old Warehouse is archived?
- Are there legal, tax, or audit retention requirements for historical site costs?
- What alerts are needed if the new Warehouse exceeds its expected run-rate after go-live?

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.
