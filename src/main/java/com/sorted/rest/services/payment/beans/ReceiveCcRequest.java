package com.sorted.rest.services.payment.beans;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.sorted.rest.services.payment.constants.PaymentConstants.CashCollectionStatus;

import lombok.Data;

@Data
public class ReceiveCcRequest implements Serializable {
    private static final long serialVersionUID = 8376574842543219203L;

    @NotNull
    private Long id;

    @NotNull
    private UserDetail user;

    @NotNull
    private CashCollectionStatus status;

    private Double receivedAmount;

    public static ReceiveCcRequest newInstance() {
        return new ReceiveCcRequest();
    }
}
