package com.sorted.rest.services.payment.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Data
public class TransactionResponseBean implements Serializable {

	private static final long serialVersionUID = 2989920741806143595L;

	private UUID id;

	private UUID customerId;

	private UUID orderId;

	private String storeId;

	private Double amount;

	private String paymentMode;

	private String paymentGateway;

	private String medium;

	private String referenceId;

	private String status;

	private TxnMetadataResponse metadata;

	private Date processedAt;

	private String entityType;

}
