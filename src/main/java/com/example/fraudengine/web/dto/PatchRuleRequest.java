package com.example.fraudengine.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request body for PATCH /api/v1/rules/{ruleId}.
 * All fields are optional — only non-null values are applied.
 */
public record PatchRuleRequest(
        Boolean enabled,
        @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL", message = "severity must be LOW, MEDIUM, HIGH or CRITICAL")
        String severity,
        @DecimalMin(value = "0.0", inclusive = false, message = "threshold must be positive")
        BigDecimal threshold,
        @Positive(message = "windowMinutes must be positive")
        Integer windowMinutes
) {
}
