package com.avairebot.contracts.servlet;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;

import java.time.Duration;

public enum LimitPlan {
    FREE {
        Bandwidth getLimit() {
            return Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
        }
    },
    PARTNERED {
        Bandwidth getLimit() {
            return Bandwidth.classic(40, Refill.intervally(40, Duration.ofMinutes(1)));
        }
    },
    PINEWOOD {
        Bandwidth getLimit() {
            return Bandwidth.classic(60, Refill.intervally(100, Duration.ofMinutes(1)));
        }
    };

    static LimitPlan resolvePlanFromApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return FREE;
        } else if (apiKey.startsWith("FACILITATOR-")) {
            return PINEWOOD;
        } else if (apiKey.startsWith("PB-PARTNER-")) {
            return PARTNERED;
        }
        return FREE;
    }
}

