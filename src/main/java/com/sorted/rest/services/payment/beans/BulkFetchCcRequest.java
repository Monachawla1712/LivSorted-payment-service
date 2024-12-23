package com.sorted.rest.services.payment.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

@Data
@ApiModel(description = "Bulk Fetch Cash Collection Request")
public class BulkFetchCcRequest implements Serializable {

    private static final long serialVersionUID = -8927594493563844997L;

    @NotEmpty
    private List<String> customerIds;

    private String date;
}