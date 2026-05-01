package com.user.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaResponse {
    private int generationsUsedThisMonth;
    private int generationsLimit;
    private int deploymentsUsedThisMonth;
    private int deploymentsLimit;
}
