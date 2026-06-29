# Prompt Log

Chronological record of AI prompts used to build this project.

## 2026-06-29 — Architecture and planning

**Prompt:** Understand requirements from `Portfolio_Assignment_V2.docx` and create an implementation plan as a Senior Java Architect.

**Outcome:** Layered Spring Boot architecture defined with packages, business rules, test matrix, and deliverables list.

## 2026-06-29 — Implementation

**Prompt:** Build this project by following clean code principles and industry best practices. The code should be easy to read, understand, and maintain, especially for junior developers.

**Outcome:** Full Maven project scaffolded with:

- `PerformanceController` REST endpoint
- `DailyReturnService` orchestration
- `DailyReturnValidator` business rules
- `ReturnCalculator` BigDecimal math
- `GlobalExceptionHandler` for request validation errors
- Unit and MockMvc tests for all three statuses
- README and Calculation Reviewer skill

## 2026-06-29 — Calculation Reviewer skill

**Prompt:** (Assignment requirement) Create reusable AI Agent named "Calculation Reviewer" to validate formulas, business rules, edge cases, and test coverage.

**Outcome:** Project skill at `.cursor/skills/calculation-reviewer/SKILL.md`.
