package com.sorted.rest.services.payment.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.services.common.upload.csv.CSVMapping;
import com.sorted.rest.services.payment.constants.PaymentConstants.EntityType;
import com.sorted.rest.services.payment.constants.PaymentConstants.WalletType;
import com.sorted.rest.services.payment.constants.PaymentConstants.PaymentRequestStatus;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class PaymentRequestUploadBean implements Serializable, CSVMapping {

	private static final long serialVersionUID = 8376574842543219203L;

	@NotEmpty
	private String entityId;

	@NotEmpty
	private String txnType;

	@NotEmpty
	private String txnDetail;

	@NotNull
	private Double amount;

	private String remarks;

	private EntityType entityType;

	private WalletType walletType;

	private String txnMode;

	private PaymentRequestStatus status;

	private PrMetadata metadata = new PrMetadata();

	public static PaymentRequestUploadBean newInstance() {
		return new PaymentRequestUploadBean();
	}

	public String computedKey() {
		return getTxnDetail();
	}

	@SuppressWarnings("unchecked")
	@Override
	public PaymentRequestUploadBean newBean() {
		return newInstance();
	}

	@Override
	@JsonIgnore
	public String getHeaderMapping() {
		return "entityId:Entity Id,txnDetail:Txn Detail,txnType:Txn Type,amount:Amount,remarks:Remarks";
	}

	private List<ErrorBean> errors = new ArrayList<>();

}
