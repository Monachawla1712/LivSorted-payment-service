package com.sorted.rest.services.payment.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class EasebuzzTransactionPayload implements Serializable {

	@JsonProperty("txnid")
	private String txnId;

	@JsonProperty("key")
	private String key;

	@JsonProperty("easepayid")
	private String easepayId;

	@JsonProperty("status")
	private String status;

	@JsonProperty("mode")
	private String mode;

	@JsonProperty("amount")
	private String amount;

	@JsonProperty("error")
	private String error;

	@JsonProperty("error_Message")
	private String errorMessage;

	@JsonProperty("hash")
	private String hash;

	@JsonProperty("productinfo")
	private String productInfo;

	@JsonProperty("firstname")
	private String firstname;

	@JsonProperty("phone")
	private String phone;

	@JsonProperty("email")
	private String email;

	@JsonProperty("surl")
	private String surl;

	@JsonProperty("furl")
	private String furl;

	@JsonProperty("unmappedstatus")
	private String unmappedStatus;

	@JsonProperty("cardCategory")
	private String cardCategory;

	@JsonProperty("addedon")
	private String addedOn;

	@JsonProperty("payment_source")
	private String paymentSource;

	@JsonProperty("PG_TYPE")
	private String pgType;

	@JsonProperty("bank_ref_num")
	private String bankRefNum;

	@JsonProperty("bankcode")
	private String bankCode;

	@JsonProperty("name_on_card")
	private String nameOnCard;

	@JsonProperty("upi_va")
	private String upiVa;

	@JsonProperty("cardnum")
	private String cardNum;

	@JsonProperty("issuing_bank")
	private String issuingBank;

	@JsonProperty("net_amount_debit")
	private String netAmountDebit;

	@JsonProperty("cash_back_percentage")
	private String cashbackPercentage;

	@JsonProperty("deduction_percentage")
	private String deductionPercentage;

	@JsonProperty("merchant_logo")
	private String merchantLogo;

	@JsonProperty("card_type")
	private String cardType;

	@JsonProperty("bank_name")
	private String bankName;

	private final static long serialVersionUID = 5665360845514107040L;

}
