package com.sorted.rest.services.payment.beans;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Date;

@Data
public class CollectCcRequest implements Serializable {
    private static final long serialVersionUID = 8376574842543219203L;

    @NotNull
    private UserDetail user;

    @NotEmpty
    private String status;

    @NotNull
    private Date date;

    @NotEmpty
    private String slot;

    @NotEmpty
    private String storeId;

    private Double collectedAmount;

    public static CollectCcRequest newInstance() {
        return new CollectCcRequest();
    }
}
