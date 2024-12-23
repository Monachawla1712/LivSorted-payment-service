package com.sorted.rest.services.payment.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class JuspaySessionResponse implements Serializable {

	private static final long serialVersionUID = 8453503875733187507L;

	private String status;

	private String id;

	@JsonProperty("order_id")
	private String orderId;

	@JsonProperty("payment_links")
	private PaymentLinks paymentLinks;

	@JsonProperty("sdk_payload")
	private SdkPayload sdkPayload;

	@Data
	public static class PaymentLinks implements Serializable {

		private static final long serialVersionUID = 8453503875733187507L;

		private String web;

		private String expiry;
	}

	@Data
	public static class SdkPayload implements Serializable {

		private static final long serialVersionUID = 8453503875733187507L;

		private String requestId;

		private String service;

		private Payload payload;
	}

	@Data
	public static class Payload implements Serializable {

		private static final long serialVersionUID = 8453503875733187507L;

		private String clientId;

		private String amount;

		private String merchantId;

		private String clientAuthToken;

		private String clientAuthTokenExpiry;

		private String environment;

		private String optionsGetUpiDeepLinks;

		private String lastName;

		private String action;

		private String customerId;

		private String returnUrl;

		private String currency;

		private String firstName;

		private String customerPhone;

		private String customerEmail;

		private String orderId;

		private String description;
	}
}