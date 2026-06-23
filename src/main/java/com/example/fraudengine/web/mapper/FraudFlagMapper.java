package com.example.fraudengine.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.example.fraudengine.domain.FraudFlag;
import com.example.fraudengine.web.dto.FraudFlagDetailResponse;
import com.example.fraudengine.web.dto.FraudFlagResponse;

@Mapper(componentModel = "spring")
public interface FraudFlagMapper {

    @Mapping(source = "transaction.transactionId", target = "transactionId")
    @Mapping(source = "customer.customerId", target = "customerId")
    FraudFlagResponse toResponse(FraudFlag flag);

    @Mapping(source = "customer.customerId", target = "customerId")
    @Mapping(source = "customer.fullName", target = "customerFullName")
    @Mapping(source = "transaction", target = "transaction")
    FraudFlagDetailResponse toDetailResponse(FraudFlag flag);

    @Mapping(source = "transactionId", target = "transactionId")
    @Mapping(source = "amount", target = "amount")
    @Mapping(source = "currency", target = "currency")
    @Mapping(source = "merchantName", target = "merchantName")
    @Mapping(source = "transactionAt", target = "transactionAt")
    FraudFlagDetailResponse.TransactionSummary toTransactionSummary(
            com.example.fraudengine.domain.Transaction transaction);
}
