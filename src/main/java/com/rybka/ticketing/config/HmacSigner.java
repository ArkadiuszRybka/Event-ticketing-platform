package com.rybka.ticketing.config;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public final class HmacSigner {
    private HmacSigner(){}

    public static String sha256Hex(String secret, String payload){
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length *2);
            for (byte b : raw) sb.append(String.format("%02x",b));
            return sb.toString();
        }catch (Exception e){
            throw new IllegalStateException("Hmac error: " + e);
        }
    }
}
