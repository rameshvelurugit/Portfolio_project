Assessment 2 — Portfolio Performance Attribution (Revised Prompt)
Role
You are a Senior Java 21 + Spring Boot Developer.

Important
Do NOT create a new project.
Assessment 2 must be implemented on top of the existing Assessment 1 project.
Extend the current application and reuse as much of the existing implementation as possible.
Do not modify or break any existing Assessment 1 functionality.
Non-regression requirements
All Assessment 1 APIs must continue to work.
All existing unit and integration tests must continue to pass.
Do not remove or rewrite existing code unless absolutely necessary.
Prefer additive changes (new classes, new endpoint method, extended enums/constants).
Existing Project (Assessment 1 — actual state)
The repository is a multi-module Maven project:

Module	Role
portfolio-parent
Parent BOM (Java 21, Spring Boot 3.5.8, Spring Cloud 2025.0.1)
portfolio-performance
Business REST API (port 8081) — implement Assessment 2 here
portfolio-gateway
API Gateway (port 8080), routes Path=/api/performance/** — no changes expected
portfolio-common
Shared JSON logging utilities
Tech stack already in use
Java 21
Spring Boot 3.x
Maven
Spring Web
Spring Validation
Spring Boot Actuator
JUnit 5, Mockito, MockMvc
Layered architecture
Global exception handling (GlobalExceptionHandler)
JSON logging (logback + logstash encoder via portfolio-common)
Distributed tracing (Zipkin)
Java records for DTOs (Jakarta Bean Validation annotations)
Constructor injection (no field @Autowired)
BigDecimal for financial math
No database — requests are processed in memory; nothing is persisted today
Conventions to follow (match Assessment 1 exactly)
DTOs: Java records in api/dto — do not introduce Lombok (it is in pom.xml but unused)
Services: direct @Service classes — no service/impl interface pattern
Controllers: PerformanceController at /api/performance — add the new endpoint here
Validation: application/validation + reusable ValidationResult
Calculation: dedicated calculator class in application/calculation
Domain enums/constants: domain package (CalculationStatus, CalculationConstants)
No repository/JPA layer exists today — do not add Spring Data unless required for idempotency (see below)
Existing endpoint (do not break)
POST /api/performance/daily-return
Implemented in PerformanceController → DailyReturnService → DailyReturnValidator + ReturnCalculator.

Objective
Add a new REST endpoint:

POST /api/performance/attribution
This endpoint calculates how each asset group (Equity, Fixed Income, Cash, etc.) contributes to overall portfolio return.

Assessment 2 should feel like a natural extension of Assessment 1, not a separate application.

The gateway already routes /api/performance/** to portfolio-performance, so the new endpoint is automatically available via port 8080 with no gateway changes.

Reuse Existing Components
Reuse from Assessment 1:

Package structure under portfolio-performance
Layered architecture (controller → service → validator + calculator)
GlobalExceptionHandler and ApiErrorResponse
ValidationResult pattern
JSON logging configuration (RequestLoggingConfiguration, RequestLoggingFilter, RequestLoggingInterceptor)
Clock / ClockConfig for processedAt
CalculationConstants (extend with attribution-specific constants)
DTO style (records, Jakarta validation, JavaDoc)
Testing approach (@WebMvcTest, @SpringBootTest + @AutoConfigureMockMvc)
Existing README.md and prompt-log.md (append only)
Avoid duplicate implementations. Create only the classes required for attribution.

Business Requirements
Input: asset groups
Each group contains:

Field	Type	Required	Description
groupName
string
Yes
e.g. "Equity", "Fixed Income", "Cash"
weightPct
number
Yes
Portfolio weight in percent
returnPct
number
No
Primary return for the period, in percent
fallbackReturnPct
number
No
Used when returnPct is null/absent
Contribution formula
For each group:

contributionPct = (weightPct × effectiveReturnPct) / 100
Where effectiveReturnPct is:

returnPct when available → pricingMode = PRIMARY
fallbackReturnPct when returnPct is null/absent but fallback exists → pricingMode = FALLBACK_USED
Total contribution
totalContributionPct = sum of all group contributionPct values
Numeric precision
Use BigDecimal for all financial calculations
Attribution scale = 3 decimal places, RoundingMode.HALF_UP
(Daily return continues to use scale 2 — do not change Assessment 1 math)
Add constants to CalculationConstants, e.g.:

public static final int ATTRIBUTION_SCALE = 3;
Status Model
Create AttributionStatus enum in domain (or extend a shared status enum if cleaner — do not change CalculationStatus semantics for daily-return):

Status	Meaning
VALID
Inputs valid; all groups have return data (primary or fallback)
DEGRADED
Exactly one group lacks both returnPct and fallbackReturnPct; partial result returned
REVIEW_REQUIRED
More than one group lacks both returns; manual review needed
INVALID_INPUT
Business validation failed (e.g. weight out of range)
Status precedence (evaluate in this order)
INVALID_INPUT — total weight < 99% or > 101% → stop calculation, return null totals
REVIEW_REQUIRED — more than one group missing both returnPct and fallbackReturnPct
DEGRADED — exactly one group missing both returns
VALID — all other successful cases (including when fallback is used)
Response field: degraded
degraded = true only when status == DEGRADED
degraded = false otherwise
Degraded processing (exactly one missing group)
Continue processing remaining groups
Missing group: effectiveReturnPct = null, contributionPct = 0.000, pricingMode = null
Include a warning naming the missing group
totalContributionPct = sum of processed groups only (missing group contributes 0)
Review required (more than one missing group)
Still calculate contributions for groups that have data
Missing groups: same as degraded (contributionPct = 0.000)
Include warning messages for each missing group
totalContributionPct = sum including zeros for missing groups
Fallback pricing
When returnPct is null/absent and fallbackReturnPct is present:

Use fallbackReturnPct as effectiveReturnPct
Set pricingMode = FALLBACK_USED
Add a warning, e.g. "fallback return used for group: Equity"
Status remains VALID (unless degraded/review rules also apply)
Warnings vs reasons
Use warnings (array of strings) in the attribution response
Empty array for VALID with no fallbacks
Daily-return continues to use reasons — do not change that API
Validation Rules
Return INVALID_INPUT when:

Total weight is less than 99% OR greater than 101%
groups is null or empty
Any groupName is null or blank
Any weightPct is negative
beginMarketValue-style rules do not apply here — only the rules below
Reuse the Assessment 1 validation style:

Bean Validation (@NotBlank, @NotNull, @Valid) on the request DTO → HTTP 400
Business rules in AttributionValidator → INVALID_INPUT in response body with HTTP 200
Weight tolerance
Total weight in [99, 101] inclusive is valid
Example: weights 60 + 30 + 10 = 100 → valid
Example: weights 50 + 30 = 80 → INVALID_INPUT
When INVALID_INPUT
totalContributionPct = null
groups = empty list or null contributions as appropriate
warnings contains the rejection reason(s)
degraded = false
Idempotency
Support simple idempotent behaviour using requestId.

Rules
requestId is required on every request (@NotBlank)
If the same requestId is received again, return the previously stored response without recalculating
If the same requestId is received with a different request body, return the cached response (first-write-wins) and log a warning — do not recalculate
Implementation
Use a simple in-memory ConcurrentHashMap<String, AttributionResponse> (preferred — no database exists today).

No TTL required
Document in README: cache is lost on application restart
Do not add H2/database unless you have a strong reason; in-memory is sufficient for this assessment
Place in application/idempotency/IdempotencyStore.java or similar — keep it simple.

Logging
Log when a duplicate requestId is detected.

API Contract
Endpoint
POST /api/performance/attribution
Content-Type: application/json
Add to existing PerformanceController (inject AttributionService alongside DailyReturnService).

Request: AttributionRequest
Field	Type	Required	Description
requestId
string
Yes
Idempotency key
portfolioId
string
Yes
Portfolio identifier
valuationDate
date
Yes
YYYY-MM-DD
groups
array
Yes
At least one GroupRequest
requestedBy
string
Yes
Submitter identifier
Request: GroupRequest
Field	Type	Required
groupName
string
Yes
weightPct
number
Yes
returnPct
number
No
fallbackReturnPct
number
No
Response: AttributionResponse
Field	Type	Description
requestId
string
Echoed
portfolioId
string
Echoed
valuationDate
date
Echoed
totalContributionPct
number
Sum of contributions (scale 3). null when INVALID_INPUT
status
string
VALID, DEGRADED, REVIEW_REQUIRED, INVALID_INPUT
degraded
boolean
true only when status == DEGRADED
warnings
array of strings
Fallback, degraded, review, or validation messages
groups
array
GroupContribution list
processedAt
timestamp
UTC ISO-8601 (use injected Clock, same as daily-return)
Response: GroupContribution
Field	Type	Description
groupName
string
Echoed
weightPct
number
Echoed
effectiveReturnPct
number
Return used in calculation. null if missing
contributionPct
number
Calculated contribution (scale 3). 0.000 if missing data
pricingMode
string
PRIMARY, FALLBACK_USED, or null if no return data
HTTP status codes (same as daily-return)
Situation	HTTP status
Missing required field, invalid JSON
400 Bad Request
Business outcome (any status in body)
200 OK
Sample Request
{
  "requestId": "REQ-ATTR-001",
  "portfolioId": "PF-1001",
  "valuationDate": "2026-06-14",
  "requestedBy": "advisor01",
  "groups": [
    { "groupName": "Equity", "weightPct": 60, "returnPct": 2.5 },
    { "groupName": "Fixed Income", "weightPct": 30, "returnPct": 0.8 },
    { "groupName": "Cash", "weightPct": 10, "returnPct": 0.1 }
  ]
}
Expected response (VALID)
{
  "requestId": "REQ-ATTR-001",
  "portfolioId": "PF-1001",
  "valuationDate": "2026-06-14",
  "totalContributionPct": 1.730,
  "status": "VALID",
  "degraded": false,
  "warnings": [],
  "groups": [
    {
      "groupName": "Equity",
      "weightPct": 60,
      "effectiveReturnPct": 2.5,
      "contributionPct": 1.500,
      "pricingMode": "PRIMARY"
    },
    {
      "groupName": "Fixed Income",
      "weightPct": 30,
      "effectiveReturnPct": 0.8,
      "contributionPct": 0.240,
      "pricingMode": "PRIMARY"
    },
    {
      "groupName": "Cash",
      "weightPct": 10,
      "effectiveReturnPct": 0.1,
      "contributionPct": 0.010,
      "pricingMode": "PRIMARY"
    }
  ],
  "processedAt": "2026-06-14T10:30:00Z"
}
Verification: Equity: 60 × 2.5 / 100 = 1.500; Fixed Income: 30 × 0.8 / 100 = 0.240; Cash: 10 × 0.1 / 100 = 0.010; Total: 1.730.

Project Structure (add only these files)
All paths under portfolio-performance/src/main/java/com/portfolio/performance/:

api/controller/PerformanceController.java     # add attribution endpoint + inject AttributionService
api/dto/AttributionRequest.java
api/dto/AttributionResponse.java
api/dto/GroupRequest.java
api/dto/GroupContribution.java
application/service/AttributionService.java
application/validation/AttributionValidator.java
application/calculation/AttributionCalculator.java
application/idempotency/IdempotencyStore.java   # ConcurrentHashMap
domain/AttributionStatus.java
domain/CalculationConstants.java                # extend with ATTRIBUTION_SCALE
Tests under portfolio-performance/src/test/java/...:

api/controller/PerformanceControllerAttributionTest.java       # @WebMvcTest (mock service)
api/controller/PerformanceControllerAttributionIntegrationTest.java  # @SpringBootTest end-to-end
application/service/AttributionServiceTest.java
application/validation/AttributionValidatorTest.java
application/calculation/AttributionCalculatorTest.java
Do not modify existing daily-return test classes.

Logging
Follow the exact same logging style as Assessment 1. Reuse existing logger configuration.

Log (at appropriate levels via existing request logging or service-level logs):

Incoming attribution request (requestId, portfolioId)
Successful response (status, totalContributionPct)
Fallback usage per group
Degraded processing
Duplicate requestId detection
Exceptions (handled by GlobalExceptionHandler)
Do not introduce a different logging framework or format.

Testing
Reuse Assessment 1 testing approach: JUnit 5, Mockito, MockMvc.

Required test scenarios
#	Scenario	Expected status
1
Valid request (all PRIMARY)
VALID
2
Invalid total weight (e.g. 80%)
INVALID_INPUT
3
Fallback pricing (returnPct null, fallbackReturnPct present)
VALID + warning + FALLBACK_USED
4
Degraded (exactly one group missing both returns)
DEGRADED, degraded=true
5
Review required (two+ groups missing both returns)
REVIEW_REQUIRED
6
Duplicate requestId (idempotency)
Same response, no recalculation
7
Missing required field (e.g. requestId)
HTTP 400
Include at least one @SpringBootTest integration test that exercises the full stack (mirroring PerformanceControllerIntegrationTest).

Documentation
README.md
Append a new section (do not rewrite or remove Assessment 1 content):

Attribution API endpoint
Request / response examples (including gateway curl on port 8080)
Business rules and formula
Validation rules (weight 99–101%)
Pricing modes (PRIMARY, FALLBACK_USED)
Degraded processing
Idempotency behaviour and restart limitation
Assumptions (in-memory cache, scale 3, status precedence)
Note that daily-return remains unchanged
prompt-log.md
Append Assessment 2 prompts and outcomes. Do not overwrite Assessment 1 history.

Reusable AI Agent
Create a simple reusable Markdown checklist at:

.cursor/skills/resiliency-reviewer/SKILL.md
(Matches Assessment 1 pattern: .cursor/skills/calculation-reviewer/SKILL.md)

The skill should review:

Validation (weight band, empty groups, negative weights)
Contribution calculation (formula, scale 3, rounding)
Fallback logic (PRIMARY vs FALLBACK_USED)
Degraded processing (exactly one missing)
Review required (two+ missing)
Status precedence
Idempotency (duplicate requestId)
Edge cases (empty list, all missing, both returns present)
Test coverage (all 7 scenarios above)
Non-regression of daily-return
Keep it simple, with pass/fail checklist items and a sample verification case (the 60/30/10 example above).

Coding Guidelines
Keep the implementation simple
Reuse Assessment 1 patterns — records, constructor injection, small focused classes
AttributionCalculator only calculates; AttributionValidator only validates; AttributionService orchestrates
No unnecessary abstractions, interfaces, or design patterns
Meaningful class and method names
JavaDoc on public APIs
Easy to explain in an interview
Expected Outcome
The application exposes both endpoints:

POST /api/performance/daily-return      (Assessment 1 — unchanged behaviour)
POST /api/performance/attribution       (Assessment 2 — new)
Both accessible via gateway at http://localhost:8080/api/performance/....

Success criteria
mvn clean verify passes (all Assessment 1 + new Assessment 2 tests)
Assessment 1 APIs and tests unchanged in behaviour
Assessment 2 is a clean extension: same architecture, same conventions, same HTTP/logging patterns
Demonstrates: foundation (A1) → extended capability (A2) with fallback, degraded processing, and idempotency — without unnecessary complexity
Assumptions (document in README)
Attribution percentages use scale 3; daily-return remains scale 2
Idempotency cache is in-memory and lost on restart
Same requestId with different body returns the first cached response
Missing group contribution is 0.000, not excluded from the groups list
Gateway requires no changes — route already covers /api/performance/**
No database is introduced for this assessment

---

## Assessment 2 — Implementation Outcome

**Date:** 2026-06-30  
**Prompt:** Implement Portfolio Performance Attribution endpoint on top of Assessment 1.

### What was built

- `POST /api/performance/attribution` added to `PerformanceController`
- New classes: DTOs (`AttributionRequest`, `AttributionResponse`, `GroupRequest`, `GroupContribution`), `AttributionService`, `AttributionValidator`, `AttributionCalculator`, `IdempotencyStore`, `AttributionStatus`, `PricingMode`
- Extended `CalculationConstants` with `ATTRIBUTION_SCALE = 3`, weight band constants
- 22 new tests (55 total in `portfolio-performance`); all Assessment 1 tests unchanged
- README attribution section, `.cursor/skills/resiliency-reviewer/SKILL.md`

### Verification

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn clean verify   # BUILD SUCCESS — 55 tests in portfolio-performance
```

### Note on sample total

The assignment sample lists `totalContributionPct: 1.730`, but the stated components sum to **1.750** (1.500 + 0.240 + 0.010). Implementation follows the formula; tests assert **1.750**.