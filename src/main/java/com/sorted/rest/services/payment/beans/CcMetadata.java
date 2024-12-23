package com.sorted.rest.services.payment.beans;

import com.sorted.rest.services.payment.constants.PaymentConstants;
import lombok.Data;

import java.util.List;

@Data
public class CcMetadata {

	private UserDetail requestedBy;

	private UserDetail collectedBy;

	private UserDetail receivedBy;

	private UserDetail approvedBy;

	private String remarks;

	private String txnMode;

	private List<String> images;

	private String storeId;

	private Boolean isFailedByHandPicked = Boolean.FALSE;

	private PaymentConstants.CashCollectionType ccType;

	public static CcMetadata newInstance(){
		return new CcMetadata();
	}

}
