package com.sorted.rest.services.payment.beans;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Date;

@Data
public class CcCreationRequest implements Serializable {

    private static final long serialVersionUID = 281251572439658776L;

    @NotNull
    private String customerId;

}
