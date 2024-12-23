package com.sorted.rest.services.payment.beans;

import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.payment.constants.PaymentConstants.PaymentRequestStatus;
import lombok.Data;

@Data
public class PaymentRequestResponse extends BaseEntity {

	private static final long serialVersionUID = -7538803140039235801L;

	private Long id;

	private String entityType;

	private String entityId;

	private PaymentRequestStatus status;

	private String txnMode;

	private String txnType;

	private String txnDetail;

	private String walletType;

	private Double Amount;

	private PrMetadata metadata = new PrMetadata();

	private String remarks;

	public static PaymentRequestResponse newInstance() {
		PaymentRequestResponse entity = new PaymentRequestResponse();
		return entity;
	}
}