package com.sorted.rest.services.payment.beans;

import com.sorted.rest.services.payment.constants.PaymentConstants.CashCollectionStatus;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class FosCcOtpRequest implements Serializable {

	private static final long serialVersionUID = 8376574842543219203L;

	@NotEmpty
	private String storeId;

	@NotEmpty
	private String txnMode;

	@NotNull
	private Double amount;

	public static FosCcOtpRequest newInstance() {
		return new FosCcOtpRequest();
	}
}
