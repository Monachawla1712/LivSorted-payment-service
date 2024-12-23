package com.sorted.rest.services.payment.beans;

import java.io.Serializable;
import java.sql.Date;
import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.sorted.rest.services.payment.constants.PaymentConstants.CashCollectionStatus;

import lombok.Data;

@Data
public class CollectConsumerCcRequest implements Serializable {

	private static final long serialVersionUID = 8376574842543219203L;

	@NotNull
	private UserDetail user;

	private Long ccId;

	private List<String> images;

	@NotNull
	private CashCollectionStatus status;

	private String txnMode;

	private String remarks;

	@NotNull
	private Date date;

	@NotEmpty
	private String customerId;

	private Double collectedAmount;

	private String storeId;

	private Boolean isFailedByHandPicked  = Boolean.FALSE;

	public static CollectConsumerCcRequest newInstance() {
		return new CollectConsumerCcRequest();
	}
}
