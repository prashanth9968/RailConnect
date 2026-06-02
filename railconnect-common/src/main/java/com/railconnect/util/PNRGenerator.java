package com.railconnect.util;

import org.springframework.stereotype.Component;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class PNRGenerator {
    private static final AtomicLong counter = new AtomicLong(1000000000L);

    public String generate() {
        // Format: 10-digit numeric PNR like IRCTC
        return String.valueOf(counter.incrementAndGet());
    }
}
