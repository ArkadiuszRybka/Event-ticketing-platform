package com.rybka.ticketing.controller;

import com.rybka.ticketing.service.PaymentWebHookService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/webhook")
@RequiredArgsConstructor
public class PaymentWebHookController {
    @Autowired
    private PaymentWebHookService webHookService;

    @PostMapping
    public ResponseEntity<?> handle(
            @RequestHeader("X-Provider-Signature") String signature,
            @RequestBody String rawBody
    ){
        var res = webHookService.handle(rawBody, signature);
        return ResponseEntity.ok(res);
    }
}
