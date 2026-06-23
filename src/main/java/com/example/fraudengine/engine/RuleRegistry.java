package com.example.fraudengine.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.example.fraudengine.domain.FraudRule;
import com.example.fraudengine.repository.FraudRuleRepository;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Maintains the registry of active {@link FraudRuleStrategy} instances and their
 * corresponding database configuration.
 *
 * <p>Rule strategies are Spring beans — they are injected by the Spring container.
 * The database-backed {@link FraudRule} configurations are loaded on first access and
 * refreshed when rules are updated via the REST API.</p>
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleRegistry {

    private final List<FraudRuleStrategy> ruleStrategies;
    private final FraudRuleRepository fraudRuleRepository;

    private volatile Map<String, FraudRule> configCache;

    public List<FraudRuleStrategy> getActiveRules() {
        Map<String, FraudRule> configs = getConfigs();
        return ruleStrategies.stream()
                .filter(r -> {
                    FraudRule cfg = configs.get(r.getRuleName());
                    return cfg != null && Boolean.TRUE.equals(cfg.getEnabled());
                })
                .toList();
    }

    public FraudRule getConfig(String ruleName) {
        return getConfigs().get(ruleName);
    }

    /**
     * Called by the service layer after a PATCH update.
     */
    
    public void invalidateCache() {
        synchronized (this) {
            configCache = null;
        }
        log.info("Rule configuration cache invalidated");
    }

    private Map<String, FraudRule> getConfigs() {
        if (configCache == null) {
            synchronized (this) {
                if (configCache == null) {
                    configCache = fraudRuleRepository.findAll().stream()
                            .collect(Collectors.toMap(FraudRule::getRuleName, Function.identity()));
                    log.info("Loaded {} rule configurations from database", configCache.size());
                }
            }
        }
        return configCache;
    }
}
