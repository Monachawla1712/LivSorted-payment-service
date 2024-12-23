package com.sorted.rest.services.payment.beans;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class UserWalletRequestBean {

	private static final long serialVersionUID = -5571723327178720853L;

	@NotNull
	@Min(value = 0, message = "Value should be greater than or equal to 0")
	private Double creditLimit;
}
