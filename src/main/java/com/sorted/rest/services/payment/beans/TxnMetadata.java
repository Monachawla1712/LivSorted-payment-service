package com.sorted.rest.services.payment.beans;

import lombok.Data;

import java.util.List;

@Data
public class TxnMetadata {

	private PaymentNotifyBean paymentNotification;

	private String displayOrderId;

	private List<Long> ccIds;

}
