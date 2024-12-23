package com.sorted.rest.services.payment.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sorted.rest.services.payment.constants.PaymentConstants.PaymentStatus;
import lombok.Data;

import java.io.Serializable;

@Data
public class EasebuzzInstacollectTransactionBean implements Serializable {

	private static final long serialVersionUID = 8453503875733187507L;

	@JsonProperty("event")
	private String event;

	@JsonProperty("data")
	private EasebuzzInstacollectTransactionPayload data;

	@Data
	public static class EasebuzzInstacollectTransactionPayload implements Serializable {

		@JsonProperty("id")
		public String id;

		@JsonProperty("created_at")
		public String createdAt;

		@JsonProperty("remitter_full_name")
		public String remitterFullName;

		@JsonProperty("remitter_account_number")
		public String remitterAccountNumber;

		@JsonProperty("remitter_account_ifsc")
		public String remitterAccountIfsc;

		@JsonProperty("remitter_phone_number")
		public Object remitterPhoneNumber;

		@JsonProperty("unique_transaction_reference")
		public String uniqueTransactionReference;

		@JsonProperty("payment_mode")
		public String paymentMode;

		@JsonProperty("amount")
		public String amount;

		@JsonProperty("service_charge")
		public String serviceCharge;

		@JsonProperty("gst_amount")
		public String gstAmount;

		@JsonProperty("service_charge_with_gst")
		public String serviceChargeWithGst;

		@JsonProperty("narration")
		public String narration;

		@JsonProperty("status")
		public String status;

		@JsonProperty("transaction_date")
		public String transactionDate;

		@JsonProperty("virtual_account")
		public EasebuzzInstacollectVAPayload virtualAccount;

		@JsonProperty("Authorization")
		public String authorization;

		private final static long serialVersionUID = 5665360845514107041L;

	}

	@Data
	public static class EasebuzzInstacollectVAPayload implements Serializable {

		@JsonProperty("id")
		public String id;

		@JsonProperty("label")
		public String label;

		@JsonProperty("virtual_account_number")
		public String virtualAccountNumber;

		@JsonProperty("virtual_ifsc_number")
		public String virtualIfscNumber;

		private final static long serialVersionUID = 5665360845514107042L;

	}

	public enum EasebuzzInstacollectEvent {
		TRANSACTION_CREDIT;
	}

	public enum EasebuzzInstacollectPaymentStatus {
		pending(PaymentStatus.IN_PROGRESS), timed_out(PaymentStatus.SUCCESS), unsettled(PaymentStatus.SUCCESS), received(PaymentStatus.SUCCESS), failure(
				PaymentStatus.FAILED), refunded(PaymentStatus.FAILED), partially_refunded(PaymentStatus.FAILED);

		private PaymentStatus value;

		private EasebuzzInstacollectPaymentStatus(PaymentStatus value) {
			this.value = value;
		}

		public PaymentStatus getValue() {
			return value;
		}

		public static EasebuzzInstacollectPaymentStatus fromString(String status) {
			for (EasebuzzInstacollectPaymentStatus value : EasebuzzInstacollectPaymentStatus.values()) {
				if (value.toString().equals(status)) {
					return value;
				}
			}
			return EasebuzzInstacollectPaymentStatus.failure;
		}
	}
}
