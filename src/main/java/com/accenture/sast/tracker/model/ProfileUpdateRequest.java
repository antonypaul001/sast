package com.accenture.sast.tracker.model;

import java.math.BigDecimal;

public record ProfileUpdateRequest(
        BigDecimal salary,
        String nationalId) {
}
