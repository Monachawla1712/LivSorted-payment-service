package com.sorted.rest.services.payment.beans;

import com.sorted.rest.services.payment.constants.PaymentConstants.CashCollectionStatus;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;

@Data
public class CashCollectionRequest implements Serializable {

	private static final long serialVersionUID = 8376574842543219203L;

	@NotNull
	private CashCollectionStatus status;

	@NotEmpty
	private String storeId;

	private UUID userId;

	private String remarks;

	private String txnMode;

	private Double amount;

	private Double billAmount;

	private String verificationCode;

	public static CashCollectionRequest newInstance() {
		return new CashCollectionRequest();
	}
}
