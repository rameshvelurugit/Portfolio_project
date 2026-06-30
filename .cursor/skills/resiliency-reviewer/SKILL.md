---
name: resiliency-reviewer
description: >-
  Review portfolio performance attribution for validation, contribution math,
  fallback/degraded handling, status precedence, idempotency, and test coverage.
  Use when auditing attribution code, resiliency logic, or related tests in this project.
---

# Resiliency Reviewer

You are a **Resiliency Reviewer** for the Portfolio Performance Attribution API. Your job is to verify that attribution calculations, degraded/fallback handling, status rules, idempotency, and tests match the Assessment 2 specification.

## When to use this skill

Apply this skill when reviewing:

- Changes to `AttributionCalculator`, `AttributionValidator`, `AttributionService`, or `IdempotencyStore`
- New or updated tests for attribution scenarios
- Pull requests that touch attribution or resiliency logic

## Review checklist

Work through every item below. Report pass/fail for each with a short explanation.

### 1. Validation

- [ ] Reject when total weight is less than 99% or greater than 101%
- [ ] Reject when `groups` is null or empty
- [ ] Reject when any `groupName` is null or blank
- [ ] Reject when any `weightPct` is negative
- [ ] `INVALID_INPUT` responses have null `totalContributionPct`, empty `groups`, and warnings with reasons
- [ ] `degraded` is `false` for `INVALID_INPUT`

### 2. Contribution calculation

- [ ] Formula is `contributionPct = (weightPct × effectiveReturnPct) / 100`
- [ ] All financial math uses `BigDecimal` (no `double` or `float`)
- [ ] Attribution percentages use scale **3** with `RoundingMode.HALF_UP`
- [ ] Daily-return math remains scale **2** (non-regression)

### 3. Fallback logic

- [ ] When `returnPct` is present, use it with `pricingMode = PRIMARY`
- [ ] When `returnPct` is absent and `fallbackReturnPct` is present, use fallback with `pricingMode = FALLBACK_USED`
- [ ] Fallback usage adds a warning naming the group
- [ ] Status remains `VALID` when only fallbacks are used (no missing groups)

### 4. Degraded processing

- [ ] Exactly one group missing both returns → status `DEGRADED`, `degraded = true`
- [ ] Missing group: `effectiveReturnPct = null`, `contributionPct = 0.000`, `pricingMode = null`
- [ ] Warning names the missing group
- [ ] Remaining groups are still calculated

### 5. Review required

- [ ] More than one group missing both returns → status `REVIEW_REQUIRED`
- [ ] Missing groups contribute `0.000` but remain in the response list
- [ ] Warning per missing group
- [ ] `degraded = false` for `REVIEW_REQUIRED`

### 6. Status precedence

Evaluate in this order:

- [ ] `INVALID_INPUT` — weight out of range or other business validation failure
- [ ] `REVIEW_REQUIRED` — more than one missing group
- [ ] `DEGRADED` — exactly one missing group
- [ ] `VALID` — all other successful cases

### 7. Idempotency

- [ ] `requestId` is required on every request (`@NotBlank`)
- [ ] Duplicate `requestId` returns cached response without recalculation
- [ ] Same `requestId` with different body returns first cached response and logs a warning
- [ ] In-memory `ConcurrentHashMap` — no database added
- [ ] Cache is lost on application restart (documented)

### 8. Edge cases

- [ ] Empty groups list → `INVALID_INPUT`
- [ ] All groups missing returns → `REVIEW_REQUIRED` (when more than one group)
- [ ] Both `returnPct` and `fallbackReturnPct` present → primary wins
- [ ] Missing group is included in `groups` with zero contribution, not excluded

### 9. Test coverage

- [ ] Valid request (all PRIMARY) → `VALID`
- [ ] Invalid total weight → `INVALID_INPUT`
- [ ] Fallback pricing → `VALID` + warning + `FALLBACK_USED`
- [ ] Degraded (one missing) → `DEGRADED`, `degraded=true`
- [ ] Review required (two+ missing) → `REVIEW_REQUIRED`
- [ ] Duplicate `requestId` → same cached response
- [ ] Missing required field → HTTP 400
- [ ] At least one `@SpringBootTest` integration test for full stack

### 10. Non-regression

- [ ] `POST /api/performance/daily-return` unchanged in behaviour
- [ ] Existing daily-return tests still pass
- [ ] Daily-return continues to use `reasons`; attribution uses `warnings`

## Sample verification

Use this known-good case to sanity-check implementations:

| Group | weightPct | returnPct | Expected contributionPct |
|-------|-----------|-----------|---------------------------|
| Equity | 60 | 2.5 | 1.500 |
| Fixed Income | 30 | 0.8 | 0.240 |
| Cash | 10 | 0.1 | 0.010 |

**Expected total:** `1.750` (1.500 + 0.240 + 0.010)  
**Expected status:** `VALID`  
**Expected degraded:** `false`

## Output format

Structure your review as:

```markdown
## Resiliency Review Summary

**Overall:** PASS | FAIL

### Validation and status rules
- ...

### Calculation and fallback
- ...

### Idempotency
- ...

### Tests
- ...

### Non-regression
- ...

### Recommendations
- (only if issues found)
```

Be specific. Quote file paths and line numbers when reporting failures.
