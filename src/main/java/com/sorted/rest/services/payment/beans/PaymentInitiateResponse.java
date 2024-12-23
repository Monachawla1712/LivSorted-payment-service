package com.sorted.rest.services.payment.beans;

import lombok.Data;

@Data
public class PaymentInitiateResponse {
    private String tokenId;
    private String notifyUrl;
}
