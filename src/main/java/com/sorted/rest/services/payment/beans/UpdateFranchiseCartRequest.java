package com.sorted.rest.services.payment.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

@Data
@ApiModel(description = "Update Franchise Store Cart Request Bean")
public class UpdateFranchiseCartRequest implements Serializable {

	private static final long serialVersionUID = -8927594493563844997L;

	private String storeId;

	private Double walletAmount;

}
