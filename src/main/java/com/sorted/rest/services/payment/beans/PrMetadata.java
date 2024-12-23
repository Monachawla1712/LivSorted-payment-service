package com.sorted.rest.services.payment.beans;

import lombok.Data;

@Data
public class PrMetadata {
    private UserDetail requestedBy;
    private UserDetail approvedBy;
}
