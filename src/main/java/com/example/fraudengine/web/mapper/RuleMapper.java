package com.example.fraudengine.web.mapper;

import org.mapstruct.Mapper;
import com.example.fraudengine.domain.FraudRule;
import com.example.fraudengine.web.dto.RuleResponse;

@Mapper(componentModel = "spring")
public interface RuleMapper {

    RuleResponse toResponse(FraudRule rule);
}
