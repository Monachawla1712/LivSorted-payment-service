package com.sorted.rest.services.payment.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class EasebuzzCreateVABean implements Serializable {

	private static final long serialVersionUID = 8453503875733187507L;

	@JsonProperty("success")
	private Boolean success;

	@JsonProperty("data")
	private EasebuzzCreateVAPayload data;

	@Data
	public static class EasebuzzCreateVAPayload implements Serializable {

		private final static long serialVersionUID = 5665360845514107041L;

		@JsonProperty("virtual_account")
		private EasebuzzVAPayload va;
	}

	@Data
	public static class EasebuzzVAPayload implements Serializable {

		@JsonProperty("id")
		public String id;

		@JsonProperty("authorized_remitters")
		public List<Object> authorizedRemitters;

		@JsonProperty("account_number")
		public String accountNumber;

		@JsonProperty("balance")
		public String balance;

		@JsonProperty("balance_amount")
		public String balanceAmount;

		@JsonProperty("phone_numbers")
		public List<Object> phoneNumbers;

		@JsonProperty("service_charge")
		public String serviceCharge;

		@JsonProperty("service_charge_with_gst")
		public String serviceChargeWithGst;

		@JsonProperty("gst_amount")
		public String gstAmount;

		@JsonProperty("kyc_signed_url")
		public Object kycSignedUrl;

		@JsonProperty("token")
		public Object token;

		@JsonProperty("created_at")
		public String createdAt;

		@JsonProperty("created_by_id")
		public Object createdById;

		@JsonProperty("label")
		public String label;

		@JsonProperty("virtual_account_number")
		public String virtualAccountNumber;

		@JsonProperty("virtual_ifsc_number")
		public String virtualIfscCode;

		@JsonProperty("virtual_upi_handle")
		public String virtualUpiHandle;

		@JsonProperty("description")
		public String description;

		@JsonProperty("is_active")
		public Boolean isActive;

		@JsonProperty("auto_deactivate_at")
		public Object autoDeactivateAt;

		@JsonProperty("upi_qrcode_remote_file_location")
		public String upiQrcodeRemoteFileLocation;

		@JsonProperty("upi_qrcode_scanner_remote_file_location")
		public String upiQrcodeScannerRemoteFileLocation;

		@JsonProperty("account_type")
		public String accountType;

		@JsonProperty("kyc_flow")
		public Boolean kycFlow;

		@JsonProperty("notification_settings")
		public Object notificationSettings;

		private final static long serialVersionUID = 5665360845514107040L;

	}
}
