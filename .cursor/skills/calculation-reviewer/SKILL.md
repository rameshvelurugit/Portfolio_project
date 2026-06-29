---
name: calculation-reviewer
description: >-
  Review portfolio daily return calculations for formula correctness, business
  rules, edge cases, and test coverage. Use when auditing daily-return code,
  return math, validation logic, or related tests in this project.
---

# Calculation Reviewer

You are a **Calculation Reviewer** for the Portfolio Performance API. Your job is to verify that return calculations, business rules, and tests match the assignment specification.

## When to use this skill

Apply this skill when reviewing:

- Changes to `ReturnCalculator`, `DailyReturnValidator`, or `DailyReturnService`
- New or updated tests for daily return scenarios
- Pull requests that touch calculation or validation logic

## Review checklist

Work through every item below. Report pass/fail for each with a short explanation.

### 1. Return formula

- [ ] Formula is `((endMarketValue - beginMarketValue - netCashFlow) / beginMarketValue) * 100`
- [ ] When `beginMarketValue == 0` and `endMarketValue == 0`, return is `0.00` (not a division error)
- [ ] `excessReturnPct = portfolioReturnPct - benchmarkReturnPct`

### 2. Numeric precision

- [ ] All money and percentage math uses `BigDecimal` (no `double` or `float`)
- [ ] Final percentages use scale **2** with `RoundingMode.HALF_UP`
- [ ] Intermediate division uses sufficient precision before final rounding

### 3. Invalid input rules

- [ ] Reject when `beginMarketValue < 0`
- [ ] Reject when `endMarketValue < 0`
- [ ] Reject when `currency` is null or blank
- [ ] Reject when `beginMarketValue == 0` and `endMarketValue != 0`
- [ ] `INVALID_INPUT` responses include clear `reasons` and null calculated fields where appropriate

### 4. Review rules

- [ ] `REVIEW_REQUIRED` when `|portfolioReturnPct - benchmarkReturnPct| > 5`
- [ ] `REVIEW_REQUIRED` when `|netCashFlow| > 20%` of `beginMarketValue`
- [ ] Multiple review reasons can appear in the same response
- [ ] Zero begin value: cash-flow threshold is zero, so non-zero cash flow triggers review

### 5. Test coverage

- [ ] Tests exist for `VALID`, `REVIEW_REQUIRED`, and `INVALID_INPUT`
- [ ] Assignment sample (`begin=1000000`, `end=1035000`, `netCashFlow=10000`) produces `2.50%` return
- [ ] Edge cases: zero-zero market values, negative cash flow, multiple validation failures
- [ ] Controller tests cover HTTP 400 for missing fields and HTTP 200 for business outcomes

### 6. Code quality

- [ ] Single responsibility: calculator only calculates, validator only validates
- [ ] Constructor injection (no field `@Autowired`)
- [ ] Public APIs have JavaDoc
- [ ] Constants (thresholds, scales) are centralized, not magic numbers in logic

## Sample verification

Use this known-good case to sanity-check implementations:

| Field | Value |
|-------|-------|
| beginMarketValue | 1,000,000 |
| endMarketValue | 1,035,000 |
| netCashFlow | 10,000 |
| benchmarkReturnPct | 1.8 |

**Expected:**

- portfolioReturnPct: `2.50`
- excessReturnPct: `0.70`
- status: `VALID`

## Output format

Structure your review as:

```markdown
## Calculation Review Summary

**Overall:** PASS | FAIL

### Formula and precision
- ...

### Business rules
- ...

### Tests
- ...

### Recommendations
- (only if issues found)
```

Be specific. Quote file paths and line numbers when reporting failures.
