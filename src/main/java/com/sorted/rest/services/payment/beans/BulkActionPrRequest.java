package com.sorted.rest.services.payment.beans;

import com.sorted.rest.services.payment.constants.PaymentConstants.PaymentRequestStatus;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
public class BulkActionPrRequest implements Serializable {
    private static final long serialVersionUID = 8376574842543219203L;

    @NotNull
    private PaymentRequestStatus status;

    @NotNull
    private List<Long> ids;

    public static BulkActionPrRequest newInstance() {
        return new BulkActionPrRequest();
    }
}
