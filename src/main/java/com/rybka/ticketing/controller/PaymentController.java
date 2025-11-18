package com.rybka.ticketing.controller;

import com.rybka.ticketing.model.dto.payment.PaymentInitRequest;
import com.rybka.ticketing.model.dto.payment.PaymentInitResposne;
import com.rybka.ticketing.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentInitResposne> init(
            @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
            @RequestBody PaymentInitRequest req
    ) {
        var resp = paymentService.init(req, idemKey);
        return ResponseEntity.status(201).body(resp);
    }
}
