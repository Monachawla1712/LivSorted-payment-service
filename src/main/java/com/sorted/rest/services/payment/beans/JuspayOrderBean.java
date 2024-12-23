package com.sorted.rest.services.payment.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class JuspayOrderBean implements Serializable {

	private static final long serialVersionUID = 8453503875733187507L;

	private String id;

	private String status;

	private Double amount;

	@JsonProperty("order_id")
	private String orderId;

	@JsonProperty("payment_method_type")
	private String paymentMethodType;

	@JsonProperty("txn_detail")
	private TxnDetail txnDetail;

	@Data
	public static class TxnDetail implements Serializable {

		private static final long serialVersionUID = 8453503875733187507L;

		private String gateway;

		@JsonProperty("error_message")
		private String errorMsg;
	}
}