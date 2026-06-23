package com.example.fraudengine.web.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RuleResponse(
        Long id,
        String ruleName,
        String description,
        Boolean enabled,
        String severity,
        BigDecimal threshold,
        Integer windowMinutes,
        OffsetDateTime updatedAt
) {
}
