# XA Bank Time Deposit

A solution to the *Time Deposit Refactoring Kata*. It refactors the existing interest calculation without changing its
behaviour, and adds a PostgreSQL-backed REST API with **exactly two endpoints**, defined contract-first with OpenAPI.

**Stack:** Java 17 · Spring Boot 4.2 · PostgreSQL 17 · Flyway · springdoc (Swagger UI) · openapi-generator ·
JUnit 6 · Testcontainers · Maven Wrapper

---

## Quick start

**Prerequisites:** JDK 17 or newer, and Docker (running). Maven is not needed: use the bundled `./mvnw`.

| Goal | Command |
|---|---|
| Run the app with demo data (starts PostgreSQL in Docker) | `./mvnw spring-boot:test-run` |
| Run against your own PostgreSQL | see below |
| Run all tests (unit + Testcontainers) | `./mvnw test` |

Against your own PostgreSQL:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/deposits \
SPRING_DATASOURCE_USERNAME=... SPRING_DATASOURCE_PASSWORD=... \
./mvnw spring-boot:run
```

`spring-boot:test-run` starts the real application together with a throw-away PostgreSQL container
(Testcontainers). Flyway creates the schema and loads demo data, and everything is removed when the app stops.
Against your own database, Flyway creates the schema but loads no demo data.

---

## Triggering the endpoints with Swagger UI

1. Start the app: `./mvnw spring-boot:test-run`
2. Open **http://localhost:8080/swagger-ui.html**. It shows the contract `src/main/resources/static/openapi.yaml`.
3. **GET `/time-deposits`** → *Try it out* → *Execute*. Returns all deposits with their withdrawals.
4. **POST `/time-deposits/balance-updates`** → *Try it out* → *Execute*. Returns `204 No Content` and applies
   **one month** of interest to every deposit.
5. Run **GET** again to see the new balances.

Expected balances after **one** update, using the demo data. The demo data is reset on every restart, and each
further POST adds another month.

| id | planType | days | before | after | why |
|---|---|---|---|---|---|
| 1 | basic | 30 | 1000.00 | 1000.00 | no interest in the first 30 days |
| 2 | basic | 45 | 1234567.00 | 1235595.81 | 1 % p.a. → 1/12 per month |
| 3 | student | 365 | 5000.00 | 5012.50 | 3 % p.a., still within the first year |
| 4 | student | 366 | 5000.00 | 5000.00 | no interest after one year |
| 5 | premium | 45 | 10000.00 | 10000.00 | interest starts after 45 days |
| 6 | premium | 46 | 10000.00 | 10041.67 | 5 % p.a. |
| 7 | gold | 100 | 750.00 | 750.00 | unknown plan type: listed, never earns interest |

The same calls with curl:

```bash
curl http://localhost:8080/time-deposits
curl -i -X POST http://localhost:8080/time-deposits/balance-updates
```

---

## API

| Method | Path | Response | Description |
|---|---|---|---|
| `GET` | `/time-deposits` | `200` + JSON array | All deposits with `id`, `planType`, `balance`, `days`, `withdrawals` (`id`, `amount`, `date`), ordered by id |
| `POST` | `/time-deposits/balance-updates` | `204` | Applies one month of interest to all deposits |

**Contract-first.** `src/main/resources/static/openapi.yaml` is the single source of truth. On every build,
openapi-generator produces the `TimeDepositsApi` interface and the DTOs from it, and the controller implements
that interface. Paths, HTTP methods and payload shapes are therefore enforced at compile time. Status codes and the
exact JSON are verified by `TimeDepositApiTest`. Swagger UI serves the same file.

---

## Interest rules

The rates are annual. Each balance update pays one month's share (`balance × rate / 12`), rounded to cents.

| Plan | Annual rate | Eligibility |
|---|---|---|
| basic | 1 % | after the first 30 days |
| student | 3 % | after the first 30 days, until day 365 |
| premium | 5 % | after 45 days |
| anything else | – | never; the balance stays unchanged |

---

## Architecture

The code follows a hexagonal architecture, and dependencies point inward only:

```
infrastructure.web ──────▶ application ──────▶ domain
infrastructure.persistence ──▶ (implements application port)
```

| Package | Contents |
|---|---|
| `org.ikigaidigital` (**domain**) | `TimeDeposit` (shared, unchanged), `TimeDepositCalculator`, interest policies, `PlanType`, `Withdrawal`. No framework dependencies. The only exception in this package is the Spring Boot entry point, `TimeDepositApplication`. |
| `…application` | Use cases `UpdateAllBalances` and `GetAllTimeDeposits`, the outbound port `TimeDepositRepository`, the read model `TimeDepositWithWithdrawals` |
| `…infrastructure.persistence` | JPA entities, Spring Data repositories, and `TimeDepositPersistenceAdapter`, which implements the port |
| `…infrastructure.web` | `TimeDepositController` (implements the generated API interface) and the DTO mapper |
| `…infrastructure.config` | Bean wiring for the domain and application classes |

**Why there is no `domain` package.** The domain deliberately stays in the root package `org.ikigaidigital`:

- **A full move would break consumers.** Moving `TimeDeposit` or `TimeDepositCalculator` into
  `org.ikigaidigital.domain` would change their fully qualified names. Every consumer importing them would stop
  compiling. That breaks the task's constraint: no breaking changes to the shared `TimeDeposit` class, and no change
  to `updateBalance`.
- **A partial move would widen the API.** Moving only the new types (`Withdrawal`, `PlanType`, the interest policies)
  would mean making `PlanType`, `InterestPolicy` and the concrete policies `public`, because the calculator in the
  root package must reach them. Today they are package-private implementation details.
- **So the root package is the domain.** The Spring Boot entry point sits there too, so that component scanning
  covers the `application` and `infrastructure` sub-packages.

Key design decisions:

- **Interest calculation (Strategy).** Each plan has an `InterestPolicy`. The shared rules (the 30-day period and
  `balance × rate / 12`) live in `AnnualRateInterestPolicy`. A `PlanType` enum (exact, case-sensitive codes) maps to
  the policies in an `EnumMap`. Adding a plan means adding a `PlanType` constant and a policy class, then registering
  the policy. `updateBalance(List<TimeDeposit>)` keeps its signature and its exact results, including floating-point
  rounding.
- **One class per use case.** Each use case declares only what it needs: the read path doesn't depend on the
  calculator. They also have different transaction semantics: read-only versus write with a row lock.
- **No lost updates.** The update use case reads deposits with a `PESSIMISTIC_WRITE` lock (PostgreSQL:
  `FOR NO KEY UPDATE`, ordered by id). Concurrent balance updates are therefore serialised. A concurrency test
  proves this.
- **Transactions.** The use cases own them (`@Transactional`). The persistence adapter is
  `@Transactional(MANDATORY)`, so calling it without a transaction fails fast instead of silently losing balance
  changes.
- **Persistence.** `GET` runs two queries in total (deposits, then all withdrawals grouped in memory), with no N+1.
  Open Session in View is off, and Flyway owns the schema (`ddl-auto: validate`).

---

## Database

Flyway migration: `src/main/resources/db/migration/V1__create_time_deposit_tables.sql`.

| Requirement | Table / column | Type |
|---|---|---|
| `timeDeposits.id` | `time_deposits.id` | `INTEGER` identity, PK |
| `timeDeposits.planType` | `time_deposits.plan_type` | `VARCHAR(50) NOT NULL` |
| `timeDeposits.days` | `time_deposits.days` | `INTEGER NOT NULL` |
| `timeDeposits.balance` | `time_deposits.balance` | `NUMERIC(19,2) NOT NULL` |
| `withdrawals.id` | `withdrawals.id` | `INTEGER` identity, PK |
| `withdrawals.timeDepositId` | `withdrawals.time_deposit_id` | `INTEGER NOT NULL`, FK + index |
| `withdrawals.amount` | `withdrawals.amount` | `NUMERIC(19,2) NOT NULL` |
| `withdrawals.date` | `withdrawals.date` | `DATE NOT NULL` |

The names use PostgreSQL's idiomatic snake_case, because PostgreSQL lowercases unquoted camelCase identifiers.

---

## Assumptions and decisions

The task description asks for assumptions to be justified. They are also documented in code comments or in the contract.

- **One update call applies one month of interest**, and `days` is not advanced. The existing calculator never changes
  `days`, so it is assumed to be maintained outside this service.
- **Withdrawals do not affect interest.** The original calculator ignores them, so they are stored and returned only.
- **The update is not idempotent.** Every call pays another month, so it is `POST`, not `PUT`.
- **`planType` is a free string in the API, not an enum.** Stored deposits may carry unknown plan types. They earn no
  interest (the original behaviour) but must still be returned.
- **`withdrawals` is a list of withdrawal records.** The requirements only name the field.
- **Money.** `TimeDeposit.balance` stays a `Double`, because the shared class must not change. Values are stored as
  `NUMERIC(19,2)` and exposed with two decimals. New code (`Withdrawal`) uses `BigDecimal`.
- **The domain lives in the root package** (see [Architecture](#architecture)). The use cases use Spring's
  `@Transactional` as a deliberate, pragmatic compromise. They carry no other framework annotations.
- **One intended behaviour deviation:** a deposit with a missing (`null`) plan type and more than 30 days used to throw
  `NullPointerException`. It is now treated like an unknown plan and left unchanged. Invalid input handling is out of
  scope, and the deviation is pinned by a dedicated test.

### Known limitations, consciously not addressed

These are blocked by the task's constraints, or out of scope:

- `Double` for money in the shared `TimeDeposit` class. A proper fix needs a contract change for its consumers.
- `days` is stored rather than derived from an opening date, so it can go stale.
- No idempotency protection for repeated update calls, for example a "last applied month" marker. The prescribed
  schema has no column for it.
- The balance update loads and locks all deposits at once, with no paging. This is fine at kata scale.

---

## Tests

`./mvnw test` runs 28 tests; Docker is required for the Testcontainers-based ones.

- **Characterization tests first:** `TimeDepositCalculatorTest` pinned the original calculator's behaviour (all
  boundaries and rounding) before it was refactored. One extra test pins the intended `null` plan-type deviation.
- **Each layer tested on its own terms:** the update use case with an in-memory fake (no mocks), the persistence
  adapter and the API end to end against real PostgreSQL, plus a deterministic concurrency test for the row lock.

---

## AI-assisted development

**Tools.** Claude Code (Anthropic's agentic coding CLI) next to IntelliJ IDEA, connected through IntelliJ's MCP server
for code inspections.

**Workflow.** Every step followed the same loop:
1. A plan agreed with the developer.
2. Implementation in small steps, with tests written alongside (characterization tests before any refactoring).
3. An independent review by a separate AI agent with no access to the implementer's reasoning. It reported findings
   as BLOCKER / SUGGESTION / OK.
4. A developer review in the IDE.
5. A commit, only after explicit approval.

**Rules the agent worked under.** These were given to the agent as project instructions:

- Respect the task constraints: `TimeDeposit` and `updateBalance` unchanged, exactly two endpoints, no
  over-engineering.
- Write characterization tests before refactoring. Tests assert behaviour, not interactions, and key tests are
  mutation-checked.
- Verify claims against the running application instead of assuming them.
- Keep commits small and atomic.

**What was AI-assisted, and why.** The agent implemented the code, tests, configuration and documentation. This was
for speed, and for systematic checking of boundary cases and framework behaviour. The developer made the design
decisions, reviewed every step, and did the final review. Examples of those decisions: the contract-first API, one
class per use case, and the Testcontainers-based local run.

**Reproducing.** Any agentic coding assistant can follow the same loop: give it the task description and the rules
above, require an independent review after each change, and approve each step.
