package com.sorted.rest.services.payment.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class JuspaySessionRequest implements Serializable {

	private static final long serialVersionUID = 8453503875733187507L;

	@JsonProperty("order_id")
	private String orderId;

	@JsonProperty("customer_id")
	private String customerId;

	private Double amount;

	@JsonProperty("payment_page_client_id")
	private String paymentPageClientId;

	@JsonProperty("return_url")
	private String returnUrl;
}