package com.sorted.rest.services.payment.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class RazorpayOrderPaidRequest implements Serializable {

	private static final long serialVersionUID = 8453503875733187507L;

	@JsonProperty("entity")
	private String entity;

	@JsonProperty("account_id")
	private String accountId;

	@JsonProperty("event")
	private String event;

	@JsonProperty("contains")
	private List<String> contains = new ArrayList<String>();

	@JsonProperty("payload")
	private Payload payload;

	@JsonProperty("created_at")
	private Integer createdAt;

	@Data
	public static class Card implements Serializable {

		@JsonProperty("id")
		private String id;

		@JsonProperty("entity")
		private String entity;

		@JsonProperty("name")
		private String name;

		@JsonProperty("last4")
		private String last4;

		@JsonProperty("network")
		private String network;

		@JsonProperty("type")
		private String type;

		@JsonProperty("issuer")
		private Object issuer;

		@JsonProperty("international")
		private Boolean international;

		@JsonProperty("emi")
		private Boolean emi;

		private final static long serialVersionUID = 570429009662716977L;

	}

	@Data
	public static class RazorpayPaymentEntity implements Serializable {

		@JsonProperty("id")
		private String id;

		@JsonProperty("entity")
		private String entity;

		@JsonProperty("amount")
		private Integer amount;

		@JsonProperty("currency")
		private String currency;

		@JsonProperty("status")
		private String status;

		@JsonProperty("order_id")
		private String orderId;

		@JsonProperty("invoice_id")
		private Object invoiceId;

		@JsonProperty("international")
		private Boolean international;

		@JsonProperty("method")
		private String method;

		@JsonProperty("amount_refunded")
		private Integer amountRefunded;

		@JsonProperty("refund_status")
		private Object refundStatus;

		@JsonProperty("captured")
		private Boolean captured;

		@JsonProperty("description")
		private String description;

		@JsonProperty("card_id")
		private String cardId;

		@JsonProperty("card")
		private Card card;

		@JsonProperty("bank")
		private String bank;

		@JsonProperty("wallet")
		private String wallet;

		@JsonProperty("vpa")
		private String vpa;

		@JsonProperty("email")
		private String email;

		@JsonProperty("contact")
		private String contact;

		@JsonProperty("notes")
		private List<Object> notes = new ArrayList<Object>();

		@JsonProperty("fee")
		private Integer fee;

		@JsonProperty("tax")
		private Integer tax;

		@JsonProperty("error_code")
		private String errorCode;

		@JsonProperty("error_description")
		private String errorDescription;

		@JsonProperty("error_source")
		private String errorSource;

		@JsonProperty("error_reason")
		private String errorReason;

		@JsonProperty("created_at")
		private Integer createdAt;

		private final static long serialVersionUID = 3026550760077731997L;

	}

	@Data
	public static class RazorpayOrderEntity implements Serializable {

		@JsonProperty("id")
		private String id;

		@JsonProperty("entity")
		private String entity;

		@JsonProperty("amount")
		private Integer amount;

		@JsonProperty("amount_paid")
		private Integer amountPaid;

		@JsonProperty("amount_due")
		private Integer amountDue;

		@JsonProperty("currency")
		private String currency;

		@JsonProperty("receipt")
		private String receipt;

		@JsonProperty("offer_id")
		private Object offerId;

		@JsonProperty("status")
		private String status;

		@JsonProperty("attempts")
		private Integer attempts;

		@JsonProperty("notes")
		private List<Object> notes = new ArrayList<Object>();

		@JsonProperty("created_at")
		private Integer createdAt;

		private final static long serialVersionUID = 1878836651307549414L;

	}

	@Data
	public static class Order implements Serializable {

		@JsonProperty("entity")
		private RazorpayOrderEntity entity;

		private final static long serialVersionUID = -6920216036995709509L;

	}

	@Data
	public static class Payment implements Serializable {

		@JsonProperty("entity")
		private RazorpayPaymentEntity entity;

		private final static long serialVersionUID = 2030125548008491338L;

	}

	@Data
	public static class Payload implements Serializable {

		@JsonProperty("payment")
		private Payment payment;

		@JsonProperty("order")
		private Order order;

		private final static long serialVersionUID = 5665360845514107040L;

	}
}
