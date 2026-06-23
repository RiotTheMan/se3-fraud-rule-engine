package com.example.fraudengine.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.example.fraudengine.domain.Transaction;
import com.example.fraudengine.web.dto.TransactionResponse;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(source = "customer.customerId", target = "customerId")
    TransactionResponse toResponse(Transaction transaction);
}
