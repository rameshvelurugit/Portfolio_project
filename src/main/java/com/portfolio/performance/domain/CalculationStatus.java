package com.portfolio.performance.domain;

/**
 * Outcome of a daily return calculation.
 *
 * <ul>
 *   <li>{@link #VALID} — inputs and results are acceptable</li>
 *   <li>{@link #REVIEW_REQUIRED} — calculation succeeded but manual review is recommended</li>
 *   <li>{@link #INVALID_INPUT} — inputs failed business validation</li>
 * </ul>
 */
public enum CalculationStatus {
    VALID,
    REVIEW_REQUIRED,
    INVALID_INPUT
}
