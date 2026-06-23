package com.example.fraudengine.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.fraudengine.domain.FraudRule;
import com.example.fraudengine.engine.RuleRegistry;
import com.example.fraudengine.repository.FraudRuleRepository;
import com.example.fraudengine.web.dto.PatchRuleRequest;
import com.example.fraudengine.web.dto.RuleResponse;
import com.example.fraudengine.web.mapper.RuleMapper;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
public class RuleController {

    private final FraudRuleRepository fraudRuleRepository;
    private final RuleMapper ruleMapper;
    private final RuleRegistry ruleRegistry;

    @GetMapping
    @PreAuthorize("hasAnyRole(@jwtSecurityConfiguration.getReadRole(), @jwtSecurityConfiguration.getWriteRole())")
    public ResponseEntity<List<RuleResponse>> listRules() {
        List<RuleResponse> rules = fraudRuleRepository.findAll()
                .stream()
                .map(ruleMapper::toResponse)
                .toList();
        return ResponseEntity.ok(rules);
    }

    /**
     * Updates rule configuration. Only non-null request fields are applied.
     * Invalidates the rule registry cache after a successful update.
     */
    @PatchMapping("/{ruleId}")
    @PreAuthorize("hasRole(@jwtSecurityConfiguration.getWriteRole())")
    public ResponseEntity<RuleResponse> patchRule(@PathVariable Long ruleId,
                                                   @Valid @RequestBody PatchRuleRequest request) {
        FraudRule rule = fraudRuleRepository.findById(ruleId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "FraudRule not found with id=" + ruleId));

        if (request.enabled() != null) rule.setEnabled(request.enabled());
        if (request.severity() != null) rule.setSeverity(request.severity());
        if (request.threshold() != null) rule.setThreshold(request.threshold());
        if (request.windowMinutes() != null) rule.setWindowMinutes(request.windowMinutes());

        FraudRule saved = fraudRuleRepository.save(rule);

        // Invalidate the in-memory cache so the engine picks up the new config on next evaluation
        ruleRegistry.invalidateCache();

        return ResponseEntity.ok(ruleMapper.toResponse(saved));
    }
}
