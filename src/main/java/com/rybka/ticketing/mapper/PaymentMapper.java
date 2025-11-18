package com.rybka.ticketing.mapper;

import com.rybka.ticketing.model.dto.payment.PaymentReadDto;
import com.rybka.ticketing.model.entity.Payment;

public class PaymentMapper {
    public static PaymentReadDto toRead(Payment p){
        return new PaymentReadDto(
                p.getId(),
                p.getOrder().getId(),
                p.getProviderRef(),
                p.getStatus().name(),
                p.getAmount(),
                p.getCurrency(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
